package pl.stapik.media.data.model.details

/**
 * Mirrors the C++ `MediaDetails` = std::variant<ScreenDetails, BookDetails,
 * AlbumDetails, GameDetails>. Which subtype is valid for a given entry is
 * determined by its MediaCategory (Movie/Series/Cartoon -> Screen, Book ->
 * Book, Album -> Album, Game -> Game) - see MediaEntrySerializer.
 */
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
