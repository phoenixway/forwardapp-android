package com.romankozak.forwardappmobile.ui.screens.attachments.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentsLibraryFilterTest {

    @Test
    fun `all filter matches every type`() {
        AttachmentLibraryType.values().forEach { type ->
            assertTrue(AttachmentLibraryFilter.All.matches(type))
        }
    }

    @Test
    fun `specific filters match only their type`() {
        assertTrue(AttachmentLibraryFilter.Notes.matches(AttachmentLibraryType.NOTE_DOCUMENT))
        assertFalse(AttachmentLibraryFilter.Notes.matches(AttachmentLibraryType.CHECKLIST))
        assertFalse(AttachmentLibraryFilter.Notes.matches(AttachmentLibraryType.LINK))

        assertTrue(AttachmentLibraryFilter.Checklists.matches(AttachmentLibraryType.CHECKLIST))
        assertFalse(AttachmentLibraryFilter.Checklists.matches(AttachmentLibraryType.NOTE_DOCUMENT))

        assertTrue(AttachmentLibraryFilter.Links.matches(AttachmentLibraryType.LINK))
        assertFalse(AttachmentLibraryFilter.Links.matches(AttachmentLibraryType.NOTE_DOCUMENT))
    }
}
