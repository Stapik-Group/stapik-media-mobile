package pl.stapik.media.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import pl.stapik.media.data.model.details.MediaDetails
import pl.stapik.media.data.serialization.JsonTrees

@Serializable(with = MediaEntrySerializer::class)
data class MediaEntry(
    val category: MediaCategory,
    val title: String,
    val releaseDate: ReleaseDate,
    val consumed: ConsumedDate,
    val details: MediaDetails,
    val id: String = "",
    val rating: Int? = null,
    val note: String = "",
    val extras: JsonObject = JsonObject(emptyMap()),
)

object EntryRating {
    const val MIN = 1
    const val MAX = 10
}

object MediaEntrySerializer : KSerializer<MediaEntry> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MediaEntry")

    private val json = Json { ignoreUnknownKeys = true }

    override fun serialize(encoder: Encoder, value: MediaEntry) {
        require(encoder is JsonEncoder) { "MediaEntry can only be serialized to JSON" }
        encoder.encodeJsonElement(JsonTrees.mergeMissing(knownJson(value), value.extras))
    }

    private fun knownJson(value: MediaEntry): JsonObject = buildJsonObject {
        put("category", json.encodeToJsonElement(value.category))
        put("title", value.title)
        put("releaseDate", json.encodeToJsonElement(value.releaseDate))
        put("consumed", json.encodeToJsonElement(value.consumed))
        put("details", detailsToJson(value.details))
        if (value.id.isNotEmpty()) put("id", value.id)
        value.rating?.let { put("rating", it) }
        if (value.note.isNotEmpty()) put("note", value.note)
    }

    override fun deserialize(decoder: Decoder): MediaEntry {
        require(decoder is JsonDecoder) { "MediaEntry can only be deserialized from JSON" }
        val obj = decoder.decodeJsonElement().jsonObject
        val category = json.decodeFromJsonElement<MediaCategory>(obj.getValue("category"))
        val entry = MediaEntry(
            category = category,
            title = json.decodeFromJsonElement<String>(obj.getValue("title")),
            releaseDate = json.decodeFromJsonElement<ReleaseDate>(obj.getValue("releaseDate")),
            consumed = json.decodeFromJsonElement<ConsumedDate>(obj.getValue("consumed")),
            details = detailsFromJson(category, obj.getValue("details").jsonObject),
            id = (obj["id"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            rating = ratingFrom(obj["rating"]),
            note = noteFrom(obj["note"]),
        )
        return entry.copy(extras = JsonTrees.unknownParts(obj, knownJson(entry)))
    }

    private fun ratingFrom(element: JsonElement?): Int? {
        if (element == null || element is JsonNull) return null
        val rating = element.jsonPrimitive.int
        require(rating in EntryRating.MIN..EntryRating.MAX) { "Rating out of range: $rating" }
        return rating
    }

    private fun noteFrom(element: JsonElement?): String {
        if (element == null || element is JsonNull) return ""
        val primitive = element.jsonPrimitive
        require(primitive.isString) { "The note is not a string" }
        return primitive.content
    }

    private fun detailsToJson(details: MediaDetails): JsonObject = when (details) {
        is MediaDetails.Screen -> json.encodeToJsonElement(details).jsonObject
        is MediaDetails.Book -> json.encodeToJsonElement(details).jsonObject
        is MediaDetails.Album -> json.encodeToJsonElement(details).jsonObject
        is MediaDetails.Game -> json.encodeToJsonElement(details).jsonObject
    }

    private fun detailsFromJson(category: MediaCategory, obj: JsonObject): MediaDetails = when (category) {
        MediaCategory.MOVIE, MediaCategory.SERIES, MediaCategory.CARTOON ->
            json.decodeFromJsonElement<MediaDetails.Screen>(obj)

        MediaCategory.BOOK -> json.decodeFromJsonElement<MediaDetails.Book>(obj)
        MediaCategory.ALBUM -> json.decodeFromJsonElement<MediaDetails.Album>(obj)
        MediaCategory.GAME -> json.decodeFromJsonElement<MediaDetails.Game>(obj)
    }
}