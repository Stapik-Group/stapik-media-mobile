package pl.stapik.media.data.repository

import pl.stapik.media.data.cache.MediaCacheStorage
import pl.stapik.media.data.cache.toCached
import pl.stapik.media.data.config.ApiConfig
import pl.stapik.media.data.config.ApiConfig.Companion.SLOT_KEY
import pl.stapik.media.data.serialization.MediaDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Read-only client for the Stapik Cloud `/documents/{slotKey}` endpoint
 * (slotKey = "media.json"), the same server and protocol stapikmedia itself
 * talks to. Falls back to the last cached copy on any network failure.
 */
class MediaRepository(
    private val cache: MediaCacheStorage,
    private val client: OkHttpClient = OkHttpClient(),
) {
    suspend fun fetchEntries(config: ApiConfig): MediaFetchOutcome = withContext(Dispatchers.IO) {
        val fresh = runCatching { fetchFromCloud(config) }

        fresh.fold(
            onSuccess = { document ->
                cache.save(document.toCached())
                MediaFetchOutcome.Fresh(document.entries, document.lastUpdate)
            },
            onFailure = { error ->
                val cached = cache.load()
                if (cached != null) {
                    MediaFetchOutcome.Cached(cached.entries, cached.updatedAt)
                } else {
                    MediaFetchOutcome.Failure(error.message ?: "Unknown network error")
                }
            },
        )
    }

    private fun fetchFromCloud(config: ApiConfig): MediaDocument {
        val url = config.serverUrl.trimEnd('/') + "/api/v1/documents/$SLOT_KEY"
        val request = Request.Builder()
            .url(url)
            .header("x-api-key", config.apiKey)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Stapik Cloud returned HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response body")
            // DocumentResponse { slotKey, content, contentHash, updatedAt }
            val envelope = Json.parseToJsonElement(body).jsonObject
            val content = envelope.getValue("content").jsonPrimitive.content
            return MediaDocument.parse(content)
        }
    }
}
