package pl.stapik.media.data.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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
        val written = json.encodeToJsonElement(entriesSerializer, value.entries).jsonArray
        encoder.encodeJsonElement(buildJsonObject { put("entries", JsonArray(written + value.unreadable)) })
    }

    override fun deserialize(decoder: Decoder): MediaPayload {
        require(decoder is JsonDecoder) { "MediaPayload can only be deserialized from JSON" }
        val items = decoder.decodeJsonElement().jsonObject["entries"]?.jsonArray ?: return MediaPayload()
        val readable = ArrayList<MediaEntry>(items.size)
        val unreadable = ArrayList<JsonElement>()
        for (item in items) {
            val entry = readEntry(item)
            if (entry != null) readable.add(entry) else unreadable.add(item)
        }
        return MediaPayload(entries = readable, unreadable = unreadable)
    }

    private fun readEntry(item: JsonElement): MediaEntry? =
        try {
            json.decodeFromJsonElement(MediaEntrySerializer, item)
        } catch (e: Exception) {
            null
        }
}
