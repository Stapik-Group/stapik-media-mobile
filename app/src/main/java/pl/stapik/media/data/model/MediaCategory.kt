package pl.stapik.media.data.model

enum class MediaCategory(val wireName: String) {
    MOVIE("movie"),
    SERIES("series"),
    CARTOON("cartoon"),
    BOOK("book"),
    ALBUM("album"),
    GAME("game");

    companion object {
        fun fromWireName(name: String): MediaCategory =
            entries.firstOrNull { it.wireName == name }
                ?: throw IllegalArgumentException("Unknown category string: $name")
    }
}
