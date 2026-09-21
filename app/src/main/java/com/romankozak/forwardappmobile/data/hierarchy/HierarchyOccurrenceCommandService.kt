package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dormant H4.0a mutation seam over canonical V2 placement primitives.
 *
 * CURRENT production structural writers do not call this service. P2 must be
 * an explicit later authority transfer.
 */
@Singleton
class HierarchyOccurrenceCommandService
    @Inject
    constructor(
        private val repository: CanonicalHierarchyPlacementRepository,
    ) {
        suspend fun occurrence(placementId: PlacementId): HierarchyOccurrenceRef? =
            repository.getPlacement(placementId)?.toHierarchyOccurrenceRef()

        suspend fun move(
            command: HierarchyOccurrenceCommand.Move,
            now: Long = System.currentTimeMillis(),
        ) {
            repository.movePlacement(
                placementId = command.placementId,
                newParentPlacementId = command.newParentPlacementId,
                position = command.position,
                now = now,
            )
        }

        suspend fun moveMany(
            command: HierarchyOccurrenceCommand.MoveMany,
            now: Long = System.currentTimeMillis(),
        ) {
            repository.movePlacements(
                moves = command.moves,
                now = now,
            )
        }

        suspend fun createAppearance(
            command: HierarchyOccurrenceCommand.CreateAppearance,
            now: Long = System.currentTimeMillis(),
        ): PlacementId =
            when (command.placementKind) {
                PlacementKind.PRIMARY ->
                    repository.createPrimaryAppearance(
                        target = command.target,
                        parentPlacementId = command.parentPlacementId,
                        position = command.position,
                        hierarchyId = command.hierarchyId,
                        now = now,
                    )

                PlacementKind.LINK ->
                    repository.createLinkAppearance(
                        target = command.target,
                        parentPlacementId = command.parentPlacementId,
                        position = command.position,
                        hierarchyId = command.hierarchyId,
                        now = now,
                    )
            }

        /**
         * Reorder requires the complete concrete sibling occurrence set.
         * Validation and persistence occur inside one repository transaction.
         */
        suspend fun reorderSiblings(
            command: HierarchyOccurrenceCommand.ReorderSiblings,
            now: Long = System.currentTimeMillis(),
        ) {
            repository.reorderSiblings(
                parentPlacementId = command.parentPlacementId,
                orderedPlacementIds = command.orderedPlacementIds,
                hierarchyId = command.hierarchyId,
                now = now,
            )
        }

        suspend fun removeOccurrence(
            command: HierarchyOccurrenceCommand.RemoveOccurrence,
            now: Long = System.currentTimeMillis(),
        ) {
            repository.removePlacement(
                placementId = command.placementId,
                now = now,
            )
        }

        suspend fun restoreOccurrence(
            command: HierarchyOccurrenceCommand.RestoreOccurrence,
            now: Long = System.currentTimeMillis(),
        ) {
            repository.restorePlacement(
                placementId = command.placementId,
                now = now,
            )
        }
    }
