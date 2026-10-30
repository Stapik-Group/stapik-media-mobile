package pl.stapik.media

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import pl.stapik.media.ui.media.MediaViewModel
import pl.stapik.media.ui.root.AppRoot

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MediaViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MediaApp).container
        viewModel = ViewModelProvider(this, container.viewModelFactory)[MediaViewModel::class.java]

        setContent {
            AppRoot(
                configStorage = container.configStorage,
                themeStorage = container.themeStorage,
                viewModel = viewModel,
                newConnectController = container::newConnectController,
            )
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.flushPendingSync()
    }

    override fun onStart() {
        super.onStart()
        viewModel.refreshOnForeground()
    }
}
