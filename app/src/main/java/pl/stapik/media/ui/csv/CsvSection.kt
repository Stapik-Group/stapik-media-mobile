package pl.stapik.media.ui.csv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale
import pl.stapik.common.ui.components.StapikButton
import pl.stapik.common.ui.components.StapikDialog
import pl.stapik.media.R
import pl.stapik.media.data.backup.ContentFiles
import pl.stapik.media.data.csv.CsvProblem
import pl.stapik.media.data.csv.CsvText
import pl.stapik.media.data.library.ImportVerdict
import pl.stapik.media.ui.media.displayName

private const val CSV_MIME_TYPE = "text/csv"
private const val ANY_MIME_TYPE = "*/*"
private const val CSV_FILE_NAME = "stapik-media.csv"
private const val PREVIEW_ROW_LIMIT = 500

@Composable
fun CsvSection(controller: CsvController) {
    val state by controller.state.collectAsState()
    val resolver = LocalContext.current.applicationContext.contentResolver
    val header = csvHeader()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV_MIME_TYPE)) { uri ->
        uri?.let {
            controller.export(header, CsvText.separatorFor(Locale.getDefault())) { bytes -> ContentFiles.write(resolver, it, bytes) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { controller.open { ContentFiles.read(resolver, it, ContentFiles.MAX_CSV_BYTES) } }
    }
    val idle = state is CsvState.Idle

    StapikButton(
        text = stringResource(R.string.csv_export),
        onClick = { exportLauncher.launch(CSV_FILE_NAME) },
        modifier = Modifier.fillMaxWidth(),
        enabled = idle,
        busy = state is CsvState.Working,
    )
    StapikButton(
        text = stringResource(R.string.csv_import),
        onClick = { importLauncher.launch(arrayOf(ANY_MIME_TYPE)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = idle,
    )

    when (val current = state) {
        CsvState.Idle, CsvState.Working -> Unit
        is CsvState.Previewing -> PreviewDialog(current.preview, controller)
        is CsvState.Finished -> StapikDialog(
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
private fun PreviewDialog(preview: CsvPreview, controller: CsvController) {
    val nothingToImport = preview.added == 0
    StapikDialog(
        title = stringResource(R.string.csv_preview_title),
        confirmLabel = stringResource(if (nothingToImport) R.string.backup_ok else R.string.csv_import_button),
        onConfirm = if (nothingToImport) controller::dismiss else controller::confirmImport,
        onDismiss = controller::dismiss,
        dismissLabel = if (nothingToImport) null else stringResource(R.string.entry_cancel),
    ) {
        Column(
            modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(stringResource(R.string.csv_preview_summary, preview.added, preview.duplicates, preview.invalid, preview.unavailable))
            preview.rows.take(PREVIEW_ROW_LIMIT).forEach { Text(rowText(it)) }
            if (preview.rows.size > PREVIEW_ROW_LIMIT) {
                Text(stringResource(R.string.csv_preview_more, preview.rows.size - PREVIEW_ROW_LIMIT))
            }
        }
    }
}

@Composable
private fun rowText(row: CsvPreviewRow): String = when (row) {
    is CsvPreviewRow.Candidate -> {
        val status = stringResource(
            when (row.verdict) {
                ImportVerdict.ADD -> R.string.csv_status_new
                ImportVerdict.DUPLICATE -> R.string.csv_status_duplicate
                ImportVerdict.ARCHIVE_NOT_LOADED -> R.string.csv_status_archive_not_loaded
            },
        )
        "$status  ${row.entry.category.displayName()} – ${row.entry.title} (${row.entry.releaseDate.date.year})"
    }

    is CsvPreviewRow.Invalid -> stringResource(R.string.csv_row_invalid, stringResource(problemResource(row.problem)), row.number, row.title)
}

private fun problemResource(problem: CsvProblem): Int = when (problem) {
    CsvProblem.MISSING_COLUMNS -> R.string.csv_problem_missing_columns
    CsvProblem.UNKNOWN_CATEGORY -> R.string.csv_problem_unknown_category
    CsvProblem.EMPTY_TITLE -> R.string.csv_problem_empty_title
    CsvProblem.INVALID_RELEASE_DATE -> R.string.csv_problem_invalid_release_date
    CsvProblem.INVALID_CONSUMED -> R.string.csv_problem_invalid_consumed
    CsvProblem.INVALID_AUDIOBOOK -> R.string.csv_problem_invalid_audiobook
    CsvProblem.INVALID_RATING -> R.string.csv_problem_invalid_rating
}

@Composable
private fun csvHeader(): List<String> = listOf(
    stringResource(R.string.csv_column_category),
    stringResource(R.string.csv_column_title),
    stringResource(R.string.csv_column_release_date),
    stringResource(R.string.csv_column_consumed),
    stringResource(R.string.csv_column_creator),
    stringResource(R.string.csv_column_publisher),
    stringResource(R.string.csv_column_genre),
    stringResource(R.string.csv_column_platform),
    stringResource(R.string.csv_column_audiobook),
    stringResource(R.string.csv_column_rating),
    stringResource(R.string.csv_column_note),
)

@Composable
private fun outcomeTitle(outcome: CsvOutcome): String = stringResource(
    when (outcome) {
        is CsvOutcome.Exported -> R.string.csv_export_done_title
        is CsvOutcome.Imported -> R.string.csv_import_done_title
        is CsvOutcome.Failed -> when (outcome.operation) {
            CsvOperation.EXPORT -> R.string.csv_export_failed_title
            CsvOperation.IMPORT -> R.string.csv_import_failed_title
        }
    },
)

@Composable
private fun outcomeText(outcome: CsvOutcome): String = when (outcome) {
    is CsvOutcome.Exported -> {
        val saved = stringResource(R.string.csv_export_done_text, outcome.entries)
        if (outcome.unavailableYears.isEmpty()) {
            saved
        } else {
            saved + "\n\n" + stringResource(R.string.backup_export_missing_archives, outcome.unavailableYears.sorted().joinToString(", "))
        }
    }

    is CsvOutcome.Imported -> stringResource(R.string.csv_import_done_text, outcome.added)
    is CsvOutcome.Failed -> failureText(outcome)
}

@Composable
private fun failureText(failed: CsvOutcome.Failed): String {
    if (failed.operation == CsvOperation.EXPORT) {
        return stringResource(R.string.csv_export_failed_text, failed.detail.ifBlank { stringResource(R.string.backup_unknown_reason) })
    }
    return stringResource(
        when (failed.failure) {
            CsvFailure.UNREADABLE -> R.string.csv_failed_unreadable
            CsvFailure.TOO_LARGE -> R.string.csv_failed_too_large
            CsvFailure.MALFORMED -> R.string.csv_failed_malformed
            CsvFailure.EMPTY -> R.string.csv_failed_empty
        },
    )
}
