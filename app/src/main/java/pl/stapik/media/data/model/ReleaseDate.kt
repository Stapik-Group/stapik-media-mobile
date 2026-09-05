package pl.stapik.media.data.model

import java.time.LocalDate

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

data class ReleaseDate(
    val date: LocalDate,
    val precision: DatePrecision,
)
