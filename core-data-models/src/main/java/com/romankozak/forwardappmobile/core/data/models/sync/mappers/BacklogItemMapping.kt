package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot

fun BacklogItem.toSnapshot(): BacklogItemSnapshot =
    BacklogItemSnapshot(
        id = id,
        contextId = contextId,
        itemType = itemType,
        entityId = entityId,
        order = order,
        updatedAt = updatedAt ?: System.currentTimeMillis(),
        version = version,
        isDeleted = isDeleted,
    )

fun BacklogItemSnapshot.toEntity(): BacklogItem =
    BacklogItem(
        id = id,
        contextId = contextId,
        itemType = itemType,
        entityId = entityId,
        order = order,
        updatedAt = updatedAt,
        version = version,
        isDeleted = isDeleted,
    )
