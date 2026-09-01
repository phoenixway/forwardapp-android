package com.romankozak.forwardappmobile.features.contexts.domain.clipboard

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem

internal data class BacklogMovePartition(
    val movable: List<BacklogItem>,
    val duplicateCount: Int,
    val invalidCount: Int,
)

/** Keeps rebuildable projections out of explicit canonical placement moves. */
internal suspend fun partitionCanonicalBacklogMove(
    items: List<BacklogItem>,
    targetContextId: String,
    hasLivePlacement: suspend (contextId: String, itemType: String, entityId: String) -> Boolean,
): BacklogMovePartition {
    val movable = mutableListOf<BacklogItem>()
    var duplicateCount = 0
    var invalidCount = 0

    items.forEach { item ->
        when {
            item.associationOwnerContextId != null -> invalidCount += 1
            hasLivePlacement(targetContextId, item.itemType, item.entityId) -> duplicateCount += 1
            else -> movable += item
        }
    }

    return BacklogMovePartition(
        movable = movable,
        duplicateCount = duplicateCount,
        invalidCount = invalidCount,
    )
}
