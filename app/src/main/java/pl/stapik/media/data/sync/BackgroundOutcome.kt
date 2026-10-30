package pl.stapik.media.data.sync

import pl.stapik.common.sync.SyncOutcome
import pl.stapik.common.sync.SyncRetryPolicy

object BackgroundOutcome {
    fun pick(main: SyncOutcome, archives: List<SyncOutcome>): SyncOutcome =
        archives.firstOrNull(SyncRetryPolicy::isWorthRetrying)?.takeUnless { SyncRetryPolicy.isWorthRetrying(main) } ?: main
}
