package pl.stapik.media.data.backup

enum class BackupFailure { NOT_A_BACKUP, WRONG_APPLICATION, NEWER_VERSION, DAMAGED, UNREADABLE }

class BackupException(val failure: BackupFailure, cause: Throwable? = null) : Exception(cause?.message ?: failure.name, cause)
