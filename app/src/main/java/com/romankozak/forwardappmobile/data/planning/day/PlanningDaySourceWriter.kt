package com.romankozak.forwardappmobile.data.planning.day

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
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

    suspend fun persist(source: PlanningReferenceTarget?, writeTask: suspend () -> DayTask): DayTask = db.withTransaction {
        source?.let { references.requireTarget(it, live = true) }
        val task = writeTask()
        if (source != null) {
            val now = System.currentTimeMillis()
            references.attach(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, task.id), source, PlanningReferenceRole.SOURCE, now)
            if (source.kind == PlanningReferenceTargetKind.ORIENTATION) {
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
