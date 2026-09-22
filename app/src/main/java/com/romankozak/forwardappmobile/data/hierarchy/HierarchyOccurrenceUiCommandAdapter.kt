package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind

/**
 * Preparatory P2 UI/command boundary.
 *
 * This adapter only prepares occurrence-native command carriers. It never
 * executes V2 mutation and therefore transfers no production authority.
 */
object HierarchyOccurrenceUiCommandAdapter {
    sealed interface Result<out T> {
        data class Resolved<T>(
            val value: T,
        ) : Result<T>

        data class Unresolved(
            val reason: Reason,
        ) : Result<Nothing>
    }

    enum class Reason {
        OCCURRENCE_IDENTITY_REQUIRED,
        DESTINATION_OCCURRENCE_REQUIRED,
        INCOMPLETE_SIBLING_OCCURRENCES,
        MIXED_SIBLING_PARENTS,
        DUPLICATE_PLACEMENT_ID,
    }

    fun move(
        source: HierarchyOccurrenceRef?,
        destinationParent: HierarchyOccurrenceRef?,
        destinationIsRoot: Boolean = false,
    ): Result<HierarchyOccurrenceCommand.Move> {
        if (source == null) return unresolved(Reason.OCCURRENCE_IDENTITY_REQUIRED)
        if (!destinationIsRoot && destinationParent == null) {
            return unresolved(Reason.DESTINATION_OCCURRENCE_REQUIRED)
        }
        return resolved(
            HierarchyOccurrenceCommand.Move(
                placementId = source.placementId,
                newParentPlacementId = destinationParent?.placementId,
            ),
        )
    }

    fun moveMany(
        sources: List<HierarchyOccurrenceRef>?,
        destinationParent: HierarchyOccurrenceRef?,
        destinationIsRoot: Boolean = false,
    ): Result<HierarchyOccurrenceCommand.MoveMany> {
        if (sources.isNullOrEmpty()) return unresolved(Reason.OCCURRENCE_IDENTITY_REQUIRED)
        if (!destinationIsRoot && destinationParent == null) {
            return unresolved(Reason.DESTINATION_OCCURRENCE_REQUIRED)
        }
        if (sources.map { it.placementId }.distinct().size != sources.size) {
            return unresolved(Reason.DUPLICATE_PLACEMENT_ID)
        }
        return resolved(
            HierarchyOccurrenceCommand.MoveMany(
                moves =
                    sources.map {
                        HierarchyPlacementMove(
                            placementId = it.placementId,
                            newParentPlacementId = destinationParent?.placementId,
                        )
                    },
            ),
        )
    }

    fun reorderSiblings(
        parentPlacementId: PlacementId?,
        orderedSiblings: List<HierarchyOccurrenceRef>?,
        expectedSiblingPlacementIds: Set<PlacementId>,
    ): Result<HierarchyOccurrenceCommand.ReorderSiblings> {
        if (orderedSiblings.isNullOrEmpty()) {
            return unresolved(Reason.INCOMPLETE_SIBLING_OCCURRENCES)
        }
        val orderedIds = orderedSiblings.map { it.placementId }
        if (orderedIds.distinct().size != orderedIds.size) {
            return unresolved(Reason.DUPLICATE_PLACEMENT_ID)
        }
        if (orderedSiblings.any { it.parentPlacementId != parentPlacementId }) {
            return unresolved(Reason.MIXED_SIBLING_PARENTS)
        }
        if (orderedIds.toSet() != expectedSiblingPlacementIds) {
            return unresolved(Reason.INCOMPLETE_SIBLING_OCCURRENCES)
        }
        return resolved(
            HierarchyOccurrenceCommand.ReorderSiblings(
                parentPlacementId = parentPlacementId,
                orderedPlacementIds = orderedIds,
            ),
        )
    }

    fun createAppearance(
        target: HierarchyTargetRef,
        destinationParent: HierarchyOccurrenceRef?,
        placementKind: PlacementKind,
        destinationIsRoot: Boolean = false,
    ): Result<HierarchyOccurrenceCommand.CreateAppearance> {
        if (!destinationIsRoot && destinationParent == null) {
            return unresolved(Reason.DESTINATION_OCCURRENCE_REQUIRED)
        }
        return resolved(
            HierarchyOccurrenceCommand.CreateAppearance(
                target = target,
                parentPlacementId = destinationParent?.placementId,
                placementKind = placementKind,
            ),
        )
    }

    fun clipboardOccurrence(
        occurrence: HierarchyOccurrenceRef?,
        sourceKind: HierarchyClipboardSourceKind,
        legacySourceId: String? = null,
    ): Result<HierarchyClipboardOccurrence> =
        occurrence?.let {
            resolved(
                HierarchyClipboardOccurrence(
                    occurrence = it,
                    sourceKind = sourceKind,
                    legacySourceId = legacySourceId,
                ),
            )
        } ?: unresolved(Reason.OCCURRENCE_IDENTITY_REQUIRED)

    fun removeOccurrence(
        occurrence: HierarchyOccurrenceRef?,
    ): Result<HierarchyDestructiveIntent.RemoveOccurrence> =
        occurrence?.let {
            resolved(HierarchyDestructiveIntent.RemoveOccurrence(it.placementId))
        } ?: unresolved(Reason.OCCURRENCE_IDENTITY_REQUIRED)

    fun deleteTarget(target: HierarchyTargetRef): HierarchyDestructiveIntent.DeleteTarget =
        HierarchyDestructiveIntent.DeleteTarget(target)

    fun restoreOccurrence(
        occurrence: HierarchyOccurrenceRef?,
    ): Result<HierarchyOccurrenceCommand.RestoreOccurrence> =
        occurrence?.let {
            resolved(HierarchyOccurrenceCommand.RestoreOccurrence(it.placementId))
        } ?: unresolved(Reason.OCCURRENCE_IDENTITY_REQUIRED)

    private fun <T> resolved(value: T): Result<T> =
        Result.Resolved(value)

    private fun unresolved(reason: Reason): Result.Unresolved =
        Result.Unresolved(reason)
}
