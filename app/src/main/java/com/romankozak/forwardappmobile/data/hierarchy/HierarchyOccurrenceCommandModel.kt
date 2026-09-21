package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind

/**
 * H4.0a concrete occurrence identity.
 *
 * PlacementId identifies the visible occurrence; target identifies the semantic
 * object shown by that occurrence. They must never be conflated.
 */
data class HierarchyOccurrenceRef(
    val placementId: PlacementId,
    val target: HierarchyTargetRef,
    val parentPlacementId: PlacementId?,
    val placementKind: PlacementKind,
    val siblingOrder: Long,
)

fun HierarchyPlacement.toHierarchyOccurrenceRef(): HierarchyOccurrenceRef =
    HierarchyOccurrenceRef(
        placementId = id,
        target = target,
        parentPlacementId = parentPlacementId,
        placementKind = placementKind,
        siblingOrder = siblingOrder,
    )

fun CanonicalV2HierarchyOccurrence.toHierarchyOccurrenceRef(): HierarchyOccurrenceRef =
    HierarchyOccurrenceRef(
        placementId = placementId,
        target = target,
        parentPlacementId = parentPlacementId,
        placementKind = placementKind,
        siblingOrder = siblingOrder,
    )

fun CanonicalV2PresentedHierarchyEntry.Occurrence.toHierarchyOccurrenceRef(): HierarchyOccurrenceRef =
    HierarchyOccurrenceRef(
        placementId = placementId,
        target = target,
        parentPlacementId = parentPlacementId,
        placementKind = placementKind,
        siblingOrder = siblingOrder,
    )

sealed interface HierarchyOccurrenceCommand {
    data class Move(
        val placementId: PlacementId,
        val newParentPlacementId: PlacementId?,
        val position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
    ) : HierarchyOccurrenceCommand

    data class MoveMany(
        val moves: List<HierarchyPlacementMove>,
    ) : HierarchyOccurrenceCommand

    data class ReorderSiblings(
        val parentPlacementId: PlacementId?,
        val orderedPlacementIds: List<PlacementId>,
        val hierarchyId: HierarchyId = HierarchyId.GENERAL,
    ) : HierarchyOccurrenceCommand

    data class CreateAppearance(
        val target: HierarchyTargetRef,
        val parentPlacementId: PlacementId?,
        val placementKind: PlacementKind,
        val position: HierarchyPlacementPosition = HierarchyPlacementPosition.LAST,
        val hierarchyId: HierarchyId = HierarchyId.GENERAL,
    ) : HierarchyOccurrenceCommand

    data class RemoveOccurrence(
        val placementId: PlacementId,
    ) : HierarchyOccurrenceCommand

    data class RestoreOccurrence(
        val placementId: PlacementId,
    ) : HierarchyOccurrenceCommand
}

/**
 * Removing one placement occurrence is not target deletion.
 * Target-delete/subtree policy remains outside H4.0a.
 */
sealed interface HierarchyDestructiveIntent {
    data class RemoveOccurrence(
        val placementId: PlacementId,
    ) : HierarchyDestructiveIntent

    data class DeleteTarget(
        val target: HierarchyTargetRef,
    ) : HierarchyDestructiveIntent
}

class HierarchySiblingSetMismatchException(
    message: String,
) : HierarchyPlacementMutationException(message)
