package pl.stapik.media.ui.media

import pl.stapik.common.presentation.SyncAction
import pl.stapik.common.presentation.SyncBadge
import pl.stapik.common.presentation.SyncUiState

private val readOnlyMessageKeys = mapOf(
    "sync.offline.network" to "media.sync.offline_network",
    "sync.offline.server" to "media.sync.offline_server",
    "sync.read_only" to "media.sync.cloud_document_missing",
)

fun SyncUiState.forReadOnlyApp(): SyncUiState {
    val key = readOnlyMessageKeys[messageKey] ?: return this
    return copy(messageKey = key, actions = if (badge == SyncBadge.READ_ONLY) setOf(SyncAction.RETRY) else actions)
}
