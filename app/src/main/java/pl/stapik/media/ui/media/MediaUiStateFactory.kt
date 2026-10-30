package pl.stapik.media.ui.media

import pl.stapik.common.presentation.DocumentUiState
import pl.stapik.media.data.document.LocalSnapshot
import pl.stapik.media.data.serialization.MediaPayload

fun mediaUiStateFor(
    document: DocumentUiState<MediaPayload>,
    snapshot: LocalSnapshot?,
    failure: MediaLoadError?,
    announceFailure: Boolean = false,
): MediaUiState = when (document) {
    DocumentUiState.Loading -> MediaUiState.Loading
    is DocumentUiState.Error -> MediaUiState.LocalDataError(document.kind)
    is DocumentUiState.Success -> {
        val lastSynced = snapshot?.takeIf { it.everSynchronized }
        if (lastSynced == null) {
            MediaUiState.Error(failure ?: MediaLoadError.NotFound)
        } else {
            MediaUiState.Success(
                entries = document.value.entries,
                isStale = failure != null,
                updatedAt = lastSynced.lastUpdate,
                unreadableEntries = document.value.unreadableEntries,
                refreshError = failure.takeIf { announceFailure },
            )
        }
    }
}
