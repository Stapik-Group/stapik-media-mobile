package pl.stapik.media.ui.backup

import pl.stapik.common.document.SafetyCopy
import pl.stapik.media.data.backup.BackupFailure
import pl.stapik.media.data.backup.BackupService
import pl.stapik.media.data.library.ReplaceReport

enum class BackupOperation { EXPORT, RESTORE }

sealed interface BackupOutcome {
    data class Exported(val entries: Int, val unavailableYears: Set<Int>) : BackupOutcome
    data class Restored(val report: ReplaceReport) : BackupOutcome
    data class Failed(val operation: BackupOperation, val failure: BackupFailure, val detail: String) : BackupOutcome
}

sealed interface BackupState {
    data object Idle : BackupState
    data object Working : BackupState
    data class ConfirmRestore(val backup: BackupService.Opened) : BackupState
    data class ChooseSafetyCopy(val copies: List<SafetyCopy>) : BackupState
    data class Finished(val outcome: BackupOutcome) : BackupState
}
