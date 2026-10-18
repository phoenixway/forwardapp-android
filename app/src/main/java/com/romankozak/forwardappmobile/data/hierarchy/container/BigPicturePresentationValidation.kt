package com.romankozak.forwardappmobile.data.hierarchy.container

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement

/** H1 owns ancestry; sections wrap siblings at one H1 parent location. No semantic inputs. */
fun validateBigPicturePresentation(
    snapshot: BigPicturePresentationSnapshot,
    placements: Collection<HierarchyPlacement>,
) {
    require(snapshot.formatVersion == 1) { "Unsupported Big Picture presentation format" }
    val byPlacement = placements.associateBy { it.id.value }
    val byContainer = snapshot.containers.associateBy { it.id }
    require(byContainer.size == snapshot.containers.size) { "Duplicate presentation container identity" }
    require(snapshot.memberships.map { it.placementId }.distinct().size == snapshot.memberships.size) {
        "Duplicate occurrence membership identity"
    }
    snapshot.containers.forEach { container ->
        require(container.id.isNotBlank() && container.hierarchyId == "GENERAL" && container.version >= 1)
        require(container.title.isNotBlank()) { "Container title must not be blank" }
        container.anchorPlacementId?.let { anchor ->
            require(byPlacement[anchor]?.hierarchyId?.value == container.hierarchyId) { "Missing container anchor" }
        }
        container.parentContainerId?.let { parent ->
            val parentContainer = requireNotNull(byContainer[parent]) { "Missing parent container" }
            require(parentContainer.hierarchyId == container.hierarchyId &&
                parentContainer.anchorPlacementId == container.anchorPlacementId) { "Container parent location differs" }
            require(container.isDeleted || !parentContainer.isDeleted) { "Live container below deleted container" }
        }
        val seen = hashSetOf<String>()
        var cursor: String? = container.id
        while (cursor != null) {
            require(seen.add(cursor)) { "Presentation container cycle" }
            cursor = byContainer[cursor]?.parentContainerId
        }
    }
    snapshot.memberships.forEach { membership ->
        require(membership.placementId.isNotBlank() && membership.hierarchyId == "GENERAL" && membership.version >= 1)
        val placement = requireNotNull(byPlacement[membership.placementId]) { "Missing member occurrence" }
        val container = requireNotNull(byContainer[membership.containerId]) { "Missing membership container" }
        require(placement.hierarchyId.value == membership.hierarchyId && container.hierarchyId == membership.hierarchyId)
        if (!membership.isDeleted && !container.isDeleted && !placement.isDeleted) {
            require(placement.parentPlacementId?.value == container.anchorPlacementId) {
                "Container membership must share the H1 sibling parent"
            }
        }
    }
}
