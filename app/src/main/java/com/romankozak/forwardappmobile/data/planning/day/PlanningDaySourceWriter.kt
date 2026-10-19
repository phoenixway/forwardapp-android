package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.data.planning.toPlanningCommitmentStrict
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import javax.inject.Inject

/** Fuses the existing Day task writer with source/participation, before alarm/event publication. */
class PlanningDaySourceWriter @Inject constructor(
    private val db: AppDatabase,
    private val references: CanonicalPlanningReferenceRepository,
    private val planning: CanonicalPlanningRepository,
) {
    suspend fun deleteTask(taskId: String, now: Long, delete: suspend () -> Unit) {
        com.romankozak.forwardappmobile.StartupTrace.mark("Planning.DayTask.delete.queued")
        db.withTransaction {
            com.romankozak.forwardappmobile.StartupTrace.mark("Planning.DayTask.delete.transaction.entered")
            delete()
            (db.planningDao().getLiveForProvenance("DAY_TASK", taskId) +
                listOfNotNull(db.planningDao().getCommitment(dayTaskParticipationId(taskId).value))).distinctBy { it.id }.filter {
                !it.isDeleted && it.role == PlanningRoleCode.DAY_TASK.value
            }.forEach { planning.tombstoneCommitment(PlanningCommitmentId(it.id), now) }
            references.retireOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, taskId), now)
        }
        com.romankozak.forwardappmobile.StartupTrace.mark("Planning.DayTask.delete.committed")
    }

    suspend fun carryForward(task: DayTask, now: Long, destination: suspend (DayTask) -> com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan): DayTask =
        carryDayTaskForward(db, references, planning, task, now, destination)

    suspend fun replaceTask(oldId: String, next: DayTask?, source: PlanningReferenceTarget?, now: Long) = db.withTransaction {
        val origins = (db.planningDao().getLiveForProvenance("DAY_TASK", oldId) +
            listOfNotNull(db.planningDao().getCommitment(dayTaskParticipationId(oldId).value)))
            .distinctBy { it.id }.filter { !it.isDeleted && it.role == PlanningRoleCode.DAY_TASK.value }
        require(origins.size <= 1) { "Ambiguous task participation" }
        deleteTask(oldId, now) {}
        if (next == null || next.isDeleted) return@withTransaction
        persist(source, now) { next }
        origins.singleOrNull()?.let { original ->
            require(source?.kind == PlanningReferenceTargetKind.ORIENTATION && source.id == original.orientationId) {
                "Task participation and typed source disagree"
            }
            val created = requireNotNull(db.planningDao().getCommitment(dayTaskParticipationId(next.id).value))
            if (created.status != original.status || created.priorityLevel != original.priorityLevel) {
                planning.saveCommitment(created.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL).copy(
                    status = PlanningCommitmentStatus.valueOf(original.status),
                    priorityLevel = original.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL).priorityLevel,
                    updatedAt = now, syncedAt = null, version = created.version + 1,
                ))
            }
        }
    }

    suspend fun persist(source: PlanningReferenceTarget?, writeTask: suspend () -> DayTask): DayTask =
        persist(source, System.currentTimeMillis(), writeTask)

    suspend fun persist(source: PlanningReferenceTarget?, now: Long, writeTask: suspend () -> DayTask): DayTask = db.withTransaction {
        source?.let { references.requireTarget(it, live = true) }
        val task = writeTask()
        if (source != null) {
            references.attach(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, task.id), source, PlanningReferenceRole.SOURCE, now)
            if (source.kind == PlanningReferenceTargetKind.ORIENTATION) {
                val existing = db.planningDao().getCommitment(dayTaskParticipationId(task.id).value)
                if (existing != null) {
                    require(!existing.isDeleted && existing.scopeId == task.dayPlanId && existing.orientationId == source.id) {
                        "Recurring task participation is deleted or has a different source/scope"
                    }
                    if (existing.commitmentOrder != task.order) {
                        planning.saveCommitment(existing.toPlanningCommitmentStrict(PlanningRoleRegistry.INITIAL).copy(
                            order = task.order, updatedAt = now, syncedAt = null, version = existing.version + 1,
                        ))
                    }
                    return@withTransaction task
                }
                planning.saveCommitment(
                    PlanningCommitment(
                        id = dayTaskParticipationId(task.id),
                        scopeId = PlanningScopeId(task.dayPlanId), orientationId = source.id,
                        role = PlanningRoleCode.DAY_TASK, order = task.order, priorityLevel = null,
                        status = PlanningCommitmentStatus.PLANNED,
                        provenance = PlanningProvenance(PlanningProvenanceKind.MANUAL, "DAY_TASK", task.id),
                        createdAt = now, updatedAt = now, syncedAt = null, version = 1, isDeleted = false,
                    ),
                )
            }
        }
        task
    }
}
