package pl.stapik.media.ui.media

import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.util.foldForSearch

enum class SortField(val key: String) {
    TITLE("title"),
    RELEASE_DATE("releaseDate"),
    CONSUMED("consumed"),
    ;

    companion object {
        fun fromKey(key: String?): SortField? = entries.firstOrNull { it.key == key }
    }
}

enum class SortDirection(val key: String) {
    ASCENDING("ascending"),
    DESCENDING("descending"),
    ;

    companion object {
        fun fromKey(key: String?): SortDirection? = entries.firstOrNull { it.key == key }
    }
}

data class EntrySort(
    val field: SortField = SortField.CONSUMED,
    val direction: SortDirection = SortDirection.DESCENDING,
) {
    val isAscending: Boolean get() = direction == SortDirection.ASCENDING

    fun withReversedDirection(): EntrySort =
        copy(direction = if (isAscending) SortDirection.DESCENDING else SortDirection.ASCENDING)

    fun comparator(): Comparator<MediaEntry> {
        val ascending: Comparator<MediaEntry> = when (field) {
            SortField.TITLE -> compareBy<MediaEntry> { foldForSearch(it.title) }
            SortField.RELEASE_DATE -> compareBy<MediaEntry> { it.releaseDate.date }
            SortField.CONSUMED -> compareBy<MediaEntry> { it.consumed.year }.thenBy { it.consumed.month }
        }
        return if (isAscending) ascending else ascending.reversed()
    }
}
