package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType

internal fun validateHierarchyPlacementGroupScopes(
    scopes: Collection<HierarchyPlacementGroupScopeEntity>,
    placements: Collection<HierarchyPlacement>,
    subjects: Collection<ManagedSubjectEntity>,
    orientations: Collection<OrientationEntity>,
    mappings: Collection<LegacySubjectMappingEntity>,
    relations: Collection<OrientationRelationEntity>,
    requireComplete: Boolean,
) {
    val placementById = placements.associateBy { it.id.value }
    val liveGroupSubjectIds =
        canonicalLiveGroupSubjectIds(
            subjects = subjects,
            orientations = orientations,
            mappings = mappings,
        )

    val livePartOfGroupsBySubject =
        relations.asSequence()
            .filter {
                !it.isDeleted &&
                    it.relationType == OrientationRelationType.PART_OF.name &&
                    it.toOrientationId in liveGroupSubjectIds
            }
            .groupBy { it.fromOrientationId }
            .mapValues { (_, edges) ->
                edges.mapTo(linkedSetOf()) { it.toOrientationId }
            }

    val liveScopes = scopes.filterNot { it.isDeleted }
    val liveScopeByPlacementId = liveScopes.associateBy { it.placementId }
    require(liveScopeByPlacementId.size == liveScopes.size) {
        "Hierarchy Group-scope state contains duplicate live placementIds"
    }

    liveScopes.forEach { scope ->
        val placement =
            requireNotNull(placementById[scope.placementId]) {
                "Live Group scope ${scope.placementId} references missing H1 placement"
            }
        require(!placement.isDeleted) {
            "Live Group scope ${scope.placementId} references deleted H1 placement"
        }
        require(scope.hierarchyId == placement.hierarchyId.value) {
            "Group scope ${scope.placementId} hierarchyId does not match H1"
        }
        require(
            placement.parentPlacementId == null &&
                placement.target.type == HierarchyTargetType.MANAGED_SUBJECT,
        ) {
            "Live Group scope ${scope.placementId} must reference a root MANAGED_SUBJECT placement"
        }

        scope.groupSubjectId?.let { groupSubjectId ->
            require(groupSubjectId in liveGroupSubjectIds) {
                "Group scope ${scope.placementId} references non-canonical Group $groupSubjectId"
            }
            require(
                groupSubjectId in
                    livePartOfGroupsBySubject[placement.target.id].orEmpty(),
            ) {
                "Group scope ${scope.placementId} has no live PART_OF membership to $groupSubjectId"
            }
        }
    }

    liveScopes
        .groupBy { scope ->
            requireNotNull(placementById[scope.placementId]).target.id
        }
        .forEach { (targetId, targetScopes) ->
            val hasNoGroup = targetScopes.any { it.groupSubjectId == null }
            val groupedIds =
                targetScopes.mapNotNullTo(linkedSetOf()) { it.groupSubjectId }
            val semanticMemberships =
                livePartOfGroupsBySubject[targetId].orEmpty()

            require(!(hasNoGroup && groupedIds.isNotEmpty())) {
                "ManagedSubject $targetId cannot mix Group and NoGroup root provenance"
            }

            if (hasNoGroup) {
                require(semanticMemberships.isEmpty()) {
                    "NoGroup root provenance for $targetId conflicts with live PART_OF membership"
                }
            } else {
                require(groupedIds == semanticMemberships) {
                    "Grouped root provenance for $targetId does not match target-wide PART_OF membership"
                }
            }
        }

    if (requireComplete) {
        val liveRootSubjectPlacementIds =
            placements.asSequence()
                .filter {
                    !it.isDeleted &&
                        it.parentPlacementId == null &&
                        it.target.type == HierarchyTargetType.MANAGED_SUBJECT
                }
                .mapTo(linkedSetOf()) { it.id.value }

        require(liveScopeByPlacementId.keys == liveRootSubjectPlacementIds) {
            val missing =
                (liveRootSubjectPlacementIds - liveScopeByPlacementId.keys).take(5)
            val extra =
                (liveScopeByPlacementId.keys - liveRootSubjectPlacementIds).take(5)
            "Authoritative Group-scope stream must cover every live root MANAGED_SUBJECT; " +
                "missing=$missing extra=$extra"
        }
    }
}

private fun canonicalLiveGroupSubjectIds(
    subjects: Collection<ManagedSubjectEntity>,
    orientations: Collection<OrientationEntity>,
    mappings: Collection<LegacySubjectMappingEntity>,
): Set<String> {
    val liveOrientationSubjectIds =
        subjects.asSequence()
            .filter {
                !it.isDeleted &&
                    it.subjectType == ManagedSubjectType.ORIENTATION.name
            }
            .mapTo(hashSetOf()) { it.id }

    val canonicalGroupOrientationIds =
        orientations.asSequence()
            .filter {
                it.subjectId in liveOrientationSubjectIds &&
                    it.kind == OrientationKind.MAIN_BEACON_GROUP.name
            }
            .mapTo(hashSetOf()) { it.subjectId }

    return mappings.asSequence()
        .filter {
            !it.isDeleted &&
                it.sourceType == LegacyOrientationSourceType.MAIN_BEACON_GROUP.name &&
                it.state == LegacySubjectMappingState.CUT_OVER.name &&
                it.subjectId in canonicalGroupOrientationIds
        }
        .mapTo(linkedSetOf()) { it.subjectId }
}
