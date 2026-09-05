package pl.stapik.media.ui.media

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Formats an ISO-8601 instant (e.g. "2026-09-04T19:49:24Z") for display. */
fun formatUpdatedAt(iso: String): String = try {
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    formatter.format(Instant.parse(iso))
} catch (e: Exception) {
    iso // fall back to the raw value rather than crash on an unexpected format
}