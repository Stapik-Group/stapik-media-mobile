package pl.stapik.media.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pl.stapik.common.presentation.DocumentController
import pl.stapik.common.presentation.DocumentUiState
import pl.stapik.common.presentation.SyncUiState
import pl.stapik.common.sync.FirstConnectionChoice
import pl.stapik.common.sync.SyncOutcome
import pl.stapik.media.data.archive.MediaArchives
import pl.stapik.media.data.config.ApiConfigStorage
import pl.stapik.media.data.document.LocalSnapshot
import pl.stapik.media.data.library.MediaLibrary
import pl.stapik.media.data.library.WriteResult
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.serialization.MediaPayload
import pl.stapik.media.ui.backup.BackupController

class MediaViewModel(
    private val controller: DocumentController<MediaPayload>,
    private val localSnapshot: StateFlow<LocalSnapshot?>,
    private val archives: MediaArchives,
    private val library: MediaLibrary,
    private val configStorage: ApiConfigStorage,
    private val resumeThrottle: RefreshThrottle,
    private val sortStorage: SortPreferenceStorage,
    val backup: BackupController,
) : ViewModel() {

    private val baseState = MutableStateFlow<MediaUiState>(MediaUiState.Loading)

    val uiState: StateFlow<MediaUiState> = combine(baseState, archives.documents, archives.years, archives.status) { state, archived, years, status ->
        if (state is MediaUiState.Success) {
            state.copy(
                entries = state.entries + archived.values.flatMap { it.entries },
                unreadableEntries = state.unreadableEntries + archived.values.sumOf { it.unreadableEntries },
                archiveYears = years,
                archiveStatus = status,
            )
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MediaUiState.Loading)

    val syncState: StateFlow<SyncUiState> = controller.sync

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _sort = MutableStateFlow(EntrySort())
    val sort: StateFlow<EntrySort> = _sort.asStateFlow()

    private val _filters = MutableStateFlow(mapOf<MediaCategory, CategoryFilter>())
    val filters: StateFlow<Map<MediaCategory, CategoryFilter>> = _filters.asStateFlow()

    private val _pendingChoice = MutableStateFlow<SyncOutcome.NeedsFirstConnectionChoice?>(null)
    val pendingChoice: StateFlow<SyncOutcome.NeedsFirstConnectionChoice?> = _pendingChoice.asStateFlow()

    private var updateJob: Job? = null
    private var connected = false

    init {
        viewModelScope.launch { _sort.value = sortStorage.load() }
        viewModelScope.launch { archives.restoreLocal() }
        refresh()
    }

    fun refresh(isPullToRefresh: Boolean = false) {
        if (updateJob?.isActive == true) return
        launchUpdate(keepView = isPullToRefresh || baseState.value is MediaUiState.Success) { synchronizeStoredCopy() }
    }

    fun reload() = launchUpdate(keepView = false) { synchronizeStoredCopy() }

    fun refreshOnForeground() {
        if (resumeThrottle.isDue()) refresh(isPullToRefresh = true)
    }

    fun discardLocalAndResync() = launchUpdate(keepView = false) { controller.discardLocalAndResync() }

    private suspend fun synchronizeStoredCopy(): SyncOutcome? {
        controller.load()
        showStoredCopy()
        return if (controller.document.value is DocumentUiState.Error) null else synchronize()
    }

    private fun showStoredCopy() {
        if (baseState.value is MediaUiState.Success) return
        val state = mediaUiStateFor(controller.document.value, localSnapshot.value, failure = null, connected = connected)
        if (state is MediaUiState.Success) {
            baseState.value = state
            _isRefreshing.value = true
        }
    }

    private fun launchUpdate(keepView: Boolean, choice: FirstConnectionChoice? = null, update: suspend () -> SyncOutcome?) {
        val previous = updateJob
        updateJob = viewModelScope.launch {
            previous?.cancelAndJoin()
            if (keepView) {
                _isRefreshing.value = true
            } else {
                baseState.value = MediaUiState.Loading
            }

            try {
                connected = configStorage.load() != null
                val state = stateAfter(update())
                if (state is MediaUiState.Success) archives.refreshYears()
                baseState.value = state
                if (state is MediaUiState.Success) promptForUnresolved(archives.syncLoaded(choice))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                baseState.value = stateAfterFailure(e)
            } finally {
                _isRefreshing.value = false
                resumeThrottle.markRefreshed()
            }
        }
    }

    suspend fun addEntry(entry: MediaEntry): WriteResult = written { library.add(entry) }

    suspend fun updateEntry(id: String, entry: MediaEntry): WriteResult = written { library.update(id, entry) }

    suspend fun deleteEntry(id: String): WriteResult = written { library.delete(id) }

    private suspend fun written(write: suspend () -> WriteResult): WriteResult {
        val result = write()
        val previous = baseState.value
        val fresh = mediaUiStateFor(controller.document.value, localSnapshot.value, failure = null, connected = connected)
        baseState.value = if (fresh is MediaUiState.Success && previous is MediaUiState.Success) {
            fresh.copy(isStale = previous.isStale, refreshError = previous.refreshError)
        } else {
            fresh
        }
        return result
    }

    fun chooseFirstConnection(choice: FirstConnectionChoice) {
        _pendingChoice.value = null
        launchUpdate(keepView = true, choice = choice) { controller.sync(choice) }
    }

    private fun promptForUnresolved(outcomes: List<SyncOutcome>) {
        val prompt = outcomes.filterIsInstance<SyncOutcome.NeedsFirstConnectionChoice>().firstOrNull() ?: return
        if (_pendingChoice.value == null) _pendingChoice.value = prompt
    }

    fun flushPendingSync() {
        viewModelScope.launch { controller.flushPendingSync() }
    }

    fun dismissNotice() {
        viewModelScope.launch { controller.dismissResolvedNotice() }
    }

    fun cancelFirstConnection() {
        _pendingChoice.value = null
    }

    private suspend fun synchronize(): SyncOutcome {
        _pendingChoice.value = null
        val outcome = controller.sync()
        if (outcome is SyncOutcome.NeedsFirstConnectionChoice) _pendingChoice.value = outcome
        return outcome
    }

    private fun stateAfter(outcome: SyncOutcome?): MediaUiState =
        mediaUiStateFor(controller.document.value, localSnapshot.value, outcome?.toMediaLoadError(), connected)

    private suspend fun stateAfterFailure(error: Exception): MediaUiState {
        controller.load()
        return mediaUiStateFor(
            controller.document.value,
            localSnapshot.value,
            error.toMediaLoadError(),
            connected,
            announceFailure = true,
        )
    }

    fun setYearFilter(category: MediaCategory, year: Int?) {
        val current = _filters.value[category] ?: CategoryFilter.None
        _filters.value += (category to current.copy(year = year, month = null))
        year?.let(::requestArchive)
    }

    fun requestArchive(year: Int) {
        viewModelScope.launch { archives.request(year) }
    }

    fun setQueryFilter(category: MediaCategory, query: String) {
        val current = _filters.value[category] ?: CategoryFilter.None
        _filters.value += (category to current.copy(query = query))
    }

    fun setSort(sort: EntrySort) {
        _sort.value = sort
        viewModelScope.launch { sortStorage.save(sort) }
    }

    fun setMonthFilter(category: MediaCategory, month: Int?) {
        val current = _filters.value[category] ?: CategoryFilter.None
        _filters.value += (category to current.copy(month = month))
    }
}

data class CategoryFilter(val year: Int? = null, val month: Int? = null, val query: String = "") {
    companion object {
        val None = CategoryFilter()
    }
}
