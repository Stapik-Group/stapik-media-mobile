package pl.stapik.media.data.archive

import pl.stapik.common.sync.SyncOutcome

sealed interface ArchiveStatus {
    data object Loading : ArchiveStatus
    data class Failed(val outcome: SyncOutcome?) : ArchiveStatus
}
