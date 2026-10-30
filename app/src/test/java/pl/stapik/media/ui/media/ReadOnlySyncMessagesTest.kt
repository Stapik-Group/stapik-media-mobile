package pl.stapik.media.ui.media

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.stapik.common.presentation.SyncAction
import pl.stapik.common.presentation.SyncBadge
import pl.stapik.common.presentation.SyncUiState

class ReadOnlySyncMessagesTest {

    @Test
    fun offlineMessagesDoNotPromiseToSendChanges() {
        val state = SyncUiState(SyncBadge.OFFLINE, "sync.offline.network", setOf(SyncAction.RETRY))

        val mapped = state.forReadOnlyApp()

        assertEquals("media.sync.offline_network", mapped.messageKey)
        assertEquals(setOf(SyncAction.RETRY), mapped.actions)
    }

    @Test
    fun aMissingCloudDocumentOffersRetryInsteadOfReconnecting() {
        val state = SyncUiState(SyncBadge.READ_ONLY, "sync.read_only", setOf(SyncAction.RECONNECT))

        val mapped = state.forReadOnlyApp()

        assertEquals("media.sync.cloud_document_missing", mapped.messageKey)
        assertEquals(setOf(SyncAction.RETRY), mapped.actions)
    }

    @Test
    fun otherMessagesStayAsTheLibraryWroteThem() {
        val state = SyncUiState(SyncBadge.AUTH, "sync.auth.unauthorized", setOf(SyncAction.RECONNECT))

        assertEquals(state, state.forReadOnlyApp())
    }
}
