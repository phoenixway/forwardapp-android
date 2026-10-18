package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId

enum class BigPictureContainerOrigin {
    HISTORICAL_GROUP,
    HISTORICAL_NO_GROUP,
    HISTORICAL_NO_BEACON,
    /** Persisted schema-199 generic H1 presentation container. */
    PRESENTATION,
}

/**
 * Presentation-only container. Membership references occurrences, never canonical entity IDs.
 * Historical scopes and generic H1 containers coexist. UI IDs must be resolved
 * through the structural index before mutation; historical scopes have no typed node.
 */
data class BigPictureContainer(
    val id: String,
    val title: String,
    val rootPlacementIds: Set<PlacementId>,
    val origin: BigPictureContainerOrigin,
)

/** Containers may hold any target class. Unassigned roots remain directly visible. */
fun bigPictureRootOccurrences(
    nodes: List<BigPictureNode>,
    containers: List<BigPictureContainer>,
): List<BigPictureNode> {
    require(containers.map { it.id }.distinct().size == containers.size) {
        "Duplicate Big Picture presentation container identity"
    }
    val roots = nodes.filter { it.placement.parentPlacementId == null }
    val rootIds = roots.mapTo(hashSetOf()) { it.placement.id }
    val assigned = hashSetOf<PlacementId>()
    containers.forEach { container ->
        container.rootPlacementIds.forEach { placementId ->
            require(placementId in rootIds) { "Container references a missing/non-root occurrence: $placementId" }
            require(assigned.add(placementId)) { "Occurrence assigned to multiple containers: $placementId" }
        }
    }
    return roots.filterNot { it.placement.id in assigned }
}
