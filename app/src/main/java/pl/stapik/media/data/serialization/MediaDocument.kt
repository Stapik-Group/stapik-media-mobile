package pl.stapik.media.data.serialization

import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.media.data.model.MediaEntry

data class MediaDocument(
    val lastUpdate: String,
    val entries: List<MediaEntry>,
) {
    companion object {
        fun parse(content: String): MediaDocument {
            val envelope = DocumentEnvelope.parse(content)
            val payload = MediaDocumentCodec.codec.decode(envelope)
            return MediaDocument(lastUpdate = envelope.lastUpdate, entries = payload.entries)
        }
    }
}
