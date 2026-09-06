package pl.stapik.media.data.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.stapik.media.data.model.MediaEntry

@Serializable
private data class MediaPayload(val entries: List<MediaEntry> = emptyList())

@Serializable
private data class MediaDocumentEnvelope(val lastUpdate: String, val payload: MediaPayload = MediaPayload())

data class MediaDocument(
    val lastUpdate: String,
    val entries: List<MediaEntry>,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(content: String): MediaDocument {
            val envelope = json.decodeFromString<MediaDocumentEnvelope>(content)
            return MediaDocument(lastUpdate = envelope.lastUpdate, entries = envelope.payload.entries)
        }
    }
}