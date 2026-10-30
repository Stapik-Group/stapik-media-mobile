package pl.stapik.media.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import pl.stapik.media.data.serialization.MediaPayload

object BackupDocument {
    const val SCHEMA_VERSION = 2

    private const val SCHEMA_VERSION_KEY = "schemaVersion"
    private const val DOCUMENT_KEY = "document"
    private const val PAYLOAD_KEY = "payload"
    private const val ENTRIES_KEY = "entries"
    private const val LAST_UPDATE_KEY = "lastUpdate"

    fun write(payload: MediaPayload, lastUpdate: String): String {
        val entries = Json.encodeToJsonElement(MediaPayload.serializer(), payload).let { (it as JsonObject).getValue(ENTRIES_KEY) }
        return buildJsonObject {
            put(SCHEMA_VERSION_KEY, SCHEMA_VERSION)
            put(
                DOCUMENT_KEY,
                buildJsonObject {
                    put(LAST_UPDATE_KEY, lastUpdate)
                    put(ENTRIES_KEY, entries)
                },
            )
        }.toString()
    }

    fun read(text: String): MediaPayload {
        val root = try {
            Json.parseToJsonElement(text) as? JsonObject ?: throw BackupException(BackupFailure.DAMAGED)
        } catch (e: IllegalArgumentException) {
            throw BackupException(BackupFailure.DAMAGED, e)
        }
        val version = (root[SCHEMA_VERSION_KEY] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
        val wrapped = version != null && root[DOCUMENT_KEY] is JsonObject
        if (wrapped && (version ?: 0) > SCHEMA_VERSION) throw BackupException(BackupFailure.NEWER_VERSION)
        val document = if (wrapped) root.getValue(DOCUMENT_KEY) as JsonObject else root
        val entries = (document[PAYLOAD_KEY] as? JsonObject)?.get(ENTRIES_KEY) ?: document[ENTRIES_KEY]
        val array = entries as? JsonArray ?: JsonArray(emptyList())
        return Json.decodeFromJsonElement(MediaPayload.serializer(), buildJsonObject { put(ENTRIES_KEY, array) })
    }
}
