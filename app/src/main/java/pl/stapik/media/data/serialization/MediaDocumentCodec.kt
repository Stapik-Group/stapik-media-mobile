package pl.stapik.media.data.serialization

import kotlinx.serialization.Serializable
import pl.stapik.common.model.DocumentCodec
import pl.stapik.media.data.model.MediaEntry

@Serializable
data class MediaPayload(val entries: List<MediaEntry> = emptyList())

object MediaDocumentCodec {
    val codec: DocumentCodec<MediaPayload> = DocumentCodec(MediaPayload.serializer()) { it.entries.isEmpty() }
}
