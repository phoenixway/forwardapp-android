package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.projectH1UnifiedStructuralGraph
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lifecycle-dependent persistence for canonical target owners.
 *
 * It does not own target lifecycle. Callers invoke it inside the same
 * AppDatabase transaction that tombstones the target; typed-H1 commands join
 * that transaction. Legacy compatibility helpers below do not open one.
 */
@Singleton
class HierarchyPlacementLifecycleCoordinator
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        /**
         * Current typed-H1 cleanup for generic semantic deletion. Caller owns the outer
         * target transaction. Unlike legacy placement-only retirement, H1 also retires
         * container memberships and updates complete mixed sibling ordering.
         * Unrelated live children still reject deletion; no subtree cascade is inferred.
         */
        suspend fun retireSemanticTargetAppearances(targetId: String, now: Long) =
            retireTypedTargetAppearances(HierarchyTargetType.MANAGED_SUBJECT, targetId, now)

        suspend fun retireWorkspaceTargetAppearances(targetId: String, now: Long) =
            retireTypedTargetAppearances(HierarchyTargetType.WORKSPACE, targetId, now)

        private suspend fun retireTypedTargetAppearances(targetType: HierarchyTargetType, targetId: String, now: Long) {
            val placements = database.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }
            val graph = projectH1UnifiedStructuralGraph(placements, BigPicturePresentationRepository(database).snapshot())
            val affected = placements.filter {
                !it.isDeleted && it.target.type == targetType && it.target.id == targetId
            }
            // Multiple appearances of the same target may be nested. Retire leaves first,
            // while the H1 authority rejects any surviving unrelated typed child.
            fun depth(id: String): Int {
                var node: H1TypedNode? = H1TypedNode.Occurrence(id)
                var depth = 0
                while (node != null) {
                    node = graph.parentOf(node)
                    depth++
                }
                return depth
            }
            H1StructuralMutationCoordinator(database).removeOccurrences(
                affected.sortedWith(compareByDescending<HierarchyPlacement> {
                    depth(it.id.value)
                }.thenBy { it.id.value }).map { it.id }, now,
            )
        }

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
