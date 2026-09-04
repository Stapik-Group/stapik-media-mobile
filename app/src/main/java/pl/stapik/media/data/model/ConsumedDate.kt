package pl.stapik.media.data.model

/** Mirrors the C++ `ConsumedDate` struct - month/year the entry was consumed. */
data class ConsumedDate(
    val month: Int,
    val year: Int,
)
