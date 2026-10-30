package pl.stapik.media.data.library

import java.time.Clock
import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.serialization.MediaPayload

class MediaLibrary(
    private val main: DocumentEditor,
    private val archives: ArchiveDirectory,
    private val clock: Clock,
    private val newId: () -> String,
    private val isConnected: suspend () -> Boolean,
) {
    private class Location(val archiveYear: Int?, val entry: MediaEntry)

    suspend fun add(draft: MediaEntry): WriteResult {
        val entry = draft.copy(id = draft.id.ifEmpty(newId))
        refusalFor(entry, existing = allEntries())?.let { return WriteResult.Refused(it) }
        editorFor(archiveYearOf(entry)).edit { it.withEntry(entry) }
        return WriteResult.Done(entry)
    }

    suspend fun update(id: String, edited: MediaEntry): WriteResult {
        val location = locate(id) ?: return WriteResult.Refused(WriteRefusal.UnknownEntry)
        val entry = edited.copy(id = id, extras = extrasFor(location.entry, edited))
        refusalFor(entry, existing = allEntries().filter { it.id != id })?.let { return WriteResult.Refused(it) }
        val target = archiveYearOf(entry)
        if (target == location.archiveYear) {
            editorFor(target).edit { payload -> payload.copy(entries = payload.entries.map { if (it.id == id) entry else it }) }
        } else {
            editorFor(target).edit { it.withEntry(entry) }
            editorFor(location.archiveYear).edit { it.withoutEntry(id) }
        }
        return WriteResult.Done(entry)
    }

    suspend fun delete(id: String): WriteResult {
        val location = locate(id) ?: return WriteResult.Refused(WriteRefusal.UnknownEntry)
        editorFor(location.archiveYear).edit { it.withoutEntry(id) }
        return WriteResult.Done(location.entry)
    }

    suspend fun snapshot(): LibrarySnapshot {
        val payloads = listOf(main.current()) + archives.loadedYears().sorted().map { archives.editorFor(it).current() }
        val everything = MediaPayload(
            entries = payloads.flatMap { it.entries },
            unreadable = payloads.flatMap { it.unreadable },
        )
        val unavailable = if (isConnected()) archives.remoteYears().orEmpty() - archives.loadedYears() else emptySet()
        return LibrarySnapshot(everything, unavailable)
    }

    suspend fun replaceAll(imported: MediaPayload): ReplaceReport {
        val byYear = withUniqueIds(imported.entries).groupBy { archiveYearOf(it) }
        val skipped = byYear.keys.filterNotNull().filterNot { canWriteYear(it) }.toSet()
        val years = archives.loadedYears() + byYear.keys.filterNotNull() - skipped
        for (year in years) {
            archives.editorFor(year).edit { MediaPayload(entries = byYear[year].orEmpty()) }
        }
        main.edit { MediaPayload(entries = byYear[null].orEmpty(), unreadable = imported.unreadable) }
        return ReplaceReport(
            applied = byYear.filterKeys { it !in skipped }.values.sumOf { it.size },
            skippedByYear = skipped.associateWith { byYear.getValue(it).size },
        )
    }

    private fun withUniqueIds(entries: List<MediaEntry>): List<MediaEntry> {
        val seen = HashSet<String>()
        return entries.map { entry ->
            val id = if (entry.id.isNotEmpty() && entry.id !in seen) entry.id else freshId(seen)
            seen.add(id)
            entry.copy(id = id)
        }
    }

    private fun freshId(taken: Set<String>): String {
        var id = newId()
        while (id in taken) id = newId()
        return id
    }

    private suspend fun refusalFor(entry: MediaEntry, existing: List<MediaEntry>): WriteRefusal? {
        EntryRules.problemWith(entry)?.let { return WriteRefusal.Invalid(it) }
        if (EntryRules.isDuplicate(entry, existing)) return WriteRefusal.Duplicate
        if (!canWriteYear(entry.consumed.year)) return WriteRefusal.ArchiveNotLoaded(entry.consumed.year)
        return null
    }

    private suspend fun canWriteYear(year: Int): Boolean {
        if (year >= currentYear() || year in archives.loadedYears() || !isConnected()) return true
        val remote = archives.remoteYears() ?: return false
        return year !in remote
    }

    private fun currentYear(): Int = LocalDate.now(clock).year

    private fun archiveYearOf(entry: MediaEntry): Int? = EntryRules.archiveYearOf(entry, currentYear())

    private fun editorFor(archiveYear: Int?): DocumentEditor = archiveYear?.let(archives::editorFor) ?: main

    private suspend fun allEntries(): List<MediaEntry> =
        main.current().entries + archives.loadedYears().flatMap { archives.editorFor(it).current().entries }

    private suspend fun locate(id: String): Location? {
        main.current().entries.firstOrNull { it.id == id }?.let { return Location(null, it) }
        for (year in archives.loadedYears()) {
            archives.editorFor(year).current().entries.firstOrNull { it.id == id }?.let { return Location(year, it) }
        }
        return null
    }

    private fun extrasFor(original: MediaEntry, edited: MediaEntry) =
        if (original.category == edited.category) original.extras else original.extras.filterKeys { it != DETAILS_KEY }.let(::JsonObject)

    private fun MediaPayload.withEntry(entry: MediaEntry) = copy(entries = entries + entry)

    private fun MediaPayload.withoutEntry(id: String) = copy(entries = entries.filter { it.id != id })

    private companion object {
        const val DETAILS_KEY = "details"
    }
}
