package pl.stapik.media.data.repository

import pl.stapik.media.data.model.MediaEntry

sealed interface MediaFetchOutcome {
    data class Fresh(val entries: List<MediaEntry>, val updatedAt: String) : MediaFetchOutcome
    data class Cached(val entries: List<MediaEntry>, val updatedAt: String) : MediaFetchOutcome
    data class Failure(val cause: Throwable) : MediaFetchOutcome
}
