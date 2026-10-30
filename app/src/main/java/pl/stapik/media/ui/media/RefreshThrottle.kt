package pl.stapik.media.ui.media

class RefreshThrottle(
    private val minIntervalMillis: Long,
    private val uptimeMillis: () -> Long,
) {
    private var lastRefreshAt: Long? = null

    fun isDue(): Boolean = lastRefreshAt?.let { uptimeMillis() - it >= minIntervalMillis } ?: true

    fun markRefreshed() {
        lastRefreshAt = uptimeMillis()
    }
}
