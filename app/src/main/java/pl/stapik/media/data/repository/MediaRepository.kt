package pl.stapik.media.data.repository

import pl.stapik.media.data.cache.MediaCacheStorage
import pl.stapik.media.data.cache.toCached
import pl.stapik.media.data.config.ApiConfig
import pl.stapik.media.data.config.ApiConfig.Companion.SLOT_KEY
import pl.stapik.media.data.serialization.MediaDocument
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.HttpTransport
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.model.SlotKey
import pl.stapik.common.sync.CloudGateway

/** The server answered, but it has no media document in its slot. */
class MediaNotFoundException : Exception("No media document on the server")

class MediaRepository(
    private val cache: MediaCacheStorage,
    private val transport: HttpTransport = OkHttpTransport(),
) {
    // The gateway keeps the derived end-to-end key, which is slow to compute, so it is reused
    // for as long as the configuration stays the same.
    private var gateway: Pair<ApiConfig, CloudGateway>? = null

    @Synchronized
    private fun gatewayFor(config: ApiConfig): CloudGateway {
        gateway?.let { (known, ready) -> if (known == config) return ready }
        val created = HttpCloudGateway(CloudConfig(config.serverUrl, config.apiKey, config.passphrase), transport)
        gateway = config to created
        return created
    }

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
        val remote = gatewayFor(config).read(SlotKey(SLOT_KEY)) ?: throw MediaNotFoundException()
        return MediaDocument.parse(remote.content)
    }
}
