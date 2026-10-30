package pl.stapik.media.ui.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EntrySortTest {

    @Test
    fun everyFieldAndDirectionIsFoundByItsOwnKey() {
        SortField.entries.forEach { assertEquals(it, SortField.fromKey(it.key)) }
        SortDirection.entries.forEach { assertEquals(it, SortDirection.fromKey(it.key)) }
    }

    @Test
    fun keysMatchTheOnesTheDesktopStores() {
        assertEquals(listOf("title", "releaseDate", "consumed"), SortField.entries.map { it.key })
        assertEquals(listOf("ascending", "descending"), SortDirection.entries.map { it.key })
    }

    @Test
    fun unknownKeysMeanNoValue() {
        assertNull(SortField.fromKey("rating"))
        assertNull(SortDirection.fromKey(null))
    }

    @Test
    fun reversingTheDirectionTwiceGivesTheSameSort() {
        val sort = EntrySort(SortField.TITLE, SortDirection.ASCENDING)

        assertEquals(sort, sort.withReversedDirection().withReversedDirection())
    }
}
