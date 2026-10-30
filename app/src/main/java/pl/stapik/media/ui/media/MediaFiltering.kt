package pl.stapik.media.ui.media

import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.util.foldForSearch

fun List<MediaEntry>.filteredFor(category: MediaCategory, filter: CategoryFilter, sort: EntrySort): List<MediaEntry> {
    val query = foldForSearch(filter.query.trim())
    return asSequence()
        .filter { it.category == category }
        .filter { filter.year == null || it.consumed.year == filter.year }
        .filter { filter.month == null || it.consumed.month == filter.month }
        .filter { query.isEmpty() || foldForSearch(it.title).contains(query) }
        .sortedWith(sort.comparator())
        .toList()
}

data class KeyedEntry(val key: String, val entry: MediaEntry)

fun List<MediaEntry>.withListKeys(): List<KeyedEntry> {
    val used = HashSet<String>()
    return map { entry ->
        val base = entry.id.ifEmpty { "${entry.title}|${entry.consumed.year}|${entry.consumed.month}" }
        KeyedEntry(uniqueKey(base, used), entry)
    }
}

private fun uniqueKey(base: String, used: MutableSet<String>): String {
    var key = base
    var repeat = 1
    while (!used.add(key)) {
        repeat++
        key = "$base#$repeat"
    }
    return key
}

fun List<MediaEntry>.availableYearsFor(category: MediaCategory, archiveYears: Set<Int>): List<Int> =
    (filter { it.category == category }.map { it.consumed.year } + archiveYears)
        .distinct()
        .sortedDescending()
