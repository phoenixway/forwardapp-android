package com.romankozak.forwardappmobile.features.missions.presentation

import org.junit.Assert.*
import org.junit.Test

class ObsidianDocumentDraftTest {
    @Test fun `copied URL uses its vault and readable display title`() {
        val url = "obsidian://open?vault=Actual&file=Folder%2FNote.md"
        val draft = buildNewDocumentDraft(DocumentCreationType.OBSIDIAN, "", url, "Ignored") as NewDocumentDraft.Obsidian
        assertEquals(url, draft.noteName)
        assertEquals("Actual", draft.vault)
        assertEquals("Note.md", draft.displayName)
    }
    @Test fun `short name or relative path keeps explicit vault and custom label`() {
        listOf("Note", "Folder/Note.md").forEach { target ->
            val draft = buildNewDocumentDraft(DocumentCreationType.OBSIDIAN, " Custom ", target, " Vault ") as NewDocumentDraft.Obsidian
            assertEquals(target, draft.noteName)
            assertEquals("Vault", draft.vault)
            assertEquals("Custom", draft.displayName)
        }
    }
    @Test fun `invalid URL is not persisted as a draft`() {
        assertTrue(runCatching { buildNewDocumentDraft(DocumentCreationType.OBSIDIAN, "", "https://example.org", "V") }.isFailure)
    }
}
