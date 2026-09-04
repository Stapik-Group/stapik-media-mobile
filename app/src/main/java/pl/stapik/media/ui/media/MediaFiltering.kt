package pl.stapik.media.ui.media

import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry

fun List<MediaEntry>.filteredFor(category: MediaCategory, filter: CategoryFilter): List<MediaEntry> =
    asSequence()
        .filter { it.category == category }
        .filter { filter.year == null || it.consumed.year == filter.year }
        .filter { filter.month == null || it.consumed.month == filter.month }
        .sortedWith(compareByDescending<MediaEntry> { it.consumed.year }.thenByDescending { it.consumed.month })
        .toList()

fun List<MediaEntry>.availableYearsFor(category: MediaCategory): List<Int> =
    filter { it.category == category }
        .map { it.consumed.year }
        .distinct()
        .sortedDescending()
