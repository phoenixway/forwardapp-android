package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.database.models.BacklogOrder
import com.romankozak.forwardappmobile.data.database.models.ListItem

object BacklogOrderUtils {
    private fun orderKey(order: BacklogOrder): String = order.id ?: "${order.listId}:${order.itemId}"
    private fun orderKey(listId: String, itemId: String, id: String?): String = id ?: "$listId:$itemId"

    private fun Long?.orZero() = this ?: 0L

    private data class Freshness(
        val orderVersion: Long,
        val updatedAt: Long,
        val isDeleted: Boolean,
    )

    private fun BacklogOrder.freshness() = Freshness(
        orderVersion = this.orderVersion.orZero(),
        updatedAt = (this.updatedAt ?: this.orderVersion).orZero(),
        isDeleted = this.isDeleted,
    )

    private fun BacklogOrder.normalized(): BacklogOrder = this.copy(
        orderVersion = this.orderVersion.orZero(),
        updatedAt = this.updatedAt ?: this.orderVersion,
        syncedAt = this.syncedAt ?: 0L,
        isDeleted = this.isDeleted,
    )

    fun dedupBacklogOrders(orders: List<BacklogOrder>): List<BacklogOrder> =
        orders.groupBy { orderKey(it) }
            .mapNotNull { (_, candidates) ->
                candidates
                    .map { it.normalized() }
                    .maxWithOrNull(
                        compareBy<BacklogOrder> { it.freshness().orderVersion }
                            .thenBy { it.freshness().updatedAt }
                            .thenBy { if (it.freshness().isDeleted) 1 else 0 },
                    )
            }

    fun listItemToBacklogOrder(listItem: ListItem, now: Long = System.currentTimeMillis()): BacklogOrder {
        val updated = listItem.updatedAt ?: listItem.version ?: now
        val orderVersion = listItem.version ?: updated
        return BacklogOrder(
            id = listItem.id,
            listId = listItem.projectId,
            itemId = listItem.entityId,
            order = listItem.order,
            orderVersion = orderVersion,
            updatedAt = updated,
            syncedAt = listItem.syncedAt,
            isDeleted = listItem.isDeleted,
        )
    }

    private fun buildOrderMap(orders: List<BacklogOrder>): Map<String, BacklogOrder> =
        dedupBacklogOrders(orders).associateBy { it.id ?: "${it.listId}:${it.itemId}" }

    fun applyBacklogOrders(
        listItems: List<ListItem>,
        backlogOrders: List<BacklogOrder>,
    ): List<ListItem> {
        if (backlogOrders.isEmpty()) return listItems
        val map = buildOrderMap(backlogOrders)
        return listItems.map { li ->
            val override = map[li.id] ?: map["${li.projectId}:${li.entityId}"] ?: return@map li
            val updated = maxOf(li.updatedAt.orZero(), override.updatedAt.orZero(), override.orderVersion.orZero())
            val version = maxOf(li.version.orZero(), override.orderVersion.orZero(), li.updatedAt.orZero(), override.updatedAt.orZero())
            li.copy(
                order = override.order,
                version = version,
                updatedAt = updated,
                syncedAt = li.syncedAt ?: override.syncedAt,
                isDeleted = override.isDeleted || li.isDeleted,
            )
        }
    }

    fun normalizeBacklogOrderSets(
        listItems: List<ListItem>,
        backlogOrders: List<BacklogOrder>,
        now: Long = System.currentTimeMillis(),
    ): NormalizedBacklogOrderResult {
        val dedupedOrders = dedupBacklogOrders(backlogOrders)
        val orderMap = buildOrderMap(dedupedOrders).toMutableMap()
        listItems.forEach { li ->
            val key = orderKey(li.projectId, li.entityId, li.id)
            if (!orderMap.containsKey(key)) {
                orderMap[key] = listItemToBacklogOrder(li, now)
            }
        }
        val seededOrders = orderMap.values.toList()
        val listWithOrders = applyBacklogOrders(listItems, seededOrders)
        // regenerate orders from applied listItems to keep freshness aligned
        val normalizedOrders = dedupBacklogOrders(
            seededOrders + listWithOrders.map { listItemToBacklogOrder(it, now) },
        )
        val appliedList = applyBacklogOrders(listWithOrders, normalizedOrders)
        return NormalizedBacklogOrderResult(
            listItems = appliedList,
            backlogOrders = normalizedOrders,
        )
    }
}

data class NormalizedBacklogOrderResult(
    val listItems: List<ListItem>,
    val backlogOrders: List<BacklogOrder>,
)
