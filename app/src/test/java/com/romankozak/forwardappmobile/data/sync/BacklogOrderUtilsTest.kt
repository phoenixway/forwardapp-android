package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogOrder
import com.romankozak.forwardappmobile.features.contexts.data.models.ListItem
import com.romankozak.forwardappmobile.features.contexts.data.models.ListItemTypeValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BacklogOrderUtilsTest {

    @Test
    fun `dedup uses LWW orderVersion then updatedAt then non tombstone`() {
        val orders = listOf(
            BacklogOrder(id = "li1", listId = "p1", itemId = "g1", order = 1, orderVersion = 1, updatedAt = 1, syncedAt = 0, isDeleted = false),
            BacklogOrder(id = "li1", listId = "p1", itemId = "g1", order = 2, orderVersion = 2, updatedAt = 5, syncedAt = 0, isDeleted = false),
            BacklogOrder(id = "li1", listId = "p1", itemId = "g1", order = 3, orderVersion = 2, updatedAt = 10, syncedAt = 0, isDeleted = true),
            BacklogOrder(id = "li1", listId = "p1", itemId = "g1", order = 4, orderVersion = 3, updatedAt = 0, syncedAt = 0, isDeleted = false),
        )
        val result = BacklogOrderUtils.dedupBacklogOrders(orders)
        assertEquals(1, result.size)
        assertEquals(3, result[0].orderVersion)
        assertEquals(4, result[0].order)
        assertTrue(!result[0].isDeleted)
    }

    @Test
    fun `applyBacklogOrders bumps version and updatedAt`() {
        val listItems = listOf(
            ListItem(
                id = "li1",
                projectId = "p1",
                itemType = ListItemTypeValues.GOAL,
                entityId = "g1",
                order = 1,
                version = 1,
                updatedAt = 1,
                syncedAt = null,
                isDeleted = false,
            ),
        )
        val orders = listOf(
            BacklogOrder(id = "li1", listId = "p1", itemId = "g1", order = 9, orderVersion = 10, updatedAt = 8, syncedAt = 0, isDeleted = false),
        )
        val applied = BacklogOrderUtils.applyBacklogOrders(listItems, orders)
        assertEquals(9, applied[0].order)
        assertEquals(10, applied[0].version)
        assertEquals(10, applied[0].updatedAt)
    }

    @Test
    fun `normalizeBacklogOrderSets seeds missing orders and keeps consistency`() {
        val listItems = listOf(
            ListItem(
                id = "li1",
                projectId = "p1",
                itemType = ListItemTypeValues.GOAL,
                entityId = "g1",
                order = 5,
                version = 7,
                updatedAt = 6,
                syncedAt = null,
                isDeleted = false,
            ),
        )
        val normalized = BacklogOrderUtils.normalizeBacklogOrderSets(listItems, emptyList(), now = 100)
        assertEquals(1, normalized.backlogOrders.size)
        assertEquals("li1", normalized.backlogOrders[0].id)
        assertEquals(5, normalized.backlogOrders[0].order)
        assertEquals(7, normalized.backlogOrders[0].orderVersion)
        assertEquals(5, normalized.listItems[0].order)
    }

    @Test
    fun `tombstone in backlog order marks list item deleted`() {
        val listItems = listOf(
            ListItem(
                id = "li1",
                projectId = "p1",
                itemType = ListItemTypeValues.GOAL,
                entityId = "g1",
                order = 1,
                version = 1,
                updatedAt = 1,
                syncedAt = null,
                isDeleted = false,
            ),
        )
        val orders = listOf(
            BacklogOrder(id = "li1", listId = "p1", itemId = "g1", order = 1, orderVersion = 5, updatedAt = 5, syncedAt = 0, isDeleted = true),
        )
        val applied = BacklogOrderUtils.applyBacklogOrders(listItems, orders)
        assertTrue(applied[0].isDeleted)
        assertEquals(5, applied[0].version)
    }
}
