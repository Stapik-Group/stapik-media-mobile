package pl.stapik.media.ui.media

import androidx.compose.runtime.saveable.Saver
import kotlinx.serialization.json.Json
import pl.stapik.media.data.model.MediaEntry

fun encodeEntryState(entry: MediaEntry): String = Json.encodeToString(MediaEntry.serializer(), entry)

fun decodeEntryState(text: String): MediaEntry? =
    try {
        Json.decodeFromString(MediaEntry.serializer(), text)
    } catch (e: Exception) {
        null
    }

val MediaEntryStateSaver: Saver<MediaEntry?, String> = Saver(
    save = { entry -> entry?.let(::encodeEntryState) },
    restore = ::decodeEntryState,
)
