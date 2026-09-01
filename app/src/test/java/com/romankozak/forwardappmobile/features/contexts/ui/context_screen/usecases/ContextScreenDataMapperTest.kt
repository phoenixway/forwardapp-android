package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ContextScreenDataMapperTest {
    @Test
    fun `legacy note placement remains visible with content identity after canonical cutover`() {
        val note =
            LegacyNoteEntity(
                id = "note-content",
                contextId = "owner",
                title = "Historical note",
                content = "Preserved content",
            )
        val placement =
            BacklogItem(
                id = "canonical-placement",
                contextId = "owner",
                itemType = BacklogItemTypeValues.NOTE,
                entityId = note.id,
                order = 0L,
            )

        val loaded =
            ContextScreenDataMapper().map(
                contextId = "owner",
                snapshot = emptySnapshot(rawItems = listOf(placement), notes = listOf(note)),
            )

        val item = loaded.items.single() as BacklogItemContent.NoteItem
        assertSame(note, item.note)
        assertEquals("canonical-placement", item.backlogItem.id)
        assertEquals("note-content", item.backlogItem.entityId)
    }

    private fun emptySnapshot(
        rawItems: List<BacklogItem>,
        notes: List<LegacyNoteEntity>,
    ): ContextScreenDataSnapshot =
        ContextScreenDataSnapshot(
            context = null,
            rawItems = rawItems,
            config = null,
            logs = emptyList(),
            checklists = emptyList(),
            noteDocuments = emptyList(),
            musicNotes = emptyList(),
            directionItems = emptyList(),
            allContexts = emptyList(),
            attachments = emptyList(),
            linkItems = emptyList(),
            reminders = emptyList(),
            recentItems = emptyList(),
            notes = notes,
            goals = emptyList(),
            subprojects = emptyList(),
            workspaceCapabilities = emptyList(),
        )
}
