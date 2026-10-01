package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement

/** Pure validation shared by runtime and migration-time H2 persistence adapters. */
internal fun validateCanonicalV1HierarchySnapshotShape(
    snapshot: CanonicalV1HierarchySnapshot,
) {
    val byKey = snapshot.occurrences.associateBy { it.occurrenceKey }
    require(byKey.size == snapshot.occurrences.size) {
        "CanonicalV1HierarchySnapshot contains duplicate occurrenceKey values"
    }

    snapshot.occurrences.forEach { occurrence ->
        require(occurrence.occurrenceKey.isNotBlank()) {
            "CanonicalV1HierarchySnapshot occurrenceKey must not be blank"
        }
        occurrence.parentOccurrenceKey?.let { parentKey ->
            require(parentKey in byKey) {
                "Snapshot occurrence ${occurrence.occurrenceKey} references missing parent $parentKey"
            }
            require(parentKey != occurrence.occurrenceKey) {
                "Snapshot occurrence ${occurrence.occurrenceKey} cannot parent itself"
            }
        }
    }

    snapshot.occurrences
        .groupBy { it.parentOccurrenceKey }
        .forEach { (parentKey, siblings) ->
            val orders = siblings.map { it.siblingOrder }
            require(orders.distinct().size == orders.size) {
                "Snapshot siblings under $parentKey contain duplicate siblingOrder values"
            }
            require(orders.sorted() == orders.indices.map(Int::toLong)) {
                "Snapshot siblings under $parentKey must use dense deterministic siblingOrder values"
            }
        }
}

/** Exact structural rerun comparison; persistence metadata is intentionally ignored. */
internal fun exactCanonicalV1HierarchyStructuralMatch(
    existing: List<HierarchyPlacement>,
    desired: List<HierarchyPlacement>,
): Boolean {
    if (existing.size != desired.size) return false
    val existingById = existing.associateBy { it.id }

    return desired.all { wanted ->
        val current = existingById[wanted.id] ?: return@all false
        !current.isDeleted &&
            current.hierarchyId == wanted.hierarchyId &&
            current.target == wanted.target &&
            current.parentPlacementId == wanted.parentPlacementId &&
            current.placementKind == wanted.placementKind &&
            current.siblingOrder == wanted.siblingOrder
    }
}
