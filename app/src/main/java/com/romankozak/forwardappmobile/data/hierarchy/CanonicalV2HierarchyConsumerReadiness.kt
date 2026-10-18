package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind

/**
 * Occurrence-native Beacon nesting prepared for the CoreLevel reader.
 *
 * Group membership and operational owner data intentionally do not appear
 * here. They remain independently owned semantic/operational inputs.
 */
data class CanonicalV2CoreLevelOccurrence(
    val placementId: PlacementId,
    val beaconPresentationId: String,
    val parentPlacementId: PlacementId?,
    val parentBeaconPresentationId: String?,
    val placementKind: PlacementKind,
    val siblingOrder: Long,
    val groupPresentationId: String?,
)

/**
 * Thin consumer projections over the one shared production V2 read snapshot.
 *
 * These functions never reconstruct topology from target ids or legacy
 * parent fields.
 */
class CanonicalV2HierarchyConsumerReadiness {
    fun coreLevelOccurrences(
        read: CanonicalV2ProductionHierarchyRead,
    ): List<CanonicalV2CoreLevelOccurrence> {
        val occurrences = read.hierarchy.occurrences.associateBy { it.placementId }
        val admitted = mutableMapOf<PlacementId, Boolean>()

        // A legal mixed Big Picture branch may not fit the historical Beacon
        // surface. Exclude that entire branch here, preserving H1 unchanged.
        // Memoize ancestry so each node is examined once, including deep trees.
        fun admitsBranch(id: PlacementId): Boolean {
            val path = mutableListOf<PlacementId>()
            var cursor: PlacementId? = id
            while (cursor != null && cursor !in admitted) {
                val current = cursor
                val node = requireNotNull(occurrences[current]) { "Missing H1 occurrence ${current.value}" }
                path += current
                val scope = read.syntheticScopeFor(current)
                if (node.target.type != HierarchyTargetType.MANAGED_SUBJECT ||
                    current in read.nativePlacementIds || scope == null ||
                    scope.kind == CanonicalV2SyntheticScopeKind.NO_BEACON
                ) {
                    admitted[current] = false
                    break
                }
                cursor = node.parentPlacementId
            }
            val result = cursor?.let { admitted.getValue(it) } ?: true
            path.forEach { admitted[it] = result }
            return result
        }

        return read.hierarchy.occurrences
            .asSequence()
            .filter { admitsBranch(it.placementId) }
            .map { projected ->
                val presented =
                    requireNotNull(read.occurrence(projected.placementId)) {
                        "Missing presented ManagedSubject occurrence ${projected.placementId.value}"
                    }

                val parent =
                    projected.parentPlacementId?.let { parentId ->
                        requireNotNull(read.occurrence(parentId)) {
                            "Missing parent occurrence ${parentId.value}"
                        }
                    }

                require(
                    parent == null ||
                        parent.target.type == HierarchyTargetType.MANAGED_SUBJECT,
                ) {
                    "CoreLevel ManagedSubject occurrence ${projected.placementId.value} " +
                        "has non-ManagedSubject structural parent " +
                        "${projected.parentPlacementId?.value}"
                }

                val scope =
                    requireNotNull(read.syntheticScopeFor(projected.placementId)) {
                        "CoreLevel occurrence ${projected.placementId.value} has no synthetic scope"
                    }
                require(scope.kind != CanonicalV2SyntheticScopeKind.NO_BEACON) {
                    "CoreLevel ManagedSubject occurrence ${projected.placementId.value} " +
                        "cannot be presented in NoBeacon"
                }

                CanonicalV2CoreLevelOccurrence(
                    placementId = projected.placementId,
                    beaconPresentationId = presented.presentationId,
                    parentPlacementId = projected.parentPlacementId,
                    parentBeaconPresentationId = parent?.presentationId,
                    placementKind = projected.placementKind,
                    siblingOrder = projected.siblingOrder,
                    groupPresentationId =
                        scope.id.takeIf {
                            scope.kind == CanonicalV2SyntheticScopeKind.GROUP
                        },
                )
            }
            .toList()
    }

    /**
     * Search/focus structural ancestry remains exact occurrence ancestry.
     * Target-oriented reveal must first resolve an explicit navigation policy
     * and then call this function with the chosen PlacementId.
     */
    fun occurrenceAncestryForSearch(
        read: CanonicalV2ProductionHierarchyRead,
        placementId: PlacementId,
    ): List<CanonicalV2PresentedHierarchyEntry.Occurrence> =
        read.structuralAncestors(placementId)
}
