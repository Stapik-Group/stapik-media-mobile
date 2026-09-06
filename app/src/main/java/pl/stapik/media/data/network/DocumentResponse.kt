package pl.stapik.media.data.network

import kotlinx.serialization.Serializable

@Serializable
data class DocumentResponse(
    val slotKey: String,
    val content: String,
    val contentHash: String,
    val updatedAt: String,
)