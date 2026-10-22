package pl.stapik.media.data.config

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.crypto.KeystoreCiphers
import pl.stapik.common.crypto.KeystoreKeys
import pl.stapik.common.settings.AesGcmLegacyDecryptor
import pl.stapik.common.settings.ConfigLoad
import pl.stapik.common.settings.ConnectionConfigStore
import pl.stapik.common.settings.LegacyFormat
import pl.stapik.common.settings.LegacyPrefsConfigReader
import pl.stapik.common.settings.LegacyPrefsSource

interface ApiConfigStorage {
    suspend fun load(): ApiConfig?
    suspend fun save(config: ApiConfig)
    suspend fun clear()
}

/** The DataStore of the pre-library versions; now only read once, to migrate the connection. */
private val Context.configDataStore by preferencesDataStore(name = "connection_config")

private class DataStoreLegacySource(private val dataStore: DataStore<Preferences>) : LegacyPrefsSource {
    override fun readAll(): Map<String, String> = runBlocking {
        dataStore.data.first().asMap().entries
            .mapNotNull { (key, value) -> (value as? String)?.let { key.name to it } }
            .toMap()
    }

    override fun clear() = runBlocking { dataStore.edit { it.clear() }; Unit }
}

/**
 * Connection settings in the library's encrypted file. The first [load] without that file migrates
 * the configuration saved by earlier versions, so the user does not connect again.
 */
class ConnectionApiConfigStorage(context: Context) : ApiConfigStorage {
    /** Shared with the connect form's controller, so that both work on the same file. */
    val store = ConnectionConfigStore(
        directory = File(context.applicationContext.filesDir, "stapik").toPath(),
        cipher = KeystoreCiphers.create(KEY_ALIAS),
        schemaVersion = SCHEMA_VERSION,
        legacy = LegacyPrefsConfigReader(
            source = DataStoreLegacySource(context.applicationContext.configDataStore),
            format = LegacyFormat.MEDIA_BUDGETING,
            decryptor = AesGcmLegacyDecryptor { KeystoreKeys.find(LEGACY_KEY_ALIAS) },
        ),
    )

    // ConnectionConfigStore is not thread-safe, and the view model and the root both load at start.
    private val lock = Mutex()

    override suspend fun load(): ApiConfig? = lock.withLock {
        withContext(Dispatchers.IO) {
            when (val result = store.load()) {
                is ConfigLoad.Loaded -> ApiConfig(result.config.normalizedApiUrl, result.config.apiKey, result.config.passphrase)
                ConfigLoad.None -> null
                else -> {
                    Log.w(TAG, "Saved connection is not usable: ${result::class.simpleName}")
                    null
                }
            }
        }
    }

    override suspend fun save(config: ApiConfig) {
        lock.withLock {
            withContext(Dispatchers.IO) { store.save(CloudConfig(config.serverUrl, config.apiKey, config.passphrase)) }
        }
    }

    override suspend fun clear() {
        lock.withLock {
            withContext(Dispatchers.IO) { store.clear() }
        }
    }

    private companion object {
        const val TAG = "ConnectionApiConfigStorage"
        const val SCHEMA_VERSION = 1
        const val KEY_ALIAS = "stapikmedia_connection_key"
        const val LEGACY_KEY_ALIAS = "stapikmedia_config_key"
    }
}
