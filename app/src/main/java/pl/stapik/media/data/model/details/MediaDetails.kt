package pl.stapik.media.data.model.details

sealed interface MediaDetails {

    data class Screen(
        val director: String,
        val genre: String,
    ) : MediaDetails

    data class Book(
        val author: String,
        val genre: String,
        val isAudiobook: Boolean,
    ) : MediaDetails

    data class Album(
        val performer: String,
        val publisher: String,
        val genre: String,
    ) : MediaDetails

    data class Game(
        val studio: String,
        val publisher: String,
        val platform: String,
    ) : MediaDetails
}
