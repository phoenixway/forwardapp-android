package com.romankozak.forwardappmobile.features.strategicmanagement

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StrategicManagementViewModelSystemShellTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `canonical Strategic works without Context shell for read and tag write`() =
        runTest(dispatcher) {
            val id = SystemContexts.STRATEGIC.raw

            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>(relaxed = true)
            val canonicalWorkspaceTagRepository = mockk<CanonicalWorkspaceTagRepository>(relaxed = true)
            val projector = mockk<SystemWorkspacePresentationContextProjector>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            val settingsRepository = mockk<SettingsRepository>()
            val attachmentsRepository = mockk<AttachmentsRepository>(relaxed = true)

            every { contextRepository.getAllContextsFlow() } returns flowOf(emptyList())
            every {
                projector.observePresentationUniverse(any())
            } returns
                flowOf(
                    listOf(
                        ContextPresentation(
                            id = id,
                            name = "Strategic",
                            description = null,
                            parentId = null,
                            roleCode = null,
                            order = 0L,
                            tags = listOf("strategic"),
                        ),
                    ),
                )

            every { attachmentsRepository.getAttachmentLibraryItems() } returns flowOf(emptyList())
            every { settingsRepository.obsidianVaultNameFlow } returns flowOf("")
            every { settingsRepository.strategicLinkedAttachmentIdsFlow } returns flowOf(emptySet())
            every { settingsRepository.strategicScopeContextsExpandedFlow } returns flowOf(true)
            every { settingsRepository.strategicScopeAttachmentsExpandedFlow } returns flowOf(true)
            every { settingsRepository.strategicConnectionsOrderFlow } returns flowOf(emptyList())

            coEvery { tagAuthority.resolve(id) } returns
                SystemWorkspaceTagAuthority.Resolution.Canonical(emptyList())

            val viewModel =
                StrategicManagementViewModel(
                    contextRepository = contextRepository,
                    canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                    canonicalWorkspaceTagRepository = canonicalWorkspaceTagRepository,
                    systemWorkspacePresentationContextProjector = projector,
                    systemWorkspaceTagAuthority = tagAuthority,
                    settingsRepository = settingsRepository,
                    attachmentsRepository = attachmentsRepository,
                    noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true),
                    musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
                    checklistRepository = mockk<ChecklistRepository>(relaxed = true),
                )

            val state = viewModel.uiState.first { !it.isLoading }

            assertEquals(id, state.allProjects.single().id)
            assertEquals(id, state.dashboardProjects.single().id)

            viewModel.addStrategicLink(id)
            advanceUntilIdle()

            coVerify(exactly = 0) { contextRepository.getContextById(id) }
            coVerify(exactly = 1) {
                contextRepository.updateContextTags(id, listOf("strategic"))
            }
        }

    @Test
    fun `Strategic tags shell free standalone Workspace through canonical tags`() =
        runTest(dispatcher) {
            val id = "standalone-strategic"
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val canonicalWorkspaceTagRepository = mockk<CanonicalWorkspaceTagRepository>(relaxed = true)
            val projector = mockk<SystemWorkspacePresentationContextProjector>()
            val tagAuthority = mockk<SystemWorkspaceTagAuthority>()
            val settingsRepository = mockk<SettingsRepository>()
            val attachmentsRepository = mockk<AttachmentsRepository>(relaxed = true)

            every { contextRepository.getAllContextsFlow() } returns flowOf(emptyList())
            coEvery { contextRepository.getContextById(id) } returns null
            coEvery { tagAuthority.resolve(id) } returns SystemWorkspaceTagAuthority.Resolution.NotSystem
            coEvery { projector.resolvePresentation(id, null) } returns standalonePresentation(id)
            coEvery { canonicalWorkspaceTagRepository.getTags(id) } returns emptyList()
            every { projector.observePresentationUniverse(any()) } returns flowOf(emptyList())
            every { attachmentsRepository.getAttachmentLibraryItems() } returns flowOf(emptyList())
            every { settingsRepository.obsidianVaultNameFlow } returns flowOf("")
            every { settingsRepository.strategicLinkedAttachmentIdsFlow } returns flowOf(emptySet())
            every { settingsRepository.strategicScopeContextsExpandedFlow } returns flowOf(true)
            every { settingsRepository.strategicScopeAttachmentsExpandedFlow } returns flowOf(true)
            every { settingsRepository.strategicConnectionsOrderFlow } returns flowOf(emptyList())

            val viewModel =
                StrategicManagementViewModel(
                    contextRepository = contextRepository,
                    canonicalWorkspaceRepository = mockk(relaxed = true),
                    canonicalWorkspaceTagRepository = canonicalWorkspaceTagRepository,
                    systemWorkspacePresentationContextProjector = projector,
                    systemWorkspaceTagAuthority = tagAuthority,
                    settingsRepository = settingsRepository,
                    attachmentsRepository = attachmentsRepository,
                    noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true),
                    musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
                    checklistRepository = mockk<ChecklistRepository>(relaxed = true),
                )

            viewModel.addStrategicLink(id)
            advanceUntilIdle()

            coVerify(exactly = 0) { contextRepository.updateContextTags(any(), any()) }
            coVerify(exactly = 1) {
                canonicalWorkspaceTagRepository.replaceTags(id, listOf("strategic"), any())
            }
        }

    private fun standalonePresentation(id: String) =
        ContextPresentation(
            id = id,
            name = "Standalone",
            description = null,
            parentId = null,
            roleCode = null,
            order = 0L,
            tags = emptyList(),
        )
}
