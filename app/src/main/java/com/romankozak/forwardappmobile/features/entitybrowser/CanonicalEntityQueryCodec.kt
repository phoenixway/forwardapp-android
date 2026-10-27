package com.romankozak.forwardappmobile.features.entitybrowser

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class DecodedCanonicalEntityQuery(val query: CanonicalEntityQuery?, val issue: String? = null)

/** Stable structured specification, never a DSL or a materialized result list. */
object CanonicalEntityQueryCodec {
    fun encode(query: CanonicalEntityQuery): String = JsonObject().apply {
        addProperty("version", 1)
        addProperty("text", query.text)
        add("entityTypes", JsonArray().apply { query.entityTypes.sortedBy { it.name }.forEach { add(it.name) } })
        add("tags", JsonArray().apply {
            query.tags.map(::normalizeCanonicalQueryTag).filter(String::isNotEmpty).distinct().sorted().forEach { add(it) }
        })
        add("scoreFilters", JsonObject().apply {
            query.scoreFilters.entries.sortedBy { it.key.code }.forEach { (dimension, range) ->
                add(dimension.code, JsonObject().apply {
                    range.min?.let { addProperty("min", it) }
                    range.max?.let { addProperty("max", it) }
                })
            }
        })
        add("sort", JsonObject().apply {
            addProperty("key", query.sort.key.name)
            addProperty("direction", query.sort.direction.name)
            query.sort.dimension?.let { addProperty("dimension", it.code) }
        })
        addProperty("unplaced", query.unplaced)
    }.toString()

    fun decode(raw: String): DecodedCanonicalEntityQuery = try {
        val obj = JsonParser.parseString(raw).asJsonObject
        require(obj.keySet() == setOf("version", "text", "entityTypes", "tags", "scoreFilters", "sort", "unplaced"))
        require(obj["version"].toString() == "1")
        require(obj["text"].isJsonPrimitive && obj["text"].asJsonPrimitive.isString)
        require(obj["unplaced"].isJsonPrimitive && obj["unplaced"].asJsonPrimitive.isBoolean)
        val types = obj["entityTypes"].asJsonArray.map { CanonicalEntityClass.valueOf(it.asString) }.toSet()
        val tags = obj["tags"].asJsonArray.map { normalizeCanonicalQueryTag(it.asString) }.filter(String::isNotEmpty).toSet()
        val ranges = obj["scoreFilters"].asJsonObject.entrySet().associate { (code, value) ->
            val dimension = requireNotNull(CanonicalScoreDimension.parse(code))
            val bounds = value.asJsonObject
            require(bounds.keySet().all { it == "min" || it == "max" })
            dimension to CanonicalScoreRange(bounds.get("min")?.asDouble, bounds.get("max")?.asDouble)
        }
        val order = obj["sort"].asJsonObject
        require(order.keySet().containsAll(setOf("key", "direction")))
        require(order.keySet().all { it in setOf("key", "direction", "dimension") })
        val dimension = order.get("dimension")?.let { requireNotNull(CanonicalScoreDimension.parse(it.asString)) }
        DecodedCanonicalEntityQuery(CanonicalEntityQuery(obj["text"].asString, types, tags, ranges,
            CanonicalEntitySort(CanonicalEntitySortKey.valueOf(order["key"].asString),
                CanonicalSortDirection.valueOf(order["direction"].asString), dimension), obj["unplaced"].asBoolean))
    } catch (_: RuntimeException) {
        DecodedCanonicalEntityQuery(null, "Запит містить непідтримуваний формат або умову. Його дані збережено; застосування недоступне.")
    }
}
