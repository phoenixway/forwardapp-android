package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.theme.ThemeSettings
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyOccurrenceRef
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyOccurrenceRef
import com.romankozak.forwardappmobile.data.logic.ContextMarkerHandler
import com.romankozak.forwardappmobile.data.repository.*
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceRepository
import com.romankozak.forwardappmobile.data.workspace.capability.*
import com.romankozak.forwardappmobile.data.orientation.*
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.*
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WorkspaceClipboardV2EventRoomIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() { kotlinx.coroutines.Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { kotlinx.coroutines.Dispatchers.resetMain() }

    private data class Fixture(
        val workspaces: CanonicalWorkspaceRepository,
        val placements: CanonicalHierarchyPlacementRepository,
        val clipboard: WorkspaceClipboardCoordinator,
        val viewModel: ContextHierarchyScreenViewModel,
        val selection: ContextSelectionCoordinator,
    )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun fixture(db: AppDatabase): Fixture {
        val placements = CanonicalHierarchyPlacementRepository(db)
        val workspaces = CanonicalWorkspaceRepository(
            database = db,
            workspaceDao = db.workspaceDao(),
            orientationDao = db.orientationDao(),
            executionLogRepository = mockk<CanonicalExecutionLogRepository>(relaxed = true),
            keyProblemsRepository = mockk<CanonicalKeyProblemsRepository>(relaxed = true),
            directionRepository = mockk<CanonicalDirectionRepository>(relaxed = true),
            inboxRepository = mockk<CanonicalInboxRepository>(relaxed = true),
            connectionsRepository = mockk<CanonicalConnectionsRepository>(relaxed = true),
            backlogRepository = mockk<CanonicalBacklogRepository>(relaxed = true),
            hierarchyPlacementLifecycleCoordinator = HierarchyPlacementLifecycleCoordinator(db),
            hierarchyPlacementRepository = placements,
        )
        val clipboard = WorkspaceClipboardCoordinator(
            canonicalWorkspaceRepository = workspaces,
            mainBeaconRepository = mockk<MainBeaconRepository>(relaxed = true),
        )
        val selection = ContextSelectionCoordinator()
        return Fixture(workspaces, placements, clipboard, viewModel(clipboard, selection), selection)
    }

    private suspend fun seed(
        db: AppDatabase,
        id: String,
        parent: String? = null,
        order: Long = 0L,
    ) {
        db.workspaceDao().upsert(listOf(WorkspaceEntity(
            id = id,
            nameOverride = id.replaceFirstChar { it.uppercase() },
            descriptionOverride = null,
            roleCode = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )))
    }

    private fun target(id: String) =
        HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id)

    private suspend fun occurrence(repo: CanonicalHierarchyPlacementRepository, id: PlacementId):
        HierarchyOccurrenceRef = requireNotNull(repo.getPlacement(id)).toHierarchyOccurrenceRef()

    private fun viewModel(
        clipboard: WorkspaceClipboardCoordinator,
        selection: ContextSelectionCoordinator,
    ): ContextHierarchyScreenViewModel {
        val contexts = mockk<ContextRepository>(relaxed = true)
        every { contexts.getAllContextsFlow() } returns flowOf(emptyList())
        val planning = mockk<PlanningUseCase>(relaxed = true)
        every { planning.filterStateFlow } returns MutableStateFlow(FilterState(
            flatList = emptyList(),
            query = "",
            searchActive = false,
            mode = PlanningMode.All,
            settings = PlanningSettingsState(),
        ))
        val hierarchy = mockk<ProjectHierarchyScreenStateUseCase>(relaxed = true)
        every { hierarchy.uiState } returns MutableStateFlow(ProjectHierarchyScreenUiState())
        every { hierarchy.observeHierarchyPresentationUniverse(any()) } returns flowOf(emptyList())
        every { hierarchy.canonicalV2Read } returns MutableStateFlow(null)
        val navigation = mockk<NavigationUseCase>(relaxed = true)
        every { navigation.isProcessingReveal } returns MutableStateFlow(false)
        val contextClipboard = mockk<ContextClipboardCoordinator>(relaxed = true)
        every { contextClipboard.uiState } returns
            MutableStateFlow<Pair<Set<String>, ContextClipboardOperationUi?>>(emptySet<String>() to null)
        every { contextClipboard.hasBeaconPayload } returns MutableStateFlow(false)
        val marker = mockk<ContextMarkerHandler>(relaxed = true)
        every { marker.contextMarkerToEmojiMap } returns MutableStateFlow(emptyMap<String, String>())
        val focus = mockk<FocusContextRepository>(relaxed = true)
        every { focus.observeActiveFocusContextIds() } returns flowOf(emptySet())
        val activity = mockk<ActivityRepository>(relaxed = true)
        every { activity.getLogStream() } returns flowOf(emptyList<ActivityRecord>())
        val theme = mockk<ThemingUseCase>()
        every { theme.themeSettings } returns flowOf(ThemeSettings())
        val search = mockk<SearchUseCase>(relaxed = true)
        every { search.subStateStack } returns
            MutableStateFlow(listOf(ProjectHierarchyScreenSubState.Hierarchy))
        val settings = mockk<SettingsRepository>(relaxed = true)
        every { settings.isBottomNavExpandedFlow } returns flowOf(false)

        return ContextHierarchyScreenViewModel(
            contextRepository = contexts,
            settingsRepo = settings,
            searchUseCase = search,
            dialogUseCase = mockk<DialogUseCase>(relaxed = true),
            ioDispatcher = dispatcher,
            contextMarkerHandler = marker,
            dayManagementRepository = mockk<DayManagementRepository>(relaxed = true),
            dayFocusesRepository = mockk<DayFocusesRepository>(relaxed = true),
            focusContextRepository = focus,
            activityRepository = activity,
            recentItemsRepository = mockk<RecentItemsRepository>(relaxed = true),
            noteRepository = mockk<LegacyNoteRepository>(relaxed = true),
            noteDocumentRepository = mockk<NoteDocumentRepository>(relaxed = true),
            checklistRepository = mockk<ChecklistRepository>(relaxed = true),
            musicNoteRepository = mockk<MusicNoteRepository>(relaxed = true),
            application = mockk<Application>(relaxed = true),
            savedStateHandle = SavedStateHandle(),
            planningUseCase = planning,
            syncUseCase = mockk<SyncUseCase>(relaxed = true),
            contextActionsUseCase = mockk<ContextActionsUseCase>(relaxed = true),
            navigationUseCase = navigation,
            themingUseCase = theme,
            settingsUseCase = mockk<SettingsUseCase>(relaxed = true),
            projectHierarchyScreenStateUseCase = hierarchy,
            contextClipboardCoordinator = contextClipboard,
            workspaceClipboardCoordinator = clipboard,
            hierarchyFocusCoordinator = mockk<HierarchyFocusCoordinator>(relaxed = true),
            contextSelectionCoordinator = selection,
            contextDialogActionCoordinator = mockk<ContextDialogActionCoordinator>(relaxed = true),
            contextMigrationCoordinator = mockk<ContextMigrationCoordinator>(relaxed = true),
        )
    }

    @Test
    fun `ViewModel V2 CUT moves exact LINK to exact destination and preserves legacy ancestry`() =
        runTest(dispatcher) {
            val db = database()
            try {
                val fx = fixture(db)
                seed(db, "legacy-parent")
                seed(db, "source", parent = "legacy-parent", order = 77L)
                seed(db, "destination")
                val primary = fx.placements.createPrimaryAppearance(target("source"), now = 10L)
                val link = fx.placements.createLinkAppearance(target("source"), now = 11L)
                val destinationPrimary =
                    fx.placements.createPrimaryAppearance(target("destination"), now = 12L)
                val destinationLink =
                    fx.placements.createLinkAppearance(target("destination"), now = 13L)
                val sourceBefore = requireNotNull(db.workspaceDao().getById("source"))
                val primaryBefore = requireNotNull(fx.placements.getPlacement(primary))

                val cutToast = async { fx.viewModel.uiEventFlow.first() }
                fx.viewModel.onEvent(
                    ContextHierarchyScreenEvent.CutWorkspace(
                        projectId = "source",
                        occurrence = occurrence(fx.placements, link),
                    ),
                )
                advanceUntilIdle()
                assertEquals("Проєкт вирізано", (cutToast.await() as ProjectUiEvent.ShowToast).message)
                assertEquals(setOf("source"), fx.clipboard.uiState.value.first)
                assertEquals(ContextClipboardOperationUi.CUT, fx.clipboard.uiState.value.second)

                val pasteToast = async { fx.viewModel.uiEventFlow.first() }
                fx.viewModel.onEvent(
                    ContextHierarchyScreenEvent.PasteWorkspace(
                        projectId = "destination",
                        destinationOccurrence = occurrence(fx.placements, destinationLink),
                    ),
                )
                advanceUntilIdle()
                assertEquals("Проєкт переміщено", (pasteToast.await() as ProjectUiEvent.ShowToast).message)
                assertEquals(destinationLink, fx.placements.getPlacement(link)?.parentPlacementId)
                assertNotEquals(destinationPrimary, fx.placements.getPlacement(link)?.parentPlacementId)
                assertEquals(primaryBefore, fx.placements.getPlacement(primary))
                assertEquals(sourceBefore, db.workspaceDao().getById("source"))
                assertFalse(fx.clipboard.hasPayload())
                assertTrue(fx.clipboard.uiState.value.first.isEmpty())
                assertNull(fx.clipboard.uiState.value.second)
            } finally {
                db.close()
            }
        }

    @Test
    fun `ViewModel V2 COPY creates fresh PRIMARY under exact destination LINK`() =
        runTest(dispatcher) {
            val db = database()
            try {
                val fx = fixture(db)
                seed(db, "legacy-parent")
                seed(db, "source", parent = "legacy-parent", order = 91L)
                seed(db, "destination")
                val sourcePrimary =
                    fx.placements.createPrimaryAppearance(target("source"), now = 10L)
                val destinationPrimary =
                    fx.placements.createPrimaryAppearance(target("destination"), now = 11L)
                val destinationLink =
                    fx.placements.createLinkAppearance(target("destination"), now = 12L)
                val beforeIds = db.workspaceDao().getAll().mapTo(mutableSetOf()) { it.id }
                val sourceBefore = requireNotNull(db.workspaceDao().getById("source"))
                val sourcePlacementBefore = requireNotNull(fx.placements.getPlacement(sourcePrimary))

                val copyToast = async { fx.viewModel.uiEventFlow.first() }
                fx.viewModel.onEvent(ContextHierarchyScreenEvent.CopyWorkspace("source"))
                advanceUntilIdle()
                assertEquals(
                    "Проєкт скопійовано в буфер",
                    (copyToast.await() as ProjectUiEvent.ShowToast).message,
                )

                val pasteToast = async { fx.viewModel.uiEventFlow.first() }
                fx.viewModel.onEvent(
                    ContextHierarchyScreenEvent.PasteWorkspace(
                        projectId = "destination",
                        destinationOccurrence = occurrence(fx.placements, destinationLink),
                    ),
                )
                advanceUntilIdle()
                assertEquals(
                    "Копію проєкту створено",
                    (pasteToast.await() as ProjectUiEvent.ShowToast).message,
                )

                val copied = db.workspaceDao().getAll().single { it.id !in beforeIds }
                assertEquals("Source (копія)", copied.nameOverride)
                val copiedPrimary =
                    requireNotNull(fx.placements.getPrimaryAppearance(target(copied.id)))
                assertEquals(destinationLink, copiedPrimary.parentPlacementId)
                assertNotEquals(destinationPrimary, copiedPrimary.parentPlacementId)
                assertEquals(sourceBefore, db.workspaceDao().getById("source"))
                assertEquals(sourcePlacementBefore, fx.placements.getPlacement(sourcePrimary))
                assertEquals(setOf("source"), fx.clipboard.uiState.value.first)
                assertEquals(ContextClipboardOperationUi.COPY, fx.clipboard.uiState.value.second)
            } finally {
                db.close()
            }
        }

    @Test
    fun `ViewModel V2 rejects mismatched source and stale destination without partial writes`() =
        runTest(dispatcher) {
            val db = database()
            try {
                val fx = fixture(db)
                seed(db, "source")
                seed(db, "other")
                seed(db, "destination")
                val source = fx.placements.createPrimaryAppearance(target("source"), now = 10L)
                val other = fx.placements.createPrimaryAppearance(target("other"), now = 11L)
                val destination =
                    fx.placements.createPrimaryAppearance(target("destination"), now = 12L)
                val beforeWorkspaces = db.workspaceDao().getAll()
                val beforePlacements = db.hierarchyPlacementDao().getAll()

                val sourceFailure = async { fx.viewModel.uiEventFlow.first() }
                fx.viewModel.onEvent(
                    ContextHierarchyScreenEvent.CutWorkspace(
                        projectId = "source",
                        occurrence = occurrence(fx.placements, other),
                    ),
                )
                advanceUntilIdle()
                val sourceMessage = (sourceFailure.await() as ProjectUiEvent.ShowToast).message
                assertTrue(sourceMessage.contains("відсутня точна occurrence"))
                assertFalse(sourceMessage.contains("Проєкт вирізано"))
                assertFalse(fx.clipboard.hasPayload())
                assertEquals(beforeWorkspaces, db.workspaceDao().getAll())
                assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())

                val cutToast = async { fx.viewModel.uiEventFlow.first() }
                fx.viewModel.onEvent(
                    ContextHierarchyScreenEvent.CutWorkspace(
                        projectId = "source",
                        occurrence = occurrence(fx.placements, source),
                    ),
                )
                advanceUntilIdle()
                assertEquals("Проєкт вирізано", (cutToast.await() as ProjectUiEvent.ShowToast).message)

                val staleDestination = occurrence(fx.placements, destination)
                fx.placements.removePlacement(destination, now = 20L)
                val beforePasteWorkspaces = db.workspaceDao().getAll()
                val beforePastePlacements = db.hierarchyPlacementDao().getAll()

                val pasteFailure = async { fx.viewModel.uiEventFlow.first() }
                fx.viewModel.onEvent(
                    ContextHierarchyScreenEvent.PasteWorkspace(
                        projectId = "destination",
                        destinationOccurrence = staleDestination,
                    ),
                )
                advanceUntilIdle()
                val failureMessage = (pasteFailure.await() as ProjectUiEvent.ShowToast).message
                assertTrue(failureMessage.contains("destination occurrence is missing"))
                assertFalse(failureMessage.contains("Проєкт переміщено"))
                assertEquals(beforePasteWorkspaces, db.workspaceDao().getAll())
                assertEquals(beforePastePlacements, db.hierarchyPlacementDao().getAll())
                assertEquals(setOf("source"), fx.clipboard.uiState.value.first)
                assertEquals(ContextClipboardOperationUi.CUT, fx.clipboard.uiState.value.second)
                assertEquals(source, fx.placements.getPrimaryAppearance(target("source"))?.id)
            } finally {
                db.close()
            }
        }

    @Test
    fun `ViewModel V2 target-only bulk CUT refuses to infer occurrences and preserves selection`() =
        runTest(dispatcher) {
            val db = database()
            try {
                val fx = fixture(db)
                seed(db, "first")
                seed(db, "second")
                val first = fx.placements.createPrimaryAppearance(target("first"), now = 10L)
                val second = fx.placements.createPrimaryAppearance(target("second"), now = 11L)
                val beforeWorkspaces = db.workspaceDao().getAll()
                val beforePlacements = db.hierarchyPlacementDao().getAll()
                fx.selection.start("first")
                fx.selection.start("second")

                val toast = async { fx.viewModel.uiEventFlow.first() }
                fx.viewModel.onEvent(ContextHierarchyScreenEvent.CutSelectedContexts)
                advanceUntilIdle()

                val message = (toast.await() as ProjectUiEvent.ShowToast).message
                assertTrue(message.contains("точних occurrences"))
                assertFalse(message.contains("Проєкт вирізано"))
                assertEquals(setOf("first", "second"), fx.selection.selectedIds.value)
                assertFalse(fx.clipboard.hasPayload())
                assertEquals(beforeWorkspaces, db.workspaceDao().getAll())
                assertEquals(beforePlacements, db.hierarchyPlacementDao().getAll())
                assertEquals(first, fx.placements.getPrimaryAppearance(target("first"))?.id)
                assertEquals(second, fx.placements.getPrimaryAppearance(target("second"))?.id)
            } finally {
                db.close()
            }
        }
}
