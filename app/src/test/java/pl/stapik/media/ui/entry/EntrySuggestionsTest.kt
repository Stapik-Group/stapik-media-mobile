package pl.stapik.media.ui.entry

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails

class EntrySuggestionsTest {
    private fun movie(director: String, genre: String, category: MediaCategory = MediaCategory.MOVIE) = entry(category, MediaDetails.Screen(director, genre))

    private fun book(author: String, genre: String) = entry(MediaCategory.BOOK, MediaDetails.Book(author, genre, false))

    private fun entry(category: MediaCategory, details: MediaDetails) = MediaEntry(
        category = category,
        title = "T",
        releaseDate = ReleaseDate(LocalDate.of(2020, 1, 1), DatePrecision.DAY),
        consumed = ConsumedDate(month = 1, year = 2024),
        details = details,
    )

    @Test
    fun valuesAreRankedByHowOftenTheyAreUsed() {
        val entries = listOf(movie("Nolan", "SF"), movie("Villeneuve", "SF"), movie("Villeneuve", "Drama"), movie("Villeneuve", "SF"))

        val lists = EntrySuggestions.collect(entries, MediaCategory.MOVIE)

        assertEquals(listOf("Villeneuve", "Nolan"), lists.creators)
        assertEquals(listOf("SF", "Drama"), lists.genres)
    }

    @Test
    fun spellingsThatDifferOnlyInCaseAreOneSuggestionWithTheCommonSpelling() {
        val entries = listOf(movie("nolan", "SF"), movie("Nolan", "SF"), movie("Nolan ", "SF"))

        assertEquals(listOf("Nolan"), EntrySuggestions.collect(entries, MediaCategory.MOVIE).creators)
    }

    @Test
    fun onlyEntriesOfTheSameFamilyContribute() {
        val entries = listOf(movie("Nolan", "SF"), book("Lem", "SF"), movie("Anderson", "Comedy", MediaCategory.CARTOON))

        val lists = EntrySuggestions.collect(entries, MediaCategory.SERIES)

        assertEquals(listOf("Anderson", "Nolan"), lists.creators)
    }

    @Test
    fun blankValuesAreNeverSuggested() {
        val entries = listOf(movie("  ", ""), movie("Nolan", " "))

        val lists = EntrySuggestions.collect(entries, MediaCategory.MOVIE)

        assertEquals(listOf("Nolan"), lists.creators)
        assertEquals(emptyList<String>(), lists.genres)
    }
}
