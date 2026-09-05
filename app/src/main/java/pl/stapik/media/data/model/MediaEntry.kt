package pl.stapik.media.data.model

import pl.stapik.media.data.model.details.MediaDetails

data class MediaEntry(
    val category: MediaCategory,
    val title: String,
    val releaseDate: ReleaseDate,
    val consumed: ConsumedDate,
    val details: MediaDetails,
)
