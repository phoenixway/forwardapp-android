package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.validateProspectiveHierarchy

/**
 * Canonical transport boundary for dormant Hierarchy V2 placements.
 *
 * Full backup/restore preserves syncedAt. Peer merge resets an accepted remote
 * winner to syncedAt=null, because ACK state belongs to the receiving device.
 */
class CanonicalHierarchyPlacementSyncStore(
    private val database: AppDatabase,
) {
    private val dao: HierarchyPlacementDao
        get() = database.hierarchyPlacementDao()

    suspend fun loadAll(): List<HierarchyPlacementSnapshot> =
        dao.getAll().map { it.toHierarchyPlacementStrict().toTransportSnapshot() }

    suspend fun loadUnsynced(): List<HierarchyPlacementSnapshot> =
        dao.getUnsynced().map { it.toHierarchyPlacementStrict().toTransportSnapshot() }

    suspend fun loadChangedSince(timestamp: Long): List<HierarchyPlacementSnapshot> =
        dao.getChangedSince(timestamp).map {
            it.toHierarchyPlacementStrict().toTransportSnapshot()
        }

    suspend fun mergeIncoming(incoming: List<HierarchyPlacementSnapshot>?) {
        if (incoming == null || incoming.isEmpty()) return

        database.withTransaction {
            require(incoming.map { it.id }.toSet().size == incoming.size) {
                "Hierarchy placement payload contains duplicate ids"
            }

            val candidates = incoming.map { it.toHierarchyPlacementTransportStrict() }
            val localById =
                dao.getAll()
                    .map { it.toHierarchyPlacementStrict() }
                    .associateBy { it.id }

            candidates.forEach { candidate ->
                localById[candidate.id]?.let { local ->
                    requireStablePlacementIdentity(local, candidate)
                }
            }

            val prospective = localById.toMutableMap()
            val winners = mutableListOf<HierarchyPlacement>()

            candidates.forEach { candidate ->
                val local = localById[candidate.id]
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
                            throw HierarchyPlacementMergeConflictException(
                                "Equal-freshness hierarchy placement divergence for ${candidate.id.value}",
                            )
                    }

                winner?.copy(syncedAt = null)?.let { accepted ->
                    prospective[accepted.id] = accepted
                    winners += accepted
                }
            }

            database.requireValidProspectiveHierarchy(prospective.values)
            if (winners.isNotEmpty()) {
                dao.upsertAll(winners.map { it.toHierarchyPlacementEntity() })
            }
        }
    }

    /**
     * Validate a full replacement before any destructive clear starts.
     * Canonical targets are resolved from the restore payload itself.
     */
    fun decodeAndValidateForRestore(
        bundle: SnapshotBundle,
        snapshots: List<HierarchyPlacementSnapshot>,
    ): List<HierarchyPlacement> {
        require(snapshots.map { it.id }.toSet().size == snapshots.size) {
            "Hierarchy placement restore payload contains duplicate ids"
        }

        val placements = snapshots.map { it.toHierarchyPlacementTransportStrict() }
        val subjects = bundle.managedSubjects.orEmpty().associateBy { it.id }
        val workspaces = bundle.workspaces.orEmpty().associateBy { it.id }

        fun targetState(target: HierarchyTargetRef): RestoreTargetState =
            when (target.type) {
                HierarchyTargetType.MANAGED_SUBJECT ->
                    subjects[target.id]?.let {
                        if (it.isDeleted) RestoreTargetState.DELETED else RestoreTargetState.LIVE
                    } ?: RestoreTargetState.MISSING

                HierarchyTargetType.WORKSPACE ->
                    workspaces[target.id]?.let {
                        if (it.isDeleted) RestoreTargetState.DELETED else RestoreTargetState.LIVE
                    } ?: RestoreTargetState.MISSING
            }

        placements.asSequence()
            .filterNot { it.isDeleted }
            .map { it.target }
            .distinct()
            .firstOrNull { targetState(it) != RestoreTargetState.LIVE }
            ?.let { target ->
                when (targetState(target)) {
                    RestoreTargetState.MISSING ->
                        throw HierarchyTargetMissingException(target)
                    RestoreTargetState.DELETED ->
                        throw HierarchyTargetDeletedException(target)
                    RestoreTargetState.LIVE -> Unit
                }
            }

        val violations =
            validateProspectiveHierarchy(placements) { target ->
                targetState(target) == RestoreTargetState.LIVE
            }
        violations.firstOrNull()?.let { violation ->
            throw InvalidHierarchyStateException(
                "${violation.code}: ${violation.message}",
            )
        }

        return placements
    }

    /**
     * Called after restore targets exist, inside the outer restore transaction.
     * No merge freshness is applied and transported syncedAt is preserved.
     */
    suspend fun restoreExactDecoded(placements: List<HierarchyPlacement>) {
        database.requireValidProspectiveHierarchy(placements)
        if (placements.isNotEmpty()) {
            dao.upsertAll(placements.map { it.toHierarchyPlacementEntity() })
        }
    }

    suspend fun markSynced(versions: List<HierarchyPlacementSyncVersion>) {
        if (versions.isEmpty()) return

        val syncedAt = System.currentTimeMillis()
        database.withTransaction {
            versions.forEach { sent ->
                dao.markSyncedIfVersionMatches(
                    id = sent.id,
                    version = sent.version,
                    syncedAt = syncedAt,
                )
            }
        }
    }
}

private enum class RestoreTargetState {
    LIVE,
    MISSING,
    DELETED,
}

private fun HierarchyPlacementSnapshot.toHierarchyPlacementTransportStrict(): HierarchyPlacement =
    try {
        require(id.isNotBlank()) { "Hierarchy placement id must not be blank" }
        require(hierarchyId == HierarchyId.GENERAL.value) {
            "Unsupported hierarchyId=$hierarchyId"
        }
        require(targetId.isNotBlank()) {
            "Hierarchy placement targetId must not be blank"
        }
        val transportParentPlacementId = parentPlacementId
        require(transportParentPlacementId == null || transportParentPlacementId.isNotBlank()) {
            "Hierarchy placement parentPlacementId must not be blank"
        }
        HierarchyPlacement(
            id = PlacementId(id),
            hierarchyId = HierarchyId.GENERAL,
            target =
                HierarchyTargetRef(
                    type = HierarchyTargetType.valueOf(targetType),
                    id = targetId,
                ),
            parentPlacementId = transportParentPlacementId?.let(::PlacementId),
            placementKind = PlacementKind.valueOf(placementKind),
            siblingOrder = siblingOrder,
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncedAt = syncedAt,
            isDeleted = isDeleted,
            version = version,
        )
    } catch (failure: RuntimeException) {
        throw IllegalArgumentException(
            "Malformed hierarchy placement snapshot id=$id",
            failure,
        )
    }

internal fun HierarchyPlacement.toTransportSnapshot(): HierarchyPlacementSnapshot =
    HierarchyPlacementSnapshot(
        id = id.value,
        hierarchyId = hierarchyId.value,
        targetType = target.type.name,
        targetId = target.id,
        parentPlacementId = parentPlacementId?.value,
        placementKind = placementKind.name,
        siblingOrder = siblingOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

private fun requireStablePlacementIdentity(
    local: HierarchyPlacement,
    incoming: HierarchyPlacement,
) {
    require(
        local.hierarchyId == incoming.hierarchyId &&
            local.target == incoming.target &&
            local.placementKind == incoming.placementKind &&
            local.createdAt == incoming.createdAt,
    ) {
        "Hierarchy placement durable identity cannot change for ${local.id.value}"
    }
}

private fun HierarchyPlacement.sameTransportSemanticState(
    other: HierarchyPlacement,
): Boolean =
    hierarchyId == other.hierarchyId &&
        target == other.target &&
        parentPlacementId == other.parentPlacementId &&
        placementKind == other.placementKind &&
        siblingOrder == other.siblingOrder &&
        createdAt == other.createdAt &&
        updatedAt == other.updatedAt &&
        isDeleted == other.isDeleted &&
        version == other.version

private fun HierarchyPlacement.differsOnlyByDeletionFrom(
    other: HierarchyPlacement,
): Boolean =
    hierarchyId == other.hierarchyId &&
        target == other.target &&
        parentPlacementId == other.parentPlacementId &&
        placementKind == other.placementKind &&
        siblingOrder == other.siblingOrder &&
        createdAt == other.createdAt &&
        updatedAt == other.updatedAt &&
        version == other.version &&
        isDeleted != other.isDeleted
