package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.TaskStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import java.util.UUID

internal fun dayTaskParticipationId(taskId: String) = PlanningCommitmentId(
    UUID.nameUUIDFromBytes("day-task-participation:$taskId".toByteArray(Charsets.UTF_8)).toString(),
)

/** Content is copied; execution, alarms and recurrence occurrence identity are not relocated. */
internal suspend fun carryDayTaskForward(
    db: AppDatabase,
    references: CanonicalPlanningReferenceRepository,
    planning: CanonicalPlanningRepository,
    requested: DayTask,
    now: Long,
    destination: suspend (DayTask) -> DayPlan,
): DayTask = db.withTransaction {
    val tasks = db.dayTaskDao()
    val source = requireNotNull(tasks.getTaskById(requested.id)) { "Завдання вже недоступне" }
    require(!source.isDeleted && source.version == requested.version) { "Завдання змінилося; відкрийте його знову" }
    val targetPlan = destination(source)
    require(targetPlan.id != source.dayPlanId) { "Carry-forward потребує іншого дня" }
    val newId = UUID.nameUUIDFromBytes("day-task-carry:v1:${source.id.length}:${source.id}:${targetPlan.id.length}:${targetPlan.id}".toByteArray(Charsets.UTF_8)).toString()
    tasks.getTaskById(newId)?.let { existing ->
        require(!existing.isDeleted) { "Перенесену участь уже видалено; автоматичне відновлення заборонене" }
        return@withTransaction existing
    }
    val next = source.copy(
        id = newId, dayPlanId = targetPlan.id, order = (tasks.getMaxOrderForDayPlan(targetPlan.id) ?: -1L) + 1,
        realizedDayFocusItemIds = emptyList(), // A new day requires explicit new day-priority choices.
        status = TaskStatus.NOT_STARTED, completed = false, completedAt = null,
        activityRecordId = null, actualDurationMinutes = null, scheduledTime = null,
        recurrenceSeriesId = null, recurrenceOccurrenceDayKey = null, recurrenceSourceSeriesVersion = null,
        createdAt = now, updatedAt = now, syncedAt = null, version = 1, isDeleted = false,
    )
    tasks.insert(next)
    db.planningReferenceDao().getForOwner(PlanningReferenceOwnerKind.DAY_TASK.name, source.id)
        .filterNot { it.isDeleted }.forEach { ref ->
            references.attach(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, next.id),
                PlanningReferenceTarget(PlanningReferenceTargetKind.valueOf(ref.targetKind), ref.targetId),
                PlanningReferenceRole.valueOf(ref.role), now)
        }
    val origins = (db.planningDao().getLiveForProvenance("DAY_TASK", source.id) +
        listOfNotNull(db.planningDao().getCommitment(dayTaskParticipationId(source.id).value)))
        .distinctBy { it.id }.filter { !it.isDeleted && it.role == PlanningRoleCode.DAY_TASK.value }
    require(origins.size <= 1) { "Неоднозначна canonical участь завдання" }
    origins.singleOrNull()?.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL)?.let { original ->
        require(original.scopeId.value == source.dayPlanId) { "Day task participation scope mismatch" }
        planning.saveCommitment(original.copy(
            id = dayTaskParticipationId(next.id), scopeId = PlanningScopeId(next.dayPlanId), order = next.order,
            status = PlanningCommitmentStatus.PLANNED,
            provenance = PlanningProvenance(PlanningProvenanceKind.CARRIED_FORWARD, "PLANNING_COMMITMENT", original.id.value),
            createdAt = now, updatedAt = now, syncedAt = null, version = 1, isDeleted = false,
        ))
    }
    next
}
