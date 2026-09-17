package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.theme.ThemeSettings
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayFocusesRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.FocusContextRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextClipboardOperationUi
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.toHierarchyPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextActionsUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextClipboardCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextDialogActionCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextMigrationCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ContextSelectionCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.DialogUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.HierarchyFocusCoordinator
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.NavigationUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.PlanningUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ProjectHierarchyScreenStateUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SearchUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SettingsUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SyncUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.ThemingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContextHierarchyScreenViewModelSystemInboxWriteTest {
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
    fun `direct Inbox note creation uses stable system owner when hierarchy has only a renamed Inbox Context`() =
        runTest(dispatcher) {
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            every { contextRepository.getAllContextsFlow() } returns flowOf(listOf(legacyInboxContext()))
            val noteDocumentRepository = mockk<NoteDocumentRepository>()
            coEvery {
                noteDocumentRepository.createDocument(any(), any(), any(), any(), any(), any())
            } returns "document"
            val viewModel =
                createViewModel(
                    contextRepository = contextRepository,
                    noteDocumentRepository = noteDocumentRepository,
                    inboxPresentation = inboxPresentation("Renamed Inbox"),
                )

            viewModel.onEvent(ContextHierarchyScreenEvent.AddNoteDocumentRequest)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                noteDocumentRepository.createDocument(
                    name = "Нова нотатка",
                    contextId = SystemContexts.INBOX.raw,
                    content = "",
                    attachmentType = BacklogItemTypeValues.NOTE_DOCUMENT,
                    roleCode = null,
                    isSystem = false,
                )
            }
        }

    @Test
    fun `direct Inbox note creation works with canonical Inbox and no Context shell`() =
        runTest(dispatcher) {
            val contextRepository = mockk<ContextRepository>(relaxed = true)
            every { contextRepository.getAllContextsFlow() } returns flowOf(emptyList())

            val noteDocumentRepository = mockk<NoteDocumentRepository>()
            coEvery {
                noteDocumentRepository.createDocument(any(), any(), any(), any(), any(), any())
            } returns "document"

            val viewModel =
                createViewModel(
                    contextRepository = contextRepository,
                    noteDocumentRepository = noteDocumentRepository,
                    inboxPresentation = inboxPresentation("Inbox"),
                )

            viewModel.onEvent(ContextHierarchyScreenEvent.AddNoteDocumentRequest)
            advanceUntilIdle()

            coVerify(exactly = 1) {
                noteDocumentRepository.createDocument(
                    name = "Нова нотатка",
                    contextId = SystemContexts.INBOX.raw,
                    content = "",
                    attachmentType = BacklogItemTypeValues.NOTE_DOCUMENT,
                    roleCode = null,
                    isSystem = false,
                )
            }
        }

    private fun createViewModel(
        contextRepository: ContextRepository,
        noteDocumentRepository: NoteDocumentRepository,
        inboxPresentation: ContextPresentation,
    ): ContextHierarchyScreenViewModel {
        val planningUseCase = mockk<PlanningUseCase>(relaxed = true)
        every { planningUseCase.filterStateFlow } returns MutableStateFlow(emptyFilterState())
        val hierarchyStateUseCase = mockk<ProjectHierarchyScreenStateUseCase>(relaxed = true)
        every { hierarchyStateUseCase.uiState } returns MutableStateFlow(ProjectHierarchyScreenUiState())
        every { hierarchyStateUseCase.observeHierarchyPresentationUniverse(any()) } returns
            flowOf(listOf(inboxPresentation.toHierarchyPresentationNode()))
        val navigationUseCase = mockk<NavigationUseCase>(relaxed = true)
        every { navigationUseCase.isProcessingReveal } returns MutableStateFlow(false)
        val clipboardCoordinator = mockk<ContextClipboardCoordinator>(relaxed = true)
        every { clipboardCoordinator.uiState } returns
            MutableStateFlow<Pair<Set<String>, ContextClipboardOperationUi?>>(emptySet<String>() to null)
        every { clipboardCoordinator.hasBeaconPayload } returns MutableStateFlow(false)
        val contextMarkerHandler = mockk<ContextMarkerHandler>(relaxed = true)
        every { contextMarkerHandler.contextMarkerToEmojiMap } returns MutableStateFlow(emptyMap<String, String>())
        val focusContextRepository = mockk<FocusContextRepository>(relaxed = true)
        every { focusContextRepository.observeActiveFocusContextIds() } returns flowOf(emptySet<String>())
        val activityRepository = mockk<ActivityRepository>(relaxed = true)
        every { activityRepository.getLogStream() } returns flowOf(emptyList<ActivityRecord>())
        val themingUseCase = mockk<ThemingUseCase>()
        every { themingUseCase.themeSettings } returns flowOf(ThemeSettings())
        val searchUseCase = mockk<SearchUseCase>(relaxed = true)
        every { searchUseCase.subStateStack } returns
            MutableStateFlow(listOf(ProjectHierarchyScreenSubState.Hierarchy))
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.isBottomNavExpandedFlow } returns flowOf(false)

        return ContextHierarchyScreenViewModel(
            contextRepository = contextRepository,
            settingsRepo = settingsRepository,
            searchUseCase = searchUseCase,
            dialogUseCase = mockk<DialogUseCase>(relaxed = true),
            ioDispatcher = dispatcher,
            contextMarkerHandler = contextMarkerHandler,
            dayManagementRepository = mockk<DayManagementRepository>(relaxed = true),
            dayFocusesRepository = mockk<DayFocusesRepository>(relaxed = true),
            focusContextRepository = focusContextRepository,
            activityRepository = activityRepository,
            recentItemsRepository = mockk<RecentItemsRepository>(relaxed = true),
            noteRepository = mockk<LegacyNoteRepository>(relaxed = true),
            noteDocumentRepository = noteDocumentRepository,
            checklistRepository = mockk<ChecklistRepository>(relaxed = true),
            musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
            application = mockk<Application>(relaxed = true),
            savedStateHandle = SavedStateHandle(),
            planningUseCase = planningUseCase,
            syncUseCase = mockk<SyncUseCase>(relaxed = true),
            contextActionsUseCase = mockk<ContextActionsUseCase>(relaxed = true),
            navigationUseCase = navigationUseCase,
            themingUseCase = themingUseCase,
            settingsUseCase = mockk<SettingsUseCase>(relaxed = true),
            projectHierarchyScreenStateUseCase = hierarchyStateUseCase,
            contextClipboardCoordinator = clipboardCoordinator,
            hierarchyFocusCoordinator = mockk<HierarchyFocusCoordinator>(relaxed = true),
            contextSelectionCoordinator = ContextSelectionCoordinator(),
            contextDialogActionCoordinator = mockk<ContextDialogActionCoordinator>(relaxed = true),
            contextMigrationCoordinator = mockk<ContextMigrationCoordinator>(relaxed = true),
        )
    }

    private fun emptyFilterState() =
        FilterState(
            flatList = emptyList(),
            query = "",
            searchActive = false,
            mode = PlanningMode.All,
            settings = PlanningSettingsState(),
        )

    private fun inboxPresentation(name: String) =
        ContextPresentation(
            id = SystemContexts.INBOX.raw,
            name = name,
            description = null,
            parentId = null,
            roleCode = null,
            order = 0L,
            tags = emptyList(),
        )

    private fun legacyInboxContext() =
        Context(
            id = SystemContexts.INBOX.raw,
            name = "Renamed Inbox",
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
        )
}
