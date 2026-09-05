package pl.stapik.media.ui.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import pl.stapik.media.ui.theme.RetroColorScheme

sealed interface ConnectStatus {
    data object Idle : ConnectStatus
    data object Testing : ConnectStatus
    data object Stale : ConnectStatus
    data class Error(val message: String) : ConnectStatus
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

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text(stringResource(R.string.connect_server_url_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.accent),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.connect_api_key_label)) },
                singleLine = true,
                visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Text(stringResource(if (apiKeyVisible) R.string.connect_hide_api_key else R.string.connect_show_api_key))
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = scheme.accent),
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
                    text = stringResource(R.string.connect_error_prefix, status.message),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )

                ConnectStatus.Idle -> Unit
            }

            Button(
                onClick = { onSave(ApiConfig(normalizeUrl(serverUrl.trim()), apiKey.trim())) },
                enabled = status !is ConnectStatus.Testing,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(stringResource(R.string.connect_save_button))
            }
        }
    }
}

private fun normalizeUrl(url: String): String =
    if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"