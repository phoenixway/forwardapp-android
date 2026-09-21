package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import javax.inject.Inject
import javax.inject.Singleton

data class HierarchyGroupRetirementResult(
    val convertedToNoGroup: List<PlacementId>,
    val removedRootOccurrences: List<PlacementId>,
    val removedOccurrences: List<PlacementId>,
)

/**
 * Dormant H4.0e mutation seam for occurrence-scoped Group/NoGroup provenance.
 *
 * Group remains synthetic. This coordinator never creates a Group hierarchy
 * parent and never derives occurrence identity from PART_OF or ordering.
 *
 * Methods do not open a transaction. Future P2 callers must fuse H1,
 * GroupScope and canonical PART_OF changes in one outer Room transaction.
 */
@Singleton
class HierarchyPlacementGroupScopeMutationCoordinator
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        private val placementDao: HierarchyPlacementDao
            get() = database.hierarchyPlacementDao()

        private val scopeDao: HierarchyPlacementGroupScopeDao
            get() = database.hierarchyPlacementGroupScopeDao()

        /**
         * Assign exact root occurrence provenance.
         *
         * null groupSubjectId means explicit NoGroup. PART_OF remains separately
         * owned semantic state and must be reconciled by the outer command.
         */
        suspend fun setRootScope(
            placementId: PlacementId,
            groupSubjectId: String?,
            now: Long,
        ) {
            require(groupSubjectId == null || groupSubjectId.isNotBlank()) {
                "Hierarchy Group scope cannot use a blank Group subject id"
            }

            val placement =
                placementDao.getById(placementId.value)
                    ?.toHierarchyPlacementStrict()
                    ?.takeUnless { it.isDeleted }
                    ?: throw HierarchyPlacementNotFoundException(placementId)

            require(placement.hierarchyId == HierarchyId.GENERAL) {
                "Group scope is supported only for GENERAL hierarchy"
            }
            require(
                placement.parentPlacementId == null &&
                    placement.target.type == HierarchyTargetType.MANAGED_SUBJECT,
            ) {
                "Group scope requires a live root MANAGED_SUBJECT occurrence: ${placementId.value}"
            }

            val current = scopeDao.getByPlacementId(placementId.value)
            if (
                current != null &&
                !current.isDeleted &&
                current.hierarchyId == placement.hierarchyId.value &&
                current.groupSubjectId == groupSubjectId
            ) {
                return
            }

            val updated =
                if (current == null) {
                    HierarchyPlacementGroupScopeEntity(
                        placementId = placementId.value,
                        hierarchyId = placement.hierarchyId.value,
                        groupSubjectId = groupSubjectId,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                } else {
                    require(current.hierarchyId == placement.hierarchyId.value) {
                        "Hierarchy Group-scope durable hierarchy identity cannot change for ${placementId.value}"
                    }
                    current.copy(
                        groupSubjectId = groupSubjectId,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = current.version + 1L,
                    )
                }

            scopeDao.upsert(updated)
        }

        /**
         * Retire provenance when an exact occurrence ceases to be a synthetic
         * Group/NoGroup root.
         */
        suspend fun retireScope(
            placementId: PlacementId,
            now: Long,
        ): Boolean {
            val current = scopeDao.getByPlacementId(placementId.value) ?: return false
            if (current.isDeleted) return false

            scopeDao.upsert(
                current.copy(
                    updatedAt = now,
                    syncedAt = null,
                    isDeleted = true,
                    version = current.version + 1L,
                ),
            )
            return true
        }

        internal suspend fun retireScopesForPlacements(
            placementIds: Collection<PlacementId>,
            now: Long,
        ): Int {
            var changed = 0
            placementIds
                .distinct()
                .sortedBy(PlacementId::value)
                .forEach { placementId ->
                    if (retireScope(placementId, now)) {
                        changed += 1
                    }
                }
            return changed
        }

        /**
         * Prepare H1 + GroupScope state for deletion of one canonical Group.
         *
         * CURRENT parity:
         * - if a target retains another Group root, all exact roots belonging
         *   to the deleted Group and their occurrence-owned subtrees disappear;
         * - if no other Group root remains for that target, affected roots
         *   survive as explicit NoGroup roots.
         *
         * This method does not mutate PART_OF or tombstone the Group target.
         * The outer transaction performs those changes and then calls
         * validateAuthoritativeState().
         */
        suspend fun reconcileGroupRetirement(
            groupSubjectId: String,
            now: Long,
        ): HierarchyGroupRetirementResult {
            require(groupSubjectId.isNotBlank()) {
                "Deleted Group subject id must not be blank"
            }

            val placements =
                placementDao.getAll()
                    .map { it.toHierarchyPlacementStrict() }
                    .associateBy { it.id }
            val liveScopes =
                scopeDao.getLiveForHierarchy(HierarchyId.GENERAL.value)

            val placementByScopeId =
                liveScopes.associate { scope ->
                    val placement =
                        placements[PlacementId(scope.placementId)]
                            ?: error(
                                "Live Group scope ${scope.placementId} references missing H1 placement",
                            )
                    scope.placementId to placement
                }

            val affectedScopes =
                liveScopes.filter { it.groupSubjectId == groupSubjectId }

            if (affectedScopes.isEmpty()) {
                return HierarchyGroupRetirementResult(
                    convertedToNoGroup = emptyList(),
                    removedRootOccurrences = emptyList(),
                    removedOccurrences = emptyList(),
                )
            }

            affectedScopes.forEach { scope ->
                val placement = placementByScopeId.getValue(scope.placementId)
                require(
                    !placement.isDeleted &&
                        placement.parentPlacementId == null &&
                        placement.target.type == HierarchyTargetType.MANAGED_SUBJECT,
                ) {
                    "Deleted Group scope ${scope.placementId} is not a live root MANAGED_SUBJECT"
                }
            }

            val liveScopesByTarget =
                liveScopes.groupBy { scope ->
                    placementByScopeId.getValue(scope.placementId).target.id
                }

            val rootsToRemove = linkedSetOf<PlacementId>()
            val rootsToNoGroup = linkedSetOf<PlacementId>()

            affectedScopes
                .groupBy { scope ->
                    placementByScopeId.getValue(scope.placementId).target.id
                }
                .toSortedMap()
                .forEach { (targetId, targetAffectedScopes) ->
                    val affectedIds =
                        targetAffectedScopes
                            .mapTo(hashSetOf()) { it.placementId }
                    val remainingScopes =
                        liveScopesByTarget[targetId]
                            .orEmpty()
                            .filter { it.placementId !in affectedIds }

                    require(remainingScopes.none { it.groupSubjectId == null }) {
                        "Grouped ManagedSubject $targetId unexpectedly mixes NoGroup provenance"
                    }

                    val destination =
                        if (remainingScopes.any { it.groupSubjectId != null }) {
                            rootsToRemove
                        } else {
                            rootsToNoGroup
                        }

                    targetAffectedScopes
                        .map { PlacementId(it.placementId) }
                        .sortedBy(PlacementId::value)
                        .forEach(destination::add)
                }

            val removedOccurrences =
                descendantClosure(
                    placements = placements.values,
                    roots = rootsToRemove,
                )

            if (removedOccurrences.isNotEmpty()) {
                val prospective =
                    placements.mapValues { (id, placement) ->
                        if (id in removedOccurrences) {
                            placement.copy(isDeleted = true)
                        } else {
                            placement
                        }
                    }

                database.requireValidProspectiveHierarchy(prospective.values)

                placementDao.upsertAll(
                    removedOccurrences
                        .sortedBy(PlacementId::value)
                        .map { placementId ->
                            val current = placements.getValue(placementId)
                            current.copy(
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = true,
                                version = current.version + 1L,
                            ).toHierarchyPlacementEntity()
                        },
                )

                retireScopesForPlacements(
                    placementIds = removedOccurrences,
                    now = now,
                )
                HierarchyPlacementLinkedAppearanceMutationCoordinator(database)
                    .retireLinkedAppearancesForPlacements(
                        placementIds = removedOccurrences,
                        now = now,
                    )
            }

            rootsToNoGroup
                .sortedBy(PlacementId::value)
                .forEach { placementId ->
                    setRootScope(
                        placementId = placementId,
                        groupSubjectId = null,
                        now = now,
                    )
                }

            return HierarchyGroupRetirementResult(
                convertedToNoGroup = rootsToNoGroup.sortedBy(PlacementId::value),
                removedRootOccurrences = rootsToRemove.sortedBy(PlacementId::value),
                removedOccurrences = removedOccurrences.sortedBy(PlacementId::value),
            )
        }

        suspend fun validateAuthoritativeState() {
            val orientationDao = database.orientationDao()

            validateHierarchyPlacementGroupScopes(
                scopes = scopeDao.getAll(),
                placements =
                    placementDao.getAll()
                        .map { it.toHierarchyPlacementStrict() },
                subjects = orientationDao.getAllManagedSubjects(),
                orientations = orientationDao.getAllOrientations(),
                mappings = orientationDao.getAllLegacyMappings(),
                relations = orientationDao.getAllOrientationRelations(),
                requireComplete = true,
            )
        }

        private fun descendantClosure(
            placements: Collection<HierarchyPlacement>,
            roots: Set<PlacementId>,
        ): Set<PlacementId> {
            if (roots.isEmpty()) return emptySet()

            val liveChildrenByParent =
                placements.asSequence()
                    .filterNot { it.isDeleted }
                    .filter { it.parentPlacementId != null }
                    .groupBy { requireNotNull(it.parentPlacementId) }

            val result = linkedSetOf<PlacementId>()
            val pending = ArrayDeque<PlacementId>()
            roots.sortedBy(PlacementId::value).forEach(pending::add)

            while (pending.isNotEmpty()) {
                val placementId = pending.removeFirst()
                if (!result.add(placementId)) continue

                liveChildrenByParent[placementId]
                    .orEmpty()
                    .sortedBy { it.id.value }
                    .forEach { pending.add(it.id) }
            }

            return result
        }
    }
