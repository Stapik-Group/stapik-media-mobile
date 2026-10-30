package pl.stapik.media.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import pl.stapik.common.ui.components.StapikButton
import pl.stapik.common.ui.components.StapikDialog
import pl.stapik.common.ui.components.StapikOptionRow
import pl.stapik.media.R
import pl.stapik.media.data.backup.BackupFailure
import pl.stapik.media.data.backup.ContentFiles
import pl.stapik.media.ui.media.formatUpdatedAt

private const val ARCHIVE_MIME_TYPE = "application/zip"
private const val ANY_MIME_TYPE = "*/*"
private const val ARCHIVE_NAME_PREFIX = "stapikmedia-"
private const val ARCHIVE_EXTENSION = ".zip"

@Composable
fun BackupSection(controller: BackupController) {
    val state by controller.state.collectAsState()
    val resolver = LocalContext.current.applicationContext.contentResolver
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(ARCHIVE_MIME_TYPE)) { uri ->
        uri?.let { controller.export { bytes -> ContentFiles.write(resolver, it, bytes) } }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { controller.open { ContentFiles.read(resolver, it) } }
    }
    val idle = state is BackupState.Idle
    val working = state is BackupState.Working

    StapikButton(
        text = stringResource(R.string.backup_export),
        onClick = { exportLauncher.launch("$ARCHIVE_NAME_PREFIX${LocalDate.now()}$ARCHIVE_EXTENSION") },
        modifier = Modifier.fillMaxWidth(),
        enabled = idle,
        busy = working,
    )
    StapikButton(
        text = stringResource(R.string.backup_restore),
        onClick = { importLauncher.launch(arrayOf(ANY_MIME_TYPE)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = idle,
    )
    StapikButton(
        text = stringResource(R.string.backup_safety_copies),
        onClick = controller::showSafetyCopies,
        modifier = Modifier.fillMaxWidth(),
        enabled = idle,
    )

    when (val current = state) {
        BackupState.Idle, BackupState.Working -> Unit
        is BackupState.ConfirmRestore -> StapikDialog(
            title = stringResource(R.string.backup_restore_confirm_title),
            confirmLabel = stringResource(R.string.backup_restore_button),
            onConfirm = controller::confirmRestore,
            onDismiss = controller::dismiss,
            dismissLabel = stringResource(R.string.entry_cancel),
        ) {
            Text(stringResource(R.string.backup_restore_confirm_text, current.backup.entries))
        }

        is BackupState.ChooseSafetyCopy -> StapikDialog(
            title = stringResource(R.string.backup_safety_copies),
            confirmLabel = stringResource(R.string.backup_ok),
            onConfirm = controller::dismiss,
            onDismiss = controller::dismiss,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (current.copies.isEmpty()) {
                    Text(stringResource(R.string.backup_safety_none))
                } else {
                    Text(stringResource(R.string.backup_safety_hint))
                    current.copies.forEach { copy ->
                        StapikOptionRow(
                            label = formatUpdatedAt(copy.createdAt.toString()),
                            selected = false,
                            onClick = { controller.openSafetyCopy(copy) },
                        )
                    }
                }
            }
        }

        is BackupState.Finished -> StapikDialog(
            title = outcomeTitle(current.outcome),
            confirmLabel = stringResource(R.string.backup_ok),
            onConfirm = controller::dismiss,
            onDismiss = controller::dismiss,
        ) {
            Text(outcomeText(current.outcome))
        }
    }
}

@Composable
private fun outcomeTitle(outcome: BackupOutcome): String = stringResource(
    when (outcome) {
        is BackupOutcome.Exported -> R.string.backup_export_done_title
        is BackupOutcome.Restored -> R.string.backup_restore_done_title
        is BackupOutcome.Failed -> when (outcome.operation) {
            BackupOperation.EXPORT -> R.string.backup_export_failed_title
            BackupOperation.RESTORE -> R.string.backup_restore_failed_title
        }
    },
)

@Composable
private fun outcomeText(outcome: BackupOutcome): String = when (outcome) {
    is BackupOutcome.Exported -> {
        val saved = stringResource(R.string.backup_export_done_text, outcome.entries)
        if (outcome.unavailableYears.isEmpty()) {
            saved
        } else {
            saved + "\n\n" + stringResource(R.string.backup_export_missing_archives, outcome.unavailableYears.sorted().joinToString(", "))
        }
    }

    is BackupOutcome.Restored -> {
        val restored = stringResource(R.string.backup_restore_done_text, outcome.report.applied)
        if (outcome.report.skipped == 0) {
            restored
        } else {
            restored + "\n\n" + stringResource(
                R.string.backup_restore_skipped_text,
                outcome.report.skipped,
                outcome.report.skippedByYear.keys.sorted().joinToString(", "),
            )
        }
    }

    is BackupOutcome.Failed -> failureText(outcome)
}

@Composable
private fun failureText(failure: BackupOutcome.Failed): String {
    val reason = failure.detail.ifBlank { stringResource(R.string.backup_unknown_reason) }
    return when {
        failure.operation == BackupOperation.EXPORT -> stringResource(R.string.backup_export_failed_text, reason)
        failure.failure == BackupFailure.UNREADABLE -> stringResource(R.string.backup_failed_unreadable, reason)
        failure.failure == BackupFailure.NOT_A_BACKUP -> stringResource(R.string.backup_failed_not_a_backup)
        failure.failure == BackupFailure.WRONG_APPLICATION -> stringResource(R.string.backup_failed_wrong_application)
        failure.failure == BackupFailure.NEWER_VERSION -> stringResource(R.string.backup_failed_newer_version)
        else -> stringResource(R.string.backup_failed_damaged)
    }
}
