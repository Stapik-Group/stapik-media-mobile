package pl.stapik.media.data.archive

import android.util.Log
import java.nio.file.Path
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import pl.stapik.common.document.DocumentCorruptedException
import pl.stapik.common.document.FileDocumentStore
import pl.stapik.common.document.PayloadCipher
import pl.stapik.common.model.SlotKey
import pl.stapik.common.sync.FirstConnectionChoice
import pl.stapik.common.sync.SyncEngine
import pl.stapik.common.sync.SyncOutcome
import pl.stapik.media.data.cloud.CloudGatewayProvider
import pl.stapik.media.data.document.TrackingDocumentStore
import pl.stapik.media.data.library.ArchiveDirectory
import pl.stapik.media.data.library.DocumentEditor
import pl.stapik.media.data.serialization.MediaDocumentCodec
import pl.stapik.media.data.serialization.MediaPayload

class MediaArchives(
    private val directory: Path,
    private val slot: SlotKey,
    private val cipher: PayloadCipher,
    private val gateways: CloudGatewayProvider,
    private val clock: Clock,
) : ArchiveDirectory {
    private class Archive(val engine: SyncEngine<MediaPayload>, val store: TrackingDocumentStore)

    private val archives = HashMap<Int, Archive>()
    private val storedFileName = Regex("${Regex.escape(slot.value)}@(\\d{4})\\.doc")
    private val mutableDocuments = MutableStateFlow<Map<Int, MediaPayload>>(emptyMap())
    private val mutableYears = MutableStateFlow<Set<Int>>(emptySet())
    private val mutableStatus = MutableStateFlow<Map<Int, ArchiveStatus>>(emptyMap())
    private var yearsKnown = false

    val documents: StateFlow<Map<Int, MediaPayload>> = mutableDocuments.asStateFlow()
    val years: StateFlow<Set<Int>> = mutableYears.asStateFlow()
    val status: StateFlow<Map<Int, ArchiveStatus>> = mutableStatus.asStateFlow()

    suspend fun restoreLocal() {
        storedYears().forEach { year -> guarded(year) { publish(year) } }
    }

    override fun loadedYears(): Set<Int> = mutableDocuments.value.keys

    override fun remoteYears(): Set<Int>? = mutableYears.value.takeIf { yearsKnown }

    override fun editorFor(year: Int): DocumentEditor = object : DocumentEditor {
        override suspend fun current(): MediaPayload = archiveFor(year).engine.load()

        override suspend fun edit(transform: (MediaPayload) -> MediaPayload) {
            archiveFor(year).engine.edit(transform)
            publish(year)
        }
    }

    suspend fun refreshYears() {
        val gateway = gateways.current()
        if (gateway == null) {
            yearsKnown = false
            return
        }
        val remote = try {
            gateway.partitions(slot).mapNotNull { ArchiveYears.yearOf(it.partition) }.toSet()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Cannot list the yearly archives", e)
            return
        }
        mutableYears.value = remote
        yearsKnown = true
        (storedYears() - remote).filter { isSettled(it) }.forEach { forget(it) }
    }

    suspend fun syncLoaded() {
        mutableDocuments.value.keys.forEach { sync(it) }
    }

    suspend fun request(year: Int) {
        if (year in mutableDocuments.value || year !in mutableYears.value) return
        mutableStatus.update { it + (year to ArchiveStatus.Loading) }
        val outcome = try {
            sync(year)
        } catch (e: CancellationException) {
            mutableStatus.update { it - year }
            throw e
        }
        mutableStatus.update { if (year in mutableDocuments.value) it - year else it + (year to ArchiveStatus.Failed(outcome)) }
    }

    private suspend fun sync(year: Int): SyncOutcome? {
        var result: SyncOutcome? = null
        guarded(year) { archive ->
            val outcome = archive.engine.sync()
            result = if (outcome is SyncOutcome.NeedsFirstConnectionChoice) {
                archive.engine.sync(FirstConnectionChoice.USE_CLOUD)
            } else {
                outcome
            }
            publish(year)
        }
        return result
    }

    private suspend fun publish(year: Int) {
        val archive = archiveFor(year)
        val payload = archive.engine.load()
        if (archive.store.snapshot.value?.everSynchronized == true || !MediaDocumentCodec.codec.isEmpty(payload)) {
            mutableDocuments.update { it + (year to payload) }
        } else {
            mutableDocuments.update { it - year }
        }
    }

    private suspend fun isSettled(year: Int): Boolean =
        try {
            val archive = archiveFor(year)
            archive.engine.load()
            val snapshot = archive.store.snapshot.value
            snapshot != null && snapshot.everSynchronized && !snapshot.dirty
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            true
        }

    private suspend fun guarded(year: Int, action: suspend (Archive) -> Unit) {
        try {
            action(archiveFor(year))
        } catch (e: CancellationException) {
            throw e
        } catch (e: DocumentCorruptedException) {
            Log.w(TAG, "The stored archive of $year is unreadable and will be downloaded again", e)
            forget(year)
        } catch (e: Exception) {
            Log.w(TAG, "The archive of $year could not be synchronised", e)
        }
    }

    private suspend fun forget(year: Int) {
        archiveFor(year).engine.discardLocalDocument()
        mutableDocuments.update { it - year }
    }

    private suspend fun storedYears(): Set<Int> = withContext(Dispatchers.IO) {
        directory.toFile().listFiles().orEmpty()
            .mapNotNull { storedFileName.matchEntire(it.name)?.groupValues?.get(1)?.toInt() }
            .toSet()
    }

    @Synchronized
    private fun archiveFor(year: Int): Archive = archives.getOrPut(year) {
        val partition = slot.inPartition(ArchiveYears.partitionKey(year))
        val store = TrackingDocumentStore(FileDocumentStore(directory = directory, slot = partition, cipher = cipher))
        Archive(SyncEngine(partition, MediaDocumentCodec.codec, store, gateways::current, clock), store)
    }

    private companion object {
        const val TAG = "MediaArchives"
    }
}
