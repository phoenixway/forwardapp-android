package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder

object BacklogOrderUtils {
    private fun orderKey(order: BacklogOrder): String = order.id ?: "${order.listId}:${order.itemId}"

    private fun orderKey(
        listId: String,
        itemId: String,
        id: String?,
    ): String = id ?: "$listId:$itemId"

    private fun Long?.orZero() = this ?: 0L

    private data class Freshness(
        val orderVersion: Long,
        val updatedAt: Long,
        val isDeleted: Boolean,
    )

    private fun BacklogOrder.freshness() =
        Freshness(
            orderVersion = this.orderVersion.orZero(),
            updatedAt = (this.updatedAt ?: this.orderVersion).orZero(),
            isDeleted = this.isDeleted,
        )

    private fun BacklogOrder.normalized(): BacklogOrder =
        this.copy(
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

    private fun listItemToBacklogOrder(
        backlogItem: BacklogItem,
        entityId: String,
        now: Long = System.currentTimeMillis(),
    ): BacklogOrder {
        val updated = backlogItem.updatedAt ?: backlogItem.version ?: now
        val orderVersion = backlogItem.version ?: updated
        return BacklogOrder(
            id = backlogItem.id,
            listId = backlogItem.contextId,
            itemId = entityId,
            order = backlogItem.order,
            orderVersion = orderVersion,
            updatedAt = updated,
            syncedAt = backlogItem.syncedAt,
            isDeleted = backlogItem.isDeleted,
        )
    }

    private fun buildOrderMap(orders: List<BacklogOrder>): Map<String, BacklogOrder> =
        dedupBacklogOrders(orders).associateBy { it.id ?: "${it.listId}:${it.itemId}" }

    fun applyBacklogOrders(
        backlogItems: List<BacklogItem>,
        backlogOrders: List<BacklogOrder>,
    ): List<BacklogItem> {
        if (backlogOrders.isEmpty()) return backlogItems
        val map = buildOrderMap(backlogOrders)
        return backlogItems.map { li ->
            val entityId = li.entityId
            val override = map[li.id] ?: map["${li.contextId}:$entityId"] ?: return@map li
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
        backlogItems: List<BacklogItem>,
        backlogOrders: List<BacklogOrder>,
        now: Long = System.currentTimeMillis(),
    ): NormalizedBacklogOrderResult {
        val dedupedOrders = dedupBacklogOrders(backlogOrders)
        val orderMap = buildOrderMap(dedupedOrders).toMutableMap()
        backlogItems.forEach { li ->
            val entityId = li.entityId
            val key = orderKey(li.contextId, entityId, li.id)
            if (!orderMap.containsKey(key)) {
                orderMap[key] = listItemToBacklogOrder(li, entityId, now)
            }
        }
        val seededOrders = orderMap.values.toList()
        val listWithOrders = applyBacklogOrders(backlogItems, seededOrders)
        // regenerate orders from applied listItems to keep freshness aligned
        val normalizedOrders =
            dedupBacklogOrders(
                seededOrders + listWithOrders.map { listItemToBacklogOrder(it, it.entityId, now) },
            )
        val appliedList = applyBacklogOrders(listWithOrders, normalizedOrders)
        return NormalizedBacklogOrderResult(
            backlogItems = appliedList,
            backlogOrders = normalizedOrders,
        )
    }
}

data class NormalizedBacklogOrderResult(
    val backlogItems: List<BacklogItem>,
    val backlogOrders: List<BacklogOrder>,
)
