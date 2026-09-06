package pl.stapik.media.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MediaCategory {
    @SerialName("movie") MOVIE,
    @SerialName("series") SERIES,
    @SerialName("cartoon") CARTOON,
    @SerialName("book") BOOK,
    @SerialName("album") ALBUM,
    @SerialName("game") GAME,
}