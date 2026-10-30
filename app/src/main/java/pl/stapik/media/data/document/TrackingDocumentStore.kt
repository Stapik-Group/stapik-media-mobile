package pl.stapik.media.data.document

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.common.model.LocalDocument
import pl.stapik.common.sync.ConflictCopy
import pl.stapik.common.sync.DocumentStore

data class LocalSnapshot(val lastUpdate: String, val everSynchronized: Boolean, val dirty: Boolean = false)

class TrackingDocumentStore(private val delegate: DocumentStore) : DocumentStore {
    private val mutableSnapshot = MutableStateFlow<LocalSnapshot?>(null)

    val snapshot: StateFlow<LocalSnapshot?> = mutableSnapshot.asStateFlow()

    override suspend fun load(): LocalDocument<DocumentEnvelope>? = delegate.load().also(::track)

    override suspend fun save(document: LocalDocument<DocumentEnvelope>) {
        delegate.save(document)
        track(document)
    }

    override suspend fun saveConflictCopy(copy: ConflictCopy) = delegate.saveConflictCopy(copy)

    override suspend fun discard() {
        delegate.discard()
        mutableSnapshot.value = null
    }

    private fun track(document: LocalDocument<DocumentEnvelope>?) {
        mutableSnapshot.value = document?.let { LocalSnapshot(it.content.lastUpdate, it.everSynchronized, it.dirty) }
    }
}
