package pl.stapik.media.data.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.MediaEntrySerializer

object MediaPayloadSerializer : KSerializer<MediaPayload> {
    private val entriesSerializer = ListSerializer(MediaEntrySerializer)
    private val json = Json { ignoreUnknownKeys = true }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MediaPayload") {
        element("entries", entriesSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: MediaPayload) {
        require(encoder is JsonEncoder) { "MediaPayload can only be serialized to JSON" }
        encoder.encodeJsonElement(
            buildJsonObject { put("entries", json.encodeToJsonElement(entriesSerializer, value.entries)) },
        )
    }

    override fun deserialize(decoder: Decoder): MediaPayload {
        require(decoder is JsonDecoder) { "MediaPayload can only be deserialized from JSON" }
        val items = decoder.decodeJsonElement().jsonObject["entries"]?.jsonArray ?: return MediaPayload()
        val readable = items.mapNotNull(::readEntry)
        return MediaPayload(entries = readable, unreadableEntries = items.size - readable.size)
    }

    private fun readEntry(item: JsonElement): MediaEntry? =
        try {
            json.decodeFromJsonElement(MediaEntrySerializer, item)
        } catch (e: Exception) {
            null
        }
}
