package com.romankozak.forwardappmobile.core.context

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextBacklogInvariantTest {
    @Test
    fun directChildIsHierarchyRelation() {
        assertTrue(isDirectHierarchyChildContext("parent", "parent"))
        assertFalse(isDirectHierarchyChildContext("parent", "other"))
        assertFalse(isDirectHierarchyChildContext("parent", null))
    }

    @Test
    fun deletingItemTombstonesItsCanonicalOrder() {
        val item =
            BacklogItem(
                id = "backlog-1",
                contextId = "parent",
                itemType = BacklogItemTypeValues.SUBLIST,
                entityId = "linked",
                order = 1,
            )
        val order =
            BacklogOrder(
                id = "backlog-1",
                listId = "parent",
                itemId = "linked",
                order = 1,
                orderVersion = 4,
                updatedAt = 100,
            )

        val result =
            tombstoneBacklogOrdersForItems(
                orders = listOf(order),
                deletedItems = listOf(item),
                now = 200,
            ).single()

        assertTrue(result.isDeleted)
        assertEquals(5L, result.orderVersion)
        assertEquals(200L, result.updatedAt)
    }

    @Test
    fun unrelatedOrderIsPreservedByDeletionPlanner() {
        val item =
            BacklogItem(
                id = "backlog-1",
                contextId = "parent",
                itemType = BacklogItemTypeValues.SUBLIST,
                entityId = "linked",
                order = 1,
            )
        val unrelated =
            BacklogOrder(
                id = "other",
                listId = "parent",
                itemId = "another",
                order = 2,
            )

        assertTrue(
            tombstoneBacklogOrdersForItems(
                orders = listOf(unrelated),
                deletedItems = listOf(item),
                now = 200,
            ).isEmpty(),
        )
    }

    @Test
    fun structuralIncomingLinkIsTombstonedButExplicitLinkSurvives() {
        val structural =
            BacklogItem(
                id = "structural",
                contextId = "parent",
                itemType = BacklogItemTypeValues.SUBLIST,
                entityId = "child",
                order = 1,
                version = 3,
            )
        val explicit =
            BacklogItem(
                id = "explicit",
                contextId = "parent",
                itemType = BacklogItemTypeValues.SUBLIST,
                entityId = "other",
                order = 2,
                version = 3,
            )

        val normalized =
            normalizeLegacyStructuralContextBacklog(
                backlogItems = listOf(structural, explicit),
                backlogOrders = emptyList(),
                parentByContextId =
                    mapOf(
                        "child" to "parent",
                        "other" to "different-parent",
                    ),
                now = 200,
            )

        assertEquals(1, normalized.tombstonedItemCount)
        assertTrue(normalized.backlogItems.first { it.id == "structural" }.isDeleted)
        assertFalse(normalized.backlogItems.first { it.id == "explicit" }.isDeleted)
    }

}
