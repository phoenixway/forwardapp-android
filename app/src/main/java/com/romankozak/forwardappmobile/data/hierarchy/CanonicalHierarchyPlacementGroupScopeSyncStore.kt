package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementGroupScopeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementGroupScopeSyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement

class HierarchyPlacementGroupScopeMergeConflictException(
    message: String,
) : IllegalStateException(message)

/**
 * Canonical transport boundary for occurrence-scoped Group/NoGroup provenance.
 *
 * H1 remains the structural stream. This side-stream is validated against H1
 * and canonical Group PART_OF semantics but never derives Group provenance from
 * PlacementId or mutates H1 topology.
 */
class CanonicalHierarchyPlacementGroupScopeSyncStore(
    private val database: AppDatabase,
) {
    private val dao: HierarchyPlacementGroupScopeDao
        get() = database.hierarchyPlacementGroupScopeDao()

    suspend fun loadAll(): List<HierarchyPlacementGroupScopeSnapshot> =
        dao.getAll().map { it.toTransportSnapshot() }

    suspend fun loadUnsynced(): List<HierarchyPlacementGroupScopeSnapshot> =
        dao.getUnsynced().map { it.toTransportSnapshot() }

    suspend fun loadChangedSince(
        timestamp: Long,
    ): List<HierarchyPlacementGroupScopeSnapshot> =
        dao.getChangedSince(timestamp).map { it.toTransportSnapshot() }

    suspend fun mergeIncoming(
        incoming: List<HierarchyPlacementGroupScopeSnapshot>?,
    ) {
        if (incoming == null) return

        database.withTransaction {
            require(
                incoming.map { it.placementId }.toSet().size == incoming.size,
            ) {
                "Hierarchy Group-scope payload contains duplicate placementIds"
            }

            val candidates = incoming.map { it.toEntityStrict() }

            // A present GroupScope stream is authoritative as a complete
            // occurrence-provenance snapshot. Validate it by itself first so
            // valid local rows cannot mask missing incoming provenance.
            validateAgainstCurrentDatabase(
                scopes = candidates,
                requireComplete = true,
            )
            val localByPlacementId =
                dao.getAll().associateBy { it.placementId }

            candidates.forEach { candidate ->
                localByPlacementId[candidate.placementId]?.let { local ->
                    requireStableIdentity(local, candidate)
                }
            }

            val prospective = localByPlacementId.toMutableMap()
            val winners = mutableListOf<HierarchyPlacementGroupScopeEntity>()

            candidates.forEach { candidate ->
                val local = localByPlacementId[candidate.placementId]
                val winner =
                    when {
                        local == null -> candidate
                        candidate.version > local.version -> candidate
                        candidate.version < local.version -> null
                        candidate.updatedAt > local.updatedAt -> candidate
                        candidate.updatedAt < local.updatedAt -> null
                        candidate.sameTransportSemanticState(local) -> null
                        candidate.differsOnlyByDeletionFrom(local) ->
                            candidate.takeIf { it.isDeleted && !local.isDeleted }
                        else ->
                            throw HierarchyPlacementGroupScopeMergeConflictException(
                                "Equal-freshness hierarchy Group-scope divergence for " +
                                    candidate.placementId,
                            )
                    }

                winner?.copy(syncedAt = null)?.let { accepted ->
                    prospective[accepted.placementId] = accepted
                    winners += accepted
                }
            }

            validateAgainstCurrentDatabase(
                scopes = prospective.values,
                requireComplete = true,
            )

            if (winners.isNotEmpty()) {
                dao.upsertAll(winners)
            }
        }
    }

    /**
     * Selective import carries complete GroupScope provenance for the incoming
     * H1 occurrence subgraph, not for unrelated local roots.
     *
     * Validate incoming provenance against exactly the incoming placements
     * first, then validate the full prospective database state after freshness
     * resolution. Normal peer merge keeps its existing full-stream contract.
     */
    suspend fun mergeIncomingSelective(
        incoming: List<HierarchyPlacementGroupScopeSnapshot>?,
        incomingPlacementIds: Set<String>,
    ) {
        if (incoming == null) return

        database.withTransaction {
            require(
                incoming.map { it.placementId }.toSet().size == incoming.size,
            ) {
                "Selective hierarchy Group-scope payload contains duplicate placementIds"
            }

            val candidates = incoming.map { it.toEntityStrict() }
            val allPlacements =
                database.hierarchyPlacementDao()
                    .getAll()
                    .map { it.toHierarchyPlacementStrict() }
            val incomingPlacements =
                allPlacements.filter { it.id.value in incomingPlacementIds }

            require(
                incomingPlacements.mapTo(linkedSetOf()) { it.id.value } ==
                    incomingPlacementIds,
            ) {
                val missing =
                    (incomingPlacementIds -
                        incomingPlacements.mapTo(hashSetOf()) { it.id.value })
                        .take(5)
                "Selective hierarchy Group-scope merge is missing incoming H1 placements: $missing"
            }

            val orientationDao = database.orientationDao()
            validateHierarchyPlacementGroupScopes(
                scopes = candidates,
                placements = incomingPlacements,
                subjects = orientationDao.getAllManagedSubjects(),
                orientations = orientationDao.getAllOrientations(),
                mappings = orientationDao.getAllLegacyMappings(),
                relations = orientationDao.getAllOrientationRelations(),
                requireComplete = true,
            )

            val localByPlacementId =
                dao.getAll().associateBy { it.placementId }

            candidates.forEach { candidate ->
                localByPlacementId[candidate.placementId]?.let { local ->
                    requireStableIdentity(local, candidate)
                }
            }

            val prospective = localByPlacementId.toMutableMap()
            val winners = mutableListOf<HierarchyPlacementGroupScopeEntity>()

            candidates.forEach { candidate ->
                val local = localByPlacementId[candidate.placementId]
                val winner =
                    when {
                        local == null -> candidate
                        candidate.version > local.version -> candidate
                        candidate.version < local.version -> null
                        candidate.updatedAt > local.updatedAt -> candidate
                        candidate.updatedAt < local.updatedAt -> null
                        candidate.sameTransportSemanticState(local) -> null
                        candidate.differsOnlyByDeletionFrom(local) ->
                            candidate.takeIf { it.isDeleted && !local.isDeleted }
                        else ->
                            throw HierarchyPlacementGroupScopeMergeConflictException(
                                "Equal-freshness hierarchy Group-scope divergence for " +
                                    candidate.placementId,
                            )
                    }

                winner?.copy(syncedAt = null)?.let { accepted ->
                    prospective[accepted.placementId] = accepted
                    winners += accepted
                }
            }

            validateAgainstCurrentDatabase(
                scopes = prospective.values,
                requireComplete = true,
            )

            if (winners.isNotEmpty()) {
                dao.upsertAll(winners)
            }
        }
    }

    /**
     * Validate full-replacement side-stream before destructive restore clear.
     *
     * [placements] is the already-decoded H1 replacement graph. The canonical
     * Group and PART_OF rows are read from [bundle], not from pre-restore DB.
     */
    fun decodeAndValidateForRestore(
        bundle: SnapshotBundle,
        snapshots: List<HierarchyPlacementGroupScopeSnapshot>,
        placements: List<HierarchyPlacement>,
        requireComplete: Boolean,
    ): List<HierarchyPlacementGroupScopeEntity> {
        require(
            snapshots.map { it.placementId }.toSet().size == snapshots.size,
        ) {
            "Hierarchy Group-scope restore payload contains duplicate placementIds"
        }

        val decoded = snapshots.map { it.toEntityStrict() }

        validateHierarchyPlacementGroupScopes(
            scopes = decoded,
            placements = placements,
            subjects = bundle.managedSubjects.orEmpty(),
            orientations = bundle.orientations.orEmpty(),
            mappings = bundle.legacySubjectMappings.orEmpty(),
            relations = bundle.orientationRelations.orEmpty(),
            requireComplete = requireComplete,
        )

        return decoded
    }

    /**
     * Called after H1 restore inside the outer restore transaction.
     * Transported syncedAt is preserved exactly.
     */
    suspend fun restoreExactDecoded(
        scopes: List<HierarchyPlacementGroupScopeEntity>,
        requireComplete: Boolean,
    ) {
        validateAgainstCurrentDatabase(
            scopes = scopes,
            requireComplete = requireComplete,
        )
        if (scopes.isNotEmpty()) {
            dao.upsertAll(scopes)
        }
    }

    suspend fun markSynced(
        versions: List<HierarchyPlacementGroupScopeSyncVersion>,
    ) {
        if (versions.isEmpty()) return

        val syncedAt = System.currentTimeMillis()
        database.withTransaction {
            versions.forEach { sent ->
                dao.markSyncedIfVersionMatches(
                    placementId = sent.placementId,
                    version = sent.version,
                    syncedAt = syncedAt,
                )
            }
        }
    }

    private suspend fun validateAgainstCurrentDatabase(
        scopes: Collection<HierarchyPlacementGroupScopeEntity>,
        requireComplete: Boolean,
    ) {
        val orientationDao = database.orientationDao()

        validateHierarchyPlacementGroupScopes(
            scopes = scopes,
            placements =
                database.hierarchyPlacementDao()
                    .getAll()
                    .map { it.toHierarchyPlacementStrict() },
            subjects = orientationDao.getAllManagedSubjects(),
            orientations = orientationDao.getAllOrientations(),
            mappings = orientationDao.getAllLegacyMappings(),
            relations = orientationDao.getAllOrientationRelations(),
            requireComplete = requireComplete,
        )
    }
}

private fun HierarchyPlacementGroupScopeSnapshot.toEntityStrict():
    HierarchyPlacementGroupScopeEntity =
    try {
        require(placementId.isNotBlank()) {
            "Hierarchy Group-scope placementId must not be blank"
        }
        require(hierarchyId == "GENERAL") {
            "Unsupported hierarchy Group-scope hierarchyId=$hierarchyId"
        }
        val transportGroupSubjectId = groupSubjectId
        require(transportGroupSubjectId == null || transportGroupSubjectId.isNotBlank()) {
            "Hierarchy Group-scope groupSubjectId must not be blank"
        }
        require(version >= 1L) {
            "Hierarchy Group-scope version must be positive"
        }

        HierarchyPlacementGroupScopeEntity(
            placementId = placementId,
            hierarchyId = hierarchyId,
            groupSubjectId = groupSubjectId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            isDeleted = isDeleted,
            version = version,
        )
    } catch (failure: RuntimeException) {
        throw IllegalArgumentException(
            "Malformed hierarchy Group-scope snapshot placementId=$placementId",
            failure,
        )
    }

internal fun HierarchyPlacementGroupScopeEntity.toTransportSnapshot() =
    HierarchyPlacementGroupScopeSnapshot(
        placementId = placementId,
        hierarchyId = hierarchyId,
        groupSubjectId = groupSubjectId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

private fun requireStableIdentity(
    local: HierarchyPlacementGroupScopeEntity,
    incoming: HierarchyPlacementGroupScopeEntity,
) {
    require(
        local.placementId == incoming.placementId &&
            local.hierarchyId == incoming.hierarchyId &&
            local.createdAt == incoming.createdAt,
    ) {
        "Hierarchy Group-scope durable identity cannot change for ${local.placementId}"
    }
}

private fun HierarchyPlacementGroupScopeEntity.sameTransportSemanticState(
    other: HierarchyPlacementGroupScopeEntity,
): Boolean =
    placementId == other.placementId &&
        hierarchyId == other.hierarchyId &&
        groupSubjectId == other.groupSubjectId &&
        createdAt == other.createdAt &&
        updatedAt == other.updatedAt &&
        isDeleted == other.isDeleted &&
        version == other.version

private fun HierarchyPlacementGroupScopeEntity.differsOnlyByDeletionFrom(
    other: HierarchyPlacementGroupScopeEntity,
): Boolean =
    placementId == other.placementId &&
        hierarchyId == other.hierarchyId &&
        groupSubjectId == other.groupSubjectId &&
        createdAt == other.createdAt &&
        updatedAt == other.updatedAt &&
        version == other.version &&
        isDeleted != other.isDeleted
