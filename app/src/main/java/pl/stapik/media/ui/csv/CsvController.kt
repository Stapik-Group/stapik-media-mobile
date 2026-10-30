package pl.stapik.media.ui.csv

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.stapik.media.data.backup.FileTooLargeException
import pl.stapik.media.data.csv.CsvText
import pl.stapik.media.data.csv.EntryCsv
import pl.stapik.media.data.csv.EntryCsvImport
import pl.stapik.media.data.library.MediaLibrary

class CsvController(
    private val library: MediaLibrary,
    private val scope: CoroutineScope,
    private val beforeImport: suspend () -> Unit,
    private val afterImport: suspend () -> Unit,
) {
    private class Failure(val failure: CsvFailure, cause: Throwable? = null) : Exception(cause?.message, cause)

    private val mutableState = MutableStateFlow<CsvState>(CsvState.Idle)

    val state: StateFlow<CsvState> = mutableState.asStateFlow()

    fun export(header: List<String>, separator: Char, write: suspend (ByteArray) -> Unit) = run(CsvOperation.EXPORT) {
        val snapshot = library.snapshot()
        val text = CsvText.write(EntryCsv.table(snapshot.payload.entries, header), separator)
        write(text.toByteArray(Charsets.UTF_8))
        CsvState.Finished(CsvOutcome.Exported(snapshot.payload.entries.size, snapshot.unavailableYears))
    }

    fun open(read: suspend () -> ByteArray) = run(CsvOperation.IMPORT) {
        val bytes = try {
            read()
        } catch (e: FileTooLargeException) {
            throw Failure(CsvFailure.TOO_LARGE, e)
        } catch (e: IOException) {
            throw Failure(CsvFailure.UNREADABLE, e)
        }
        val table = CsvText.parse(bytes.toString(Charsets.UTF_8)) ?: throw Failure(CsvFailure.MALFORMED)
        val parsed = EntryCsvImport.parse(table)
        if (parsed.isEmpty()) throw Failure(CsvFailure.EMPTY)
        val verdicts = library.previewImport(parsed.mapNotNull { it.entry }).iterator()
        val rows = parsed.map { row ->
            row.entry?.let { CsvPreviewRow.Candidate(it, verdicts.next()) } ?: CsvPreviewRow.Invalid(row.number, row.title, row.problem!!)
        }
        CsvState.Previewing(CsvPreview(rows))
    }

    fun confirmImport() {
        val pending = mutableState.value as? CsvState.Previewing ?: return
        run(CsvOperation.IMPORT) {
            beforeImport()
            val added = library.importEntries(pending.preview.candidates)
            afterImport()
            CsvState.Finished(CsvOutcome.Imported(added.size))
        }
    }

    fun dismiss() {
        val current = mutableState.value
        if (current !is CsvState.Working) mutableState.compareAndSet(current, CsvState.Idle)
    }

    private fun run(operation: CsvOperation, body: suspend () -> CsvState) {
        val before = mutableState.value
        if (before is CsvState.Working || before is CsvState.Finished) return
        if (!mutableState.compareAndSet(before, CsvState.Working)) return
        scope.launch { mutableState.value = outcomeOf(operation, body) }
    }

    private suspend fun outcomeOf(operation: CsvOperation, body: suspend () -> CsvState): CsvState =
        try {
            body()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Failure) {
            CsvState.Finished(CsvOutcome.Failed(operation, e.failure, e.message.orEmpty()))
        } catch (e: Exception) {
            CsvState.Finished(CsvOutcome.Failed(operation, CsvFailure.UNREADABLE, e.message.orEmpty()))
        }
}
