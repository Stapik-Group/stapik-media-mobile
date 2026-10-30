package pl.stapik.media.data.library

import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.serialization.MediaPayload

enum class EntryProblem { TITLE_MISSING, RATING_OUT_OF_RANGE, MONTH_OUT_OF_RANGE }

sealed interface WriteRefusal {
    data object Duplicate : WriteRefusal
    data object UnknownEntry : WriteRefusal
    data class ArchiveNotLoaded(val year: Int) : WriteRefusal
    data class Invalid(val problem: EntryProblem) : WriteRefusal
}

sealed interface WriteResult {
    data class Done(val entry: MediaEntry) : WriteResult
    data class Refused(val refusal: WriteRefusal) : WriteResult
}

data class LibrarySnapshot(val payload: MediaPayload, val unavailableYears: Set<Int>)

data class ReplaceReport(val applied: Int, val skippedByYear: Map<Int, Int>) {
    val skipped: Int get() = skippedByYear.values.sum()
}
