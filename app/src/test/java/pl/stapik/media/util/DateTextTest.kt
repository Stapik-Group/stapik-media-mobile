package pl.stapik.media.util

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.ReleaseDate

class DateTextTest {
    private val patterns: DatePatterns = { _, skeleton ->
        when (skeleton) {
            "yMMMd" -> "d MMM y"
            "yMMM" -> "MMM y"
            "yMMMM" -> "LLLL y"
            else -> skeleton
        }
    }

    private fun release(precision: DatePrecision) = ReleaseDate(LocalDate.of(2021, 10, 22), precision)

    @Test
    fun aReleaseDateFollowsItsPrecision() {
        assertEquals("22 Oct 2021", formatReleaseDate(release(DatePrecision.DAY), Locale.ENGLISH, patterns))
        assertEquals("Oct 2021", formatReleaseDate(release(DatePrecision.MONTH), Locale.ENGLISH, patterns))
        assertEquals("2021", formatReleaseDate(release(DatePrecision.YEAR), Locale.ENGLISH, patterns))
    }

    @Test
    fun aConsumedDateShowsTheMonthNameInTheGivenLanguage() {
        assertEquals("March 2024", formatConsumedDate(ConsumedDate(month = 3, year = 2024), Locale.ENGLISH, patterns))
        assertEquals("marzec 2024", formatConsumedDate(ConsumedDate(month = 3, year = 2024), Locale.forLanguageTag("pl"), patterns))
    }

    @Test
    fun theOrderOfThePartsComesFromThePattern() {
        val japanese: DatePatterns = { _, _ -> "y'年'M'月'd'日'" }

        assertEquals("2021年10月22日", formatReleaseDate(release(DatePrecision.DAY), Locale.JAPAN, japanese))
    }

    @Test
    fun aPatternThatCannotBeUsedFallsBackToTheIsoDate() {
        val broken: DatePatterns = { _, _ -> "{{" }

        assertEquals("2021-10-22", formatReleaseDate(release(DatePrecision.DAY), Locale.ENGLISH, broken))
    }

    @Test
    fun anImpossibleConsumedMonthStillProducesText() {
        assertEquals("13/2024", formatConsumedDate(ConsumedDate(month = 13, year = 2024), Locale.ENGLISH, patterns))
    }
}
