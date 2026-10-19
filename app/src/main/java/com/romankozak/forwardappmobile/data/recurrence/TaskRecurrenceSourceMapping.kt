package com.romankozak.forwardappmobile.data.recurrence

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.shared.core.models.recurrence.*

/** Gson bypasses constructor checks. Validate typed source explicitly, including unknown kinds. */
internal fun decodeTaskRecurrenceTemplate(json: String, gson: Gson): RecurringTaskTemplate {
    val obj = JsonParser.parseString(json).asJsonObject
    val source = obj.get("planningSource")?.takeUnless { it.isJsonNull }?.let { value ->
        require(value.isJsonObject) { "Invalid recurring task planningSource" }
        val fields = value.asJsonObject
        val kind = fields.get("kind")
        val id = fields.get("id")
        require(kind != null && kind.isJsonPrimitive && kind.asJsonPrimitive.isString &&
            id != null && id.isJsonPrimitive && id.asJsonPrimitive.isString) { "Incomplete typed recurrence source" }
        RecurringTaskPlanningSource(RecurringTaskSourceKind.valueOf(kind.asString), id.asString)
    }
    return gson.fromJson(json, RecurringTaskTemplate::class.java).copy(planningSource = source)
}
