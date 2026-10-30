package pl.stapik.media.data.cloud

import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.HttpTransport
import pl.stapik.common.sync.CloudGateway
import pl.stapik.media.data.config.ApiConfig
import pl.stapik.media.data.config.ApiConfigStorage

class CloudGatewayProvider(
    private val configStorage: ApiConfigStorage,
    private val transport: HttpTransport,
) {
    private var gateway: Pair<ApiConfig, CloudGateway>? = null

    suspend fun current(): CloudGateway? = configStorage.load()?.let(::gatewayFor)

    @Synchronized
    private fun gatewayFor(config: ApiConfig): CloudGateway {
        gateway?.let { (known, ready) -> if (known == config) return ready }
        val created = HttpCloudGateway(CloudConfig(config.serverUrl, config.apiKey, config.passphrase), transport)
        gateway = config to created
        return created
    }
}
