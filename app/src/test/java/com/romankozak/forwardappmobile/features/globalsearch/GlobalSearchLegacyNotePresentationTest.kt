package com.romankozak.forwardappmobile.features.globalsearch

import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalSearchLegacyNotePresentationTest {
    @Test
    fun `Note result has identity and presentation independent of historical Context`() {
        val result =
            GlobalSearchResultItem.NoteItem(
                LegacyNoteEntity(
                    id = "note-123",
                    contextId = "obsolete-context",
                    title = "Standalone observation",
                    content = "Field notes",
                    updatedAt = 123L,
                ),
            )

        assertEquals("note_note-123", result.uniqueId)
        assertEquals(123L, result.timestamp)
        assertEquals("Standalone observation", resultTitle(result))
        assertEquals("notes", result.groupKey())
        assertEquals("Нотатка", result.typePresentation().label)
        assertTrue(GlobalSearchType.Notes.matches(result))
        assertFalse(GlobalSearchType.Attachments.matches(result))
        assertFalse(GlobalSearchType.Contexts.matches(result))
    }

    @Test
    fun `previous all-types filter includes newly introduced Notes`() {
        val previousAll =
            GlobalSearchType.entries
                .filterNot { it == GlobalSearchType.Notes }
                .map { it.name }

        assertEquals(
            GlobalSearchType.entries.toSet(),
            resolveGlobalSearchTypeSelection(previousAll),
        )
    }

    @Test
    fun `previous partial filter remains partial`() {
        assertEquals(
            setOf(GlobalSearchType.Attachments),
            resolveGlobalSearchTypeSelection(listOf("Attachments")),
        )
        assertEquals(
            setOf(GlobalSearchType.Notes),
            resolveGlobalSearchTypeSelection(listOf("Notes")),
        )
    }

    @Test
    fun `empty and unknown-only saved filter use default all types`() {
        val all = GlobalSearchType.entries.toSet()

        assertEquals(all, resolveGlobalSearchTypeSelection(emptyList()))
        assertEquals(
            all,
            resolveGlobalSearchTypeSelection(listOf("RemovedSearchType")),
        )
    }
}
