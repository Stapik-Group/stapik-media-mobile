package pl.stapik.media.ui.media

import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stapik.common.presentation.DocumentUiState
import pl.stapik.common.presentation.LoadErrorKind
import pl.stapik.media.data.document.LocalSnapshot
import pl.stapik.media.data.serialization.MediaPayload

class MediaUiStateFactoryTest {
    private val synchronizedCopy = LocalSnapshot(lastUpdate = "2026-10-30T10:00:00Z", everSynchronized = true)
    private val loaded = DocumentUiState.Success(MediaPayload(unreadable = listOf(JsonNull, JsonNull)))
    private val empty = DocumentUiState.Success(MediaPayload())

    @Test
    fun unreadableLocalDataWinsOverAnyFailure() {
        val state = mediaUiStateFor(DocumentUiState.Error(LoadErrorKind.CORRUPTED), synchronizedCopy, MediaLoadError.NoNetwork, connected = true)

        assertEquals(MediaUiState.LocalDataError(LoadErrorKind.CORRUPTED), state)
    }

    @Test
    fun anEmptyLocalListIsShownWithoutAConnection() {
        val state = mediaUiStateFor(empty, snapshot = null, failure = null, connected = false) as MediaUiState.Success

        assertTrue(state.entries.isEmpty())
        assertFalse(state.connected)
        assertFalse(state.isStale)
    }

    @Test
    fun anEmptyListIsShownEvenWhenTheFirstSynchronisationFails() {
        val state = mediaUiStateFor(empty, snapshot = null, failure = MediaLoadError.NoNetwork, connected = true) as MediaUiState.Success

        assertTrue(state.isStale)
        assertEquals("", state.updatedAt)
    }

    @Test
    fun dataThatWasNeverSynchronisedIsStillShown() {
        val neverSynchronised = LocalSnapshot(lastUpdate = "2026-10-30T09:00:00Z", everSynchronized = false)

        val state = mediaUiStateFor(loaded, neverSynchronised, failure = null, connected = true) as MediaUiState.Success

        assertFalse(state.isStale)
        assertEquals("2026-10-30T09:00:00Z", state.updatedAt)
    }

    @Test
    fun aReadOnlyKeyTurnsEditingOff() {
        val readOnly = synchronizedCopy.copy(canWrite = false)

        assertFalse((mediaUiStateFor(loaded, readOnly, failure = null, connected = true) as MediaUiState.Success).canEdit)
        assertTrue((mediaUiStateFor(loaded, synchronizedCopy, failure = null, connected = true) as MediaUiState.Success).canEdit)
        assertTrue((mediaUiStateFor(empty, snapshot = null, failure = null, connected = false) as MediaUiState.Success).canEdit)
    }

    @Test
    fun aSuccessfulRefreshIsNotStale() {
        val state = mediaUiStateFor(loaded, synchronizedCopy, failure = null, connected = true) as MediaUiState.Success

        assertFalse(state.isStale)
        assertEquals(2, state.unreadableEntries)
        assertNull(state.refreshError)
    }

    @Test
    fun aSynchronisedCopyStaysVisibleWhenTheRefreshFails() {
        val state = mediaUiStateFor(loaded, synchronizedCopy, MediaLoadError.NoNetwork, connected = true) as MediaUiState.Success

        assertTrue(state.isStale)
        assertNull(state.refreshError)
    }

    @Test
    fun anAnnouncedFailureIsKeptNextToTheStoredCopy() {
        val failure = MediaLoadError.Unknown("No space left on device")

        val state = mediaUiStateFor(loaded, synchronizedCopy, failure, connected = true, announceFailure = true) as MediaUiState.Success

        assertTrue(state.isStale)
        assertEquals(failure, state.refreshError)
    }
}
