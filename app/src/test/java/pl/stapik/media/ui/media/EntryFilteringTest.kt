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

class EntryFilteringTest {
    private val titles = { entries: List<MediaEntry> -> entries.map { it.title } }

    @Test
    fun theQueryIgnoresCaseAndDiacritics() {
        val entries = listOf(entry("Żółć"), entry("Dune"))

        val found = entries.filteredFor(MediaCategory.MOVIE, CategoryFilter(query = "ZOLC"), EntrySort())

        assertEquals(listOf("Żółć"), titles(found))
    }

    @Test
    fun aBlankQueryMatchesEverything() {
        val entries = listOf(entry("Dune"), entry("Arrival"))

        val found = entries.filteredFor(MediaCategory.MOVIE, CategoryFilter(query = "   "), EntrySort())

        assertEquals(2, found.size)
    }

    @Test
    fun entriesAreSortedByTitleWithoutRegardToDiacritics() {
        val entries = listOf(entry("Dune"), entry("Ćma"), entry("Apple"))
        val sort = EntrySort(SortField.TITLE, SortDirection.ASCENDING)

        assertEquals(listOf("Apple", "Ćma", "Dune"), titles(entries.filteredFor(MediaCategory.MOVIE, CategoryFilter.None, sort)))
    }

    @Test
    fun theDefaultOrderIsNewestConsumedFirstAndKeepsTheDocumentOrderOfTies() {
        val entries = listOf(
            entry("Old", year = 2023, month = 5),
            entry("First tie", year = 2024, month = 2),
            entry("Second tie", year = 2024, month = 2),
            entry("Newest", year = 2024, month = 9),
        )

        val ordered = entries.filteredFor(MediaCategory.MOVIE, CategoryFilter.None, EntrySort())

        assertEquals(listOf("Newest", "First tie", "Second tie", "Old"), titles(ordered))
    }

    @Test
    fun entriesAreSortedByReleaseDateInBothDirections() {
        val entries = listOf(entry("B", released = 2010), entry("A", released = 2000), entry("C", released = 2020))
        val ascending = EntrySort(SortField.RELEASE_DATE, SortDirection.ASCENDING)

        assertEquals(listOf("A", "B", "C"), titles(entries.filteredFor(MediaCategory.MOVIE, CategoryFilter.None, ascending)))
        assertEquals(
            listOf("C", "B", "A"),
            titles(entries.filteredFor(MediaCategory.MOVIE, CategoryFilter.None, ascending.withReversedDirection())),
        )
    }

    private fun entry(title: String, year: Int = 2024, month: Int = 3, released: Int = 2020) = MediaEntry(
        category = MediaCategory.MOVIE,
        title = title,
        releaseDate = ReleaseDate(LocalDate.of(released, 1, 1), DatePrecision.DAY),
        consumed = ConsumedDate(month = month, year = year),
        details = MediaDetails.Screen(director = "Director", genre = "Genre"),
    )
}
