package com.romankozak.forwardappmobile.data.hierarchy.container

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement

sealed interface BigPicturePresentationEntry {
    val id: String
    val siblingOrder: Long
    data class Container(override val id: String, override val siblingOrder: Long) : BigPicturePresentationEntry
    data class Occurrence(override val id: String, override val siblingOrder: Long) : BigPicturePresentationEntry
}

/**
 * One explicit mixed sequence: order, container-before-occurrence tie, durable ID.
 * Membership never owns an order: an occurrence keeps its H1 siblingOrder.
 * Parent containers wrap a subset of siblings at the same H1 anchor, not another H1 tree.
 */
fun bigPicturePresentationEntries(
    snapshot: BigPicturePresentationSnapshot,
    placements: List<HierarchyPlacement>,
    anchorPlacementId: String? = null,
    parentContainerId: String? = null,
): List<BigPicturePresentationEntry> {
    validateBigPicturePresentation(snapshot, placements)
    val live = placements.filter { !it.isDeleted && it.hierarchyId.value == "GENERAL" }
    if (anchorPlacementId != null && live.none { it.id.value == anchorPlacementId }) return emptyList()
    val containers = snapshot.containers.filter { !it.isDeleted && it.anchorPlacementId == anchorPlacementId }
        .associateBy { it.id }
    if (parentContainerId != null && parentContainerId !in containers) return emptyList()
    val memberships = snapshot.memberships.filter { !it.isDeleted && it.containerId in containers }
        .associateBy { it.placementId }
    return buildList {
        containers.values.filter { it.parentContainerId == parentContainerId }.forEach {
            add(BigPicturePresentationEntry.Container(it.id, it.siblingOrder))
        }
        live.filter { it.parentPlacementId?.value == anchorPlacementId &&
            memberships[it.id.value]?.containerId == parentContainerId }.forEach {
            add(BigPicturePresentationEntry.Occurrence(it.id.value, it.siblingOrder))
        }
    }.sortedWith(compareBy<BigPicturePresentationEntry> { it.siblingOrder }
        .thenBy { if (it is BigPicturePresentationEntry.Container) 0 else 1 }.thenBy { it.id })
}
