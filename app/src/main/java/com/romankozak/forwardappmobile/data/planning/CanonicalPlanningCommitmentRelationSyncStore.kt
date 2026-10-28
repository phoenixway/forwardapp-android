package com.romankozak.forwardappmobile.data.planning

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.PlanningCommitmentRelationRecord

/** Version winners are validated as one prospective DAG before any write. */
class CanonicalPlanningCommitmentRelationSyncStore(private val db: AppDatabase) {
    private val dao get() = db.planningCommitmentRelationDao()
    suspend fun requireCurrentValid() {
        CanonicalPlanningCommitmentRelationRepository(db).validate(dao.getAll().map { it.toDomain() })
    }
    suspend fun merge(records: List<PlanningCommitmentRelationRecord>?) = db.withTransaction {
        if (records == null) {
            // Older Planning ingress may retire commitments but cannot carry composition.
            // The same explicit endpoint lifecycle rule applies atomically to local links.
            val current = dao.getAll().filterNot { it.isDeleted }
            if (current.isEmpty()) return@withTransaction
            val repository = CanonicalPlanningCommitmentRelationRepository(db)
            val live = repository.liveEndpoints()
            val unavailable = current.flatMap {
                listOf(com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId(it.sourceCommitmentId),
                    com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningCommitmentId(it.targetCommitmentId))
            }.filterNot { it in live }.toSet()
            repository.retireEndpoints(unavailable, System.currentTimeMillis())
            return@withTransaction
        }
        require(records.map { it.id }.distinct().size == records.size) { "Duplicate composition identity" }
        val prospective = dao.getAll().associateBy { it.id }.toMutableMap()
        val winners = records.mapNotNull { record ->
            val row = record.toEntity()
            val decoded = row.toDomain()
            require(com.romankozak.forwardappmobile.shared.core.domain.planning.validatePlanningCommitmentRelations(
                listOf(decoded), setOf(decoded.sourceCommitmentId, decoded.targetCommitmentId)).isEmpty()) {
                "Malformed composition record"
            }
            val old = prospective[row.id]
            if (old != null) require(listOf(old.sourceCommitmentId, old.targetCommitmentId, old.relationType, old.createdAt) ==
                listOf(row.sourceCommitmentId, row.targetCommitmentId, row.relationType, row.createdAt)) { "Composition identity changed" }
            when {
                old == null || row.version > old.version -> row.copy(syncedAt = null).also { prospective[it.id] = it }
                row.version < old.version -> null
                row.copy(syncedAt = null) == old.copy(syncedAt = null) -> null
                else -> error("Equal-version composition divergence: ${row.id}")
            }
        }
        CanonicalPlanningCommitmentRelationRepository(db).validate(prospective.values.map { it.toDomain() })
        dao.upsert(winners)
    }

    suspend fun restore(records: List<PlanningCommitmentRelationRecord>) = db.withTransaction {
        val rows = records.map { it.toEntity() }
        CanonicalPlanningCommitmentRelationRepository(db).validate(rows.map { it.toDomain() })
        dao.clear()
        dao.upsert(rows) // Exact backup metadata, including syncedAt.
    }
}
