package pl.stapik.media.ui.media

import pl.stapik.common.presentation.DocumentUiState
import pl.stapik.media.data.document.LocalSnapshot
import pl.stapik.media.data.serialization.MediaPayload

fun mediaUiStateFor(
    document: DocumentUiState<MediaPayload>,
    snapshot: LocalSnapshot?,
    failure: MediaLoadError?,
    connected: Boolean,
    announceFailure: Boolean = false,
): MediaUiState = when (document) {
    DocumentUiState.Loading -> MediaUiState.Loading
    is DocumentUiState.Error -> MediaUiState.LocalDataError(document.kind)
    is DocumentUiState.Success -> MediaUiState.Success(
        entries = document.value.entries,
        isStale = failure != null,
        updatedAt = snapshot?.lastUpdate.orEmpty(),
        unreadableEntries = document.value.unreadableEntries,
        refreshError = failure.takeIf { announceFailure },
        connected = connected,
    )
}
