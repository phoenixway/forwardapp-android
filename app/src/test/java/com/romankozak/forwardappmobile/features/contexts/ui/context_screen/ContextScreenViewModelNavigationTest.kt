package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.context.ContextCapabilitiesResolver
import com.romankozak.forwardappmobile.core.context.ContextSessionStore
import com.romankozak.forwardappmobile.core.context.DefaultContextController
import com.romankozak.forwardappmobile.core.context.DefaultContextState
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.ViewId
import com.romankozak.forwardappmobile.core.context.ViewSet
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ContextArtifactRepository
import com.romankozak.forwardappmobile.data.repository.ContextLogRepository
import com.romankozak.forwardappmobile.data.repository.ContextMarkdownExporter
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.ContextTimeTrackingRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.ner.NerManager
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SearchUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContextScreenViewModelNavigationTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `onProjectViewChange persists resolved view mode`() = runTest {
        val contextRepository = mockk<ContextRepository>(relaxed = true)
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val contextHandler = mockk<ContextHandler>(relaxed = true)
        val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
        val nerManager = mockk<NerManager>(relaxed = true)
        val reminderParser = mockk<ReminderParser>(relaxed = true)
        val activityRepository = mockk<ActivityRepository>(relaxed = true)
        val contextMarkdownExporter = mockk<ContextMarkdownExporter>(relaxed = true)
        val dayManagementRepository = mockk<DayManagementRepository>(relaxed = true)
        val goalRepository = mockk<GoalRepository>(relaxed = true)
        val listItemRepository = mockk<ListItemRepository>(relaxed = true)
        val noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true)
        val checklistRepository = mockk<com.romankozak.forwardappmobile.data.repository.ChecklistRepository>(relaxed = true)
        val reminderRepository = mockk<ReminderRepository>(relaxed = true)
        val recentItemsRepository = mockk<RecentItemsRepository>(relaxed = true)
        val contextLogRepository = mockk<ContextLogRepository>(relaxed = true)
        val directionRepository = mockk<com.romankozak.forwardappmobile.data.repository.DirectionRepository>(relaxed = true)
        val noteRepository = mockk<LegacyNoteRepository>(relaxed = true)
        val inboxRepository = mockk<InboxRepository>(relaxed = true)
        val contextStructureRepository = mockk<ContextStructureRepository>(relaxed = true)
        val contextArtifactRepository = mockk<ContextArtifactRepository>(relaxed = true)
        val contextTimeTrackingRepository = mockk<ContextTimeTrackingRepository>(relaxed = true)
        val searchUseCase = mockk<SearchUseCase>(relaxed = true)
        val clearAndNavigateHomeUseCase = mockk<com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases.ClearAndNavigateHomeUseCase>(relaxed = true)
        val application = mockk<Application>(relaxed = true)

        val savedStateHandle = SavedStateHandle(mapOf("listId" to "c1"))

        val config =
            ContextConfiguration(
                id = "cfg",
                contextId = "c1",
                enableBacklog = true,
                enableInbox = false,
                enableLog = false,
                enableArtifact = false,
                enableDashboard = false,
                enableAttachments = false,
                enableAdvanced = false,
                enableAutoLinkSubprojects = false,
            )

        every { contextRepository.getContextByIdFlow("c1") } returns flowOf(
            com.romankozak.forwardappmobile.core.data.models.entities.Context(
                id = "c1",
                name = "Test",
                description = null,
                parentId = null,
                createdAt = 0L,
                updatedAt = 0L,
                tags = emptyList(),
                relatedLinks = emptyList(),
            ),
        )
        every { listItemRepository.getItemsForContextStream("c1") } returns flowOf(emptyList())
        every { contextStructureRepository.observeStructureOnly("c1") } returns flowOf(config)
        every { contextLogRepository.getContextLogsStream("c1") } returns flowOf(emptyList())
        every { checklistRepository.getChecklistsForContext("c1") } returns flowOf(emptyList())
        every { noteDocumentRepository.getDocumentsForContext("c1") } returns flowOf(emptyList())
        every { directionRepository.getDirectionItemsForContext("c1") } returns flowOf(emptyList())
        every { reminderRepository.getRemindersForEntityFlow("c1") } returns flowOf(emptyList())
        every { recentItemsRepository.getRecentItemsForContextFlow("c1") } returns flowOf(emptyList())
        every { noteRepository.getNotesForContext("c1") } returns flowOf(emptyList())
        every { goalRepository.getGoalsByContextIdFlow("c1") } returns flowOf(emptyList())
        every { contextRepository.getSubprojectsByParentIdFlow("c1") } returns flowOf(emptyList())

        val initialState =
            DefaultContextState(
                id = ContextId("c1"),
                features = CapabilitySet(emptySet()),
                views = ViewSet(emptySet(), ViewId("backlog")),
                config = config,
            )
        val contextSessionStore =
            ContextSessionStore(
                DefaultContextController(initialState),
                ContextCapabilitiesResolver(),
            )

        val viewModel =
            ContextScreenViewModel(
                searchUseCase = searchUseCase,
                application = application,
                contextRepository = contextRepository,
                settingsRepository = settingsRepository,
                contextHandler = contextHandler,
                alarmScheduler = alarmScheduler,
                nerManager = nerManager,
                reminderParser = reminderParser,
                activityRepository = activityRepository,
                contextMarkdownExporter = contextMarkdownExporter,
                savedStateHandle = savedStateHandle,
                dayManagementRepository = dayManagementRepository,
                clearAndNavigateHomeUseCase = clearAndNavigateHomeUseCase,
                ioDispatcher = dispatcher,
                goalRepository = goalRepository,
                listItemRepository = listItemRepository,
                noteDocumentRepository = noteDocumentRepository,
                checklistRepository = checklistRepository,
                reminderRepository = reminderRepository,
                recentItemsRepository = recentItemsRepository,
                contextLogRepository = contextLogRepository,
                directionRepository = directionRepository,
                noteRepository = noteRepository,
                inboxRepository = inboxRepository,
                contextStructureRepository = contextStructureRepository,
                contextArtifactRepository = contextArtifactRepository,
                contextTimeTrackingRepository = contextTimeTrackingRepository,
                contextSessionStore = contextSessionStore,
            )

        val resolved = contextSessionStore.selectView(ContextViewMode.INBOX)
        assertThat(resolved).isEqualTo(ContextViewMode.BACKLOG)

        viewModel.onProjectViewChange(ContextViewMode.INBOX)

        coVerify {
            contextRepository.updateContextViewMode("c1", ContextViewMode.BACKLOG)
        }
    }
}
