package pl.stapik.media.data.library

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails
import pl.stapik.media.data.serialization.MediaPayload

class MediaLibraryTest {
    private class FakeEditor(var payload: MediaPayload = MediaPayload()) : DocumentEditor {
        override suspend fun current(): MediaPayload = payload

        override suspend fun edit(transform: (MediaPayload) -> MediaPayload) {
            payload = transform(payload)
        }
    }

    private class FakeArchives(var remote: Set<Int>?, val editors: MutableMap<Int, FakeEditor> = mutableMapOf()) : ArchiveDirectory {
        override fun loadedYears(): Set<Int> = editors.keys

        override fun remoteYears(): Set<Int>? = remote

        override fun editorFor(year: Int): DocumentEditor = editors.getOrPut(year) { FakeEditor() }
    }

    private val clock = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC)
    private val main = FakeEditor()
    private val archives = FakeArchives(remote = emptySet())
    private var connected = true
    private var nextId = 0
    private val library = MediaLibrary(main, archives, clock, { "id-${++nextId}" }, { connected })

    private fun entry(
        title: String,
        consumedYear: Int = 2026,
        consumedMonth: Int = 3,
        released: Int = 2020,
        category: MediaCategory = MediaCategory.MOVIE,
        id: String = "",
    ) = MediaEntry(
        category = category,
        title = title,
        releaseDate = ReleaseDate(LocalDate.of(released, 1, 1), DatePrecision.DAY),
        consumed = ConsumedDate(month = consumedMonth, year = consumedYear),
        details = MediaDetails.Screen(director = "D", genre = "G"),
        id = id,
    )

    private fun refusal(result: WriteResult) = (result as WriteResult.Refused).refusal

    @Test
    fun anAddedEntryOfTheCurrentYearLandsInTheMainDocumentWithAnId() = runBlocking {
        val result = library.add(entry("Dune")) as WriteResult.Done

        assertEquals("id-1", result.entry.id)
        assertEquals(listOf("Dune"), main.payload.entries.map { it.title })
    }

    @Test
    fun anEntryOfAnEarlierYearGoesToTheLoadedArchive() = runBlocking {
        archives.editors[2024] = FakeEditor()

        library.add(entry("Arrival", consumedYear = 2024))

        assertEquals(listOf("Arrival"), archives.editors.getValue(2024).payload.entries.map { it.title })
        assertTrue(main.payload.entries.isEmpty())
    }

    @Test
    fun anArchiveTheCloudHasButThePhoneDoesNotIsNotWritable() = runBlocking {
        archives.remote = setOf(2024)

        assertEquals(WriteRefusal.ArchiveNotLoaded(2024), refusal(library.add(entry("Arrival", consumedYear = 2024))))
    }

    @Test
    fun anArchiveWhoseExistenceIsUnknownIsNotWritableWhileConnected() = runBlocking {
        archives.remote = null

        assertEquals(WriteRefusal.ArchiveNotLoaded(2024), refusal(library.add(entry("Arrival", consumedYear = 2024))))
    }

    @Test
    fun anOldYearIsWritableWhenNoCloudIsConnectedOrTheCloudLacksIt() = runBlocking {
        connected = false
        archives.remote = setOf(2024)
        assertTrue(library.add(entry("Offline", consumedYear = 2024)) is WriteResult.Done)

        connected = true
        archives.remote = setOf(2020)
        assertTrue(library.add(entry("Fresh", consumedYear = 2023)) is WriteResult.Done)
    }

    @Test
    fun theSameWorkTwiceIsRefusedIgnoringCaseAndSpaces() = runBlocking {
        library.add(entry("Dune"))

        assertEquals(WriteRefusal.Duplicate, refusal(library.add(entry("  DUNE "))))
    }

    @Test
    fun aRemakeWithAnotherReleaseYearIsAllowed() = runBlocking {
        library.add(entry("Dune", released = 1984))

        assertTrue(library.add(entry("Dune", released = 2021)) is WriteResult.Done)
    }

    @Test
    fun invalidEntriesAreRefused() = runBlocking {
        assertEquals(WriteRefusal.Invalid(EntryProblem.TITLE_MISSING), refusal(library.add(entry("   "))))
        assertEquals(WriteRefusal.Invalid(EntryProblem.MONTH_OUT_OF_RANGE), refusal(library.add(entry("A", consumedMonth = 13))))
        assertEquals(WriteRefusal.Invalid(EntryProblem.RATING_OUT_OF_RANGE), refusal(library.add(entry("B").copy(rating = 11))))
    }

    @Test
    fun anUpdateKeepsThePositionAndTheIdentity() = runBlocking {
        library.add(entry("First"))
        library.add(entry("Second"))

        library.update("id-1", entry("First (director's cut)"))

        assertEquals(listOf("First (director's cut)", "Second"), main.payload.entries.map { it.title })
        assertEquals("id-1", main.payload.entries.first().id)
    }

    @Test
    fun anEntryCanBeSavedAgainWithoutBeingItsOwnDuplicate() = runBlocking {
        library.add(entry("Dune"))

        assertTrue(library.update("id-1", entry("Dune", consumedMonth = 9)) is WriteResult.Done)
    }

    @Test
    fun changingTheConsumedYearMovesTheEntryBetweenDocuments() = runBlocking {
        archives.editors[2024] = FakeEditor()
        library.add(entry("Dune"))

        library.update("id-1", entry("Dune", consumedYear = 2024))

        assertTrue(main.payload.entries.isEmpty())
        assertEquals(listOf("id-1"), archives.editors.getValue(2024).payload.entries.map { it.id })
    }

    @Test
    fun aMoveIntoAnUnloadedArchiveLeavesTheEntryWhereItWas() = runBlocking {
        library.add(entry("Dune"))
        archives.remote = setOf(2024)

        assertEquals(WriteRefusal.ArchiveNotLoaded(2024), refusal(library.update("id-1", entry("Dune", consumedYear = 2024))))
        assertEquals(1, main.payload.entries.size)
    }

    @Test
    fun updatingAnUnknownEntryIsRefused() = runBlocking {
        assertEquals(WriteRefusal.UnknownEntry, refusal(library.update("nope", entry("X"))))
    }

    @Test
    fun unknownFieldsStayOnAnEditedEntryUnlessItsCategoryChanges() = runBlocking {
        val extras = JsonObject(mapOf("shelf" to JsonPrimitive("A3"), "details" to JsonObject(mapOf("isbn" to JsonPrimitive("1")))))
        main.payload = MediaPayload(listOf(entry("Solaris", id = "b1").copy(extras = extras)))

        library.update("b1", entry("Solaris", consumedMonth = 4))
        assertEquals(extras, main.payload.entries.single().extras)

        library.update("b1", entry("Solaris", category = MediaCategory.BOOK))
        assertEquals(JsonObject(mapOf("shelf" to JsonPrimitive("A3"))), main.payload.entries.single().extras)
    }

    @Test
    fun deletingReturnsTheRemovedEntryForUndo() = runBlocking {
        library.add(entry("Dune"))

        val removed = (library.delete("id-1") as WriteResult.Done).entry

        assertEquals("Dune", removed.title)
        assertTrue(main.payload.entries.isEmpty())
        assertEquals(WriteRefusal.UnknownEntry, refusal(library.delete("id-1")))
    }

    @Test
    fun generatedIdsLookLikeTheDesktopOnes() {
        val id = newEntryId()

        assertEquals(16, id.length)
        assertTrue(id.all { it in '0'..'9' || it in 'a'..'f' })
    }
}
