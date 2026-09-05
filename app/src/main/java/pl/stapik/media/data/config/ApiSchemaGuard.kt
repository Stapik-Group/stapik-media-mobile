package pl.stapik.media.data.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.apiSchemaDataStore by preferencesDataStore(name = "api_schema")

class ApiSchemaGuard(
    private val context: Context,
    private val apiConfigStorage: ApiConfigStorage,
) {
    suspend fun ensureCurrentSchema() {
        val storedVersion = context.apiSchemaDataStore.data.first()[KEY_SCHEMA_VERSION] ?: 0
        if (storedVersion < CURRENT_SCHEMA_VERSION) {
            apiConfigStorage.clear()
            context.apiSchemaDataStore.edit { it[KEY_SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION }
        }
    }

    private companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        val KEY_SCHEMA_VERSION = intPreferencesKey("api_schema_version")
    }
}