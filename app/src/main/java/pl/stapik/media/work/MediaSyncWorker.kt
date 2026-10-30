package pl.stapik.media.work

import android.content.Context
import androidx.work.WorkerParameters
import pl.stapik.common.sync.SyncOutcome
import pl.stapik.common.work.SyncWorker
import pl.stapik.media.MediaApp

class MediaSyncWorker(context: Context, params: WorkerParameters) : SyncWorker(context, params) {
    override suspend fun runSync(): SyncOutcome = (applicationContext as MediaApp).container.synchronizeInBackground()
}
