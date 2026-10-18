package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.data.hierarchy.container.H1LegacyOccurrenceCreation
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Legacy occurrence-only compatibility command seam.
 *
 * Existing UI use-cases still invoke this service; it is not canonical
 * mixed H1 structural authority. R2F.2B typed UX remains deferred.
 */
@Singleton
class HierarchyOccurrenceCommandService
    @Inject
    constructor(
        private val repository: CanonicalHierarchyPlacementRepository,
        private val coordinator: H1StructuralMutationCoordinator,
    ) {
        suspend fun occurrence(placementId: PlacementId): HierarchyOccurrenceRef? =
            repository.getPlacement(placementId)?.toHierarchyOccurrenceRef()

        suspend fun move(
            command: HierarchyOccurrenceCommand.Move,
            now: Long = System.currentTimeMillis(),
        ) {
            coordinator.moveOccurrences(
                moves = listOf(
                    HierarchyPlacementMove(
                        placementId = command.placementId,
                        newParentPlacementId = command.newParentPlacementId,
                        position = command.position,
                    ),
                ),
                now = now,
            )
        }

        suspend fun moveMany(
            command: HierarchyOccurrenceCommand.MoveMany,
            now: Long = System.currentTimeMillis(),
        ) {
            coordinator.moveOccurrences(
                moves = command.moves,
                now = now,
            )
        }

        /**
         * Atomic occurrence-only creation batch for Clipboard consumers.
         * The coordinator is the sole structural writer.
         */
        suspend fun createAppearances(
            commands: List<HierarchyOccurrenceCommand.CreateAppearance>,
            now: Long = System.currentTimeMillis(),
        ): List<PlacementId> {
            commands.forEach { command ->
                if (command.hierarchyId != HierarchyId.GENERAL) {
                    throw UnsupportedHierarchyException(command.hierarchyId)
                }
            }

            return coordinator.createLegacyOccurrences(
                creations = commands.map { command ->
                    H1LegacyOccurrenceCreation(
                        target = command.target,
                        placementKind = command.placementKind,
                        parentPlacementId = command.parentPlacementId,
                        position = command.position,
                    )
                },
                now = now,
            )
        }

        suspend fun createAppearance(
            command: HierarchyOccurrenceCommand.CreateAppearance,
            now: Long = System.currentTimeMillis(),
        ): PlacementId {
            if (command.hierarchyId != HierarchyId.GENERAL) {
                throw UnsupportedHierarchyException(command.hierarchyId)
            }

            return coordinator.createLegacyOccurrence(
                target = command.target,
                placementKind = command.placementKind,
                parentPlacementId = command.parentPlacementId,
                position = command.position,
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
            require(command.hierarchyId == HierarchyId.GENERAL) {
                "Unified H1 reorder supports GENERAL hierarchy only"
            }
            try {
                coordinator.reorderOccurrenceSiblings(
                    parentPlacementId = command.parentPlacementId?.value,
                    orderedPlacementIds = command.orderedPlacementIds.map { it.value },
                    now = now,
                )
            } catch (error: IllegalArgumentException) {
                throw HierarchySiblingSetMismatchException(
                    error.message ?: "Incomplete or mixed H1 sibling set",
                )
            }
        }

        suspend fun removeOccurrence(
            command: HierarchyOccurrenceCommand.RemoveOccurrence,
            now: Long = System.currentTimeMillis(),
        ) {
            coordinator.removeOccurrence(
                placementId = command.placementId,
                now = now,
            )
        }

        suspend fun removeOccurrenceSubtree(
            command: HierarchyOccurrenceCommand.RemoveOccurrenceSubtree,
            now: Long = System.currentTimeMillis(),
        ): List<PlacementId> =
            coordinator.removeOccurrenceSubtree(
                placementId = command.rootPlacementId,
                now = now,
            )

        suspend fun restoreOccurrence(
            command: HierarchyOccurrenceCommand.RestoreOccurrence,
            now: Long = System.currentTimeMillis(),
        ) {
            coordinator.restoreOccurrence(
                placementId = command.placementId,
                now = now,
            )
        }
    }
