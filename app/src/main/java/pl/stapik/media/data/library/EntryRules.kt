package pl.stapik.media.data.library

import pl.stapik.media.data.model.EntryRating
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.util.foldCase

object EntryRules {
    private val MONTHS = 1..12

    fun problemWith(entry: MediaEntry): EntryProblem? = when {
        entry.title.isBlank() -> EntryProblem.TITLE_MISSING
        entry.rating != null && entry.rating !in EntryRating.MIN..EntryRating.MAX -> EntryProblem.RATING_OUT_OF_RANGE
        entry.consumed.month !in MONTHS -> EntryProblem.MONTH_OUT_OF_RANGE
        else -> null
    }

    fun titleKey(title: String): String = foldCase(title.trim())

    fun isSameWork(left: MediaEntry, right: MediaEntry): Boolean =
        left.category == right.category &&
            left.releaseDate.date.year == right.releaseDate.date.year &&
            titleKey(left.title) == titleKey(right.title)

    fun isDuplicate(candidate: MediaEntry, existing: List<MediaEntry>): Boolean =
        existing.any { it.id != candidate.id && isSameWork(it, candidate) }

    fun archiveYearOf(entry: MediaEntry, currentYear: Int): Int? = entry.consumed.year.takeIf { it < currentYear }
}
