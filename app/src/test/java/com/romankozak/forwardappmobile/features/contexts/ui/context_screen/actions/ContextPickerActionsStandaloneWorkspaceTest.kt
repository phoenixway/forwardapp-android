package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextKeyProblemsRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.FocusContextRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextPickerActionsStandaloneWorkspaceTest {
    @Test
    fun `root picker creation authors standalone Workspace and not Context`() = runBlocking {
        val contextRepository = mockk<ContextRepository>(relaxed = true)
        val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>(relaxed = true)

        coEvery {
            canonicalWorkspaceRepository.create(
                nameOverride = "Operations",
                descriptionOverride = null,
                parentWorkspaceId = null,
                roleCode = null,
                now = any(),
            )
        } returns "standalone-workspace"

        val actions =
            ContextPickerActions(
                repositories =
                    ContextPickerRepositories(
                        contextRepository = contextRepository,
                        canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                        contextKeyProblemsRepository = mockk<ContextKeyProblemsRepository>(relaxed = true),
                        focusContextRepository = mockk<FocusContextRepository>(relaxed = true),
                        noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true),
                        musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
                        checklistRepository = mockk<ChecklistRepository>(relaxed = true),
                    ),
                listChooserFlowActions = mockk(relaxed = true),
                systemWorkspacePresentationContextProjector =
                    mockk<SystemWorkspacePresentationContextProjector>(relaxed = true),
                loggerTag = "test",
            )

        val id = actions.createRootContextForPicker("  Operations  ")

        assertEquals("standalone-workspace", id)
        coVerify(exactly = 1) {
            canonicalWorkspaceRepository.create(
                nameOverride = "Operations",
                descriptionOverride = null,
                parentWorkspaceId = null,
                roleCode = null,
                now = any(),
            )
        }
    }

    @Test
    fun `blank root picker creation authors nothing`() = runBlocking {
        val contextRepository = mockk<ContextRepository>(relaxed = true)
        val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>(relaxed = true)

        val actions =
            ContextPickerActions(
                repositories =
                    ContextPickerRepositories(
                        contextRepository = contextRepository,
                        canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                        contextKeyProblemsRepository = mockk(relaxed = true),
                        focusContextRepository = mockk(relaxed = true),
                        noteDocumentRepository = mockk(relaxed = true),
                        musicNoteRepository = mockk(relaxed = true),
                        checklistRepository = mockk(relaxed = true),
                    ),
                listChooserFlowActions = mockk(relaxed = true),
                systemWorkspacePresentationContextProjector = mockk(relaxed = true),
                loggerTag = "test",
            )

        assertNull(actions.createRootContextForPicker("   "))

        coVerify(exactly = 0) {
            canonicalWorkspaceRepository.create(any(), any(), any(), any(), any())
        }
    }
}
