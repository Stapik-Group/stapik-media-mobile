package pl.stapik.media.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.stapik.media.data.serialization.LocalDateSerializer
import java.time.LocalDate

@Serializable
enum class DatePrecision {
    @SerialName("day") DAY,
    @SerialName("month") MONTH,
    @SerialName("year") YEAR,
}

@Serializable
data class ReleaseDate(
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val precision: DatePrecision,
)