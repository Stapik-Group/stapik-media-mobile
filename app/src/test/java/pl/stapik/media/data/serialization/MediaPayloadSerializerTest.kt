package pl.stapik.media.data.serialization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import pl.stapik.common.model.DocumentDecodeException
import pl.stapik.common.model.DocumentEnvelope

class MediaPayloadSerializerTest {

    @Test
    fun readableEntriesSurviveNextToUnreadableOnes() {
        val payload = decode(
            entry("Dune"),
            entry("Unknown category", category = "hologram"),
            entry("Impossible date", releaseDate = "2021-02-31"),
            """{"category":"movie","title":"No details"}""",
            entry("Arrival"),
        )

        assertEquals(listOf("Dune", "Arrival"), payload.entries.map { it.title })
        assertEquals(3, payload.unreadableEntries)
    }

    @Test
    fun aDocumentWithoutUnreadableEntriesReportsNone() {
        val payload = decode(entry("Dune"))

        assertEquals(1, payload.entries.size)
        assertEquals(0, payload.unreadableEntries)
    }

    @Test
    fun aMissingEntriesListMeansAnEmptyDocument() {
        val payload = MediaDocumentCodec.codec.decode(DocumentEnvelope.parse("""{"lastUpdate":"2026-01-01T00:00:00Z","payload":{}}"""))

        assertEquals(0, payload.entries.size)
        assertEquals(0, payload.unreadableEntries)
    }

    @Test
    fun entriesThatAreNotAListMakeTheWholeDocumentUnreadable() {
        val envelope = DocumentEnvelope.parse("""{"lastUpdate":"2026-01-01T00:00:00Z","payload":{"entries":"nope"}}""")

        assertThrows(DocumentDecodeException::class.java) { MediaDocumentCodec.codec.decode(envelope) }
    }

    private fun decode(vararg entries: String): MediaPayload {
        val text = """{"lastUpdate":"2026-01-01T00:00:00Z","payload":{"entries":[${entries.joinToString(",")}]}}"""
        return MediaDocumentCodec.codec.decode(DocumentEnvelope.parse(text))
    }

    private fun entry(title: String, category: String = "movie", releaseDate: String = "2021-10-22") =
        """{"category":"$category","title":"$title","releaseDate":{"date":"$releaseDate","precision":"day"},""" +
            """"consumed":{"month":3,"year":2024},"details":{"director":"Director","genre":"Genre"}}"""
}
