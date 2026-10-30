package pl.stapik.media.data.serialization

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import pl.stapik.common.model.DocumentCodec
import pl.stapik.media.data.model.MediaEntry

@Serializable(with = MediaPayloadSerializer::class)
data class MediaPayload(
    val entries: List<MediaEntry> = emptyList(),
    val unreadable: List<JsonElement> = emptyList(),
) {
    val unreadableEntries: Int get() = unreadable.size
}

object MediaDocumentCodec {
    val codec: DocumentCodec<MediaPayload> = DocumentCodec(MediaPayload.serializer()) { it.entries.isEmpty() && it.unreadable.isEmpty() }
}
