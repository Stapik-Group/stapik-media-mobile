package pl.stapik.media.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.stapik.media.data.config.ApiConfigStorage
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.repository.MediaFetchOutcome
import pl.stapik.media.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaViewModel(
    private val repository: MediaRepository,
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
                val config = configStorage.load()
                if (config == null) {
                    _uiState.value = MediaUiState.NotConnected
                    return@launch
                }

                when (val outcome = repository.fetchEntries(config)) {
                    is MediaFetchOutcome.Fresh ->
                        _uiState.value = MediaUiState.Success(outcome.entries, isStale = false, outcome.updatedAt)

                    is MediaFetchOutcome.Cached ->
                        _uiState.value = MediaUiState.Success(outcome.entries, isStale = true, outcome.updatedAt)

                    is MediaFetchOutcome.Failure ->
                        _uiState.value = MediaUiState.Error(outcome.message)
                }
            } catch (e: Exception) {
                _uiState.value = MediaUiState.Error(e.message ?: e::class.simpleName ?: "Unknown error")
            } finally {
                _isRefreshing.value = false
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