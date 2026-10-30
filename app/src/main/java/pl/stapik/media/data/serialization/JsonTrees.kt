package pl.stapik.media.data.serialization

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object JsonTrees {
    fun unknownParts(original: JsonObject, known: JsonObject): JsonObject {
        val result = LinkedHashMap<String, JsonElement>()
        for ((key, value) in original) {
            val counterpart = known[key]
            when {
                counterpart == null -> result[key] = value
                value is JsonObject && counterpart is JsonObject -> {
                    val nested = unknownParts(value, counterpart)
                    if (nested.isNotEmpty()) result[key] = nested
                }
            }
        }
        return JsonObject(result)
    }

    fun mergeMissing(base: JsonObject, extras: JsonObject): JsonObject {
        if (extras.isEmpty()) return base
        val result = LinkedHashMap<String, JsonElement>(base)
        for ((key, extra) in extras) {
            val present = result[key]
            when {
                present == null -> result[key] = extra
                present is JsonObject && extra is JsonObject -> result[key] = mergeMissing(present, extra)
            }
        }
        return JsonObject(result)
    }
}
