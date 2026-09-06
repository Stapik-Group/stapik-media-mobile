package pl.stapik.media.ui.connect

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import pl.stapik.media.R
import pl.stapik.media.data.config.ApiConfig
import pl.stapik.media.ui.common.ScreenHeader
import pl.stapik.media.ui.media.MediaLoadError
import pl.stapik.media.ui.media.toDisplayMessage
import pl.stapik.media.ui.theme.RetroButton
import pl.stapik.media.ui.theme.RetroColorScheme
import pl.stapik.media.ui.theme.RetroTextField

sealed interface ConnectStatus {
    data object Idle : ConnectStatus
    data object Testing : ConnectStatus
    data object Stale : ConnectStatus
    data class Error(val error: MediaLoadError) : ConnectStatus
}

@Composable
fun ConnectScreen(
    initialConfig: ApiConfig?,
    status: ConnectStatus,
    scheme: RetroColorScheme,
    onBack: (() -> Unit)?,
    onSave: (ApiConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var serverUrl by remember { mutableStateOf(initialConfig?.serverUrl.orEmpty()) }
    var apiKey by remember { mutableStateOf(initialConfig?.apiKey.orEmpty()) }
    var apiKeyVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.connect_title), scheme = scheme, onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            RetroTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = stringResource(R.string.connect_server_url_label),
                scheme = scheme,
                keyboardType = KeyboardType.Uri,
                modifier = Modifier.fillMaxWidth(),
            )

            RetroTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = stringResource(R.string.connect_api_key_label),
                scheme = scheme,
                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingContent = {
                    Text(
                        text = stringResource(if (apiKeyVisible) R.string.connect_hide_api_key else R.string.connect_show_api_key),
                        color = scheme.accent,
                        modifier = Modifier.clickable { apiKeyVisible = !apiKeyVisible },
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )

            when (status) {
                is ConnectStatus.Testing -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.connect_testing), color = scheme.textMuted)
                }

                is ConnectStatus.Stale -> Text(
                    text = stringResource(R.string.connect_stale_warning),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )

                is ConnectStatus.Error -> Text(
                    text = stringResource(R.string.connect_error_prefix, status.error.toDisplayMessage()),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )

                ConnectStatus.Idle -> Unit
            }

            RetroButton(
                text = stringResource(R.string.connect_save_button),
                onClick = { onSave(ApiConfig(normalizeUrl(serverUrl.trim()), apiKey.trim())) },
                scheme = scheme,
                enabled = status !is ConnectStatus.Testing,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

private fun normalizeUrl(url: String): String =
    if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"