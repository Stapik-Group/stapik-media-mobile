package pl.stapik.media.data.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

interface ApiConfigStorage {
    suspend fun load(): ApiConfig?
    suspend fun save(config: ApiConfig)
    suspend fun clear()
}

private val Context.configDataStore by preferencesDataStore(name = "connection_config")

class DataStoreApiConfigStorage(
    private val context: Context,
    private val crypto: CryptoManager = CryptoManager(),
) : ApiConfigStorage {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url_enc")
        val API_KEY = stringPreferencesKey("api_key_enc")
    }

    override suspend fun load(): ApiConfig? {
        val prefs = context.configDataStore.data.first()
        val serverUrlEnc = prefs[Keys.SERVER_URL] ?: return null
        val apiKeyEnc = prefs[Keys.API_KEY] ?: return null

        return try {
            ApiConfig(
                serverUrl = crypto.decrypt(serverUrlEnc),
                apiKey = crypto.decrypt(apiKeyEnc),
            )
        } catch (_: Exception) {
            clear()
            null
        }
    }

    override suspend fun save(config: ApiConfig) {
        context.configDataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = crypto.encrypt(config.serverUrl)
            prefs[Keys.API_KEY] = crypto.encrypt(config.apiKey)
        }
    }

    override suspend fun clear() {
        context.configDataStore.edit { it.clear() }
    }
}