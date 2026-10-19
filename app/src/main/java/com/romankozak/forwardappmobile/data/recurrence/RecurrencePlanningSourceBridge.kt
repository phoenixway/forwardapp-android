package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.data.planning.day.PlanningDaySourceWriter
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.recurrence.*
import javax.inject.Inject

/** Fused by the calling recurrence transaction. Scheduling remains recurrence-v2-owned. */
class RecurrencePlanningSourceBridge @Inject constructor(
    private val db: AppDatabase,
    private val references: CanonicalPlanningReferenceRepository,
    private val writer: PlanningDaySourceWriter,
) {
    suspend fun source(taskId: String): RecurringTaskPlanningSource? {
        val rows = liveReferences(taskId).filter { it.role == PlanningReferenceRole.SOURCE.name }
        require(rows.size <= 1) { "Ambiguous recurring task source: $taskId" }
        return rows.singleOrNull()?.let {
            RecurringTaskPlanningSource(RecurringTaskSourceKind.valueOf(it.targetKind), it.targetId)
        }
    }

    fun available(source: RecurringTaskPlanningSource?): Boolean {
        if (source == null) return true
        return try {
            references.requireTarget(source.target(), live = true)
            true
        } catch (_: IllegalArgumentException) {
            // A deleted/missing source cannot spawn new live participation. No identity is inferred.
            false
        }
    }

    suspend fun materialize(task: DayTask, source: RecurringTaskPlanningSource?, now: Long) {
        writer.persist(source?.target(), now) { task }
    }

    /** An imported occurrence may precede its Planning metadata. Explicit edge tombstones still win. */
    suspend fun repairMissing(task: DayTask, source: RecurringTaskPlanningSource?, now: Long) {
        if (source == null) return
        val edges = db.planningReferenceDao().getForOwner(PlanningReferenceOwnerKind.DAY_TASK.name, task.id)
            .filter { it.role == PlanningReferenceRole.SOURCE.name }
        if (edges.any { it.isDeleted }) return
        require(edges.all { it.targetKind == source.kind.name && it.targetId == source.id }) {
            "Recurring occurrence and template sources disagree"
        }
        materialize(task, source, now)
    }

    suspend fun retire(taskId: String, now: Long) = writer.deleteTask(taskId, now) {}

    suspend fun replace(old: DayTask, next: DayTask?, now: Long) {
        val refs = liveReferences(old.id)
        val source = source(old.id)
        writer.replaceTask(old.id, next, source?.target(), now)
        if (next == null || next.isDeleted) return
        refs.filter { it.role == PlanningReferenceRole.RELATED.name }.forEach {
            references.attach(
                PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, next.id),
                PlanningReferenceTarget(PlanningReferenceTargetKind.valueOf(it.targetKind), it.targetId),
                PlanningReferenceRole.RELATED, now,
            )
        }
    }

    private suspend fun liveReferences(id: String) =
        db.planningReferenceDao().getForOwner(PlanningReferenceOwnerKind.DAY_TASK.name, id).filterNot { it.isDeleted }

    private fun RecurringTaskPlanningSource.target() =
        PlanningReferenceTarget(PlanningReferenceTargetKind.valueOf(kind.name), id)
}
