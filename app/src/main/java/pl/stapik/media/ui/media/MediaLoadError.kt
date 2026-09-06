package pl.stapik.media.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.stapik.media.R
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface MediaLoadError {
    data object NoNetwork : MediaLoadError
    data object Unauthorized : MediaLoadError
    data object NotFound : MediaLoadError
    data object CannotVerify : MediaLoadError
    data class Unknown(val message: String) : MediaLoadError
}

fun Throwable.toMediaLoadError(): MediaLoadError = when (this) {
    is UnknownHostException, is SocketTimeoutException -> MediaLoadError.NoNetwork
    is HttpException -> when (code()) {
        401, 403 -> MediaLoadError.Unauthorized
        404 -> MediaLoadError.NotFound
        else -> MediaLoadError.Unknown(message())
    }
    else -> MediaLoadError.Unknown(message ?: this::class.simpleName ?: "Unknown error")
}

@Composable
fun MediaLoadError.toDisplayMessage(): String = when (this) {
    MediaLoadError.NoNetwork -> stringResource(R.string.error_no_network)
    MediaLoadError.Unauthorized -> stringResource(R.string.error_unauthorized)
    MediaLoadError.NotFound -> stringResource(R.string.error_not_found)
    MediaLoadError.CannotVerify -> stringResource(R.string.error_cannot_verify)
    is MediaLoadError.Unknown -> stringResource(R.string.error_unknown, message)
}