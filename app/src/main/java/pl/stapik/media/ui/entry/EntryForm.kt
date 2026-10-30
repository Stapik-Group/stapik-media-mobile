package pl.stapik.media.ui.entry

import java.time.LocalDate
import java.time.YearMonth
import kotlinx.serialization.json.JsonObject
import pl.stapik.media.data.library.EntryRules
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails

enum class FormProblem { TITLE_EMPTY, RELEASE_YEAR_INVALID, CONSUMED_YEAR_INVALID }

sealed interface FormResult {
    data class Ready(val entry: MediaEntry) : FormResult
    data class Invalid(val problems: Set<FormProblem>) : FormResult
}

data class EntryForm(
    val category: MediaCategory,
    val title: String = "",
    val releasePrecision: DatePrecision = DatePrecision.DAY,
    val releaseYear: String = "",
    val releaseMonth: Int = 1,
    val releaseDay: Int = 1,
    val consumedYear: String = "",
    val consumedMonth: Int = 1,
    val rating: Int? = null,
    val note: String = "",
    val creator: String = "",
    val genre: String = "",
    val publisher: String = "",
    val platform: String = "",
    val isAudiobook: Boolean = false,
    val id: String = "",
    val extras: JsonObject = JsonObject(emptyMap()),
) {
    fun toEntry(): FormResult {
        val problems = mutableSetOf<FormProblem>()
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) problems += FormProblem.TITLE_EMPTY
        val releaseYearValue = releaseYear.trim().toIntOrNull()?.takeIf { it in RELEASE_YEARS }
        if (releaseYearValue == null) problems += FormProblem.RELEASE_YEAR_INVALID
        val consumedYearValue = consumedYear.trim().toIntOrNull()?.takeIf { it in CONSUMED_YEARS }
        if (consumedYearValue == null) problems += FormProblem.CONSUMED_YEAR_INVALID
        if (releaseYearValue == null || consumedYearValue == null || problems.isNotEmpty()) return FormResult.Invalid(problems)

        return FormResult.Ready(
            MediaEntry(
                category = category,
                title = cleanTitle,
                releaseDate = releaseDate(releaseYearValue),
                consumed = ConsumedDate(month = consumedMonth, year = consumedYearValue),
                details = details(),
                id = id,
                rating = rating,
                note = note.trim(),
                extras = extras,
            ),
        )
    }

    private fun releaseDate(year: Int): ReleaseDate {
        val month = if (releasePrecision == DatePrecision.YEAR) 1 else releaseMonth
        val day = if (releasePrecision == DatePrecision.DAY) minOf(releaseDay, YearMonth.of(year, month).lengthOfMonth()) else 1
        return ReleaseDate(LocalDate.of(year, month, day), releasePrecision)
    }

    private fun details(): MediaDetails = when (category) {
        MediaCategory.MOVIE, MediaCategory.SERIES, MediaCategory.CARTOON ->
            MediaDetails.Screen(director = creator.trim(), genre = genre.trim())
        MediaCategory.BOOK -> MediaDetails.Book(author = creator.trim(), genre = genre.trim(), isAudiobook = isAudiobook)
        MediaCategory.ALBUM -> MediaDetails.Album(performer = creator.trim(), publisher = publisher.trim(), genre = genre.trim())
        MediaCategory.GAME -> MediaDetails.Game(studio = creator.trim(), publisher = publisher.trim(), platform = platform.trim())
    }

    fun toSaved(): List<Any?> = listOf(
        title, releasePrecision.name, releaseYear, releaseMonth, releaseDay, consumedYear, consumedMonth,
        rating ?: NO_RATING, note, creator, genre, publisher, platform, isAudiobook,
    )

    companion object {
        val RELEASE_YEARS = EntryRules.RELEASE_YEARS
        val CONSUMED_YEARS = EntryRules.CONSUMED_YEARS
        private const val NO_RATING = 0

        fun blank(category: MediaCategory, today: LocalDate): EntryForm = EntryForm(
            category = category,
            releaseYear = today.year.toString(),
            consumedYear = today.year.toString(),
            consumedMonth = today.monthValue,
        )

        fun from(entry: MediaEntry): EntryForm {
            val base = EntryForm(
                category = entry.category,
                title = entry.title,
                releasePrecision = entry.releaseDate.precision,
                releaseYear = entry.releaseDate.date.year.toString(),
                releaseMonth = entry.releaseDate.date.monthValue,
                releaseDay = entry.releaseDate.date.dayOfMonth,
                consumedYear = entry.consumed.year.toString(),
                consumedMonth = entry.consumed.month,
                rating = entry.rating,
                note = entry.note,
                id = entry.id,
                extras = entry.extras,
            )
            return when (val details = entry.details) {
                is MediaDetails.Screen -> base.copy(creator = details.director, genre = details.genre)
                is MediaDetails.Book -> base.copy(creator = details.author, genre = details.genre, isAudiobook = details.isAudiobook)
                is MediaDetails.Album -> base.copy(creator = details.performer, publisher = details.publisher, genre = details.genre)
                is MediaDetails.Game -> base.copy(creator = details.studio, publisher = details.publisher, platform = details.platform)
            }
        }

        fun fromSaved(base: EntryForm, saved: List<Any?>): EntryForm = base.copy(
            title = saved[0] as String,
            releasePrecision = DatePrecision.valueOf(saved[1] as String),
            releaseYear = saved[2] as String,
            releaseMonth = saved[3] as Int,
            releaseDay = saved[4] as Int,
            consumedYear = saved[5] as String,
            consumedMonth = saved[6] as Int,
            rating = (saved[7] as Int).takeIf { it != NO_RATING },
            note = saved[8] as String,
            creator = saved[9] as String,
            genre = saved[10] as String,
            publisher = saved[11] as String,
            platform = saved[12] as String,
            isAudiobook = saved[13] as Boolean,
        )
    }
}
