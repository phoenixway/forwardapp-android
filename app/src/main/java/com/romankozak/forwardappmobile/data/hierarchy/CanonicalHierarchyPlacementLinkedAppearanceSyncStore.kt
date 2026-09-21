package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementLinkedAppearanceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementLinkedAppearanceSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementLinkedAppearanceSyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType

class HierarchyPlacementLinkedAppearanceMergeConflictException(
    message: String,
) : IllegalStateException(message)

/**
 * Canonical transport boundary for sparse exact linked-presentation provenance.
 *
 * A live row means true for one exact Workspace occurrence. Missing live rows
 * mean false, so this stream must never infer state from PlacementKind,
 * ancestry, or target identity.
 */
class CanonicalHierarchyPlacementLinkedAppearanceSyncStore(
    private val database: AppDatabase,
) {
    private val dao: HierarchyPlacementLinkedAppearanceDao
        get() = database.hierarchyPlacementLinkedAppearanceDao()

    suspend fun loadAll(): List<HierarchyPlacementLinkedAppearanceSnapshot> =
        dao.getAll().map { it.toTransportSnapshot() }

    suspend fun loadUnsynced(): List<HierarchyPlacementLinkedAppearanceSnapshot> =
        dao.getUnsynced().map { it.toTransportSnapshot() }

    suspend fun loadChangedSince(
        timestamp: Long,
    ): List<HierarchyPlacementLinkedAppearanceSnapshot> =
        dao.getChangedSince(timestamp).map { it.toTransportSnapshot() }

    suspend fun mergeIncoming(
        incoming: List<HierarchyPlacementLinkedAppearanceSnapshot>?,
    ) {
        if (incoming == null) return

        database.withTransaction {
            val candidates = decodeDistinct(incoming)
            validateAgainstCurrentDatabase(candidates)

            val localByPlacementId = dao.getAll().associateBy { it.placementId }
            val winners = resolveWinners(localByPlacementId, candidates)

            if (winners.isNotEmpty()) {
                dao.upsertAll(winners)
            }

            validateAgainstCurrentDatabase(dao.getAll())
        }
    }

    /**
     * Selective import may only carry provenance rows whose exact PlacementIds
     * belong to the retained H1 closure.
     */
    suspend fun mergeIncomingSelective(
        incoming: List<HierarchyPlacementLinkedAppearanceSnapshot>?,
        incomingPlacementIds: Set<String>,
    ) {
        if (incoming == null) return

        database.withTransaction {
            val candidates = decodeDistinct(incoming)
            require(candidates.all { it.placementId in incomingPlacementIds }) {
                val extra =
                    candidates
                        .asSequence()
                        .map { it.placementId }
                        .filterNot { it in incomingPlacementIds }
                        .take(5)
                        .toList()
                "Selective hierarchy linked-appearance payload escapes incoming H1 closure: $extra"
            }

            val placements =
                database.hierarchyPlacementDao()
                    .getAll()
                    .map { it.toHierarchyPlacementStrict() }
            val incomingPlacements =
                placements.filter { it.id.value in incomingPlacementIds }

            require(
                incomingPlacements.mapTo(linkedSetOf()) { it.id.value } ==
                    incomingPlacementIds,
            ) {
                val missing =
                    (
                        incomingPlacementIds -
                            incomingPlacements.mapTo(hashSetOf()) { it.id.value }
                    ).take(5)
                "Selective hierarchy linked-appearance merge is missing incoming H1 placements: $missing"
            }

            validateLinkedAppearances(
                rows = candidates,
                placements = incomingPlacements,
            )

            val localByPlacementId = dao.getAll().associateBy { it.placementId }
            val winners = resolveWinners(localByPlacementId, candidates)

            if (winners.isNotEmpty()) {
                dao.upsertAll(winners)
            }

            validateAgainstCurrentDatabase(dao.getAll())
        }
    }

    fun decodeAndValidateForRestore(
        snapshots: List<HierarchyPlacementLinkedAppearanceSnapshot>,
        placements: List<HierarchyPlacement>,
    ): List<HierarchyPlacementLinkedAppearanceEntity> {
        val decoded = decodeDistinct(snapshots)
        validateLinkedAppearances(
            rows = decoded,
            placements = placements,
        )
        return decoded
    }

    /**
     * Called after H1 restore inside the outer restore transaction.
     * Transported syncedAt is preserved exactly.
     */
    suspend fun restoreExactDecoded(
        rows: List<HierarchyPlacementLinkedAppearanceEntity>,
    ) {
        validateAgainstCurrentDatabase(rows)
        if (rows.isNotEmpty()) {
            dao.upsertAll(rows)
        }
    }

    suspend fun markSynced(
        versions: List<HierarchyPlacementLinkedAppearanceSyncVersion>,
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
        rows: Collection<HierarchyPlacementLinkedAppearanceEntity>,
    ) {
        validateLinkedAppearances(
            rows = rows,
            placements =
                database.hierarchyPlacementDao()
                    .getAll()
                    .map { it.toHierarchyPlacementStrict() },
        )
    }

    private fun decodeDistinct(
        snapshots: List<HierarchyPlacementLinkedAppearanceSnapshot>,
    ): List<HierarchyPlacementLinkedAppearanceEntity> {
        require(
            snapshots.map { it.placementId }.toSet().size == snapshots.size,
        ) {
            "Hierarchy linked-appearance payload contains duplicate placementIds"
        }
        return snapshots.map { it.toEntityStrict() }
    }

    private fun resolveWinners(
        localByPlacementId: Map<String, HierarchyPlacementLinkedAppearanceEntity>,
        candidates: List<HierarchyPlacementLinkedAppearanceEntity>,
    ): List<HierarchyPlacementLinkedAppearanceEntity> {
        val winners = mutableListOf<HierarchyPlacementLinkedAppearanceEntity>()

        candidates.forEach { candidate ->
            val local = localByPlacementId[candidate.placementId]
            local?.let { requireStableIdentity(it, candidate) }

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
                        throw HierarchyPlacementLinkedAppearanceMergeConflictException(
                            "Equal-freshness hierarchy linked-appearance divergence for " +
                                candidate.placementId,
                        )
                }

            winner?.copy(syncedAt = null)?.let(winners::add)
        }

        return winners
    }
}

internal fun validateHierarchyPlacementLinkedAppearances(
    rows: Collection<HierarchyPlacementLinkedAppearanceEntity>,
    placements: Collection<HierarchyPlacement>,
) {
    validateLinkedAppearances(rows, placements)
}

private fun validateLinkedAppearances(
    rows: Collection<HierarchyPlacementLinkedAppearanceEntity>,
    placements: Collection<HierarchyPlacement>,
) {
    val placementById = placements.associateBy { it.id.value }

    require(rows.map { it.placementId }.toSet().size == rows.size) {
        "Hierarchy linked-appearance state contains duplicate placementIds"
    }

    rows.forEach { row ->
        require(row.hierarchyId == "GENERAL") {
            "Unsupported hierarchy linked-appearance hierarchyId=${row.hierarchyId}"
        }
        require(row.version >= 1L) {
            "Hierarchy linked-appearance version must be positive for ${row.placementId}"
        }

        val placement =
            requireNotNull(placementById[row.placementId]) {
                "Hierarchy linked-appearance ${row.placementId} references missing H1 placement"
            }

        require(placement.hierarchyId.value == row.hierarchyId) {
            "Hierarchy linked-appearance ${row.placementId} hierarchyId does not match H1"
        }
        require(placement.target.type == HierarchyTargetType.WORKSPACE) {
            "Hierarchy linked-appearance ${row.placementId} must reference a Workspace occurrence"
        }
        if (!row.isDeleted) {
            require(!placement.isDeleted) {
                "Live hierarchy linked-appearance ${row.placementId} references deleted H1 placement"
            }
        }
    }
}

private fun HierarchyPlacementLinkedAppearanceSnapshot.toEntityStrict():
    HierarchyPlacementLinkedAppearanceEntity =
    try {
        require(placementId.isNotBlank()) {
            "Hierarchy linked-appearance placementId must not be blank"
        }
        require(hierarchyId == "GENERAL") {
            "Unsupported hierarchy linked-appearance hierarchyId=$hierarchyId"
        }
        require(version >= 1L) {
            "Hierarchy linked-appearance version must be positive"
        }

        HierarchyPlacementLinkedAppearanceEntity(
            placementId = placementId,
            hierarchyId = hierarchyId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            isDeleted = isDeleted,
            version = version,
        )
    } catch (failure: RuntimeException) {
        throw IllegalArgumentException(
            "Malformed hierarchy linked-appearance snapshot placementId=$placementId",
            failure,
        )
    }

internal fun HierarchyPlacementLinkedAppearanceEntity.toTransportSnapshot() =
    HierarchyPlacementLinkedAppearanceSnapshot(
        placementId = placementId,
        hierarchyId = hierarchyId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

private fun requireStableIdentity(
    local: HierarchyPlacementLinkedAppearanceEntity,
    incoming: HierarchyPlacementLinkedAppearanceEntity,
) {
    require(
        local.placementId == incoming.placementId &&
            local.hierarchyId == incoming.hierarchyId &&
            local.createdAt == incoming.createdAt,
    ) {
        "Hierarchy linked-appearance durable identity cannot change for ${local.placementId}"
    }
}

private fun HierarchyPlacementLinkedAppearanceEntity.sameTransportSemanticState(
    other: HierarchyPlacementLinkedAppearanceEntity,
): Boolean =
    placementId == other.placementId &&
        hierarchyId == other.hierarchyId &&
        createdAt == other.createdAt &&
        updatedAt == other.updatedAt &&
        isDeleted == other.isDeleted &&
        version == other.version

private fun HierarchyPlacementLinkedAppearanceEntity.differsOnlyByDeletionFrom(
    other: HierarchyPlacementLinkedAppearanceEntity,
): Boolean =
    placementId == other.placementId &&
        hierarchyId == other.hierarchyId &&
        createdAt == other.createdAt &&
        updatedAt == other.updatedAt &&
        version == other.version &&
        isDeleted != other.isDeleted
