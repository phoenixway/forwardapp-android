package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType

/**
 * Presentation metadata for one canonical Beacon Group.
 *
 * [canonicalSubjectId] owns semantic PART_OF membership. [id]/[title]/[order]
 * are presentation metadata only and never participate in persisted topology.
 */
data class CanonicalV2HierarchyGroupPresentation(
    val id: String,
    val canonicalSubjectId: String,
    val title: String,
    val order: Long,
)

/**
 * Builds synthetic Group/NoGroup presentation scopes from persisted
 * occurrence-scoped provenance.
 *
 * HierarchyPlacement owns topology. HierarchyPlacementGroupScope owns the exact
 * Group/NoGroup scope of each root MANAGED_SUBJECT occurrence. PART_OF remains
 * target-wide semantic state and is checked for consistency only. It can never
 * choose an occurrence, create a parent, or recover missing provenance.
 */
class CanonicalV2HierarchyScopePlanner {
    fun plan(
        hierarchy: CanonicalV2HierarchyProjection,
        groups: Collection<CanonicalV2HierarchyGroupPresentation>,
        relations: Collection<OrientationRelationEntity>,
        groupScopes: Collection<HierarchyPlacementGroupScopeEntity>,
        noGroupTitle: String = "No group",
    ): List<CanonicalV2SyntheticScopeInput> {
        val groupList =
            groups.sortedWith(
                compareBy<CanonicalV2HierarchyGroupPresentation> { it.order }
                    .thenBy { it.title.lowercase() }
                    .thenBy { it.id },
            )

        require(groupList.map { it.id }.distinct().size == groupList.size) {
            "Duplicate Group presentation id"
        }
        require(
            groupList.map { it.canonicalSubjectId }.distinct().size ==
                groupList.size,
        ) {
            "Duplicate canonical Group subject"
        }

        val groupBySubjectId = groupList.associateBy { it.canonicalSubjectId }
        val membershipGroupSubjectsByTarget =
            relations.asSequence()
                .filter {
                    !it.isDeleted &&
                        it.relationType == OrientationRelationType.PART_OF.name &&
                        it.toOrientationId in groupBySubjectId
                }
                .groupBy { it.fromOrientationId }
                .mapValues { (_, edges) ->
                    edges.mapTo(linkedSetOf()) { it.toOrientationId }
                }

        val occurrenceById = hierarchy.occurrences.associateBy { it.placementId }
        val roots =
            hierarchy.rootPlacementIds.map { rootId ->
                requireNotNull(occurrenceById[rootId]) {
                    "Projection root ${rootId.value} is missing"
                }
            }
        val subjectRoots =
            roots.filter { it.target.type == HierarchyTargetType.MANAGED_SUBJECT }
        val subjectRootIds =
            subjectRoots.mapTo(linkedSetOf()) { it.placementId.value }

        val liveScopes = groupScopes.filterNot { it.isDeleted }
        require(
            liveScopes.all { it.hierarchyId == hierarchy.hierarchyId.value },
        ) {
            "Live Group-scope state contains a foreign hierarchy"
        }

        val liveScopeByPlacementId = liveScopes.associateBy { it.placementId }
        require(liveScopeByPlacementId.size == liveScopes.size) {
            "Hierarchy Group-scope state contains duplicate live placementIds"
        }

        require(liveScopeByPlacementId.keys == subjectRootIds) {
            val missing = (subjectRootIds - liveScopeByPlacementId.keys).take(5)
            val extra = (liveScopeByPlacementId.keys - subjectRootIds).take(5)
            "Persisted Group-scope provenance must cover every root MANAGED_SUBJECT; " +
                "missing=$missing extra=$extra"
        }

        subjectRoots
            .groupBy { it.target }
            .forEach { (target, targetRoots) ->
                val scopes =
                    targetRoots.map { root ->
                        requireNotNull(liveScopeByPlacementId[root.placementId.value])
                    }
                val hasNoGroup = scopes.any { it.groupSubjectId == null }
                val groupedSubjectIds =
                    scopes.mapNotNullTo(linkedSetOf()) { scope ->
                        scope.groupSubjectId?.also { groupSubjectId ->
                            require(groupSubjectId in groupBySubjectId) {
                                "Group scope ${scope.placementId} references unknown canonical " +
                                    "Group $groupSubjectId"
                            }
                        }
                    }
                val memberships =
                    membershipGroupSubjectsByTarget[target.id].orEmpty()

                require(!(hasNoGroup && groupedSubjectIds.isNotEmpty())) {
                    "ManagedSubject ${target.id} cannot mix Group and NoGroup root provenance"
                }

                if (hasNoGroup) {
                    require(memberships.isEmpty()) {
                        "NoGroup root provenance for ${target.id} conflicts with PART_OF membership"
                    }
                } else {
                    require(groupedSubjectIds == memberships) {
                        "Grouped root provenance for ${target.id} does not match PART_OF membership"
                    }
                }
            }

        val rootsByGroupSubject =
            groupList.associate { group ->
                group.canonicalSubjectId to mutableListOf<PlacementId>()
            }.toMutableMap()
        val noGroupPlacementIds = mutableListOf<PlacementId>()

        subjectRoots.forEach { root ->
            val scope =
                requireNotNull(liveScopeByPlacementId[root.placementId.value])

            val groupSubjectId = scope.groupSubjectId
            if (groupSubjectId == null) {
                noGroupPlacementIds += root.placementId
            } else {
                requireNotNull(rootsByGroupSubject[groupSubjectId]) {
                    "Group scope ${scope.placementId} references unknown canonical Group " +
                        groupSubjectId
                } += root.placementId
            }
        }

        return buildList {
            groupList.forEach { group ->
                add(
                    CanonicalV2SyntheticScopeInput(
                        kind = CanonicalV2SyntheticScopeKind.GROUP,
                        id = group.id,
                        title = group.title,
                        order = group.order,
                        rootPlacementIds =
                            rootsByGroupSubject.getValue(group.canonicalSubjectId),
                    ),
                )
            }

            if (noGroupPlacementIds.isNotEmpty()) {
                add(
                    CanonicalV2SyntheticScopeInput(
                        kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                        id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                        title = noGroupTitle,
                        order = 0,
                        rootPlacementIds = noGroupPlacementIds,
                    ),
                )
            }
        }
    }
}
