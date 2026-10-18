package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*

/** Coverage survives placement tombstones. It is never generic structure or semantic ownership. */
class GroupScopeCoverageStore(private val db: AppDatabase) {
    private val dao get() = db.groupScopeApplicabilityDao()
    suspend fun snapshot() = GroupScopeCoverageSnapshot(records = dao.all().map { it.record() })
    suspend fun dirty() = snapshot().let { it.copy(records = it.records.filter { r -> r.syncedAt == null }) }
    suspend fun changedSince(time: Long) = snapshot().let { it.copy(records = it.records.filter { r -> r.updatedAt > time }) }
    suspend fun markSynced(versions: List<GroupScopeCoverageVersion>, now: Long) = db.withTransaction {
        versions.forEach { dao.ack(it.placementId, it.version, now) }
    }

    suspend fun ensureLegacy(placements: List<HierarchyPlacement>) {
        val existing = dao.all().mapTo(hashSetOf()) { it.placementId }
        val missing = placements.filter { it.needsGroupScopeCoverage() && it.id.value !in existing }
        if (missing.isEmpty()) return // Ordinary CRUD must never rescan semantic migration evidence.
        val o = db.orientationDao()
        val classified = classifyLegacyCoverage(missing, db.hierarchyPlacementGroupScopeDao().getAll(),
            o.getAllManagedSubjects(), o.getAllAspects(), o.getAllLegacyMappings())
        dao.upsert(classified.filter { it.placementId !in existing }.map(::GroupScopeApplicabilityEntity))
    }

    suspend fun recordNative(placements: List<HierarchyPlacement>) {
        val relevant = placements.filter { it.needsGroupScopeCoverage() }
        if (relevant.isEmpty()) return
        val old = dao.all().mapTo(hashSetOf()) { it.placementId }
        require(relevant.none { it.id.value in old }) { "Applicability identity already exists" }
        dao.upsert(relevant.map { placement -> GroupScopeApplicabilityEntity(placement.id.value, "GENERAL", "NATIVE",
            placement.createdAt, placement.updatedAt, null, 1) })
    }

    /** Explicit historical Group command is the ONLY lineage-adoption boundary. */
    suspend fun establishHistorical(placement: HierarchyPlacement, now: Long) =
        establishHistorical(listOf(placement), now)

    suspend fun establishHistorical(placements: List<HierarchyPlacement>, now: Long) {
        val oldById = dao.all().associateBy { it.placementId }
        val rows = placements.mapNotNull { placement ->
            require(placement.needsGroupScopeCoverage())
            val old = oldById[placement.id.value]
            if (old?.mode == "HISTORICAL") null
            else if (old == null) GroupScopeApplicabilityEntity(placement.id.value, "GENERAL", "HISTORICAL",
                placement.createdAt, maxOf(now, placement.updatedAt), null, 1)
            else old.copy(mode = "HISTORICAL", updatedAt = maxOf(now, Math.addExact(old.updatedAt, 1)),
                syncedAt = null, version = Math.addExact(old.version, 1))
        }
        if (rows.isNotEmpty()) dao.upsert(rows)
    }

    fun decodeForRestore(bundle: SnapshotBundle, placements: List<HierarchyPlacement>,
        scopes: List<HierarchyPlacementGroupScopeEntity>): GroupScopeCoverageSnapshot {
        val result = bundle.groupScopeCoverage ?: GroupScopeCoverageSnapshot(records = classifyLegacyCoverage(
            placements, scopes, bundle.managedSubjects.orEmpty(), bundle.aspects.orEmpty(), bundle.legacySubjectMappings.orEmpty()))
        validateCoverageRecords(result, placements)
        return result
    }

    suspend fun restoreExact(payload: GroupScopeCoverageSnapshot) {
        validateCoverageRecords(payload, db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() })
        dao.clear()
        dao.upsert(payload.records.map(::GroupScopeApplicabilityEntity))
    }

    /** An explicit envelope cannot omit coverage for any incoming relevant occurrence. */
    suspend fun merge(payload: GroupScopeCoverageSnapshot?, incomingIds: Set<String>? = null) = db.withTransaction {
        val placements = db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }
        if (payload == null) {
            ensureLegacy(placements)
            return@withTransaction
        }
        validateCoverageRecords(payload,
            if (incomingIds == null) placements else placements.filter { it.id.value in incomingIds })
        val incomingKeys = payload.records.mapTo(hashSetOf()) { it.placementId }
        ensureLegacy(placements.filterNot { it.id.value in incomingKeys })
        val local = dao.all().associateBy { it.placementId }
        val winners = payload.records.mapNotNull { incoming ->
            val old = local[incoming.placementId]?.record()
            if (old == null) incoming.copy(syncedAt = null) else {
                require(old.hierarchyId == incoming.hierarchyId && old.createdAt == incoming.createdAt) { "Coverage identity changed" }
                when {
                    incoming.version < old.version -> null
                    incoming.version == old.version -> {
                        require(incoming.copy(syncedAt = null) == old.copy(syncedAt = null)) { "Equal-version coverage divergence" }
                        null
                    }
                    else -> {
                        require(incoming.mode == old.mode || incoming.mode == "HISTORICAL") { "Coverage lineage cannot be downgraded" }
                        incoming.copy(syncedAt = null)
                    }
                }
            }
        }
        dao.upsert(winners.map(::GroupScopeApplicabilityEntity))
        validateCoverageRecords(snapshot(), placements)
    }

    /** Location/applicability only; legacy fused commands retain their own PART_OF transaction boundary. */
    suspend fun validateCurrent() {
        val placements = db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }
        ensureLegacy(placements)
        val records = snapshot()
        validateCoverageRecords(records, placements)
        val scopes = db.hierarchyPlacementGroupScopeDao().getAll().associateBy { it.placementId }
        val containers = db.bigPicturePresentationDao().containers().filterNot { it.record.isDeleted }.mapTo(hashSetOf()) { it.record.id }
        val members = db.bigPicturePresentationDao().memberships().filter {
            !it.record.isDeleted && it.record.containerId in containers
        }.mapTo(hashSetOf()) { it.record.placementId }
        val byId = placements.associateBy { it.id.value }
        records.records.forEach { r ->
            val p = byId.getValue(r.placementId)
            val scope = scopes[r.placementId]
            if (r.mode == "HISTORICAL") require(scope != null) { "HISTORICAL occurrence lost GroupScope history" }
            if (scope != null && !scope.isDeleted) {
                require(r.mode != "NATIVE" && !p.isDeleted && p.parentPlacementId == null && p.id.value !in members) {
                    "Active GroupScope is incompatible with applicability/location"
                }
            }
            if (r.mode == "LEGACY_UNKNOWN" && !p.isDeleted && p.parentPlacementId == null) {
                require(scope != null && !scope.isDeleted && p.id.value !in members) { "LEGACY_UNKNOWN root requires conservative GroupScope admission" }
            }
        }
    }
}

internal fun HierarchyPlacement.needsGroupScopeCoverage() =
    hierarchyId == HierarchyId.GENERAL && target.type == HierarchyTargetType.MANAGED_SUBJECT

internal fun classifyLegacyCoverage(
    placements: List<HierarchyPlacement>, scopes: List<HierarchyPlacementGroupScopeEntity>,
    subjects: List<ManagedSubjectEntity>, aspects: List<AspectEntity>, mappings: List<LegacySubjectMappingEntity>,
): List<GroupScopeApplicabilityRecord> {
    val historical = scopes.mapTo(hashSetOf()) { it.placementId }
    val aspectIds = aspects.mapTo(hashSetOf()) { it.subjectId }
    val safeAspects = subjects.filter { it.subjectType == "ASPECT" && it.id in aspectIds }.mapTo(hashSetOf()) { it.id }
    val mapped = mappings.filter { it.sourceType in setOf("MAIN_BEACON", "MAIN_BEACON_GROUP") }.mapTo(hashSetOf()) { it.subjectId }
    return placements.filter { it.needsGroupScopeCoverage() }.map { p ->
        GroupScopeApplicabilityRecord(p.id.value, "GENERAL", when {
            p.id.value in historical -> "HISTORICAL"
            p.target.id in safeAspects && p.target.id !in mapped -> "NATIVE"
            else -> "LEGACY_UNKNOWN"
        }, p.createdAt, p.updatedAt, null, 1)
    }
}

internal fun validateCoverageRecords(payload: GroupScopeCoverageSnapshot, placements: Collection<HierarchyPlacement>) {
    require(payload.formatVersion == 1) { "Unsupported GroupScope coverage format" }
    require(payload.records.map { it.placementId }.distinct().size == payload.records.size) { "Duplicate coverage identity" }
    val expected = placements.filter { it.needsGroupScopeCoverage() }.mapTo(hashSetOf()) { it.id.value }
    require(payload.records.mapTo(hashSetOf()) { it.placementId } == expected) { "Incomplete GroupScope applicability coverage" }
    payload.records.forEach {
        require(it.hierarchyId == "GENERAL" && it.placementId.isNotBlank() && it.version > 0) { "Invalid coverage record" }
        GroupScopeApplicabilityMode.valueOf(it.mode)
    }
}
