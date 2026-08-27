package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder

private fun norm(id: String?): String? =
    id?.trim()?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

fun isDirectHierarchyChildContext(
    ownerContextId: String,
    targetParentId: String?,
): Boolean = norm(ownerContextId) != null && norm(ownerContextId) == norm(targetParentId)

fun tombstoneBacklogOrdersForItems(
    orders: List<BacklogOrder>,
    deletedItems: List<BacklogItem>,
    now: Long,
): List<BacklogOrder> {
    val ids = deletedItems.mapTo(hashSetOf()) { it.id }
    val keys = deletedItems.mapTo(hashSetOf()) { it.contextId to it.entityId }

    return orders
        .filterNot { it.isDeleted }
        .filter { it.id in ids || (it.listId to it.itemId) in keys }
        .map {
            it.copy(
                isDeleted = true,
                updatedAt = maxOf(now, (it.updatedAt ?: 0L) + 1L),
                syncedAt = null,
                orderVersion =
                    if (it.orderVersion == Long.MAX_VALUE) Long.MAX_VALUE
                    else it.orderVersion + 1L,
            )
        }
}


data class ContextBacklogNormalization(
    val backlogItems: List<BacklogItem>,
    val backlogOrders: List<BacklogOrder>,
    val tombstonedItemCount: Int,
)

fun normalizeLegacyStructuralContextBacklog(
    backlogItems: List<BacklogItem>,
    backlogOrders: List<BacklogOrder>,
    parentByContextId: Map<String, String?>,
    now: Long,
): ContextBacklogNormalization {
    val structuralItems =
        backlogItems.filter { item ->
            !item.isDeleted &&
                item.itemType == "SUBLIST" &&
                isDirectHierarchyChildContext(
                    ownerContextId = item.contextId,
                    targetParentId = parentByContextId[item.entityId],
                )
        }

    if (structuralItems.isEmpty()) {
        return ContextBacklogNormalization(
            backlogItems = backlogItems,
            backlogOrders = backlogOrders,
            tombstonedItemCount = 0,
        )
    }

    val structuralIds = structuralItems.mapTo(hashSetOf()) { it.id }
    val tombstonedItems =
        backlogItems.map { item ->
            if (item.id !in structuralIds) {
                item
            } else {
                item.copy(
                    isDeleted = true,
                    updatedAt = maxOf(now, (item.updatedAt ?: 0L) + 1L),
                    syncedAt = null,
                    version =
                        if (item.version == Long.MAX_VALUE) Long.MAX_VALUE
                        else item.version + 1L,
                )
            }
        }

    val orderTombstones =
        tombstoneBacklogOrdersForItems(
            orders = backlogOrders,
            deletedItems = structuralItems,
            now = now,
        )
    val tombstoneById = orderTombstones.associateBy { it.id }
    val tombstoneByKey = orderTombstones.associateBy { it.listId to it.itemId }

    return ContextBacklogNormalization(
        backlogItems = tombstonedItems,
        backlogOrders =
            backlogOrders.map { order ->
                tombstoneById[order.id]
                    ?: tombstoneByKey[order.listId to order.itemId]
                    ?: order
            },
        tombstonedItemCount = structuralItems.size,
    )
}
