package pl.stapik.media.data.model.details

import kotlinx.serialization.Serializable

sealed interface MediaDetails {

    @Serializable
    data class Screen(
        val director: String,
        val genre: String,
    ) : MediaDetails

    @Serializable
    data class Book(
        val author: String,
        val genre: String,
        val isAudiobook: Boolean,
    ) : MediaDetails

    @Serializable
    data class Album(
        val performer: String,
        val publisher: String,
        val genre: String,
    ) : MediaDetails

    @Serializable
    data class Game(
        val studio: String,
        val publisher: String,
        val platform: String,
    ) : MediaDetails
}