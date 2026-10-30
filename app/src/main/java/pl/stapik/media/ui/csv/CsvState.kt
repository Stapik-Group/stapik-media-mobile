package pl.stapik.media.ui.csv

import pl.stapik.media.data.csv.CsvProblem
import pl.stapik.media.data.library.ImportVerdict
import pl.stapik.media.data.model.MediaEntry

enum class CsvFailure { UNREADABLE, TOO_LARGE, MALFORMED, EMPTY }

sealed interface CsvPreviewRow {
    data class Candidate(val entry: MediaEntry, val verdict: ImportVerdict) : CsvPreviewRow
    data class Invalid(val number: Int, val title: String, val problem: CsvProblem) : CsvPreviewRow
}

data class CsvPreview(val rows: List<CsvPreviewRow>) {
    val candidates: List<MediaEntry> = rows.filterIsInstance<CsvPreviewRow.Candidate>().map { it.entry }
    val added: Int = countOf(ImportVerdict.ADD)
    val duplicates: Int = countOf(ImportVerdict.DUPLICATE)
    val unavailable: Int = countOf(ImportVerdict.ARCHIVE_NOT_LOADED)
    val invalid: Int = rows.count { it is CsvPreviewRow.Invalid }

    private fun countOf(verdict: ImportVerdict) = rows.count { it is CsvPreviewRow.Candidate && it.verdict == verdict }
}

enum class CsvOperation { EXPORT, IMPORT }

sealed interface CsvOutcome {
    data class Exported(val entries: Int, val unavailableYears: Set<Int>) : CsvOutcome
    data class Imported(val added: Int) : CsvOutcome
    data class Failed(val operation: CsvOperation, val failure: CsvFailure, val detail: String) : CsvOutcome
}

sealed interface CsvState {
    data object Idle : CsvState
    data object Working : CsvState
    data class Previewing(val preview: CsvPreview) : CsvState
    data class Finished(val outcome: CsvOutcome) : CsvState
}
