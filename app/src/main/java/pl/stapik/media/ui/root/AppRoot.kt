package pl.stapik.media.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pl.stapik.media.R
import pl.stapik.media.data.config.ApiConfig
import pl.stapik.media.data.config.ApiConfigStorage
import pl.stapik.media.data.config.ApiSchemaGuard
import pl.stapik.media.ui.about.AboutScreen
import pl.stapik.media.ui.connect.ConnectScreen
import pl.stapik.media.ui.connect.ConnectStatus
import pl.stapik.media.ui.media.MediaPagerScreen
import pl.stapik.media.ui.media.MediaUiState
import pl.stapik.media.ui.media.MediaViewModel
import pl.stapik.media.ui.theme.AppTheme
import pl.stapik.media.ui.theme.ThemePreferenceStorage
import pl.stapik.media.ui.theme.ThemeScreen
import pl.stapik.media.ui.theme.colorsFor
import pl.stapik.media.ui.theme.toMaterialColorScheme

@Composable
fun AppRoot(
    configStorage: ApiConfigStorage,
    themeStorage: ThemePreferenceStorage,
    viewModel: MediaViewModel,
) {
    val context = LocalContext.current
    val apiSchemaGuard = remember { ApiSchemaGuard(context.applicationContext, configStorage) }
    var schemaChecked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apiSchemaGuard.ensureCurrentSchema()
        schemaChecked = true
    }

    if (!schemaChecked) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Media) }
    var savedConfig by remember { mutableStateOf<ApiConfig?>(null) }
    var connectStatus by remember { mutableStateOf<ConnectStatus>(ConnectStatus.Idle) }
    var currentTheme by remember { mutableStateOf(AppTheme.CLASSIC) }
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scheme = colorsFor(currentTheme)

    LaunchedEffect(Unit) {
        savedConfig = configStorage.load()
        currentTheme = themeStorage.load()
        if (savedConfig == null) screen = AppScreen.Connect
    }

    val canGoBackToMedia = screen != AppScreen.Media && !(screen is AppScreen.Connect && savedConfig == null)
    BackHandler(enabled = canGoBackToMedia) { screen = AppScreen.Media }

    MaterialTheme(colorScheme = scheme.toMaterialColorScheme()) {
        Scaffold(
            floatingActionButton = {
                Box {
                    FloatingActionButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_content_description))
                    }

                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_connect)) },
                            onClick = { menuExpanded = false; screen = AppScreen.Connect },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_theme_label)) },
                            onClick = { menuExpanded = false; screen = AppScreen.Theme },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_about)) },
                            onClick = { menuExpanded = false; screen = AppScreen.About },
                        )
                    }
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
                        scheme = scheme,
                        onBack = if (savedConfig != null) { { screen = AppScreen.Media } } else null,
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

                    is AppScreen.Theme -> ThemeScreen(
                        currentTheme = currentTheme,
                        scheme = scheme,
                        onSelect = { theme ->
                            currentTheme = theme
                            scope.launch { themeStorage.save(theme) }
                        },
                        onBack = { screen = AppScreen.Media },
                        modifier = Modifier.fillMaxSize(),
                    )

                    is AppScreen.About -> AboutScreen(
                        scheme = scheme,
                        onBack = { screen = AppScreen.Media },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}