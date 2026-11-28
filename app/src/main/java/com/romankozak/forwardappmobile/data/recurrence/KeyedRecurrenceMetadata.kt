package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceRole
import com.romankozak.forwardappmobile.data.planning.day.dayTaskParticipationId
import com.romankozak.forwardappmobile.data.planning.day.PersonalPlanningDay
import com.romankozak.forwardappmobile.data.planning.day.verifyPersonalPlanningDay
import com.romankozak.forwardappmobile.data.planning.planningReferenceId
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitment
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentStatus
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningProvenanceKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.domain.recurrence.recurrenceOccurrenceId
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind

internal suspend fun AppDatabase.verifiedRecurrenceDay(planId: String): PersonalPlanningDay? {
    val extension = dayPlanningScopeExtensionDao().get(planId)?.takeUnless { it.isDeleted } ?: return null
    return verifyPersonalPlanningDay(
        extension.scopeId,
        extension.calendarDayStartMillis,
        extension.localDate,
        extension.timeZoneId,
    )
}

internal suspend fun AppDatabase.verifiedRecurrenceDayStart(planId: String, dayKey: String): Long? {
    val day = verifiedRecurrenceDay(planId) ?: return null
    check(day.date.toString() == dayKey) { "Recurring occurrence day does not match its keyed DayPlan" }
    return day.start.coerceAtLeast(0)
}

internal suspend fun AppDatabase.recurrenceDayKey(planId: String, date: Long): String {
    val day = verifiedRecurrenceDay(planId) ?: return canonicalLocalDayKey(date)
    check(date == day.start) { "DayPlan and its keyed recurrence date disagree: $planId" }
    return day.date.toString()
}

internal suspend fun AppDatabase.hasKeyedRecurrenceCreation(
    kind: RecurringSeriesKind,
    occurrenceId: String,
    planId: String,
    seriesId: String?,
    dayKey: String?,
    createdAt: Long,
): Boolean =
    seriesId != null && dayKey != null &&
        occurrenceId == recurrenceOccurrenceId(kind, seriesId, dayKey) &&
        createdAt == verifiedRecurrenceDayStart(planId, dayKey)

internal suspend fun AppDatabase.isInitialKeyedRecurrenceParticipation(row: PlanningCommitment): Boolean {
    if (row.version != 1L || row.isDeleted || row.role != PlanningRoleCode.DAY_TASK ||
        row.status != PlanningCommitmentStatus.PLANNED || row.priorityLevel != null ||
        row.provenance.kind != PlanningProvenanceKind.MANUAL || row.provenance.sourceType != "DAY_TASK") {
        return false
    }
    val taskId = row.provenance.sourceId ?: return false
    if (row.id != dayTaskParticipationId(taskId)) return false
    val task = dayTaskDao().getByIdForCanonicalRecurrenceSync(taskId) ?: return false
    return task.dayPlanId == row.scopeId.value && task.createdAt == row.createdAt &&
        hasKeyedRecurrenceCreation(
            RecurringSeriesKind.TASK, task.id, task.dayPlanId, task.recurrenceSeriesId,
            task.recurrenceOccurrenceDayKey, task.createdAt,
        )
}

internal suspend fun AppDatabase.isInitialKeyedRecurrenceSource(row: PlanningReferenceEntity): Boolean {
    if (row.version != 1L || row.isDeleted || row.role != PlanningReferenceRole.SOURCE.name ||
        row.id != planningReferenceId(row.ownerKind, row.ownerId, row.targetKind, row.targetId, row.role)) {
        return false
    }
    return when (row.ownerKind) {
        PlanningReferenceOwnerKind.DAY_TASK.name -> {
            val task = dayTaskDao().getByIdForCanonicalRecurrenceSync(row.ownerId) ?: return false
            task.createdAt == row.createdAt && hasKeyedRecurrenceCreation(
                RecurringSeriesKind.TASK, task.id, task.dayPlanId, task.recurrenceSeriesId,
                task.recurrenceOccurrenceDayKey, task.createdAt,
            )
        }
        PlanningReferenceOwnerKind.DAY_FOCUS.name -> {
            val item = dayFocusItemDao().getByIdForCanonicalRecurrenceSync(row.ownerId) ?: return false
            item.createdAt == row.createdAt && hasKeyedRecurrenceCreation(
                RecurringSeriesKind.valueOf(item.type.name), item.id, item.dayPlanId, item.recurrenceSeriesId,
                item.recurrenceOccurrenceDayKey, item.createdAt,
            )
        }
        else -> false
    }
}
