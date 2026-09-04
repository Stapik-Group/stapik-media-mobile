package pl.stapik.media.data.model

import pl.stapik.media.data.model.details.MediaDetails

/** Mirrors the C++ `MediaEntry` struct. */
data class MediaEntry(
    val category: MediaCategory,
    val title: String,
    val releaseDate: ReleaseDate,
    val consumed: ConsumedDate,
    val details: MediaDetails,
)
