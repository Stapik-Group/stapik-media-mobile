package pl.stapik.media.ui.media

import kotlinx.serialization.json.JsonNull
import org.junit.Assert.assertEquals
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

    @Test
    fun unreadableLocalDataWinsOverAnyFailure() {
        val state = mediaUiStateFor(DocumentUiState.Error(LoadErrorKind.CORRUPTED), synchronizedCopy, MediaLoadError.NoNetwork)

        assertEquals(MediaUiState.LocalDataError(LoadErrorKind.CORRUPTED), state)
    }

    @Test
    fun withoutASynchronisedCopyTheFailureIsShown() {
        val state = mediaUiStateFor(loaded, snapshot = null, failure = MediaLoadError.NoNetwork)

        assertEquals(MediaUiState.Error(MediaLoadError.NoNetwork), state)
    }

    @Test
    fun withoutASynchronisedCopyAndWithoutFailureTheCloudDocumentIsMissing() {
        val state = mediaUiStateFor(loaded, LocalSnapshot("", everSynchronized = false), failure = null)

        assertEquals(MediaUiState.Error(MediaLoadError.NotFound), state)
    }

    @Test
    fun aSuccessfulRefreshIsNotStale() {
        val state = mediaUiStateFor(loaded, synchronizedCopy, failure = null) as MediaUiState.Success

        assertEquals(false, state.isStale)
        assertEquals(2, state.unreadableEntries)
        assertNull(state.refreshError)
    }

    @Test
    fun aSynchronisedCopyStaysVisibleWhenTheRefreshFails() {
        val state = mediaUiStateFor(loaded, synchronizedCopy, MediaLoadError.NoNetwork) as MediaUiState.Success

        assertTrue(state.isStale)
        assertNull(state.refreshError)
    }

    @Test
    fun anAnnouncedFailureIsKeptNextToTheStoredCopy() {
        val failure = MediaLoadError.Unknown("No space left on device")

        val state = mediaUiStateFor(loaded, synchronizedCopy, failure, announceFailure = true) as MediaUiState.Success

        assertTrue(state.isStale)
        assertEquals(failure, state.refreshError)
    }
}
