package pl.stapik.media.data.library

import pl.stapik.media.data.serialization.MediaPayload

interface DocumentEditor {
    suspend fun current(): MediaPayload

    suspend fun edit(transform: (MediaPayload) -> MediaPayload)
}

interface ArchiveDirectory {
    fun loadedYears(): Set<Int>

    fun remoteYears(): Set<Int>?

    fun editorFor(year: Int): DocumentEditor
}
