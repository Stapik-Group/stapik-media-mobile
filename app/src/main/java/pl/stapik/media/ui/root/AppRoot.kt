package pl.stapik.media.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.stapik.media.R
import pl.stapik.media.data.config.ApiConfig
import pl.stapik.media.data.config.ApiConfigStorage
import pl.stapik.media.ui.about.AboutScreen
import pl.stapik.media.ui.connect.ConnectScreen
import pl.stapik.media.ui.connect.ConnectStatus
import pl.stapik.media.ui.media.MediaPagerScreen
import pl.stapik.media.ui.media.MediaUiState
import pl.stapik.media.ui.media.MediaViewModel
import pl.stapik.media.ui.theme.AppTheme
import pl.stapik.media.ui.theme.colorsFor

@Composable
fun AppRoot(
    configStorage: ApiConfigStorage,
    viewModel: MediaViewModel,
    theme: AppTheme = AppTheme.CLASSIC,
) {
    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Media) }
    var savedConfig by remember { mutableStateOf<ApiConfig?>(null) }
    var connectStatus by remember { mutableStateOf<ConnectStatus>(ConnectStatus.Idle) }
    val scope = rememberCoroutineScope()
    val scheme = colorsFor(theme)

    LaunchedEffect(Unit) {
        savedConfig = configStorage.load()
        if (savedConfig == null) screen = AppScreen.Connect
    }

    // Only intercept back on Connect if there's already a working config to
    // fall back to - on first run (no config yet) there's nowhere else to go.
    BackHandler(enabled = screen is AppScreen.Connect && savedConfig != null) {
        screen = AppScreen.Media
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { screen = AppScreen.Connect }) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_content_description))
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (screen) {
                is AppScreen.Media -> MediaPagerScreen(
                    viewModel = viewModel,
                    scheme = scheme,
                    modifier = Modifier.fillMaxSize(),
                )

                is AppScreen.Connect -> ConnectScreen(
                    initialConfig = savedConfig,
                    status = connectStatus,
                    onSave = { config ->
                        scope.launch {
                            connectStatus = ConnectStatus.Testing
                            configStorage.save(config)
                            savedConfig = config
                            viewModel.refresh()

                            when (val result = viewModel.uiState.first { it !is MediaUiState.Loading }) {
                                is MediaUiState.Success -> if (result.isStale) {
                                    connectStatus = ConnectStatus.Stale
                                } else {
                                    connectStatus = ConnectStatus.Idle
                                    screen = AppScreen.Media
                                }

                                is MediaUiState.Error -> connectStatus = ConnectStatus.Error(result.message)
                                is MediaUiState.NotConnected -> connectStatus = ConnectStatus.Error("unexpected state")
                                is MediaUiState.Loading -> Unit
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                is AppScreen.About -> AboutScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}