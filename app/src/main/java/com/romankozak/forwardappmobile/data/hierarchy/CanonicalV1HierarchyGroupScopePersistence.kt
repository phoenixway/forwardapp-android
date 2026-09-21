package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType

/**
 * Deterministic H2 persistence projection for synthetic Group/NoGroup
 * occurrence provenance.
 *
 * Group remains synthetic and this stream stays separate from H1 topology.
 */
internal fun CanonicalV1HierarchySnapshot.toDeterministicHierarchyPlacementGroupScopes(
    now: Long,
): List<HierarchyPlacementGroupScopeEntity> {
    validateRootGroupScopeProvenance()

    return occurrences.mapNotNull { occurrence ->
        val scope = occurrence.rootGroupScope ?: return@mapNotNull null

        HierarchyPlacementGroupScopeEntity(
            placementId =
                CanonicalV1HierarchyMaterializer.deterministicPlacementId(
                    hierarchyId = hierarchyId.value,
                    occurrenceKey = occurrence.occurrenceKey,
                ).value,
            hierarchyId = hierarchyId.value,
            groupSubjectId =
                when (scope.kind) {
                    CanonicalV1RootGroupScopeKind.GROUP ->
                        requireNotNull(scope.groupSubjectId)

                    CanonicalV1RootGroupScopeKind.NO_GROUP -> null
                },
            createdAt = now,
            updatedAt = now,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
    }
}

private fun CanonicalV1HierarchySnapshot.validateRootGroupScopeProvenance() {
    occurrences.forEach { occurrence ->
        val isManagedSubjectRoot =
            occurrence.parentOccurrenceKey == null &&
                occurrence.target.type == HierarchyTargetType.MANAGED_SUBJECT
        require(isManagedSubjectRoot == (occurrence.rootGroupScope != null)) {
            if (isManagedSubjectRoot) {
                "Root MANAGED_SUBJECT ${occurrence.occurrenceKey} requires explicit Group/NoGroup provenance"
            } else {
                "Only root MANAGED_SUBJECT occurrences may carry Group provenance: ${occurrence.occurrenceKey}"
            }
        }
    }

    val scoped = occurrences.filter { it.rootGroupScope != null }

    scoped.forEach { occurrence ->
        require(occurrence.parentOccurrenceKey == null) {
            "Only root occurrences may carry Group provenance: ${occurrence.occurrenceKey}"
        }
        require(occurrence.target.type == HierarchyTargetType.MANAGED_SUBJECT) {
            "Only MANAGED_SUBJECT roots may carry Group provenance: ${occurrence.occurrenceKey}"
        }

        val scope = requireNotNull(occurrence.rootGroupScope)
        when (scope.kind) {
            CanonicalV1RootGroupScopeKind.GROUP ->
                require(!scope.groupSubjectId.isNullOrBlank()) {
                    "GROUP root ${occurrence.occurrenceKey} has no canonical Group subject"
                }

            CanonicalV1RootGroupScopeKind.NO_GROUP ->
                require(scope.groupSubjectId == null) {
                    "NO_GROUP root ${occurrence.occurrenceKey} cannot carry Group subject"
                }
        }
    }

    scoped
        .groupBy { it.target }
        .forEach { (target, appearances) ->
            val kinds =
                appearances.mapTo(linkedSetOf()) {
                    requireNotNull(it.rootGroupScope).kind
                }
            require(
                !(
                    CanonicalV1RootGroupScopeKind.GROUP in kinds &&
                        CanonicalV1RootGroupScopeKind.NO_GROUP in kinds
                ),
            ) {
                "ManagedSubject ${target.id} cannot have grouped and NoGroup root occurrences simultaneously"
            }
        }
}

internal fun exactDeterministicHierarchyGroupScopeMatch(
    existing: List<HierarchyPlacementGroupScopeEntity>,
    desired: List<HierarchyPlacementGroupScopeEntity>,
): Boolean {
    if (existing.size != desired.size) return false
    val existingByPlacement = existing.associateBy { it.placementId }

    return desired.all { wanted ->
        val current = existingByPlacement[wanted.placementId] ?: return@all false
        !current.isDeleted &&
            current.hierarchyId == wanted.hierarchyId &&
            current.groupSubjectId == wanted.groupSubjectId
    }
}

internal fun buildGroupScopeConflictMessage(
    existing: List<HierarchyPlacementGroupScopeEntity>,
    desired: List<HierarchyPlacementGroupScopeEntity>,
): String {
    val existingIds = existing.mapTo(sortedSetOf()) { it.placementId }
    val desiredIds = desired.mapTo(sortedSetOf()) { it.placementId }
    val extra = (existingIds - desiredIds).take(5)
    val missing = (desiredIds - existingIds).take(5)

    return buildString {
        append("H2 Group provenance requires an empty table or exact deterministic rerun; ")
        append("existing=${existing.size}, desired=${desired.size}")
        if (extra.isNotEmpty()) append(", extraPlacementIds=$extra")
        if (missing.isNotEmpty()) append(", missingPlacementIds=$missing")
        if (extra.isEmpty() && missing.isEmpty()) {
            append(", same placement ids carry different Group/NoGroup provenance")
        }
    }
}
