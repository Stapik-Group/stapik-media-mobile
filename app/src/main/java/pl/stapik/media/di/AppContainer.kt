package pl.stapik.media.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import java.io.File
import java.time.Clock
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.crypto.KeystoreCiphers
import pl.stapik.common.document.FileDocumentStore
import pl.stapik.common.model.SlotKey
import pl.stapik.common.presentation.ConnectController
import pl.stapik.common.presentation.DocumentController
import pl.stapik.common.sync.SyncEngine
import pl.stapik.media.data.cloud.CloudGatewayProvider
import pl.stapik.media.data.config.ApiConfig
import pl.stapik.media.data.config.ApiConfigStorage
import pl.stapik.media.data.config.ConnectionApiConfigStorage
import pl.stapik.media.data.document.TrackingDocumentStore
import pl.stapik.media.data.serialization.MediaDocumentCodec
import pl.stapik.media.ui.media.MediaViewModel
import pl.stapik.media.ui.theme.DataStoreThemePreferenceStorage
import pl.stapik.media.ui.theme.ThemePreferenceStorage
import pl.stapik.media.util.stapikDataDirectory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val connectionStorage = ConnectionApiConfigStorage(appContext)
    private val transport = OkHttpTransport()
    private val slot = SlotKey(ApiConfig.SLOT_KEY)
    private val documentStore = TrackingDocumentStore(
        FileDocumentStore(
            directory = appContext.stapikDataDirectory().toPath(),
            slot = slot,
            cipher = KeystoreCiphers.create(DOCUMENT_KEY_ALIAS),
        ),
    )
    private val gateways = CloudGatewayProvider(connectionStorage, transport)
    private val engine = SyncEngine(slot, MediaDocumentCodec.codec, documentStore, gateways::current, Clock.systemUTC())

    val configStorage: ApiConfigStorage = connectionStorage
    val themeStorage: ThemePreferenceStorage = DataStoreThemePreferenceStorage(appContext)

    init {
        File(appContext.filesDir, OBSOLETE_CACHE_PATH).delete()
    }

    fun newConnectController(initial: CloudConfig?) = ConnectController(
        store = connectionStorage.store,
        gatewayFactory = { config -> HttpCloudGateway(config, transport) },
        documents = listOf(engine),
        initial = initial,
    )

    val viewModelFactory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val controller = DocumentController(engine = engine) { configStorage.load() != null }
            return MediaViewModel(controller, documentStore.snapshot, configStorage) as T
        }
    }

    private companion object {
        const val DOCUMENT_KEY_ALIAS = "stapikmedia_data_key"
        const val OBSOLETE_CACHE_PATH = "datastore/media_cache.preferences_pb"
    }
}
