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
import kotlinx.coroutines.flow.map
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
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.serialization.MediaPayload

class MediaViewModel(
    private val controller: DocumentController<MediaPayload>,
    private val localSnapshot: StateFlow<LocalSnapshot?>,
    private val archives: MediaArchives,
    private val configStorage: ApiConfigStorage,
    private val resumeThrottle: RefreshThrottle,
    private val sortStorage: SortPreferenceStorage,
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
        .map { it.forReadOnlyApp() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, controller.sync.value.forReadOnlyApp())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _sort = MutableStateFlow(EntrySort())
    val sort: StateFlow<EntrySort> = _sort.asStateFlow()

    private val _filters = MutableStateFlow(mapOf<MediaCategory, CategoryFilter>())
    val filters: StateFlow<Map<MediaCategory, CategoryFilter>> = _filters.asStateFlow()

    private var updateJob: Job? = null

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
        val state = mediaUiStateFor(controller.document.value, localSnapshot.value, failure = null)
        if (state is MediaUiState.Success) {
            baseState.value = state
            _isRefreshing.value = true
        }
    }

    private fun launchUpdate(keepView: Boolean, update: suspend () -> SyncOutcome?) {
        val previous = updateJob
        updateJob = viewModelScope.launch {
            previous?.cancelAndJoin()
            if (keepView) {
                _isRefreshing.value = true
            } else {
                baseState.value = MediaUiState.Loading
            }

            try {
                if (configStorage.load() == null) {
                    baseState.value = MediaUiState.NotConnected
                    return@launch
                }

                val state = stateAfter(update())
                if (state is MediaUiState.Success) archives.refreshYears()
                baseState.value = state
                if (state is MediaUiState.Success) archives.syncLoaded()
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

    private suspend fun synchronize(): SyncOutcome {
        val outcome = controller.sync()
        return if (outcome is SyncOutcome.NeedsFirstConnectionChoice) {
            controller.sync(FirstConnectionChoice.USE_CLOUD)
        } else {
            outcome
        }
    }

    private fun stateAfter(outcome: SyncOutcome?): MediaUiState =
        mediaUiStateFor(controller.document.value, localSnapshot.value, outcome?.toMediaLoadError())

    private suspend fun stateAfterFailure(error: Exception): MediaUiState {
        controller.load()
        return mediaUiStateFor(controller.document.value, localSnapshot.value, error.toMediaLoadError(), announceFailure = true)
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
