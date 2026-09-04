package pl.stapik.media

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import pl.stapik.media.data.cache.DataStoreMediaCacheStorage
import pl.stapik.media.data.config.DataStoreApiConfigStorage
import pl.stapik.media.data.repository.MediaRepository
import pl.stapik.media.ui.media.MediaViewModel
import pl.stapik.media.ui.root.AppRoot
import pl.stapik.media.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configStorage = DataStoreApiConfigStorage(applicationContext)
        val cacheStorage = DataStoreMediaCacheStorage(applicationContext)
        val repository = MediaRepository(cacheStorage)

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
                viewModel = viewModel,
                theme = AppTheme.CLASSIC, // TODO: persist user's theme choice, mirroring desktop's Settings -> Theme
            )
        }
    }
}
