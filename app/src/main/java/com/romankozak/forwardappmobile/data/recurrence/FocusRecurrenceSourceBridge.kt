package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceRole
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTarget
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceTargetKind
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import javax.inject.Inject

/** Exact Orientation provenance for a Day priority; all calls run in the caller's transaction. */
class FocusRecurrenceSourceBridge @Inject constructor(
    private val db: AppDatabase,
    private val references: CanonicalPlanningReferenceRepository,
) {
    suspend fun sourceOrientationId(itemId: String): String? {
        val sources = db.planningReferenceDao().getForOwner(PlanningReferenceOwnerKind.DAY_FOCUS.name, itemId)
            .filter { !it.isDeleted && it.role == PlanningReferenceRole.SOURCE.name }
        require(sources.size <= 1) { "Ambiguous Day priority source: $itemId" }
        return sources.singleOrNull()?.let { row ->
            require(row.targetKind == PlanningReferenceTargetKind.ORIENTATION.name) {
                "Day priority source must be an Orientation: $itemId"
            }
            row.targetId
        }
    }

    fun available(orientationId: String?): Boolean {
        if (orientationId == null) return true
        return try {
            references.requireTarget(target(orientationId), live = true)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    suspend fun materialize(itemId: String, orientationId: String?, now: Long) {
        if (orientationId == null) return
        references.attach(owner(itemId), target(orientationId), PlanningReferenceRole.SOURCE, now)
    }

    suspend fun repairMissing(itemId: String, orientationId: String?, now: Long) {
        if (orientationId == null) return
        val sources = db.planningReferenceDao().getForOwner(PlanningReferenceOwnerKind.DAY_FOCUS.name, itemId)
            .filter { it.role == PlanningReferenceRole.SOURCE.name }
        if (sources.any { it.isDeleted }) return // An explicit tombstone must not be resurrected.
        require(sources.all { it.targetKind == PlanningReferenceTargetKind.ORIENTATION.name && it.targetId == orientationId }) {
            "Day priority occurrence and series sources disagree: $itemId"
        }
        materialize(itemId, orientationId, now)
    }

    suspend fun replace(oldId: String, newId: String, now: Long) {
        val rows = db.planningReferenceDao().getForOwner(PlanningReferenceOwnerKind.DAY_FOCUS.name, oldId)
            .filterNot { it.isDeleted }
        rows.forEach { row ->
            references.attach(
                owner(newId),
                PlanningReferenceTarget(PlanningReferenceTargetKind.valueOf(row.targetKind), row.targetId),
                PlanningReferenceRole.valueOf(row.role), now,
            )
        }
        references.retireOwner(owner(oldId), now)
    }

    suspend fun retire(itemId: String, now: Long) {
        references.retireOwner(owner(itemId), now)
    }

    private fun owner(id: String) = PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_FOCUS, id)

    private fun target(id: String) = PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, id)
}
