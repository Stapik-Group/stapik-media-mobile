package pl.stapik.media.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.stapik.common.presentation.DocumentController
import pl.stapik.common.presentation.DocumentUiState
import pl.stapik.common.sync.FirstConnectionChoice
import pl.stapik.common.sync.SyncOutcome
import pl.stapik.media.data.config.ApiConfigStorage
import pl.stapik.media.data.document.LocalSnapshot
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.serialization.MediaPayload

class MediaViewModel(
    private val controller: DocumentController<MediaPayload>,
    private val localSnapshot: StateFlow<LocalSnapshot?>,
    private val configStorage: ApiConfigStorage,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Loading)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _filters = MutableStateFlow(mapOf<MediaCategory, CategoryFilter>())
    val filters: StateFlow<Map<MediaCategory, CategoryFilter>> = _filters.asStateFlow()

    init {
        refresh()
    }

    fun refresh(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullToRefresh) {
                _isRefreshing.value = true
            } else {
                _uiState.value = MediaUiState.Loading
            }

            try {
                if (configStorage.load() == null) {
                    _uiState.value = MediaUiState.NotConnected
                    return@launch
                }

                controller.load()
                _uiState.value = stateAfter(synchronize())
            } catch (e: Exception) {
                _uiState.value = MediaUiState.Error(e.toMediaLoadError())
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

    private fun stateAfter(outcome: SyncOutcome): MediaUiState = when (val document = controller.document.value) {
        DocumentUiState.Loading -> MediaUiState.Loading
        is DocumentUiState.Error -> MediaUiState.Error(MediaLoadError.LocalUnreadable(document.kind.messageKey))
        is DocumentUiState.Success -> {
            val failure = outcome.toMediaLoadError()
            val lastSynced = localSnapshot.value?.takeIf { it.everSynchronized }
            if (lastSynced == null) {
                MediaUiState.Error(failure ?: MediaLoadError.NotFound)
            } else {
                MediaUiState.Success(document.value.entries, isStale = failure != null, updatedAt = lastSynced.lastUpdate)
            }
        }
    }

    fun setYearFilter(category: MediaCategory, year: Int?) {
        val current = _filters.value[category] ?: CategoryFilter.None
        _filters.value += (category to current.copy(year = year, month = null))
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
