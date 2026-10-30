package pl.stapik.media.ui.entry

import java.util.TreeMap
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.details.MediaDetails
import pl.stapik.media.util.foldCase

object EntrySuggestions {
    data class Lists(
        val creators: List<String> = emptyList(),
        val publishers: List<String> = emptyList(),
        val genres: List<String> = emptyList(),
        val platforms: List<String> = emptyList(),
    )

    private enum class Family { SCREEN, BOOK, ALBUM, GAME }

    private class Tally {
        private val spellings = HashMap<String, TreeMap<String, Int>>()

        fun add(value: String) {
            val trimmed = value.trim()
            if (trimmed.isEmpty()) return
            val counts = spellings.getOrPut(foldCase(trimmed)) { TreeMap() }
            counts[trimmed] = (counts[trimmed] ?: 0) + 1
        }

        fun ranked(): List<String> =
            spellings.entries
                .map { (key, counts) -> Triple(key, counts.entries.maxByOrNull { it.value }!!.key, counts.values.sum()) }
                .sortedWith(compareByDescending<Triple<String, String, Int>> { it.third }.thenBy { it.first })
                .map { it.second }
    }

    fun collect(entries: List<MediaEntry>, category: MediaCategory): Lists {
        val family = familyOf(category)
        val creators = Tally()
        val publishers = Tally()
        val genres = Tally()
        val platforms = Tally()

        for (entry in entries) {
            if (familyOf(entry.category) != family) continue
            when (val details = entry.details) {
                is MediaDetails.Screen -> {
                    creators.add(details.director)
                    genres.add(details.genre)
                }
                is MediaDetails.Book -> {
                    creators.add(details.author)
                    genres.add(details.genre)
                }
                is MediaDetails.Album -> {
                    creators.add(details.performer)
                    publishers.add(details.publisher)
                    genres.add(details.genre)
                }
                is MediaDetails.Game -> {
                    creators.add(details.studio)
                    publishers.add(details.publisher)
                    platforms.add(details.platform)
                }
            }
        }
        return Lists(creators.ranked(), publishers.ranked(), genres.ranked(), platforms.ranked())
    }

    private fun familyOf(category: MediaCategory): Family = when (category) {
        MediaCategory.MOVIE, MediaCategory.SERIES, MediaCategory.CARTOON -> Family.SCREEN
        MediaCategory.BOOK -> Family.BOOK
        MediaCategory.ALBUM -> Family.ALBUM
        MediaCategory.GAME -> Family.GAME
    }
}
