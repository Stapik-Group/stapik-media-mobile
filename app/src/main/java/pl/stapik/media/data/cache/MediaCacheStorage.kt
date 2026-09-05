package pl.stapik.media.data.cache

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import pl.stapik.media.data.model.MediaEntry
import pl.stapik.media.data.serialization.MediaDocument
import pl.stapik.media.data.serialization.MediaEntrySerializer
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

interface MediaCacheStorage {
    suspend fun load(): CachedMedia?
    suspend fun save(cached: CachedMedia)
}

data class CachedMedia(
    val entries: List<MediaEntry>,
    val updatedAt: String,
)

private val Context.mediaCacheDataStore by preferencesDataStore(name = "media_cache")

class DataStoreMediaCacheStorage(private val context: Context) : MediaCacheStorage {

    private object Keys {
        val ENTRIES = stringPreferencesKey("entries_json")
        val UPDATED_AT = stringPreferencesKey("updated_at")
    }

    override suspend fun load(): CachedMedia? {
        val prefs = context.mediaCacheDataStore.data.first()
        val entriesJson = prefs[Keys.ENTRIES] ?: return null
        val updatedAt = prefs[Keys.UPDATED_AT] ?: return null

        return runCatching {
            val array = Json.parseToJsonElement(entriesJson).jsonArray
            val entries = array.map { MediaEntrySerializer.fromJson(it.jsonObject) }
            CachedMedia(entries, updatedAt)
        }.getOrElse { error ->
            Log.w(TAG, "Failed to decode cached media entries", error)
            null
        }
    }

    override suspend fun save(cached: CachedMedia) {
        val array = JsonArray(cached.entries.map { MediaEntrySerializer.toJson(it) })
        context.mediaCacheDataStore.edit { prefs ->
            prefs[Keys.ENTRIES] = array.toString()
            prefs[Keys.UPDATED_AT] = cached.updatedAt
        }
    }

    private companion object {
        const val TAG = "DataStoreMediaCacheStorage"
    }
}
fun MediaDocument.toCached() = CachedMedia(entries = entries, updatedAt = lastUpdate)
