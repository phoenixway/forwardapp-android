package com.romankozak.forwardappmobile.data.planning

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.database.AppDatabase

/** Separate nullable generation: absent payload never claims reference authority. */
class CanonicalPlanningReferenceSyncStore(private val db: AppDatabase) {
    private val dao = db.planningReferenceDao()
    private val endpoints = CanonicalPlanningReferenceRepository(db)

    suspend fun merge(rows: List<PlanningReferenceEntity>?) {
        if (rows == null) return
        db.withTransaction {
            validateRecords(rows)
            val prospective = dao.getAll().associateBy { it.id }.toMutableMap()
            val winners = mutableListOf<PlanningReferenceEntity>()
            rows.forEach { incoming ->
                val previous = prospective[incoming.id]
                if (previous != null) {
                    require(identity(previous) == identity(incoming)) { "Planning reference identity changed" }
                }
                when {
                    previous == null || incoming.version > previous.version -> {
                        val winner = incoming.copy(syncedAt = null)
                        prospective[incoming.id] = winner
                        winners += winner
                    }
                    incoming.version < previous.version -> Unit
                    incoming.copy(syncedAt = null) == previous.copy(syncedAt = null) -> Unit
                    else -> error("Equal-version Planning reference divergence: ${incoming.id}")
                }
            }
            validateState(prospective.values.toList())
            winners.forEach { dao.upsert(it) }
        }
    }

    suspend fun restore(rows: List<PlanningReferenceEntity>?) {
        if (rows == null) return
        db.withTransaction {
            validateRecords(rows)
            validateState(rows)
            dao.clear()
            rows.forEach { dao.upsert(it) }
        }
    }

    private fun validateRecords(rows: List<PlanningReferenceEntity>) {
        require(rows.map { it.id }.distinct().size == rows.size) { "Duplicate Planning reference ID" }
        rows.forEach {
            require(it.id.isNotBlank() && it.version >= 1 && it.createdAt >= 0 && it.updatedAt >= 0)
            PlanningReferenceOwner(PlanningReferenceOwnerKind.valueOf(it.ownerKind), it.ownerId)
            PlanningReferenceTarget(PlanningReferenceTargetKind.valueOf(it.targetKind), it.targetId)
            PlanningReferenceRole.valueOf(it.role)
        }
    }

    private fun validateState(rows: List<PlanningReferenceEntity>) {
        require(rows.map(::identity).distinct().size == rows.size) { "Duplicate Planning reference tuple" }
        require(rows.filter { !it.isDeleted && it.role == PlanningReferenceRole.SOURCE.name }
            .groupBy { it.ownerKind to it.ownerId }.values.none { it.size > 1 }) { "Multiple Planning sources" }
        rows.filterNot { it.isDeleted }.forEach {
            endpoints.requireOwner(PlanningReferenceOwner(PlanningReferenceOwnerKind.valueOf(it.ownerKind), it.ownerId), live = false)
            // Tombstoned targets remain representable history; the live catalog controls presentation.
            endpoints.requireTarget(PlanningReferenceTarget(PlanningReferenceTargetKind.valueOf(it.targetKind), it.targetId), live = false)
        }
    }

    private fun identity(row: PlanningReferenceEntity) =
        listOf(row.ownerKind, row.ownerId, row.targetKind, row.targetId, row.role)
}
