package pl.stapik.media.util

import android.text.format.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import pl.stapik.media.data.model.ConsumedDate
import pl.stapik.media.data.model.DatePrecision
import pl.stapik.media.data.model.ReleaseDate

typealias DatePatterns = (locale: Locale, skeleton: String) -> String

val systemDatePatterns: DatePatterns = { locale, skeleton -> DateFormat.getBestDateTimePattern(locale, skeleton) }

fun formatReleaseDate(
    releaseDate: ReleaseDate,
    locale: Locale = Locale.getDefault(),
    patterns: DatePatterns = systemDatePatterns,
): String {
    val skeleton = when (releaseDate.precision) {
        DatePrecision.DAY -> "yMMMd"
        DatePrecision.MONTH -> "yMMM"
        DatePrecision.YEAR -> "y"
    }
    return formatDate(releaseDate.date, skeleton, locale, patterns)
}

fun formatConsumedDate(
    consumed: ConsumedDate,
    locale: Locale = Locale.getDefault(),
    patterns: DatePatterns = systemDatePatterns,
): String =
    try {
        formatDate(LocalDate.of(consumed.year, consumed.month, 1), "yMMMM", locale, patterns)
    } catch (e: java.time.DateTimeException) {
        "${consumed.month}/${consumed.year}"
    }

private fun formatDate(date: LocalDate, skeleton: String, locale: Locale, patterns: DatePatterns): String =
    try {
        DateTimeFormatter.ofPattern(patterns(locale, skeleton), locale).format(date)
    } catch (e: IllegalArgumentException) {
        date.toString()
    }
