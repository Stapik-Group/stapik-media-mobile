package pl.stapik.media

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import pl.stapik.media.ui.media.MediaViewModel
import pl.stapik.media.ui.root.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MediaApp).container
        val viewModel = ViewModelProvider(this, container.viewModelFactory)[MediaViewModel::class.java]

        setContent {
            AppRoot(
                configStorage = container.configStorage,
                themeStorage = container.themeStorage,
                viewModel = viewModel,
                newConnectController = container::newConnectController,
            )
        }
    }
}
