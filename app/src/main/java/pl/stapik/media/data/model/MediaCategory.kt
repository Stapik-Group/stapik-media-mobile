package pl.stapik.media.data.model

/**
 * Mirrors the C++ `MediaCategory` enum in stapik-media, in the same declaration
 * order: this order also drives the page order in the category pager.
 */
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
