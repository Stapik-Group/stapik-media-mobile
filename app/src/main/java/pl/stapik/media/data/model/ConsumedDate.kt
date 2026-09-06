package pl.stapik.media.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ConsumedDate(
    val month: Int,
    val year: Int,
)