package pl.stapik.media.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.SyncOutcome

class BackgroundOutcomeTest {
    private val offline = SyncOutcome.Offline(CloudError.Network("no route"))

    @Test
    fun anArchiveThatCouldNotBeReachedKeepsTheRetryAlive() {
        assertEquals(offline, BackgroundOutcome.pick(SyncOutcome.Synchronized, listOf(SyncOutcome.Synchronized, offline)))
    }

    @Test
    fun theMainOutcomeWinsWhenEverythingElseIsFine() {
        assertEquals(SyncOutcome.Synchronized, BackgroundOutcome.pick(SyncOutcome.Synchronized, listOf(SyncOutcome.Synchronized)))
    }

    @Test
    fun anOfflineMainDocumentStaysTheOutcome() {
        assertEquals(offline, BackgroundOutcome.pick(offline, listOf(SyncOutcome.Synchronized)))
    }

    @Test
    fun noArchivesMeansTheMainOutcome() {
        assertEquals(SyncOutcome.LocalOnly, BackgroundOutcome.pick(SyncOutcome.LocalOnly, emptyList()))
    }
}
