package pl.stapik.media.ui.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshThrottleTest {
    private var now = 1_000L
    private val throttle = RefreshThrottle(minIntervalMillis = 60_000L) { now }

    @Test
    fun theFirstRefreshIsAlwaysDue() {
        assertTrue(throttle.isDue())
    }

    @Test
    fun aRefreshRightAfterAnotherIsNotDue() {
        throttle.markRefreshed()
        now += 59_999L

        assertFalse(throttle.isDue())
    }

    @Test
    fun aRefreshIsDueOnceTheIntervalHasPassed() {
        throttle.markRefreshed()
        now += 60_000L

        assertTrue(throttle.isDue())
    }

    @Test
    fun markingRestartsTheInterval() {
        throttle.markRefreshed()
        now += 70_000L
        throttle.markRefreshed()
        now += 10_000L

        assertFalse(throttle.isDue())
    }
}
