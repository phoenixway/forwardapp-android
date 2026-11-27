package com.romankozak.forwardappmobile.core.data.models.entities.day_management

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

/** Occurrence-specific intent and provenance for same-day links written by inheritance. */
@JsonAdapter(TaskRealizesOverrideJsonAdapter::class)
data class TaskRealizesOverride(
    val themes: TaskRealizesTargetOverrides = TaskRealizesTargetOverrides(),
    val priorities: TaskRealizesTargetOverrides = TaskRealizesTargetOverrides(),
)

data class TaskRealizesTargetOverrides(
    val excluded: Set<String> = emptySet(),
    val explicit: Set<String> = emptySet(),
    val inheritedLinks: Map<String, String> = emptyMap(),
) {
    init {
        require(excluded.intersect(explicit).isEmpty()) { "A Realizes target cannot be explicit and excluded" }
        require((excluded + explicit + inheritedLinks.keys + inheritedLinks.values).none { it.isBlank() }) {
            "Realizes target and concrete link IDs must be nonblank"
        }
    }
}

/** Shared by Room and snapshot Gson codecs. A versioned empty object explicitly resets intent. */
class TaskRealizesOverrideJsonAdapter : JsonSerializer<TaskRealizesOverride>, JsonDeserializer<TaskRealizesOverride> {
    override fun serialize(
        source: TaskRealizesOverride,
        type: Type,
        context: JsonSerializationContext,
    ): JsonElement = JsonObject().apply {
        addProperty("version", 1)
        add("themes", encode(source.themes, context))
        add("priorities", encode(source.priorities, context))
    }

    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext,
    ): TaskRealizesOverride {
        val record = json.requireObject("Realizes override", setOf("version", "themes", "priorities"))
        val version = record["version"]?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
        if (version == null || !version.isNumber || version.asString != "1") {
            throw JsonParseException("Unsupported Realizes override version")
        }
        return TaskRealizesOverride(themes = decode(record["themes"]), priorities = decode(record["priorities"]))
    }

    private fun encode(overrides: TaskRealizesTargetOverrides, context: JsonSerializationContext): JsonObject =
        JsonObject().apply {
            add("excluded", context.serialize(overrides.excluded.sorted()))
            add("explicit", context.serialize(overrides.explicit.sorted()))
            add("inheritedLinks", context.serialize(overrides.inheritedLinks.toSortedMap()))
        }

    private fun decode(json: JsonElement?): TaskRealizesTargetOverrides {
        if (json == null) return TaskRealizesTargetOverrides()
        val record = json.requireObject("Realizes target overrides", setOf("excluded", "explicit", "inheritedLinks"))
        val inheritedLinks = record["inheritedLinks"]?.let { links ->
            if (!links.isJsonObject) throw JsonParseException("inheritedLinks must be an object")
            links.asJsonObject.entrySet().associate { (target, endpoint) -> target to endpoint.requireId() }
        }.orEmpty()
        return TaskRealizesTargetOverrides(
            excluded = record["excluded"].ids(),
            explicit = record["explicit"].ids(),
            inheritedLinks = inheritedLinks,
        )
    }

    private fun JsonElement?.ids(): Set<String> {
        if (this == null) return emptySet()
        if (!isJsonArray) throw JsonParseException("Realizes target IDs must be an array")
        return asJsonArray.map { it.requireId() }.toSet()
    }

    private fun JsonElement.requireId(): String {
        if (!isJsonPrimitive || !asJsonPrimitive.isString || asString.isBlank()) {
            throw JsonParseException("Realizes target and concrete link IDs must be nonblank strings")
        }
        return asString
    }

    private fun JsonElement.requireObject(name: String, allowedFields: Set<String>): JsonObject {
        if (!isJsonObject || !allowedFields.containsAll(asJsonObject.keySet())) {
            throw JsonParseException("Invalid $name")
        }
        return asJsonObject
    }
}
