package pl.stapik.media.ui.entry

import java.time.LocalDate
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.model.ReleaseDate
import pl.stapik.media.data.model.details.MediaDetails

class EntryFormTest {
    private val today = LocalDate.of(2026, 10, 30)

    private fun ready(form: EntryForm): MediaEntry = (form.toEntry() as FormResult.Ready).entry

    private fun problems(form: EntryForm): Set<FormProblem> = (form.toEntry() as FormResult.Invalid).problems

    @Test
    fun aBlankFormStartsAtTodayWithTheFirstDayOfTheYear() {
        val form = EntryForm.blank(MediaCategory.BOOK, today)

        assertEquals("2026", form.releaseYear)
        assertEquals("2026", form.consumedYear)
        assertEquals(10, form.consumedMonth)
        assertEquals(1, form.releaseMonth)
        assertEquals(1, form.releaseDay)
    }

    @Test
    fun aFilledFormBecomesATrimmedEntryWithDetailsOfItsCategory() {
        val form = EntryForm.blank(MediaCategory.GAME, today).copy(
            title = "  Outer Wilds ",
            creator = " Mobius Digital ",
            publisher = "Annapurna",
            platform = " PC",
            rating = 9,
            note = " great ",
        )

        val entry = ready(form)

        assertEquals("Outer Wilds", entry.title)
        assertEquals(MediaDetails.Game(studio = "Mobius Digital", publisher = "Annapurna", platform = "PC"), entry.details)
        assertEquals(9, entry.rating)
        assertEquals("great", entry.note)
        assertEquals(ConsumedDate(month = 10, year = 2026), entry.consumed)
    }

    @Test
    fun everyCategoryFamilyGetsItsOwnDetails() {
        val base = EntryForm.blank(MediaCategory.MOVIE, today).copy(title = "T", creator = "C", genre = "G", publisher = "P")

        assertEquals(MediaDetails.Screen("C", "G"), ready(base).details)
        assertEquals(MediaDetails.Book("C", "G", true), ready(base.copy(category = MediaCategory.BOOK, isAudiobook = true)).details)
        assertEquals(MediaDetails.Album("C", "P", "G"), ready(base.copy(category = MediaCategory.ALBUM)).details)
    }

    @Test
    fun anEmptyTitleAndBadYearsAreReported() {
        val form = EntryForm.blank(MediaCategory.MOVIE, today).copy(title = "  ", releaseYear = "0", consumedYear = "1899")

        assertEquals(
            setOf(FormProblem.TITLE_EMPTY, FormProblem.RELEASE_YEAR_INVALID, FormProblem.CONSUMED_YEAR_INVALID),
            problems(form),
        )
        assertEquals(setOf(FormProblem.RELEASE_YEAR_INVALID), problems(EntryForm.blank(MediaCategory.MOVIE, today).copy(title = "T", releaseYear = "")))
    }

    @Test
    fun theDayIsClampedToTheLengthOfTheMonth() {
        val form = EntryForm.blank(MediaCategory.MOVIE, today).copy(
            title = "T", releaseYear = "2023", releaseMonth = 2, releaseDay = 31, releasePrecision = DatePrecision.DAY,
        )

        assertEquals(ReleaseDate(LocalDate.of(2023, 2, 28), DatePrecision.DAY), ready(form).releaseDate)
    }

    @Test
    fun lowerPrecisionsIgnoreTheFinerParts() {
        val form = EntryForm.blank(MediaCategory.MOVIE, today).copy(title = "T", releaseYear = "2023", releaseMonth = 7, releaseDay = 15)

        assertEquals(LocalDate.of(2023, 7, 1), ready(form.copy(releasePrecision = DatePrecision.MONTH)).releaseDate.date)
        assertEquals(LocalDate.of(2023, 1, 1), ready(form.copy(releasePrecision = DatePrecision.YEAR)).releaseDate.date)
    }

    @Test
    fun editingAnEntryAndSavingItAgainChangesNothing() {
        val entry = MediaEntry(
            category = MediaCategory.BOOK,
            title = "Solaris",
            releaseDate = ReleaseDate(LocalDate.of(1961, 6, 1), DatePrecision.MONTH),
            consumed = ConsumedDate(month = 5, year = 2023),
            details = MediaDetails.Book(author = "Lem", genre = "SF", isAudiobook = true),
            id = "b1",
            rating = 9,
            note = "Great",
            extras = JsonObject(mapOf("shelf" to JsonPrimitive("A3"))),
        )

        assertEquals(entry, ready(EntryForm.from(entry)))
    }

    @Test
    fun theFormSurvivesBeingSavedAndRestored() {
        val base = EntryForm.blank(MediaCategory.ALBUM, today)
        val filled = base.copy(
            title = "Kind of Blue", releasePrecision = DatePrecision.YEAR, releaseYear = "1959", rating = 10, note = "n",
            creator = "Miles", genre = "Jazz", publisher = "Columbia", isAudiobook = false,
        )

        assertEquals(filled, EntryForm.fromSaved(base, filled.toSaved()))
    }
}
