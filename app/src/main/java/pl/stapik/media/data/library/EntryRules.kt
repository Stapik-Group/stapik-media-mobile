package pl.stapik.media.data.library

import pl.stapik.media.data.model.EntryRating
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.util.foldCase

object EntryRules {
    val RELEASE_YEARS = 1..2100
    val CONSUMED_YEARS = 1900..2100
    private val MONTHS = 1..12

    data class WorkKey(val category: MediaCategory, val releaseYear: Int, val title: String)

    fun problemWith(entry: MediaEntry): EntryProblem? = when {
        entry.title.isBlank() -> EntryProblem.TITLE_MISSING
        entry.rating != null && entry.rating !in EntryRating.MIN..EntryRating.MAX -> EntryProblem.RATING_OUT_OF_RANGE
        entry.consumed.month !in MONTHS -> EntryProblem.MONTH_OUT_OF_RANGE
        else -> null
    }

    fun titleKey(title: String): String = foldCase(title.trim())

    fun workKey(entry: MediaEntry) = WorkKey(entry.category, entry.releaseDate.date.year, titleKey(entry.title))

    fun isSameWork(left: MediaEntry, right: MediaEntry): Boolean = workKey(left) == workKey(right)

    fun isDuplicate(candidate: MediaEntry, existing: List<MediaEntry>): Boolean =
        existing.any { it.id != candidate.id && isSameWork(it, candidate) }

    fun archiveYearOf(entry: MediaEntry, currentYear: Int): Int? = entry.consumed.year.takeIf { it < currentYear }
}
