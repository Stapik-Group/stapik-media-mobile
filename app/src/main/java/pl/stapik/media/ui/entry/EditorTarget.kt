package pl.stapik.media.ui.entry

import androidx.compose.runtime.saveable.Saver
import pl.stapik.media.data.model.MediaCategory
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.ui.media.decodeEntryState
import pl.stapik.media.ui.media.encodeEntryState

sealed interface EditorTarget {
    data class New(val category: MediaCategory) : EditorTarget
    data class Edit(val entry: MediaEntry) : EditorTarget
}

private const val NEW_PREFIX = "new:"
private const val EDIT_PREFIX = "edit:"

fun encodeEditorTarget(target: EditorTarget?): String? = when (target) {
    null -> null
    is EditorTarget.New -> NEW_PREFIX + target.category.name
    is EditorTarget.Edit -> EDIT_PREFIX + encodeEntryState(target.entry)
}

fun decodeEditorTarget(text: String): EditorTarget? = when {
    text.startsWith(NEW_PREFIX) ->
        MediaCategory.entries.firstOrNull { it.name == text.removePrefix(NEW_PREFIX) }?.let(EditorTarget::New)
    text.startsWith(EDIT_PREFIX) -> decodeEntryState(text.removePrefix(EDIT_PREFIX))?.let(EditorTarget::Edit)
    else -> null
}

val EditorTargetSaver: Saver<EditorTarget?, String> = Saver(
    save = { target -> encodeEditorTarget(target) },
    restore = ::decodeEditorTarget,
)
