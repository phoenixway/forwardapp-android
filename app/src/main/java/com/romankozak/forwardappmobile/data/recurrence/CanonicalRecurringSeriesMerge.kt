package com.romankozak.forwardappmobile.data.recurrence

import com.google.gson.JsonParser
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalRecurringSeriesEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind

/** Preserves template intent when a legacy peer sends a newer edit to the same series. */
internal fun planCanonicalRecurringSeriesMerge(
    incoming: List<CanonicalRecurringSeriesSnapshot>,
    local: List<CanonicalRecurringSeriesEntity>,
    restoreExact: Boolean,
): List<CanonicalRecurringSeriesEntity> {
    val localById = local.associateBy { it.id }
    return incoming.mapNotNull { snapshot ->
        val previous = localById[snapshot.id]
        val freshness = when {
            previous == null -> 1
            snapshot.version != previous.version -> snapshot.version.compareTo(previous.version)
            snapshot.updatedAt != previous.updatedAt -> snapshot.updatedAt.compareTo(previous.updatedAt)
            else -> snapshot.isDeleted.compareTo(previous.isDeleted)
        }
        if (freshness < 0) return@mapNotNull null

        if (snapshot.kind != RecurringSeriesKind.TASK.name) {
            return@mapNotNull if (freshness > 0) snapshot.toEntity() else null
        }
        val incomingTemplate = snapshot.template.asJsonObject
        val incomingIntent = incomingTemplate.readRealizesIntent()
        if (restoreExact || previous == null || previous.kind != snapshot.kind) {
            return@mapNotNull if (freshness > 0) snapshot.toEntity() else null
        }
        val localTemplate = JsonParser.parseString(previous.templateJson).asJsonObject
        val localIntent = localTemplate.readRealizesIntent()

        when {
            freshness > 0 && !incomingIntent.isPresent && localIntent.isPresent ->
                snapshot.copy(
                    template = incomingTemplate.deepCopy().apply(localIntent::writeInto),
                ).toEntity()
            freshness > 0 -> snapshot.toEntity()
            !localIntent.isPresent && incomingIntent.isPresent ->
                // A peer may already have received the same legacy revision without its intent.
                previous.copy(templateJson = localTemplate.apply(incomingIntent::writeInto).toString())
            else -> {
                require(!localIntent.isPresent || !incomingIntent.isPresent ||
                    localIntent.defaults == incomingIntent.defaults && localIntent.changes == incomingIntent.changes) {
                    "Conflicting Realizes defaults at the same revision for task series ${snapshot.id}"
                }
                null
            }
        }
    }
}

private class TaskRealizesIntent(
    private val defaultsJson: JsonElement?,
    private val changesJson: JsonElement?,
) {
    val defaults = defaultsJson?.decodeTaskRealizesDefaults()
    val changes = changesJson?.decodeTaskRealizesChanges().orEmpty()
    val isPresent = defaults != null || changes.isNotEmpty()

    fun writeInto(template: JsonObject) {
        defaultsJson?.let { template.add(TASK_REALIZES_DEFAULTS_KEY, it) }
        changesJson?.let { template.add(TASK_REALIZES_CHANGES_KEY, it) }
    }
}

private fun JsonObject.readRealizesIntent(): TaskRealizesIntent =
    TaskRealizesIntent(get(TASK_REALIZES_DEFAULTS_KEY), get(TASK_REALIZES_CHANGES_KEY))
