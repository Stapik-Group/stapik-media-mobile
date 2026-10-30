package pl.stapik.media.ui.media

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

class EntryStateTest {

    @Test
    fun anEntrySurvivesTheRoundTripThroughSavedState() {
        val entry = MediaEntry(
            category = MediaCategory.GAME,
            title = "Outer Wilds",
            releaseDate = ReleaseDate(LocalDate.of(2019, 5, 28), DatePrecision.DAY),
            consumed = ConsumedDate(month = 7, year = 2024),
            details = MediaDetails.Game(studio = "Mobius Digital", publisher = "Annapurna", platform = "PC"),
            id = "entry-1",
        )

        assertEquals(entry, decodeEntryState(encodeEntryState(entry)))
    }

    @Test
    fun savedStateThatCannotBeReadMeansNoEntry() {
        assertNull(decodeEntryState("{\"category\":\"movie\"}"))
        assertNull(decodeEntryState("not json"))
    }
}
