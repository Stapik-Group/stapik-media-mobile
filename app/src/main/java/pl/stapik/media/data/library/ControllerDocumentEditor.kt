package pl.stapik.media.data.library

import pl.stapik.common.presentation.DocumentController
import pl.stapik.common.sync.SyncEngine
import pl.stapik.media.data.serialization.MediaPayload

class ControllerDocumentEditor(
    private val controller: DocumentController<MediaPayload>,
    private val engine: SyncEngine<MediaPayload>,
) : DocumentEditor {
    override suspend fun current(): MediaPayload = engine.load()

    override suspend fun edit(transform: (MediaPayload) -> MediaPayload) {
        controller.editDebounced(transform)
    }
}
