package com.romankozak.forwardappmobile.sync

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Rejects recurrence-v1 TASK semantics while the payload is still raw JSON.
 *
 * Compatibility field names may still be serialized by current DTOs with
 * neutral values, so presence alone is not a violation:
 *   recurringTaskId: null
 *   nextOccurrenceTime: null
 *   recurringTasks: []
 *
 * Non-null occurrence markers or a non-empty recurringTasks collection are
 * legacy TASK recurrence state and must not cross an ingress boundary.
 */
fun requireNoLegacyTaskRecurrenceV1(rawJson: String) {
    inspectLegacyTaskRecurrenceV1(
        element = JsonParser.parseString(rawJson),
        path = "$",
    )
}

private fun inspectLegacyTaskRecurrenceV1(
    element: JsonElement,
    path: String,
) {
    when {
        element.isJsonObject -> {
            val objectValue = element.asJsonObject
            rejectLegacyRecurringMasters(objectValue, path)
            rejectLegacyDayTaskMarkers(objectValue, path)

            objectValue.entrySet().forEach { (key, child) ->
                inspectLegacyTaskRecurrenceV1(
                    element = child,
                    path = "$path.$key",
                )
            }
        }

        element.isJsonArray -> {
            element.asJsonArray.forEachIndexed { index, child ->
                inspectLegacyTaskRecurrenceV1(
                    element = child,
                    path = "$path[$index]",
                )
            }
        }
    }
}

private fun rejectLegacyRecurringMasters(
    objectValue: JsonObject,
    path: String,
) {
    val recurringTasks = objectValue.get("recurringTasks") ?: return
    if (recurringTasks.isJsonNull || !recurringTasks.isJsonArray) return

    if (recurringTasks.asJsonArray.size() > 0) {
        throw IllegalArgumentException(
            "Legacy TASK recurrence-v1 payload is not supported: " +
                "$path.recurringTasks contains ${recurringTasks.asJsonArray.size()} legacy master(s)",
        )
    }
}

private fun rejectLegacyDayTaskMarkers(
    objectValue: JsonObject,
    path: String,
) {
    val dayTasks = objectValue.get("dayTasks") ?: return
    if (dayTasks.isJsonNull || !dayTasks.isJsonArray) return

    dayTasks.asJsonArray.forEachIndexed { index, taskElement ->
        if (!taskElement.isJsonObject) return@forEachIndexed

        val task = taskElement.asJsonObject
        val taskId =
            task.get("id")
                ?.takeUnless { it.isJsonNull }
                ?.let { runCatching { it.asString }.getOrNull() }
                ?: "<unknown>"

        listOf("recurringTaskId", "nextOccurrenceTime").forEach { field ->
            val value = task.get(field)
            if (value != null && !value.isJsonNull) {
                throw IllegalArgumentException(
                    "Legacy TASK recurrence-v1 payload is not supported: " +
                        "$path.dayTasks[$index] id=$taskId has non-null $field",
                )
            }
        }
    }
}
