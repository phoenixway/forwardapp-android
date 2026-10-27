package com.romankozak.forwardappmobile.features.entitylibrary

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.features.entitybrowser.*

internal data class EntityLibraryQueryPreferences(
    val types: Set<CanonicalEntityClass>? = null,
    val tags: Set<String> = emptySet(),
    val ranges: Map<CanonicalScoreDimension, CanonicalScoreRange> = emptyMap(),
) {
    fun encode(): String = JsonObject().apply {
        addProperty("version", 1)
        types?.let { add("types", JsonArray().apply { it.sortedBy { it.name }.forEach { add(it.name) } }) }
        add("tags", JsonArray().apply { tags.sorted().forEach { add(it) } })
        add("scores", JsonObject().apply {
            ranges.forEach { (dimension, range) -> add(dimension.name, JsonObject().apply {
                range.min?.let { addProperty("min", it) }; range.max?.let { addProperty("max", it) }
            }) }
        })
    }.toString()

    companion object {
        fun decode(value: String): EntityLibraryQueryPreferences = try {
            val obj = JsonParser.parseString(value).asJsonObject
            require(obj.get("version")?.asInt == 1)
            val types = obj.get("types")?.asJsonArray?.map { CanonicalEntityClass.valueOf(it.asString) }?.toSet()
            val tags = obj.get("tags")?.asJsonArray?.map { normalizeCanonicalQueryTag(it.asString) }.orEmpty().filter(String::isNotEmpty).toSet()
            val ranges = obj.getAsJsonObject("scores")?.entrySet().orEmpty().associate { (key, value) ->
                val range = value.asJsonObject
                CanonicalScoreDimension.valueOf(key) to CanonicalScoreRange(range.get("min")?.asDouble, range.get("max")?.asDouble)
            }
            EntityLibraryQueryPreferences(types, tags, ranges)
        } catch (_: RuntimeException) { EntityLibraryQueryPreferences() }
    }
}
