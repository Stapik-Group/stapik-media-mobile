package pl.stapik.media.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import pl.stapik.common.document.BackupArchive
import pl.stapik.common.document.BackupContents
import pl.stapik.common.document.BackupFormatException
import pl.stapik.common.document.SafetyCopies
import pl.stapik.common.document.SafetyCopy
import pl.stapik.media.data.library.MediaLibrary
import pl.stapik.media.data.library.ReplaceReport
import pl.stapik.media.data.serialization.MediaPayload

class BackupService(
    private val library: MediaLibrary,
    private val safetyCopies: SafetyCopies,
    private val clock: Clock,
    private val appVersion: String,
) {
    class Exported(val bytes: ByteArray, val entries: Int, val unavailableYears: Set<Int>)

    class Opened(val payload: MediaPayload, val createdAt: Instant) {
        val entries: Int get() = payload.entries.size
    }

    suspend fun export(): Exported {
        val snapshot = library.snapshot()
        return Exported(archiveOf(snapshot.payload), snapshot.payload.entries.size, snapshot.unavailableYears)
    }

    fun open(bytes: ByteArray): Opened {
        val contents = try {
            BackupArchive.read(ByteArrayInputStream(bytes))
        } catch (e: BackupFormatException) {
            throw BackupException(BackupFailure.NOT_A_BACKUP, e)
        }
        if (contents.appId != APP_ID) throw BackupException(BackupFailure.WRONG_APPLICATION)
        val document = contents.document ?: throw BackupException(BackupFailure.DAMAGED)
        return Opened(BackupDocument.read(document), contents.createdAt)
    }

    suspend fun restore(backup: Opened): ReplaceReport {
        val previous = library.snapshot().payload
        try {
            safetyCopies.save(archiveOf(previous))
        } catch (e: IOException) {
            throw BackupException(BackupFailure.UNREADABLE, e)
        }
        return try {
            library.replaceAll(backup.payload)
        } catch (e: Throwable) {
            withContext(NonCancellable) { runCatching { library.replaceAll(previous) } }
            if (e is CancellationException) throw e
            throw BackupException(BackupFailure.UNREADABLE, e)
        }
    }

    suspend fun savedCopies(): List<SafetyCopy> = safetyCopies.list()

    suspend fun openSafetyCopy(copy: SafetyCopy): Opened =
        try {
            open(safetyCopies.read(copy.name))
        } catch (e: IOException) {
            throw BackupException(BackupFailure.UNREADABLE, e)
        }

    private fun archiveOf(payload: MediaPayload): ByteArray {
        val createdAt = clock.instant().truncatedTo(ChronoUnit.SECONDS)
        val contents = BackupContents(
            appId = APP_ID,
            createdAt = createdAt,
            document = BackupDocument.write(payload, createdAt.toString()),
            appVersion = appVersion,
        )
        return ByteArrayOutputStream().also { BackupArchive.write(it, contents) }.toByteArray()
    }

    private companion object {
        const val APP_ID = "stapikmedia"
    }
}
