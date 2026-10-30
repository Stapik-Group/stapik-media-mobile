package pl.stapik.media.data.archive

sealed interface ArchiveStatus {
    data object Loading : ArchiveStatus
    data object Failed : ArchiveStatus
}
