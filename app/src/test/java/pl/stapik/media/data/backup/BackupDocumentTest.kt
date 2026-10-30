package pl.stapik.media.data.backup

import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails
import pl.stapik.media.data.serialization.MediaPayload

class BackupDocumentTest {
    private fun entry(title: String, id: String = "0123456789abcdef") = MediaEntry(
        category = MediaCategory.MOVIE,
        title = title,
        releaseDate = ReleaseDate(LocalDate.of(2020, 5, 1), DatePrecision.MONTH),
        consumed = ConsumedDate(month = 3, year = 2026),
        details = MediaDetails.Screen(director = "D", genre = "G"),
        id = id,
        rating = 8,
        note = "good",
    )

    private fun failureOf(text: String): BackupFailure = assertThrows(BackupException::class.java) { BackupDocument.read(text) }.failure

    @Test
    fun aWrittenDocumentIsTheDesktopLocalFileWrapper() {
        val written = Json.parseToJsonElement(BackupDocument.write(MediaPayload(listOf(entry("Dune"))), "2026-10-30T10:00:00Z")).jsonObject

        assertEquals(2, written.getValue("schemaVersion").jsonPrimitive.int)
        val document = written.getValue("document").jsonObject
        assertEquals("2026-10-30T10:00:00Z", document.getValue("lastUpdate").jsonPrimitive.content)
        assertEquals(1, document.getValue("entries").jsonArray.size)
    }

    @Test
    fun entriesSurviveAWriteAndReadWithUnknownFieldsAndUnreadableOnes() {
        val extras = JsonObject(mapOf("shelf" to JsonPrimitive("A3")))
        val raw = JsonObject(mapOf("title" to JsonPrimitive("broken")))
        val payload = MediaPayload(listOf(entry("Dune").copy(extras = extras)), listOf(raw))

        val read = BackupDocument.read(BackupDocument.write(payload, "2026-10-30T10:00:00Z"))

        assertEquals(payload.entries, read.entries)
        assertEquals(listOf(raw), read.unreadable)
    }

    @Test
    fun theCloudEnvelopeIsReadToo() {
        val document = Json.parseToJsonElement(BackupDocument.write(MediaPayload(listOf(entry("Dune"))), "x")).jsonObject.getValue("document")
        val cloud = JsonObject(mapOf("lastUpdate" to JsonPrimitive("x"), "payload" to document)).toString()

        assertEquals(listOf("Dune"), BackupDocument.read(cloud).entries.map { it.title })
    }

    @Test
    fun aDocumentWithoutEntriesIsEmpty() {
        assertTrue(BackupDocument.read("""{"schemaVersion":2,"document":{"lastUpdate":"x"}}""").entries.isEmpty())
    }

    @Test
    fun aNewerSchemaIsRefused() {
        assertEquals(BackupFailure.NEWER_VERSION, failureOf("""{"schemaVersion":3,"document":{"entries":[]}}"""))
    }

    @Test
    fun textThatIsNotAJsonObjectIsDamaged() {
        assertEquals(BackupFailure.DAMAGED, failureOf("not json"))
        assertEquals(BackupFailure.DAMAGED, failureOf("[1,2]"))
    }
}
