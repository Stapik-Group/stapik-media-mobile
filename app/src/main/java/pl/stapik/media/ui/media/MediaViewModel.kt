package pl.stapik.media.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.serialization.MediaPayload

class MediaViewModel(
    private val controller: DocumentController<MediaPayload>,
    private val localSnapshot: StateFlow<LocalSnapshot?>,
    private val archives: MediaArchives,
    private val configStorage: ApiConfigStorage,
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

    private val _filters = MutableStateFlow(mapOf<MediaCategory, CategoryFilter>())
    val filters: StateFlow<Map<MediaCategory, CategoryFilter>> = _filters.asStateFlow()

    init {
        viewModelScope.launch { archives.restoreLocal() }
        refresh()
    }

    fun refresh(isPullToRefresh: Boolean = false) = launchUpdate(isPullToRefresh) {
        controller.load()
        if (controller.document.value is DocumentUiState.Error) null else synchronize()
    }

    fun discardLocalAndResync() = launchUpdate(isPullToRefresh = false) { controller.discardLocalAndResync() }

    private fun launchUpdate(isPullToRefresh: Boolean, update: suspend () -> SyncOutcome?) {
        viewModelScope.launch {
            if (isPullToRefresh) {
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
            } catch (e: Exception) {
                baseState.value = MediaUiState.Error(e.toMediaLoadError())
            } finally {
                _isRefreshing.value = false
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

    private fun stateAfter(outcome: SyncOutcome?): MediaUiState = when (val document = controller.document.value) {
        DocumentUiState.Loading -> MediaUiState.Loading
        is DocumentUiState.Error -> MediaUiState.LocalDataError(document.kind)
        is DocumentUiState.Success -> {
            val failure = outcome?.toMediaLoadError()
            val lastSynced = localSnapshot.value?.takeIf { it.everSynchronized }
            if (lastSynced == null) {
                MediaUiState.Error(failure ?: MediaLoadError.NotFound)
            } else {
                MediaUiState.Success(
                    entries = document.value.entries,
                    isStale = failure != null,
                    updatedAt = lastSynced.lastUpdate,
                    unreadableEntries = document.value.unreadableEntries,
                )
            }
        }
    }

    fun setYearFilter(category: MediaCategory, year: Int?) {
        val current = _filters.value[category] ?: CategoryFilter.None
        _filters.value += (category to current.copy(year = year, month = null))
        year?.let(::requestArchive)
    }

    fun requestArchive(year: Int) {
        viewModelScope.launch { archives.request(year) }
    }

    fun setMonthFilter(category: MediaCategory, month: Int?) {
        val current = _filters.value[category] ?: CategoryFilter.None
        _filters.value += (category to current.copy(month = month))
    }
}

data class CategoryFilter(val year: Int? = null, val month: Int? = null) {
    companion object {
        val None = CategoryFilter()
    }
}
