package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot

fun BacklogItem.toSnapshot(): BacklogItemSnapshot = BacklogItemSnapshot(
    id,
    contextId,
    itemType,
    entityId,
    order,
    updatedAt ?: System.currentTimeMillis(),
    version,
    isDeleted
)

fun BacklogItemSnapshot.toEntity(): BacklogItem = BacklogItem(
    id,
    contextId,
    itemType,
    entityId,
    order,
    updatedAt,
    version = version,
    isDeleted = isDeleted
)