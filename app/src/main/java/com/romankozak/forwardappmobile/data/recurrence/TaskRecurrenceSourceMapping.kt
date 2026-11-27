package com.romankozak.forwardappmobile.data.recurrence

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.requireLocalDayKey
import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskPlanningSource
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskRealizesDefaults
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSourceKind
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate

internal const val TASK_REALIZES_DEFAULTS_KEY = "realizesDefaults"
internal const val TASK_REALIZES_CHANGES_KEY = "realizesChanges"

/** Stable persisted keys; R8 field names are accepted only as legacy input. */
internal fun encodeTaskRecurrenceTemplate(template: RecurringTaskTemplate): String =
    JsonObject().apply {
        addProperty("title", template.title)
        addProperty("description", template.description)
        addProperty("goalId", template.goalId)
        add("linkedProjectIds", template.linkedProjectIds.toJsonArray())
        add("linkedAttachmentIds", template.linkedAttachmentIds.toJsonArray())
        addProperty("priority", template.priority.name)
        addProperty("estimatedDurationMinutes", template.estimatedDurationMinutes)
        addProperty("points", template.points)
        addProperty("projectId", template.projectId)
        addProperty("taskType", template.taskType)
        addProperty("executionStrictness", template.executionStrictness)
        template.planningSource?.let { source ->
            add("planningSource", JsonObject().apply {
                addProperty("kind", source.kind.name)
                addProperty("id", source.id)
            })
        }
        template.realizesDefaults?.let { defaults ->
            add(TASK_REALIZES_DEFAULTS_KEY, defaults.toJson())
        }
        if (template.realizesChanges.isNotEmpty()) {
            add(TASK_REALIZES_CHANGES_KEY, JsonObject().apply {
                template.realizesChanges.toSortedMap().forEach { (dayKey, defaults) ->
                    add(requireLocalDayKey(dayKey), defaults.toJson())
                }
            })
        }
    }.toString()

internal fun decodeTaskRecurrenceTemplate(json: String): RecurringTaskTemplate {
    val obj = JsonParser.parseString(json).asJsonObject
    val source = obj.field("planningSource", "l")?.takeUnless { it.isJsonNull }?.let { value ->
        require(value.isJsonObject) { "Invalid recurring task planningSource" }
        val fields = value.asJsonObject
        val kind = fields.field("kind", "a").stringOrNull()
        val id = fields.field("id", "b").stringOrNull()
        require(kind != null && !id.isNullOrBlank()) { "Incomplete typed recurrence source" }
        RecurringTaskPlanningSource(RecurringTaskSourceKind.valueOf(kind), id)
    }
    return RecurringTaskTemplate(
        title = requireNotNull(obj.field("title", "a").stringOrNull()) { "Missing recurring task title" },
        description = obj.field("description", "b").stringOrNull(),
        goalId = obj.field("goalId", "c").stringOrNull(),
        linkedProjectIds = obj.field("linkedProjectIds", "d").stringList("linkedProjectIds"),
        linkedAttachmentIds = obj.field("linkedAttachmentIds", "e").stringList("linkedAttachmentIds"),
        priority = TaskPriority.valueOf(
            requireNotNull(obj.field("priority", "f").stringOrNull()) { "Missing recurring task priority" },
        ),
        estimatedDurationMinutes = obj.field("estimatedDurationMinutes", "g").longOrNull(),
        points = requireNotNull(obj.field("points", "h").intOrNull()) { "Missing recurring task points" },
        projectId = obj.field("projectId", "i").stringOrNull(),
        taskType = obj.field("taskType", "j").stringOrNull(),
        executionStrictness = obj.field("executionStrictness", "k").stringOrNull(),
        planningSource = source,
        realizesDefaults = obj.get(TASK_REALIZES_DEFAULTS_KEY)?.decodeTaskRealizesDefaults(),
        realizesChanges = obj.get(TASK_REALIZES_CHANGES_KEY)?.decodeTaskRealizesChanges().orEmpty(),
    )
}

private fun RecurringTaskRealizesDefaults.toJson(): JsonObject =
    JsonObject().apply {
        add("themeDefinitionIds", themeDefinitionIds.sorted().toJsonArray())
        add("prioritySeriesIds", prioritySeriesIds.sorted().toJsonArray())
    }

internal fun JsonElement.decodeTaskRealizesDefaults(): RecurringTaskRealizesDefaults {
    require(isJsonObject) { "Invalid recurring task realizesDefaults" }
    return RecurringTaskRealizesDefaults(
        themeDefinitionIds = asJsonObject.get("themeDefinitionIds").stringList("themeDefinitionIds").toSet(),
        prioritySeriesIds = asJsonObject.get("prioritySeriesIds").stringList("prioritySeriesIds").toSet(),
    )
}

internal fun JsonElement.decodeTaskRealizesChanges(): Map<String, RecurringTaskRealizesDefaults> {
    require(isJsonObject) { "Invalid recurring task realizesChanges" }
    return asJsonObject.entrySet().associate { (dayKey, defaults) ->
        requireLocalDayKey(dayKey) to defaults.decodeTaskRealizesDefaults()
    }
}

private fun List<String>.toJsonArray(): JsonArray =
    JsonArray().also { array -> forEach { value -> array.add(value) } }

private fun JsonObject.field(stable: String, legacy: String): JsonElement? = get(stable) ?: get(legacy)

private fun JsonElement?.stringOrNull(): String? = this?.takeUnless { it.isJsonNull }?.asString

private fun JsonElement?.longOrNull(): Long? = this?.takeUnless { it.isJsonNull }?.asLong

private fun JsonElement?.intOrNull(): Int? = this?.takeUnless { it.isJsonNull }?.asInt

private fun JsonElement?.stringList(field: String): List<String> {
    require(this != null && isJsonArray) { "Missing or invalid recurring task $field" }
    return asJsonArray.map { element ->
        require(element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            "Invalid recurring task $field entry"
        }
        element.asString
    }
}
