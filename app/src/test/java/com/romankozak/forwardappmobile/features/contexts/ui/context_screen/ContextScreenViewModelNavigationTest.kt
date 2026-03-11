@file:Suppress("PackageNaming")

package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.context.ContextCapabilitiesResolver
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.ContextSessionStore
import com.romankozak.forwardappmobile.core.context.DefaultContextController
import com.romankozak.forwardappmobile.core.context.DefaultContextState
import com.romankozak.forwardappmobile.core.context.ViewId
import com.romankozak.forwardappmobile.core.context.ViewSet
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.navigation.ClearAndNavigateHomeUseCase
import com.romankozak.forwardappmobile.core.navigation.capability.actions.CapabilityViewActionRegistry
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ContextArtifactRepository
import com.romankozak.forwardappmobile.data.repository.ContextKeyProblemsRepository
import com.romankozak.forwardappmobile.data.repository.ContextLogRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.ContextTimeTrackingRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.FocusContextRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.domain.ner.NerManager
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogClipboardUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.ContextMarkdownExporter
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContextScreenViewModelNavigationTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val contextId = "c1"

    @Before
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `onProjectViewChange persists resolved view mode`() =
        runTest {
            val fixture = createFixture()
            val resolved = fixture.contextSessionStore.selectView(ContextViewMode.INBOX)
            assertThat(resolved).isEqualTo(ContextViewMode.DASHBOARD)

            fixture.viewModel.onProjectViewChange(ContextViewMode.INBOX)

            coVerify {
                fixture.contextRepository.updateContextViewMode(contextId, ContextViewMode.DASHBOARD)
            }
        }

    private fun createFixture(): Fixture {
        val contextRepository = mockk<ContextRepository>(relaxed = true)
        val config = createConfig()
        stubFlows(contextRepository, config)

        val contextSessionStore = createContextSessionStore(config)
        val viewModel =
            ContextScreenViewModel(
                searchUseCase = mockk(relaxed = true),
                application = mockk<Application>(relaxed = true),
                contextRepository = contextRepository,
                settingsRepository = mockk(relaxed = true),
                contextHandler = mockk<ContextHandler>(relaxed = true),
                alarmScheduler = mockk<AlarmScheduler>(relaxed = true),
                nerManager = mockk<NerManager>(relaxed = true),
                reminderParser = mockk<ReminderParser>(relaxed = true),
                activityRepository = mockk<ActivityRepository>(relaxed = true),
                contextMarkdownExporter = mockk<ContextMarkdownExporter>(relaxed = true),
                savedStateHandle = SavedStateHandle(mapOf("listId" to contextId)),
                dayManagementRepository = mockk<DayManagementRepository>(relaxed = true),
                clearAndNavigateHomeUseCase = mockk<ClearAndNavigateHomeUseCase>(relaxed = true),
                ioDispatcher = dispatcher,
                goalRepository = goalRepository,
                listItemRepository = listItemRepository,
                noteDocumentRepository = noteDocumentRepository,
                musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
                checklistRepository = checklistRepository,
                reminderRepository = reminderRepository,
                recentItemsRepository = recentItemsRepository,
                contextLogRepository = contextLogRepository,
                directionRepository = directionRepository,
                noteRepository = noteRepository,
                inboxRepository = mockk<InboxRepository>(relaxed = true),
                contextStructureRepository = contextStructureRepository,
                contextArtifactRepository = mockk<ContextArtifactRepository>(relaxed = true),
                contextKeyProblemsRepository = contextKeyProblemsRepository,
                focusContextRepository = focusContextRepository,
                contextTimeTrackingRepository = mockk<ContextTimeTrackingRepository>(relaxed = true),
                contextSessionStore = contextSessionStore,
                backlogClipboardUseCase = mockk<BacklogClipboardUseCase>(relaxed = true),
                capabilityViewActionRegistry = mockk<CapabilityViewActionRegistry>(relaxed = true),
            )
        return Fixture(viewModel, contextSessionStore, contextRepository)
    }

    private val listItemRepository = mockk<ListItemRepository>(relaxed = true)
    private val goalRepository = mockk<GoalRepository>(relaxed = true)
    private val noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true)
    private val checklistRepository =
        mockk<com.romankozak.forwardappmobile.data.repository.ChecklistRepository>(relaxed = true)
    private val reminderRepository = mockk<ReminderRepository>(relaxed = true)
    private val recentItemsRepository = mockk<RecentItemsRepository>(relaxed = true)
    private val contextLogRepository = mockk<ContextLogRepository>(relaxed = true)
    private val directionRepository =
        mockk<com.romankozak.forwardappmobile.data.repository.DirectionRepository>(relaxed = true)
    private val noteRepository = mockk<LegacyNoteRepository>(relaxed = true)
    private val contextStructureRepository = mockk<ContextStructureRepository>(relaxed = true)
    private val contextKeyProblemsRepository = mockk<ContextKeyProblemsRepository>(relaxed = true)
    private val focusContextRepository = mockk<FocusContextRepository>(relaxed = true)

    private fun createConfig(): ContextConfiguration =
        ContextConfiguration(
            id = "cfg",
            contextId = contextId,
            enableBacklog = true,
            enableInbox = false,
            enableLog = false,
            enableArtifact = false,
            enableDashboard = false,
            enableAttachments = false,
            enableAdvanced = false,
            enableAutoLinkSubprojects = false,
        )

    private fun stubFlows(
        contextRepository: ContextRepository,
        config: ContextConfiguration,
    ) {
        every { contextRepository.getContextByIdFlow(contextId) } returns
            flowOf(
                com.romankozak.forwardappmobile.core.data.models.entities.Context(
                    id = contextId,
                    name = "Test",
                    description = null,
                    parentId = null,
                    createdAt = 0L,
                    updatedAt = 0L,
                    tags = emptyList(),
                    relatedLinks = emptyList(),
                ),
            )
        every { listItemRepository.getItemsForContextStream(contextId) } returns flowOf(emptyList())
        every { contextStructureRepository.observeStructureOnly(contextId) } returns flowOf(config)
        every { contextLogRepository.getContextLogsStream(contextId) } returns flowOf(emptyList())
        every { checklistRepository.getChecklistsForContext(contextId) } returns flowOf(emptyList())
        every { noteDocumentRepository.getDocumentsForContext(contextId) } returns flowOf(emptyList())
        every { directionRepository.getDirectionItemsForContext(contextId) } returns flowOf(emptyList())
        every { reminderRepository.getRemindersForEntityFlow(contextId) } returns flowOf(emptyList())
        every { recentItemsRepository.getRecentItemsForContextFlow(contextId) } returns flowOf(emptyList())
        every { noteRepository.getNotesForContext(contextId) } returns flowOf(emptyList())
        every { goalRepository.getGoalsByContextIdFlow(contextId) } returns flowOf(emptyList())
        every { contextRepository.getSubprojectsByParentIdFlow(contextId) } returns flowOf(emptyList())
        every { contextRepository.getAttachmentLibraryItemsFlow() } returns flowOf(emptyList())
        every { contextKeyProblemsRepository.observe(contextId) } returns
            flowOf(ContextKeyProblemsRepository.KeyProblemsData())
        every { focusContextRepository.observeActiveFocusContextIds(any()) } returns flowOf(emptySet())
    }

    private fun createContextSessionStore(config: ContextConfiguration): ContextSessionStore {
        val initialState =
            DefaultContextState(
                id = ContextId(contextId),
                features = CapabilitySet(emptySet()),
                views = ViewSet(emptySet(), ViewId("backlog")),
                config = config,
            )
        return ContextSessionStore(
            DefaultContextController(initialState),
            ContextCapabilitiesResolver(),
        )
    }

    private data class Fixture(
        val viewModel: ContextScreenViewModel,
        val contextSessionStore: ContextSessionStore,
        val contextRepository: ContextRepository,
    )
}
