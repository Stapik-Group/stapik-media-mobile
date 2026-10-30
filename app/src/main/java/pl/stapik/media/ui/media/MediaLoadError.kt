package pl.stapik.media.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import pl.stapik.common.i18n.Messages
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.CloudException
import pl.stapik.common.sync.SyncOutcome
import pl.stapik.common.sync.UnreadableKind
import pl.stapik.media.R

sealed interface MediaLoadError {
    data object NoNetwork : MediaLoadError
    data object Unauthorized : MediaLoadError
    data object NotFound : MediaLoadError

    /** The cloud data cannot be read (encrypted, wrong passphrase, newer format); [messageKey] is a library message key. */
    data class Unreadable(val messageKey: String) : MediaLoadError

    data class Unknown(val message: String) : MediaLoadError
}

fun Throwable.toMediaLoadError(): MediaLoadError = when (this) {
    is CloudException -> error.toMediaLoadError()
    else -> MediaLoadError.Unknown(message ?: this::class.simpleName ?: "Unknown error")
}

fun CloudError.toMediaLoadError(): MediaLoadError = when (this) {
    CloudError.Unauthorized, CloudError.Forbidden -> MediaLoadError.Unauthorized
    is CloudError.Network -> MediaLoadError.NoNetwork
    is CloudError.Unreadable -> MediaLoadError.Unreadable(unreadableKey(kind))
    is CloudError.ServerError -> MediaLoadError.Unknown("HTTP $httpCode")
    is CloudError.RateLimited -> MediaLoadError.Unknown("HTTP 429")
    CloudError.Conflict -> MediaLoadError.Unknown("Conflict")
}

fun SyncOutcome.toMediaLoadError(): MediaLoadError? = when (this) {
    is SyncOutcome.Offline -> cause.toMediaLoadError()
    is SyncOutcome.AuthProblem -> MediaLoadError.Unauthorized
    is SyncOutcome.RemoteUnreadable -> MediaLoadError.Unreadable(unreadableKey(kind))
    is SyncOutcome.NewerSchema -> MediaLoadError.Unknown(this::class.simpleName.orEmpty())
    is SyncOutcome.NeedsFirstConnectionChoice, SyncOutcome.LocalOnly, SyncOutcome.Synchronized, SyncOutcome.ServerWon,
    SyncOutcome.LocalWon, SyncOutcome.KeptLocalUnsynced -> null
}

private fun unreadableKey(kind: UnreadableKind): String = when (kind) {
    UnreadableKind.PASSPHRASE_REQUIRED -> "sync.unreadable.passphrase_required"
    UnreadableKind.DECRYPTION_FAILED -> "sync.unreadable.wrong_passphrase"
    UnreadableKind.UNSUPPORTED_VERSION -> "sync.unreadable.newer_version"
    UnreadableKind.OTHER -> "sync.unreadable.other"
}

@Composable
fun MediaLoadError.toDisplayMessage(): String = when (this) {
    MediaLoadError.NoNetwork -> stringResource(R.string.error_no_network)
    MediaLoadError.Unauthorized -> stringResource(R.string.error_unauthorized)
    MediaLoadError.NotFound -> stringResource(R.string.error_not_found)
    is MediaLoadError.Unreadable -> Messages.text(LocalContext.current, messageKey)
    is MediaLoadError.Unknown -> stringResource(R.string.error_unknown, message)
}
