package com.romankozak.forwardappmobile.ui.screens.selectiveimport

import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.sync.BackupDiff
import com.romankozak.forwardappmobile.data.sync.DiffResult
import com.romankozak.forwardappmobile.data.sync.DiffStatus
import com.romankozak.forwardappmobile.data.sync.UpdatedItem
import com.romankozak.forwardappmobile.features.sync.selectiveimport.toSelectable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectiveImportDiffTest {

    private fun listItem(
        id: String,
        projectId: String = "p1",
        order: Long,
        entityId: String = "e1",
        itemType: String = "TYPE"
    ) = ListItem(
        id = id,
        projectId = projectId,
        itemType = itemType,
        entityId = entityId,
        order = order
    )

    @Test
    fun `order-only change is surfaced as order change message`() {
        val local = listItem(id = "1", order = 1)
        val incoming = listItem(id = "1", order = 3)
        val diff = BackupDiff(
            listItems = DiffResult(
                updated = listOf(UpdatedItem(local = local, incoming = incoming))
            )
        )

        val selectable = diff.toSelectable().listItems.single()

        assertEquals(DiffStatus.UPDATED, selectable.status)
        assertEquals("Порядок: 1 → 3", selectable.changeInfo)
    }

    @Test
    fun `order plus content change shows combined message`() {
        val local = listItem(id = "1", order = 1, entityId = "old")
        val incoming = listItem(id = "1", order = 2, entityId = "new")
        val diff = BackupDiff(
            listItems = DiffResult(
                updated = listOf(UpdatedItem(local = local, incoming = incoming))
            )
        )

        val selectable = diff.toSelectable().listItems.single()

        assertEquals(DiffStatus.UPDATED, selectable.status)
        assertEquals("Порядок: 1 → 2, інші зміни", selectable.changeInfo)
    }

    @Test
    fun `new list item has no change message`() {
        val incoming = listItem(id = "1", order = 5)
        val diff = BackupDiff(
            listItems = DiffResult(
                added = listOf(incoming)
            )
        )

        val selectable = diff.toSelectable().listItems.single()

        assertEquals(DiffStatus.NEW, selectable.status)
        assertNull(selectable.changeInfo)
    }

    @Test
    fun `deleted list item is marked and disabled`() {
        val local = listItem(id = "1", order = 7)
        val diff = BackupDiff(
            listItems = DiffResult(
                deleted = listOf(local)
            )
        )

        val selectable = diff.toSelectable().listItems.single()

        assertEquals(DiffStatus.DELETED, selectable.status)
        assertEquals(false, selectable.isSelected)
        assertEquals(false, selectable.isSelectable)
        assertNull(selectable.changeInfo)
    }
}
