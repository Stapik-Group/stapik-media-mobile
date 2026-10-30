package pl.stapik.media.data.cloud

import pl.stapik.common.model.CloudInfo
import pl.stapik.common.model.CloudScope
import pl.stapik.common.model.SlotKey
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.CloudException
import pl.stapik.common.sync.CloudGateway
import pl.stapik.common.sync.RemoteDocument

class ReadOnlyCloudGateway(private val delegate: CloudGateway) : CloudGateway by delegate {
    override suspend fun info(): CloudInfo = delegate.info().copy(scope = CloudScope.READ_ONLY)

    override suspend fun write(slot: SlotKey, content: String, baseline: String): RemoteDocument =
        throw CloudException(CloudError.Forbidden)
}
