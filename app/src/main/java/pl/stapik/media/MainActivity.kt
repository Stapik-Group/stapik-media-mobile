package pl.stapik.media

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import pl.stapik.media.data.cache.DataStoreMediaCacheStorage
import pl.stapik.media.data.config.ConnectionApiConfigStorage
import pl.stapik.media.data.repository.MediaRepository
import pl.stapik.media.ui.media.MediaViewModel
import pl.stapik.media.ui.root.AppRoot
import pl.stapik.media.ui.theme.DataStoreThemePreferenceStorage
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.presentation.ConnectController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val configStorage = ConnectionApiConfigStorage(applicationContext)
        val themeStorage = DataStoreThemePreferenceStorage(applicationContext)
        val cacheStorage = DataStoreMediaCacheStorage(applicationContext)
        val transport = OkHttpTransport()
        val repository = MediaRepository(cacheStorage, transport)

        val viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    @Suppress("UNCHECKED_CAST")
                    return MediaViewModel(repository, configStorage) as T
                }
            },
        )[MediaViewModel::class.java]

        setContent {
            AppRoot(
                configStorage = configStorage,
                themeStorage = themeStorage,
                viewModel = viewModel,
                newConnectController = { initial ->
                    ConnectController(configStorage.store, { config -> HttpCloudGateway(config, transport) }, initial)
                },
            )
        }
    }
}