package pl.stapik.media.data.csv

import java.time.DateTimeException
import java.time.LocalDate
import java.util.Locale
import pl.stapik.media.data.library.EntryRules
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.EntryRating
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails

enum class CsvProblem {
    MISSING_COLUMNS,
    UNKNOWN_CATEGORY,
    EMPTY_TITLE,
    INVALID_RELEASE_DATE,
    INVALID_CONSUMED,
    INVALID_AUDIOBOOK,
    INVALID_RATING,
}

class CsvImportRow(val number: Int, val title: String, val entry: MediaEntry?, val problem: CsvProblem?)

object EntryCsvImport {
    private const val CATEGORY = 0
    private const val TITLE = 1
    private const val RELEASE_DATE = 2
    private const val CONSUMED = 3
    private const val CREATOR = 4
    private const val PUBLISHER = 5
    private const val GENRE = 6
    private const val PLATFORM = 7
    private const val AUDIOBOOK = 8
    private const val RATING = 9
    private const val NOTE = 10
    private const val REQUIRED_COLUMNS = CONSUMED + 1

    private val RELEASE_DATE_PATTERN = Regex("(\\d{4})(?:-(\\d{2})(?:-(\\d{2}))?)?")
    private val CONSUMED_PATTERN = Regex("(\\d{4})-(\\d{2})")

    fun parse(table: List<List<String>>): List<CsvImportRow> =
        table.withIndex()
            .filterNot { (index, row) -> index == 0 && isHeader(row) }
            .map { (index, row) -> parseRow(index + 1, row) }

    private fun isHeader(row: List<String>): Boolean = categoryOf(row) == null

    private fun categoryOf(row: List<String>): MediaCategory? = EntryCsv.categoryOf(cell(row, CATEGORY).lowercase(Locale.ROOT))

    private fun cell(row: List<String>, column: Int): String = row.getOrNull(column)?.trim().orEmpty()

    private fun parseRow(number: Int, row: List<String>): CsvImportRow {
        val title = cell(row, TITLE)
        fun failed(problem: CsvProblem) = CsvImportRow(number, title, null, problem)

        if (row.size < REQUIRED_COLUMNS) return failed(CsvProblem.MISSING_COLUMNS)
        val category = categoryOf(row) ?: return failed(CsvProblem.UNKNOWN_CATEGORY)
        if (title.isEmpty()) return failed(CsvProblem.EMPTY_TITLE)
        val releaseDate = parseReleaseDate(cell(row, RELEASE_DATE)) ?: return failed(CsvProblem.INVALID_RELEASE_DATE)
        val consumed = parseConsumed(cell(row, CONSUMED)) ?: return failed(CsvProblem.INVALID_CONSUMED)
        val audiobook = parseAudiobook(cell(row, AUDIOBOOK)) ?: return failed(CsvProblem.INVALID_AUDIOBOOK)
        val ratingText = cell(row, RATING)
        val rating = ratingText.takeIf { it.isNotEmpty() }?.let { text ->
            text.toIntOrNull()?.takeIf { it in EntryRating.MIN..EntryRating.MAX } ?: return failed(CsvProblem.INVALID_RATING)
        }
        val entry = MediaEntry(
            category = category,
            title = title,
            releaseDate = releaseDate,
            consumed = consumed,
            details = detailsOf(category, row, audiobook),
            rating = rating,
            note = cell(row, NOTE),
        )
        return CsvImportRow(number, title, entry, null)
    }

    private fun parseReleaseDate(text: String): ReleaseDate? {
        val parts = RELEASE_DATE_PATTERN.matchEntire(text)?.groupValues ?: return null
        val year = parts[1].toInt()
        if (year !in EntryRules.RELEASE_YEARS) return null
        val month = parts[2].toIntOrNull()
        val day = parts[3].toIntOrNull()
        val precision = when {
            day != null -> DatePrecision.DAY
            month != null -> DatePrecision.MONTH
            else -> DatePrecision.YEAR
        }
        return try {
            ReleaseDate(LocalDate.of(year, month ?: 1, day ?: 1), precision)
        } catch (e: DateTimeException) {
            null
        }
    }

    private fun parseConsumed(text: String): ConsumedDate? {
        val parts = CONSUMED_PATTERN.matchEntire(text)?.groupValues ?: return null
        val year = parts[1].toInt()
        val month = parts[2].toInt()
        return ConsumedDate(month = month, year = year).takeIf { year in EntryRules.CONSUMED_YEARS && month in 1..12 }
    }

    private fun parseAudiobook(text: String): Boolean? = when (text.lowercase(Locale.ROOT)) {
        "", "false" -> false
        "true" -> true
        else -> null
    }

    private fun detailsOf(category: MediaCategory, row: List<String>, audiobook: Boolean): MediaDetails {
        val creator = cell(row, CREATOR)
        val publisher = cell(row, PUBLISHER)
        val genre = cell(row, GENRE)
        return when (category) {
            MediaCategory.MOVIE, MediaCategory.SERIES, MediaCategory.CARTOON -> MediaDetails.Screen(creator, genre)
            MediaCategory.BOOK -> MediaDetails.Book(creator, genre, audiobook)
            MediaCategory.ALBUM -> MediaDetails.Album(creator, publisher, genre)
            MediaCategory.GAME -> MediaDetails.Game(creator, publisher, cell(row, PLATFORM))
        }
    }
}
