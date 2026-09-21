package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lifecycle-dependent persistence for canonical target owners.
 *
 * It does not own target lifecycle and does not open a transaction. Callers
 * invoke it inside the same AppDatabase transaction that tombstones the target.
 */
@Singleton
class HierarchyPlacementLifecycleCoordinator
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        suspend fun tombstoneManagedSubjectTarget(
            targetId: String,
            now: Long,
        ): Int =
            tombstoneTargets(
                targetType = HierarchyTargetType.MANAGED_SUBJECT,
                targetIds = listOf(targetId),
                now = now,
            )

        suspend fun tombstoneWorkspaceTarget(
            targetId: String,
            now: Long,
        ): Int =
            tombstoneWorkspaceTargets(listOf(targetId), now)

        suspend fun tombstoneWorkspaceTargets(
            targetIds: Collection<String>,
            now: Long,
        ): Int =
            tombstoneTargets(
                targetType = HierarchyTargetType.WORKSPACE,
                targetIds = targetIds.toList(),
                now = now,
            )

        private suspend fun tombstoneTargets(
            targetType: HierarchyTargetType,
            targetIds: List<String>,
            now: Long,
        ): Int {
            val ids =
                targetIds.asSequence()
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                    .toList()
            if (ids.isEmpty()) return 0

            val dao = database.hierarchyPlacementDao()
            val before =
                dao.getAll()
                    .map { it.toHierarchyPlacementStrict() }
                    .associateBy { it.id }
            val affected =
                dao.getLiveByTargets(targetType.name, ids)
                    .map { it.toHierarchyPlacementStrict().id }
                    .toSet()
            if (affected.isEmpty()) return 0

            val prospective =
                before.mapValues { (id, placement) ->
                    if (id in affected) {
                        placement.copy(isDeleted = true)
                    } else {
                        placement
                    }
                }

            prospective.values
                .firstOrNull {
                    !it.isDeleted && it.parentPlacementId in affected
                }
                ?.parentPlacementId
                ?.let { throw HierarchyChildPolicyRejectedException(it) }

            database.requireValidProspectiveHierarchy(prospective.values)

            HierarchyPlacementGroupScopeMutationCoordinator(database)
                .retireScopesForPlacements(
                    placementIds = affected,
                    now = now,
                )
            HierarchyPlacementLinkedAppearanceMutationCoordinator(database)
                .retireLinkedAppearancesForPlacements(
                    placementIds = affected,
                    now = now,
                )

            dao.upsertAll(
                affected.sortedBy(PlacementId::value).map { id ->
                    val current = before.getValue(id)
                    current.copy(
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = true,
                        version = current.version + 1L,
                    ).toHierarchyPlacementEntity()
                },
            )
            return affected.size
        }
    }
