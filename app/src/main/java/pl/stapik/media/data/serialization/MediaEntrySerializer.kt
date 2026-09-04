package pl.stapik.media.data.serialization

import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.LocalDate

/**
 * Field-for-field mirror of the desktop app's `MediaEntrySerializer.cpp`.
 * Any change to the JSON shape on the desktop side must be mirrored here.
 */
object MediaEntrySerializer {

    fun toJson(entry: MediaEntry): JsonObject = buildJsonObject {
        put("category", entry.category.wireName)
        put("title", entry.title)
        put("releaseDate", buildJsonObject {
            put("date", entry.releaseDate.date.toString()) // ISO-8601 yyyy-MM-dd
            put("precision", entry.releaseDate.precision.wireName)
        })
        put("consumed", buildJsonObject {
            put("month", entry.consumed.month)
            put("year", entry.consumed.year)
        })
        put("details", detailsToJson(entry.details))
    }

    fun fromJson(json: JsonObject): MediaEntry {
        val category = MediaCategory.fromWireName(json.getValue("category").jsonPrimitive.content)
        val rd = json.getValue("releaseDate").jsonObject
        val precision = DatePrecision.fromWireName(rd.getValue("precision").jsonPrimitive.content)

        return MediaEntry(
            category = category,
            title = json.getValue("title").jsonPrimitive.content,
            releaseDate = ReleaseDate(
                date = LocalDate.parse(rd.getValue("date").jsonPrimitive.content),
                precision = precision,
            ),
            consumed = json.getValue("consumed").jsonObject.let {
                ConsumedDate(
                    month = it.getValue("month").jsonPrimitive.content.toInt(),
                    year = it.getValue("year").jsonPrimitive.content.toInt(),
                )
            },
            details = detailsFromJson(category, json.getValue("details").jsonObject),
        )
    }

    private fun detailsToJson(details: MediaDetails): JsonObject = when (details) {
        is MediaDetails.Screen -> buildJsonObject {
            put("director", details.director)
            put("genre", details.genre)
        }

        is MediaDetails.Book -> buildJsonObject {
            put("author", details.author)
            put("genre", details.genre)
            put("isAudiobook", details.isAudiobook)
        }

        is MediaDetails.Album -> buildJsonObject {
            put("performer", details.performer)
            put("publisher", details.publisher)
            put("genre", details.genre)
        }

        is MediaDetails.Game -> buildJsonObject {
            put("studio", details.studio)
            put("publisher", details.publisher)
            put("platform", details.platform)
        }
    }

    private fun detailsFromJson(category: MediaCategory, json: JsonObject): MediaDetails =
        when (category) {
            MediaCategory.MOVIE, MediaCategory.SERIES, MediaCategory.CARTOON -> MediaDetails.Screen(
                director = json.getValue("director").jsonPrimitive.content,
                genre = json.getValue("genre").jsonPrimitive.content,
            )

            MediaCategory.BOOK -> MediaDetails.Book(
                author = json.getValue("author").jsonPrimitive.content,
                genre = json.getValue("genre").jsonPrimitive.content,
                isAudiobook = json.getValue("isAudiobook").jsonPrimitive.boolean,
            )

            MediaCategory.ALBUM -> MediaDetails.Album(
                performer = json.getValue("performer").jsonPrimitive.content,
                publisher = json.getValue("publisher").jsonPrimitive.content,
                genre = json.getValue("genre").jsonPrimitive.content,
            )

            MediaCategory.GAME -> MediaDetails.Game(
                studio = json.getValue("studio").jsonPrimitive.content,
                publisher = json.getValue("publisher").jsonPrimitive.content,
                platform = json.getValue("platform").jsonPrimitive.content,
            )
        }
}
