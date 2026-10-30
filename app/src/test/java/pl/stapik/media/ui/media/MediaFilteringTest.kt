package pl.stapik.media.ui.media

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails

class MediaFilteringTest {

    @Test
    fun keysAreUniqueForEntriesWithTheSameTitleAndConsumptionMonth() {
        val entries = listOf(entry("Dune"), entry("Dune"), entry("Dune"))

        val keys = entries.withListKeys().map { it.key }

        assertEquals(entries.size, keys.toSet().size)
    }

    @Test
    fun keysUseTheEntryIdWhenItIsPresent() {
        val keys = listOf(entry("Dune", id = "a1"), entry("Dune", id = "b2")).withListKeys().map { it.key }

        assertEquals(listOf("a1", "b2"), keys)
    }

    @Test
    fun keysStayUniqueWhenEntriesShareAnId() {
        val keys = listOf(entry("Dune", id = "a1"), entry("Arrival", id = "a1")).withListKeys().map { it.key }

        assertEquals(2, keys.toSet().size)
    }

    @Test
    fun keysWithoutIdSeparateTitleYearAndMonth() {
        val keys = listOf(entry("A", year = 2020, month = 11), entry("A2", year = 2020, month = 1)).withListKeys().map { it.key }

        assertEquals(2, keys.toSet().size)
    }

    @Test
    fun keysFollowTheOrderOfTheEntries() {
        val entries = listOf(entry("First"), entry("Second"))

        assertEquals(entries, entries.withListKeys().map { it.entry })
    }

    private fun entry(title: String, year: Int = 2024, month: Int = 3, id: String = "") = MediaEntry(
        category = MediaCategory.MOVIE,
        title = title,
        releaseDate = ReleaseDate(LocalDate.of(2020, 1, 1), DatePrecision.DAY),
        consumed = ConsumedDate(month = month, year = year),
        details = MediaDetails.Screen(director = "Director", genre = "Genre"),
        id = id,
    )
}
