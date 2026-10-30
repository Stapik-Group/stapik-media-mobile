package pl.stapik.media.ui.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.stapik.common.document.SafetyCopy
import pl.stapik.media.data.backup.BackupException
import pl.stapik.media.data.backup.BackupFailure
import pl.stapik.media.data.backup.BackupService
import pl.stapik.media.data.backup.FileTooLargeException

class BackupController(
    private val service: BackupService,
    private val scope: CoroutineScope,
    private val afterRestore: suspend () -> Unit,
) {
    private val mutableState = MutableStateFlow<BackupState>(BackupState.Idle)

    val state: StateFlow<BackupState> = mutableState.asStateFlow()

    fun export(write: suspend (ByteArray) -> Unit) = run(BackupOperation.EXPORT) {
        val exported = service.export()
        write(exported.bytes)
        BackupState.Finished(BackupOutcome.Exported(exported.entries, exported.unavailableYears))
    }

    fun open(read: suspend () -> ByteArray) = run(BackupOperation.RESTORE) {
        BackupState.ConfirmRestore(service.open(read()))
    }

    fun showSafetyCopies() = run(BackupOperation.RESTORE) {
        BackupState.ChooseSafetyCopy(service.savedCopies())
    }

    fun openSafetyCopy(copy: SafetyCopy) = run(BackupOperation.RESTORE) {
        BackupState.ConfirmRestore(service.openSafetyCopy(copy))
    }

    fun confirmRestore() {
        val pending = mutableState.value as? BackupState.ConfirmRestore ?: return
        run(BackupOperation.RESTORE) {
            val report = service.restore(pending.backup)
            afterRestore()
            BackupState.Finished(BackupOutcome.Restored(report))
        }
    }

    fun dismiss() {
        val current = mutableState.value
        if (current !is BackupState.Working) mutableState.compareAndSet(current, BackupState.Idle)
    }

    private fun run(operation: BackupOperation, body: suspend () -> BackupState) {
        val before = mutableState.value
        if (before is BackupState.Working || before is BackupState.Finished) return
        if (!mutableState.compareAndSet(before, BackupState.Working)) return
        scope.launch { mutableState.value = outcomeOf(operation, body) }
    }

    private suspend fun outcomeOf(operation: BackupOperation, body: suspend () -> BackupState): BackupState =
        try {
            body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: FileTooLargeException) {
            BackupState.Finished(BackupOutcome.Failed(operation, BackupFailure.NOT_A_BACKUP, e.message.orEmpty()))
        } catch (e: BackupException) {
            BackupState.Finished(BackupOutcome.Failed(operation, e.failure, e.message.orEmpty()))
        } catch (e: Exception) {
            BackupState.Finished(BackupOutcome.Failed(operation, BackupFailure.UNREADABLE, e.message.orEmpty()))
        }
}
