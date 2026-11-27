package com.romankozak.forwardappmobile.data.daythemes

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentSnapshot
import com.romankozak.forwardappmobile.database.AppDatabase

/** Moves exact Day Theme memberships inside the transaction that replaces the tasks. */
internal suspend fun AppDatabase.rekeyTaskThemeAssignments(
    replacements: List<Pair<DayTask, DayTask>>,
    now: Long,
) {
    val dao = canonicalDayThemeDao()
    for ((dayPlanId, dayReplacements) in replacements.groupBy { (source, _) -> source.dayPlanId }) {
        val replacementIds = dayReplacements.associate { (source, replacement) ->
            check(replacement.dayPlanId == dayPlanId) { "Task theme assignments are day-local" }
            source.id to replacement.id
        }
        val row = dao.getAssignmentDocumentByDayPlanId(dayPlanId) ?: continue
        if (row.isDeleted) continue
        val document = row.toCanonicalSnapshot()
        val assignedSources = document.assignments.filter { it.entityId in replacementIds }
        if (assignedSources.isEmpty()) continue

        val destinationIds = assignedSources.map { replacementIds.getValue(it.entityId) }.toSet()
        val (affected, unaffected) = document.assignments.partition {
            it.entityId in replacementIds || it.entityId in destinationIds
        }
        val rekeyed = affected.groupBy { replacementIds[it.entityId] ?: it.entityId }
            .map { (entityId, assignments) ->
                DayThemeAssignmentSnapshot(entityId, assignments.flatMap { it.dayThemeIds }.distinct().sorted())
            }
        val updated = document.copy(
            assignments = (unaffected + rekeyed).sortedBy { it.entityId },
            updatedAt = now,
            syncedAt = null,
            version = document.version + 1,
        )
        dao.upsertAssignmentDocuments(listOf(updated.toCanonicalEntity()))
    }
}
