package pl.stapik.media.data.repository

import pl.stapik.media.data.model.MediaEntry

/** Mirrors CalendarFetchOutcome from the calendar companion app. */
sealed interface MediaFetchOutcome {
    data class Fresh(val entries: List<MediaEntry>, val updatedAt: String) : MediaFetchOutcome
    data class Cached(val entries: List<MediaEntry>, val updatedAt: String) : MediaFetchOutcome
    data class Failure(val message: String) : MediaFetchOutcome
}
