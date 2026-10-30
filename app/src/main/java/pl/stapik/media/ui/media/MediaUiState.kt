package pl.stapik.media.ui.media

import pl.stapik.common.presentation.LoadErrorKind
import pl.stapik.media.data.archive.ArchiveStatus
import pl.stapik.media.data.model.MediaEntry

sealed interface MediaUiState {
    data object Loading : MediaUiState
    data object NotConnected : MediaUiState

    data class Success(
        val entries: List<MediaEntry>,
        val isStale: Boolean,
        val updatedAt: String,
        val archiveYears: Set<Int> = emptySet(),
        val archiveStatus: Map<Int, ArchiveStatus> = emptyMap(),
        val unreadableEntries: Int = 0,
    ) : MediaUiState

    data class Error(val error: MediaLoadError) : MediaUiState

    data class LocalDataError(val kind: LoadErrorKind) : MediaUiState
}