package com.romankozak.forwardappmobile.data.hierarchy.container

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyPlacementStrict
import com.romankozak.forwardappmobile.database.AppDatabase

/**
 * Legacy occurrence-primitive compatibility cleanup only; never used by
 * the typed H1StructuralMutationCoordinator. Caller owns the transaction.
 *
 * Reconcile presentation only. No semantic target, ancestry or GroupScope
 * writes originate here.
 */
internal class BigPictureHierarchyLifecycleCoordinator(
    private val db: AppDatabase,
) {
    suspend fun reconcileLegacyOccurrenceMutation(now: Long) {
        val before = BigPicturePresentationRepository(db).snapshot()
        if (before.containers.isEmpty() && before.memberships.isEmpty()) return

        val placements =
            db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }
        val byPlacement = placements.associateBy { it.id.value }
        val containers = before.containers.associateBy { it.id }.toMutableMap()

        // When an H1 anchor is retired, its sections and all nested sections
        // become historical tombstones. A live section must never sit below
        // a deleted presentation parent.
        var changed: Boolean
        do {
            changed = false
            before.containers.forEach { row ->
                if (containers[row.id]?.isDeleted != false) return@forEach

                val anchorRetired =
                    row.anchorPlacementId?.let { byPlacement[it]?.isDeleted == true } == true
                val parentRetired =
                    row.parentContainerId?.let { containers[it]?.isDeleted == true } == true

                if (anchorRetired || parentRetired) {
                    containers[row.id] =
                        row.copy(
                            isDeleted = true,
                            version = row.version + 1,
                            updatedAt = nextLifecycleTime(row.updatedAt, now),
                            syncedAt = null,
                        )
                    changed = true
                }
            }
        } while (changed)

        val memberships = before.memberships.map { row ->
            if (row.isDeleted) return@map row

            val placement = byPlacement[row.placementId]
            val container = containers[row.containerId]

            val invalidLocation =
                placement != null &&
                    container != null &&
                    placement.parentPlacementId?.value != container.anchorPlacementId

            if (placement?.isDeleted == true ||
                container?.isDeleted == true ||
                invalidLocation
            ) {
                row.copy(
                    isDeleted = true,
                    version = row.version + 1,
                    updatedAt = nextLifecycleTime(row.updatedAt, now),
                    syncedAt = null,
                )
            } else {
                row
            }
        }

        val after = BigPicturePresentationSnapshot(
            containers = before.containers.map { containers.getValue(it.id) },
            memberships = memberships,
            formatVersion = before.formatVersion,
        )

        // Fail closed on foreign anchors, missing identities or invalid graph
        // topology. The caller's transaction will roll back the H1 mutation.
        projectH1UnifiedStructuralGraph(placements, after)

        val changedContainers =
            after.containers.filter { it != before.containers.first { old -> old.id == it.id } }
        val beforeMemberships = before.memberships.associateBy { it.placementId }
        val changedMemberships =
            after.memberships.filter { it != beforeMemberships[it.placementId] }

        val dao = db.bigPicturePresentationDao()
        if (changedContainers.isNotEmpty()) {
            dao.upsertContainers(changedContainers.map(::BigPictureContainerEntity))
        }
        if (changedMemberships.isNotEmpty()) {
            dao.upsertMemberships(changedMemberships.map(::BigPictureMembershipEntity))
        }
    }
}

private fun nextLifecycleTime(previous: Long, requested: Long): Long {
    if (requested > previous) return requested
    require(previous < Long.MAX_VALUE) {
        "Cannot advance Big Picture lifecycle timestamp"
    }
    return previous + 1
}
