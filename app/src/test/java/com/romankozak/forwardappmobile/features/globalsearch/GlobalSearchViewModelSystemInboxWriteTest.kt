package com.romankozak.forwardappmobile.features.globalsearch

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavigationCommand
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
class GlobalSearchViewModelSystemInboxWriteTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `Quick Catch writes to stable system Inbox without Context discovery`() =
        runTest(dispatcher) {
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            every { contextRepository.getAllContextsFlow() } returns flowOf(listOf(legacyInboxContext()))
            val inboxRepository = mockk<InboxRepository>()
            coEvery { inboxRepository.addInboxRecord(any(), any()) } returns "record"
            val viewModel = createViewModel(contextRepository, inboxRepository)

            viewModel.setMode(OmniboxMode.QuickCatchInbox)
            viewModel.onQueryChange("  Capture this  ")
            viewModel.onSubmitSearch()
            advanceUntilIdle()

            coVerify(exactly = 1) {
                inboxRepository.addInboxRecord(
                    text = "Capture this",
                    contextId = SystemContexts.INBOX.raw,
                )
            }
            verify(exactly = 0) { contextRepository.getAllContextsFlow() }
        }

    @Test
    fun `quick create authors a standalone Workspace without Context persistence`() =
        runTest(dispatcher) {
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val canonicalWorkspaceRepository = mockk<CanonicalWorkspaceRepository>()
            coEvery {
                canonicalWorkspaceRepository.create(any(), any(), any(), any(), any())
            } returns "standalone-workspace"
            val viewModel =
                createViewModel(
                    contextRepository = contextRepository,
                    inboxRepository = mockk(relaxed = true),
                    canonicalWorkspaceRepository = canonicalWorkspaceRepository,
                )
            viewModel.enhancedNavigationManager =
                EnhancedNavigationManager(
                    savedStateHandle = SavedStateHandle(),
                    scope = backgroundScope,
                )
            val command = async { viewModel.enhancedNavigationManager.navigationCommandFlow.first() }

            viewModel.createContext("  Operations  ")
            advanceUntilIdle()

            coVerify(exactly = 1) {
                canonicalWorkspaceRepository.create(
                    nameOverride = "Operations",
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    now = any(),
                )
            }
            coVerify(exactly = 0) { contextRepository.createContextWithId(any(), any(), any()) }
            assertEquals(
                NavTarget.ContextHierarchy(projectIdToReveal = "standalone-workspace"),
                (command.await() as NavigationCommand.NavigateTarget).target,
            )
        }

    @Test
    fun `shell-free operational search result opens normal owner route`() =
        runTest(dispatcher) {
            val projectId = "retired-operational-owner"
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            val projector = mockk<SystemWorkspacePresentationContextProjector>()
            coEvery { projector.resolvePresentation(projectId) } returns
                ContextPresentation(
                    id = projectId,
                    name = "Restored operations",
                    description = "Canonical owner",
                    parentId = null,
                    roleCode = null,
                    order = 0L,
                    tags = emptyList(),
                )
            coEvery { contextRepository.getContextById(projectId) } returns null
            val viewModel =
                createViewModel(
                    contextRepository = contextRepository,
                    inboxRepository = mockk(relaxed = true),
                    presentationProjector = projector,
                )
            viewModel.enhancedNavigationManager =
                EnhancedNavigationManager(
                    savedStateHandle = SavedStateHandle(),
                    scope = backgroundScope,
                )
            val command = async { viewModel.enhancedNavigationManager.navigationCommandFlow.first() }

            viewModel.navigateToProjectForResult(projectId, null)
            advanceUntilIdle()

            assertEquals(
                NavTarget.ContextDetail(contextId = projectId),
                (command.await() as NavigationCommand.NavigateTarget).target,
            )
        }

    private fun createViewModel(
        contextRepository: ContextRepository,
        inboxRepository: InboxRepository,
        canonicalWorkspaceRepository: CanonicalWorkspaceRepository = mockk(relaxed = true),
        presentationProjector: SystemWorkspacePresentationContextProjector = mockk(relaxed = true),
    ): GlobalSearchViewModel {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.obsidianVaultNameFlow } returns flowOf("")
        every { settingsRepository.globalSearchCurrentModeFlow } returns flowOf(OmniboxMode.DataSearch)
        every { settingsRepository.globalSearchSelectedTypesFlow } returns flowOf(emptySet<String>())
        every { settingsRepository.globalSearchModeDisplayPrefsFlow } returns flowOf(OmniboxModeDisplayPrefsState())
        every { settingsRepository.globalSearchRecentInputsFlow(any()) } returns flowOf(emptyList<String>())

        return GlobalSearchViewModel(
            contextRepository = contextRepository,
            settingsRepository = settingsRepository,
            inboxRepository = inboxRepository,
            activityRepository = mockk<ActivityRepository>(relaxed = true),
            reminderRepository = mockk<ReminderRepository>(relaxed = true),
            noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true),
            musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
            checklistRepository = mockk<ChecklistRepository>(relaxed = true),
            canonicalWorkspaceRepository = canonicalWorkspaceRepository,
            systemWorkspacePresentationContextProjector = presentationProjector,
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun legacyInboxContext() =
        Context(
            id = "legacy-inbox",
            name = "Inbox",
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
        )
}
