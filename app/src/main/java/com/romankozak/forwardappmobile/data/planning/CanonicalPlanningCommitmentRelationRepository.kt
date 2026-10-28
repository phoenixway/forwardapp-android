package com.romankozak.forwardappmobile.data.planning

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.planning.*
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Sole composition writer. Existing semantic, H1 and PlanningReference authorities are untouched. */
@Singleton
class CanonicalPlanningCommitmentRelationRepository @Inject constructor(private val db: AppDatabase) {
    private val dao get() = db.planningCommitmentRelationDao()
    fun observeAll() = dao.observeAll().map { rows -> rows.map { it.toDomain() } }
    fun observeIncoming(target: PlanningCommitmentId) = dao.observeIncoming(target.value).map { rows -> rows.map { it.toDomain() } }
    fun observeOutgoing(source: PlanningCommitmentId) = dao.observeOutgoing(source.value).map { rows -> rows.map { it.toDomain() } }

    suspend fun addRelation(source: PlanningCommitmentId, target: PlanningCommitmentId,
        type: PlanningCommitmentRelationType = PlanningCommitmentRelationType.COMPONENT_OF,
        now: Long = System.currentTimeMillis()): PlanningCommitmentRelation = db.withTransaction {
        val rows = dao.getAll().map { it.toDomain() }
        val previous = rows.filter { it.sourceCommitmentId == source && it.targetCommitmentId == target && it.relationType == type }
        val live = previous.filterNot { it.isDeleted }
        require(live.size <= 1) { "Duplicate live composition" }
        live.singleOrNull()?.let { validate(rows); return@withTransaction it }
        val old = previous.maxWithOrNull(compareBy<PlanningCommitmentRelation> { it.version }.thenBy { it.id.value })
        val key = listOf(source.value, target.value, type.name).joinToString("") { "${it.length}:$it" }
        val siblingOrder = rows.filter { !it.isDeleted && it.targetCommitmentId == target }.maxOfOrNull { it.order }
        val next = PlanningCommitmentRelation(
            old?.id ?: PlanningCommitmentRelationId(UUID.nameUUIDFromBytes("planning-composition:v1:$key".toByteArray(Charsets.UTF_8)).toString()),
            source, target, type, siblingOrder?.let { Math.addExact(it, 1) } ?: 0,
            old?.createdAt ?: now, maxOf(now, old?.updatedAt ?: old?.createdAt ?: now), null, false,
            Math.addExact(old?.version ?: 0, 1))
        validate(rows.filterNot { it.id == next.id } + next)
        dao.upsert(listOf(next.toEntity()))
        next
    }

    suspend fun removeRelation(id: PlanningCommitmentRelationId, expectedVersion: Long? = null,
        now: Long = System.currentTimeMillis()) = db.withTransaction {
        val row = requireNotNull(dao.get(id.value)) { "Missing composition relation" }
        require(expectedVersion == null || row.version == expectedVersion) { "Stale composition relation" }
        if (!row.isDeleted) dao.upsert(listOf(row.copy(isDeleted = true, updatedAt = maxOf(now, row.updatedAt ?: row.createdAt),
            syncedAt = null, version = Math.addExact(row.version, 1))))
    }

    suspend fun reorderComponents(target: PlanningCommitmentId, ids: List<PlanningCommitmentRelationId>,
        now: Long = System.currentTimeMillis()) = db.withTransaction {
        val current = dao.getAll().filter { !it.isDeleted && it.targetCommitmentId == target.value }
        require(ids.distinct().size == ids.size && ids.map { it.value }.toSet() == current.map { it.id }.toSet()) {
            "Reorder must contain the exact live component relation set"
        }
        val byId = current.associateBy { it.id }
        dao.upsert(ids.mapIndexedNotNull { index, id ->
            byId.getValue(id.value).takeIf { it.relationOrder != index.toLong() }?.let {
                it.copy(relationOrder = index.toLong(), updatedAt = maxOf(now, it.updatedAt ?: it.createdAt),
                    syncedAt = null, version = Math.addExact(it.version, 1))
            }
        })
    }

    suspend fun retireEndpoints(ids: Set<PlanningCommitmentId>, now: Long) = db.withTransaction {
        val keys = ids.mapTo(hashSetOf()) { it.value }
        dao.getAll().filter { !it.isDeleted && (it.sourceCommitmentId in keys || it.targetCommitmentId in keys) }
            .forEach { removeRelation(PlanningCommitmentRelationId(it.id), it.version, now) }
    }

    /** One transaction supplies the complete mapping, avoiding partial source/target remaps. */
    suspend fun carryRelations(replacements: Map<PlanningCommitmentId, PlanningCommitmentId>,
        now: Long = System.currentTimeMillis()) = db.withTransaction {
        val pairs = remapPlanningComposition(dao.getAll().map { it.toDomain() }, replacements, liveEndpoints())
        pairs.forEach { (source, target) -> addRelation(source, target, now = now) }
    }

    internal suspend fun validate(rows: List<PlanningCommitmentRelation>) {
        if (rows.isEmpty()) return
        val errors = validatePlanningCommitmentRelations(rows, liveEndpoints())
        require(errors.isEmpty()) { errors.joinToString() }
    }

    internal suspend fun liveEndpoints(): Set<PlanningCommitmentId> {
        val scopes = db.planningDao().getAllScopes().filterNot { it.isDeleted }.mapTo(hashSetOf()) { it.id }
        val orientations = db.orientationDao().getAllManagedSubjects().filter { !it.isDeleted && it.subjectType == "ORIENTATION" }
            .mapTo(hashSetOf()) { it.id }.intersect(db.orientationDao().getAllOrientations().map { it.subjectId }.toSet())
        return db.planningDao().getAllCommitments().filter { !it.isDeleted && it.scopeId in scopes && it.orientationId in orientations }
            .mapTo(hashSetOf()) { PlanningCommitmentId(it.id) }
    }
}
