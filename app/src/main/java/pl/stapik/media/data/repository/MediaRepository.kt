package pl.stapik.media.data.repository

import pl.stapik.media.data.cache.MediaCacheStorage
import pl.stapik.media.data.cache.toCached
import pl.stapik.media.data.config.ApiConfig
import pl.stapik.media.data.config.ApiConfig.Companion.SLOT_KEY
import pl.stapik.media.data.network.NetworkModule
import pl.stapik.media.data.serialization.MediaDocument

class MediaRepository(
    private val cache: MediaCacheStorage,
) {
    suspend fun fetchEntries(config: ApiConfig): MediaFetchOutcome {
        val fresh = runCatching { fetchFromCloud(config) }

        fresh.onSuccess { document -> cache.save(document.toCached()) }

        return fresh.fold(
            onSuccess = { document -> MediaFetchOutcome.Fresh(document.entries, document.lastUpdate) },
            onFailure = { error ->
                val cached = runCatching { cache.load() }.getOrNull()
                if (cached != null) {
                    MediaFetchOutcome.Cached(cached.entries, cached.updatedAt)
                } else {
                    MediaFetchOutcome.Failure(error)
                }
            },
        )
    }

    private suspend fun fetchFromCloud(config: ApiConfig): MediaDocument {
        val api = NetworkModule.createApi(config.serverUrl)
        val response = api.getDocument(slotKey = SLOT_KEY, apiKey = config.apiKey)
        return MediaDocument.parse(response.content)
    }
}