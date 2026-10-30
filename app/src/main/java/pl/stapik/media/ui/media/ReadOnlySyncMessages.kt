package pl.stapik.media.ui.media

import pl.stapik.common.presentation.SyncUiState

private val readOnlyMessageKeys = mapOf(
    "sync.offline.network" to "media.sync.offline_network",
    "sync.offline.server" to "media.sync.offline_server",
)

fun SyncUiState.forReadOnlyApp(): SyncUiState {
    val key = readOnlyMessageKeys[messageKey] ?: return this
    return copy(messageKey = key)
}
