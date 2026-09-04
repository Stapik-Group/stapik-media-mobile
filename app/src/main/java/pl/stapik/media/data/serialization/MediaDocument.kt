package pl.stapik.media.data.serialization

import pl.stapik.media.data.model.MediaEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

data class MediaDocument(
    val lastUpdate: String,
    val entries: List<MediaEntry>,
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(content: String): MediaDocument {
            val root = json.parseToJsonElement(content).jsonObject
            val payload = root.getValue("payload").jsonObject
            val entries = payload.getValue("entries").jsonArray.map {
                MediaEntrySerializer.fromJson(it.jsonObject)
            }
            return MediaDocument(
                lastUpdate = root.getValue("lastUpdate").toString(),
                entries = entries,
            )
        }
    }
}