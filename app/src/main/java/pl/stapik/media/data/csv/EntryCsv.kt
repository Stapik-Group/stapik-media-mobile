package pl.stapik.media.data.csv

import java.util.Locale
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.details.MediaDetails

object EntryCsv {
    const val COLUMN_COUNT = 11

    fun table(entries: List<MediaEntry>, header: List<String>): List<List<String>> =
        listOf(header) + entries.map(::rowOf)

    fun keyOf(category: MediaCategory): String = category.name.lowercase(Locale.ROOT)

    fun categoryOf(key: String): MediaCategory? = MediaCategory.entries.firstOrNull { keyOf(it) == key }

    private fun rowOf(entry: MediaEntry): List<String> {
        val columns = DetailColumns.of(entry.details)
        return listOf(
            keyOf(entry.category),
            entry.title,
            releaseDateText(entry),
            "%04d-%02d".format(Locale.ROOT, entry.consumed.year, entry.consumed.month),
            columns.creator,
            columns.publisher,
            columns.genre,
            columns.platform,
            columns.audiobook,
            entry.rating?.toString().orEmpty(),
            entry.note,
        )
    }

    private fun releaseDateText(entry: MediaEntry): String {
        val iso = entry.releaseDate.date.toString()
        return when (entry.releaseDate.precision) {
            DatePrecision.DAY -> iso
            DatePrecision.MONTH -> iso.take(MONTH_LENGTH)
            DatePrecision.YEAR -> iso.take(YEAR_LENGTH)
        }
    }

    private class DetailColumns(
        val creator: String = "",
        val publisher: String = "",
        val genre: String = "",
        val platform: String = "",
        val audiobook: String = "",
    ) {
        companion object {
            fun of(details: MediaDetails): DetailColumns = when (details) {
                is MediaDetails.Screen -> DetailColumns(creator = details.director, genre = details.genre)
                is MediaDetails.Book -> DetailColumns(creator = details.author, genre = details.genre, audiobook = details.isAudiobook.toString())
                is MediaDetails.Album -> DetailColumns(creator = details.performer, publisher = details.publisher, genre = details.genre)
                is MediaDetails.Game -> DetailColumns(creator = details.studio, publisher = details.publisher, platform = details.platform)
            }
        }
    }

    private const val YEAR_LENGTH = 4
    private const val MONTH_LENGTH = 7
}
