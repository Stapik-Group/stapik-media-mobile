package pl.stapik.media.ui.entry

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails

class EditorTargetTest {
    private val entry = MediaEntry(
        category = MediaCategory.ALBUM,
        title = "Kind of Blue",
        releaseDate = ReleaseDate(LocalDate.of(1959, 8, 17), DatePrecision.DAY),
        consumed = ConsumedDate(month = 2, year = 2024),
        details = MediaDetails.Album(performer = "Miles Davis", publisher = "Columbia", genre = "Jazz"),
        id = "a1",
    )

    @Test
    fun aNewEntryTargetKeepsItsCategory() {
        val target = EditorTarget.New(MediaCategory.GAME)

        assertEquals(target, decodeEditorTarget(encodeEditorTarget(target)!!))
    }

    @Test
    fun anEditedEntryTargetKeepsTheWholeEntry() {
        val target = EditorTarget.Edit(entry)

        assertEquals(target, decodeEditorTarget(encodeEditorTarget(target)!!))
    }

    @Test
    fun noTargetIsSavedAsNothing() {
        assertNull(encodeEditorTarget(null))
    }

    @Test
    fun unreadableSavedStateMeansNoTarget() {
        assertNull(decodeEditorTarget("new:SPACESHIP"))
        assertNull(decodeEditorTarget("edit:{}"))
        assertNull(decodeEditorTarget("something else"))
    }
}
