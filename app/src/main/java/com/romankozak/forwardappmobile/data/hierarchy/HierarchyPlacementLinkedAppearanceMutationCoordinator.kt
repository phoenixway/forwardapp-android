package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementLinkedAppearanceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime mutation seam for sparse exact linked-presentation provenance.
 *
 * A live row means true for exactly one Workspace occurrence. Absence means
 * false. PlacementKind must never be used as a substitute for this metadata.
 *
 * Methods do not open a transaction. Callers fuse sidecar changes with H1 in
 * the same outer Room transaction.
 */
@Singleton
class HierarchyPlacementLinkedAppearanceMutationCoordinator
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        private val placementDao: HierarchyPlacementDao
            get() = database.hierarchyPlacementDao()

        private val linkedDao: HierarchyPlacementLinkedAppearanceDao
            get() = database.hierarchyPlacementLinkedAppearanceDao()

        suspend fun setLinkedAppearance(
            placementId: PlacementId,
            now: Long,
        ) {
            val placement =
                placementDao.getById(placementId.value)
                    ?.toHierarchyPlacementStrict()
                    ?.takeUnless { it.isDeleted }
                    ?: throw HierarchyPlacementNotFoundException(placementId)

            require(placement.hierarchyId == HierarchyId.GENERAL) {
                "Linked appearance is supported only for GENERAL hierarchy"
            }
            require(placement.target.type == HierarchyTargetType.WORKSPACE) {
                "Linked appearance requires a live Workspace occurrence: ${placementId.value}"
            }

            val current = linkedDao.getByPlacementId(placementId.value)
            if (
                current != null &&
                !current.isDeleted &&
                current.hierarchyId == placement.hierarchyId.value
            ) {
                return
            }

            val updated =
                if (current == null) {
                    HierarchyPlacementLinkedAppearanceEntity(
                        placementId = placementId.value,
                        hierarchyId = placement.hierarchyId.value,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                } else {
                    require(current.hierarchyId == placement.hierarchyId.value) {
                        "Hierarchy linked-appearance durable hierarchy identity cannot change for ${placementId.value}"
                    }
                    current.copy(
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = current.version + 1L,
                    )
                }

            linkedDao.upsert(updated)
        }

        suspend fun retireLinkedAppearance(
            placementId: PlacementId,
            now: Long,
        ): Boolean {
            val current = linkedDao.getByPlacementId(placementId.value) ?: return false
            if (current.isDeleted) return false

            val placement =
                placementDao.getById(placementId.value)
                    ?.toHierarchyPlacementStrict()
                    ?: throw HierarchyPlacementNotFoundException(placementId)

            require(placement.hierarchyId.value == current.hierarchyId) {
                "Hierarchy linked-appearance ${placementId.value} hierarchyId does not match H1"
            }
            require(placement.target.type == HierarchyTargetType.WORKSPACE) {
                "Hierarchy linked-appearance ${placementId.value} must reference a Workspace occurrence"
            }

            linkedDao.upsert(
                current.copy(
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = true,
                    version = current.version + 1L,
                ),
            )
            return true
        }

        internal suspend fun retireLinkedAppearancesForPlacements(
            placementIds: Collection<PlacementId>,
            now: Long,
        ): Int {
            var changed = 0
            placementIds
                .distinct()
                .sortedBy(PlacementId::value)
                .forEach { placementId ->
                    if (retireLinkedAppearance(placementId, now)) {
                        changed += 1
                    }
                }
            return changed
        }

        /**
         * Restore provenance only when this exact sidecar was tombstoned by the
         * same occurrence-removal operation.
         *
         * A separately retired linked-presentation row must stay retired even
         * if its H1 occurrence is restored later, so deletion-time correlation
         * is deliberately required.
         */
        suspend fun restoreIfRetiredWithPlacement(
            placementId: PlacementId,
            placementDeletedAt: Long,
            now: Long,
        ): Boolean {
            val current = linkedDao.getByPlacementId(placementId.value) ?: return false
            if (!current.isDeleted || current.updatedAt != placementDeletedAt) return false

            val placement =
                placementDao.getById(placementId.value)
                    ?.toHierarchyPlacementStrict()
                    ?.takeUnless { it.isDeleted }
                    ?: throw HierarchyPlacementNotFoundException(placementId)

            require(placement.hierarchyId.value == current.hierarchyId) {
                "Hierarchy linked-appearance ${placementId.value} hierarchyId does not match H1"
            }
            require(placement.target.type == HierarchyTargetType.WORKSPACE) {
                "Hierarchy linked-appearance ${placementId.value} must reference a Workspace occurrence"
            }

            linkedDao.upsert(
                current.copy(
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = false,
                    version = current.version + 1L,
                ),
            )
            return true
        }
    }
