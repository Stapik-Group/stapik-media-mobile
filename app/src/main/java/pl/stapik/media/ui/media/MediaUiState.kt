package pl.stapik.media.ui.media

import pl.stapik.media.data.model.MediaEntry

sealed interface MediaUiState {
    data object Loading : MediaUiState
    data object NotConnected : MediaUiState

    data class Success(
        val entries: List<MediaEntry>,
        val isStale: Boolean,
        val updatedAt: String,
    ) : MediaUiState

    data class Error(val error: MediaLoadError) : MediaUiState
}