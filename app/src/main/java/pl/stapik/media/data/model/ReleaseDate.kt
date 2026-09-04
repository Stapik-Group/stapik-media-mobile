package pl.stapik.media.data.model

import java.time.LocalDate

/** Mirrors the C++ `DatePrecision` enum. */
enum class DatePrecision(val wireName: String) {
    DAY("day"),
    MONTH("month"),
    YEAR("year");

    companion object {
        fun fromWireName(name: String): DatePrecision =
            entries.firstOrNull { it.wireName == name }
                ?: throw IllegalArgumentException("Unknown precision string: $name")
    }
}

/**
 * Mirrors the C++ `ReleaseDate` struct. The desktop app always stores a full
 * ISO date string regardless of precision (padding day/month with a
 * placeholder when unknown) - [precision] tells the UI how much of [date] is
 * actually meaningful to display.
 */
data class ReleaseDate(
    val date: LocalDate,
    val precision: DatePrecision,
)
