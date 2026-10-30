package pl.stapik.media.data.csv

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails

class EntryCsvTest {
    private val header = List(EntryCsv.COLUMN_COUNT) { "h$it" }

    private fun dateFor(precision: DatePrecision): LocalDate = when (precision) {
        DatePrecision.DAY -> LocalDate.of(2021, 10, 22)
        DatePrecision.MONTH -> LocalDate.of(2021, 10, 1)
        DatePrecision.YEAR -> LocalDate.of(2021, 1, 1)
    }

    private fun entry(
        category: MediaCategory,
        details: MediaDetails,
        precision: DatePrecision = DatePrecision.DAY,
        rating: Int? = null,
        note: String = "",
    ) = MediaEntry(
        category = category,
        title = "Title",
        releaseDate = ReleaseDate(dateFor(precision), precision),
        consumed = ConsumedDate(month = 3, year = 2026),
        details = details,
        id = "ignored",
        rating = rating,
        note = note,
    )

    private fun rowOf(entry: MediaEntry) = EntryCsv.table(listOf(entry), header)[1]

    @Test
    fun theTableStartsWithTheHeader() {
        assertEquals(header, EntryCsv.table(emptyList(), header).single())
    }

    @Test
    fun aScreenEntryFillsTheDirectorAndGenreColumns() {
        val row = rowOf(entry(MediaCategory.MOVIE, MediaDetails.Screen("Villeneuve", "SF"), rating = 9, note = "good"))

        assertEquals(listOf("movie", "Title", "2021-10-22", "2026-03", "Villeneuve", "", "SF", "", "", "9", "good"), row)
    }

    @Test
    fun releaseDatesAreWrittenWithTheirPrecision() {
        val details = MediaDetails.Screen("", "")

        assertEquals("2021-10", rowOf(entry(MediaCategory.MOVIE, details, DatePrecision.MONTH))[2])
        assertEquals("2021", rowOf(entry(MediaCategory.MOVIE, details, DatePrecision.YEAR))[2])
    }

    @Test
    fun booksWriteTheAudiobookFlagAndGamesTheirPlatform() {
        assertEquals("true", rowOf(entry(MediaCategory.BOOK, MediaDetails.Book("A", "G", true)))[8])
        assertEquals(listOf("S", "P", "", "PC"), rowOf(entry(MediaCategory.GAME, MediaDetails.Game("S", "P", "PC"))).subList(4, 8))
    }

    @Test
    fun anExportedTableIsImportedWithoutLoss() {
        val entries = listOf(
            entry(MediaCategory.MOVIE, MediaDetails.Screen("D", "G"), DatePrecision.MONTH, rating = 7, note = "n; \"q\""),
            entry(MediaCategory.BOOK, MediaDetails.Book("A", "G", true)),
            entry(MediaCategory.ALBUM, MediaDetails.Album("P", "L", "G"), DatePrecision.YEAR),
            entry(MediaCategory.GAME, MediaDetails.Game("S", "P", "PC")),
        )

        val table = CsvText.parse(CsvText.write(EntryCsv.table(entries, header), CsvText.SEMICOLON))!!
        val imported = EntryCsvImport.parse(table)

        assertEquals(entries.map { it.copy(id = "") }, imported.map { it.entry })
        assertNull(imported.firstNotNullOfOrNull { it.problem })
    }

    @Test
    fun theHeaderRowIsSkippedAndRowNumbersCountItAsRowOne() {
        val rows = EntryCsvImport.parse(listOf(header, listOf("movie", "Dune", "2021", "2026-03")))

        assertEquals(listOf(2), rows.map { it.number })
        assertNotNull(rows.single().entry)
    }

    @Test
    fun aFirstRowWithAKnownCategoryIsData() {
        assertEquals(listOf(1), EntryCsvImport.parse(listOf(listOf("MOVIE", "Dune", "2021", "2026-03"))).map { it.number })
    }

    private fun problemOf(vararg cells: String) = EntryCsvImport.parse(listOf(header, cells.toList())).single().problem

    @Test
    fun everyKindOfBadRowIsNamed() {
        assertEquals(CsvProblem.MISSING_COLUMNS, problemOf("movie", "Dune", "2021"))
        assertEquals(CsvProblem.UNKNOWN_CATEGORY, problemOf("film", "Dune", "2021", "2026-03"))
        assertEquals(CsvProblem.EMPTY_TITLE, problemOf("movie", "  ", "2021", "2026-03"))
        assertEquals(CsvProblem.INVALID_RELEASE_DATE, problemOf("movie", "Dune", "2021-02-30", "2026-03"))
        assertEquals(CsvProblem.INVALID_RELEASE_DATE, problemOf("movie", "Dune", "0000", "2026-03"))
        assertEquals(CsvProblem.INVALID_RELEASE_DATE, problemOf("movie", "Dune", "2021-1", "2026-03"))
        assertEquals(CsvProblem.INVALID_CONSUMED, problemOf("movie", "Dune", "2021", "2026-13"))
        assertEquals(CsvProblem.INVALID_CONSUMED, problemOf("movie", "Dune", "2021", "1899-01"))
        assertEquals(CsvProblem.INVALID_CONSUMED, problemOf("movie", "Dune", "2021", "2026"))
        assertEquals(CsvProblem.INVALID_AUDIOBOOK, problemOf("book", "Dune", "2021", "2026-03", "", "", "", "", "maybe"))
        assertEquals(CsvProblem.INVALID_RATING, problemOf("movie", "Dune", "2021", "2026-03", "", "", "", "", "", "11"))
        assertEquals(CsvProblem.INVALID_RATING, problemOf("movie", "Dune", "2021", "2026-03", "", "", "", "", "", "x"))
    }

    @Test
    fun cellsAreTrimmedAndAMissingRatingStaysEmpty() {
        val entry = EntryCsvImport.parse(listOf(listOf(" Movie ", "  Dune ", " 2021-10-22 ", " 2026-03 ", " D ", "", " G "))).single().entry!!

        assertEquals("Dune", entry.title)
        assertEquals(MediaDetails.Screen("D", "G"), entry.details)
        assertNull(entry.rating)
        assertEquals("", entry.note)
    }
}
