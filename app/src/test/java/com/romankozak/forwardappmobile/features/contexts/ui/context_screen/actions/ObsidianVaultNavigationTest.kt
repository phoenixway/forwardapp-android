package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.handlers.NoteDocumentHandler
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObsidianVaultNavigationTest {
    private val settings = mockk<SettingsRepository>()
    private fun navigation() = NavigationActions(mockk(relaxed = true), settings, mockk(), "handle_link")
    private val link = RelatedLink(type = LinkType.OBSIDIAN, target = "Folder/Note & plan", vault = " My & Vault ")

    @Test fun `Connections uses explicit vault without requiring global settings`() = runTest {
        val result = navigation().resolveLinkItemClick(link)
        assertThat(result).isEqualTo(NavigationActions.LinkItemClickResult.OpenUri(
            "obsidian://open?vault=My%20%26%20Vault&file=Folder%2FNote%20%26%20plan"))
        verify { settings wasNot Called }
    }

    @Test fun `explicit vault takes precedence over another global vault`() = runTest {
        every { settings.obsidianVaultNameFlow } returns flowOf("Other")
        assertThat(navigation().resolveObsidianNoteOpen("Note", "Local")).isEqualTo(
            NavigationActions.OpenObsidianNoteResult.OpenUri("obsidian://open?vault=Local&file=Note"))
    }

    @Test fun `missing or blank explicit vault falls back to settings`() = runTest {
        every { settings.obsidianVaultNameFlow } returns flowOf(" Global vault ")
        listOf(null, "  ").forEach { vault ->
            assertThat(navigation().resolveObsidianNoteOpen("Note", vault)).isEqualTo(
                NavigationActions.OpenObsidianNoteResult.OpenUri("obsidian://open?vault=Global%20vault&file=Note"))
        }
    }

    @Test fun `missing explicit and global vault safely reports unavailable`() = runTest {
        every { settings.obsidianVaultNameFlow } returns flowOf("")
        assertThat(navigation().resolveLinkItemClick(link.copy(vault = null))).isEqualTo(
            NavigationActions.LinkItemClickResult.VaultNotConfigured)
    }

    @Test fun `creating note encodes explicit vault and does not require global settings`() = runTest {
        val listener = mockk<NoteDocumentHandler.ResultListener>(relaxed = true)
        NoteDocumentHandler(mockk(), settings, mockk(), listener, this)
            .createObsidianNote("Note & plan", " My & Vault ")
        testScheduler.runCurrent()
        verify(exactly = 1) { listener.openUri("obsidian://new?vault=My+%26+Vault&name=Note+%26+plan") }
        verify(exactly = 0) { listener.showSnackbar(any()) }
        verify { settings wasNot Called }
    }

    @Test fun `copied Obsidian URL opens unchanged without global vault`() = runTest {
        val url = "obsidian://open?vault=Actual&file=Folder%2FNote.md%23Heading"
        assertThat(navigation().resolveLinkItemClick(link.copy(target = url, vault = "Wrong"))).isEqualTo(
            NavigationActions.LinkItemClickResult.OpenUri(url))
        verify { settings wasNot Called }
    }

    @Test fun `invalid external URL fails safely`() = runTest {
        assertThat(navigation().resolveObsidianNoteOpen("https://example.org")).isEqualTo(
            NavigationActions.OpenObsidianNoteResult.InvalidAddress)
    }

    @Test fun `new note at vault-relative path uses file not name`() = runTest {
        val listener = mockk<NoteDocumentHandler.ResultListener>(relaxed = true)
        NoteDocumentHandler(mockk(), settings, mockk(), listener, this).createObsidianNote("Folder/Note.md", "Vault")
        testScheduler.runCurrent()
        verify { listener.openUri("obsidian://new?vault=Vault&file=Folder%2FNote.md") }
    }
}
