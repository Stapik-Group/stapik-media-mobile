package pl.stapik.media.ui.entry

import androidx.compose.runtime.saveable.Saver

fun entryFormSaver(initial: EntryForm): Saver<EntryForm, List<Any?>> = Saver(
    save = { it.toSaved() },
    restore = { EntryForm.fromSaved(initial, it) },
)
