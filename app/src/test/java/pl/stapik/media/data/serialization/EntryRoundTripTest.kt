package pl.stapik.media.data.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.stapik.common.model.DocumentEnvelope
import pl.stapik.media.data.model.MediaEntry

class EntryRoundTripTest {
    private val book = """
        {"category":"book","title":"Solaris",
         "releaseDate":{"date":"1961-01-01","precision":"year","calendar":"gregorian"},
         "consumed":{"month":5,"year":2023,"approximate":true},
         "details":{"author":"Lem","genre":"SF","isAudiobook":false,"isbn":"123"},
         "id":"b1","rating":9,"note":"Great","shelf":"A3"}
    """.trimIndent()

    @Test
    fun ratingAndNoteAreRead() {
        val entry = decode(book)

        assertEquals(9, entry.rating)
        assertEquals("Great", entry.note)
        assertEquals("b1", entry.id)
    }

    @Test
    fun unknownFieldsAtEveryLevelSurviveARoundTrip() {
        val original = Json.parseToJsonElement(book)

        assertEquals(original, encode(decode(book)))
    }

    @Test
    fun editedFieldsWinWhileUnknownOnesStay() {
        val edited = decode(book).copy(title = "Solaris (new edition)", rating = null, note = "")

        val written = encode(edited).jsonObject

        assertEquals(JsonPrimitive("Solaris (new edition)"), written["title"])
        assertNull(written["rating"])
        assertNull(written["note"])
        assertEquals(JsonPrimitive("A3"), written["shelf"])
        assertEquals(JsonPrimitive("123"), written["details"]?.jsonObject?.get("isbn"))
    }

    @Test
    fun nullAndEmptyOptionalFieldsAreWrittenBackAsTheyCame() {
        val original = Json.parseToJsonElement(book.replace("\"rating\":9,\"note\":\"Great\"", "\"rating\":null,\"note\":\"\""))

        assertEquals(original, encode(decode(original.toString())))
    }

    @Test
    fun aRatingOutsideTheScaleMakesTheEntryUnreadable() {
        val payload = payload(book.replace("\"rating\":9", "\"rating\":11"))

        assertEquals(0, payload.entries.size)
        assertEquals(1, payload.unreadableEntries)
    }

    @Test
    fun unreadableEntriesAreWrittenBackUntouchedAfterTheReadableOnes() {
        val unreadable = Json.parseToJsonElement("""{"category":"hologram","title":"Lost"}""")
        val loaded = payload(book, unreadable.toString())
        val previous = DocumentEnvelope.parse("""{"lastUpdate":"2026-01-01T00:00:00Z","payload":{"entries":[],"futureKey":1},"extra":2}""")

        val envelope = MediaDocumentCodec.codec.encode(loaded, previous, "2026-02-02T00:00:00Z")

        val written = envelope.payload["entries"]!!.jsonArray
        assertEquals(2, written.size)
        assertEquals(unreadable, written.last())
        assertEquals(JsonPrimitive(1), envelope.payload["futureKey"])
    }

    @Test
    fun aDocumentWithOnlyUnreadableEntriesIsNotEmpty() {
        val loaded = payload("""{"category":"hologram"}""")

        assertEquals(false, MediaDocumentCodec.codec.isEmpty(loaded))
    }

    private fun decode(text: String): MediaEntry = Json.decodeFromString(MediaEntry.serializer(), text)

    private fun encode(entry: MediaEntry) = Json.encodeToJsonElement(MediaEntry.serializer(), entry)

    private fun payload(vararg entries: String): MediaPayload {
        val text = """{"lastUpdate":"2026-01-01T00:00:00Z","payload":{"entries":[${entries.joinToString(",")}]}}"""
        return MediaDocumentCodec.codec.decode(DocumentEnvelope.parse(text))
    }
}
