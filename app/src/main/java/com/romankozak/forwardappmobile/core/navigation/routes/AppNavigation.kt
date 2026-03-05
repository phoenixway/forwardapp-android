package com.romankozak.forwardappmobile.core.navigation.routes

import android.util.Log
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItemType
import com.romankozak.forwardappmobile.core.navigation.AppNavigationViewModel
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.core.navigation.NavigationCommand
import com.romankozak.forwardappmobile.core.navigation.ui.PlaceholderScreen
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerScreen
import com.romankozak.forwardappmobile.features.ai.insights.AiInsightsScreen
import com.romankozak.forwardappmobile.features.attachments.specific_types.checklist.ChecklistScreen
import com.romankozak.forwardappmobile.features.attachments.specific_types.notedocument.NoteDocumentEditorScreen
import com.romankozak.forwardappmobile.features.attachments.specific_types.notedocument.NoteDocumentScreen
import com.romankozak.forwardappmobile.features.attachments.specific_types.musicnote.MusicNoteScreen
import com.romankozak.forwardappmobile.features.attachments.specific_types.script.ScriptChooserScreen
import com.romankozak.forwardappmobile.features.attachments.specific_types.script.ScriptEditorScreen
import com.romankozak.forwardappmobile.features.attachments.specific_types.script.ScriptsLibraryScreen
import com.romankozak.forwardappmobile.features.attachments.ui.library.AttachmentsLibraryScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_chooser.FilterableListChooserScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_chooser.FilterableListChooserViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_configuration.ProjectStructureScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_configuration.StructurePresetEditorScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_configuration.StructurePresetsScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ProjectHierarchyScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_properties.ProjectSettingsScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ProjectsScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.goalproperties.GoalSettingsScreen
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.inbox.InboxEditorScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.EditTaskScreen
import com.romankozak.forwardappmobile.features.dev_task.KanbanScreen
import com.romankozak.forwardappmobile.features.globalsearch.GlobalSearchScreen
import com.romankozak.forwardappmobile.features.globalsearch.GlobalSearchViewModel
import com.romankozak.forwardappmobile.features.lifestate.LifeStateScreen
import com.romankozak.forwardappmobile.features.mainscreen.CharacterScreen
import com.romankozak.forwardappmobile.features.mainscreen.CommandDeckEvent
import com.romankozak.forwardappmobile.features.mainscreen.CommandDeckViewModel
import com.romankozak.forwardappmobile.features.mainscreen.MainScreenLayout
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalManagementScreen
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.features.reminders.list.RemindersScreen
import com.romankozak.forwardappmobile.features.settings.ManageContextsScreen
import com.romankozak.forwardappmobile.features.settings.settings.SettingsScreen
import com.romankozak.forwardappmobile.features.sync.SyncScreen
import com.romankozak.forwardappmobile.features.sync.selectiveimport.SelectiveImportScreen
import com.romankozak.forwardappmobile.features.vet_case.VetCaseHistoryScreen
import com.romankozak.forwardappmobile.features.vet_case.VetCaseSummaryScreen
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder

const val MAIN_GRAPH_ROUTE = NavigationRoutes.MAIN_GRAPH
const val COMMAND_DECK_ROUTE = NavigationRoutes.COMMAND_DECK
const val CHARACTER_SCREEN_ROUTE = NavigationRoutes.CHARACTER
const val GOAL_LISTS_ROUTE = NavigationRoutes.GOAL_LISTS
const val AI_INSIGHTS_ROUTE = NavigationRoutes.AI_INSIGHTS
const val LIFE_STATE_ROUTE = NavigationRoutes.LIFE_STATE
const val SELECTIVE_IMPORT_ROUTE = NavigationRoutes.SELECTIVE_IMPORT_PATTERN
const val PLACEHOLDER_ROUTE = NavigationRoutes.PLACEHOLDER_PATTERN
const val KANBAN_ROUTE = NavigationRoutes.KANBAN
const val VET_CASE_SUMMARY_ROUTE = NavigationRoutes.VET_CASE_SUMMARY
const val VET_CASE_HISTORY_ROUTE = NavigationRoutes.VET_CASE_HISTORY

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(syncDataViewModel: SyncDataViewModel) {
    val navController = rememberNavController()
    val appNavigationViewModel: AppNavigationViewModel = hiltViewModel()

    val navigationManager = appNavigationViewModel.navigationManager

    // Прив'язка NavHostController до NavigationDispatcher
    LaunchedEffect(navController) {
        appNavigationViewModel.attachNavController(navController)
    }

    LaunchedEffect(navigationManager, navController) {
        navigationManager.navigationCommandFlow.collect { command ->
            when (command) {
                is NavigationCommand.Navigate -> {
                    val options = command.builder
                    navController.navigate(command.route, options ?: {})
                }

                is NavigationCommand.NavigateTarget -> {
                    val route = NavTargetRouter.routeOf(command.target)
                    val options = command.builder
                    navController.navigate(route, options ?: {})
                }

                is NavigationCommand.PopBack -> {
                    if (command.key != null && command.value != null) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(command.key, command.value)
                    }
                    navController.popBackStack()
                }
            }
        }
    }

    SharedTransitionLayout {
        NavHost(navController = navController, startDestination = MAIN_GRAPH_ROUTE) {
            navigation(startDestination = COMMAND_DECK_ROUTE, route = MAIN_GRAPH_ROUTE) {
                mainGraph(
                    navController,
                    syncDataViewModel,
                    appNavigationViewModel,
                    this@SharedTransitionLayout,
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    syncDataViewModel: SyncDataViewModel,
    appNavigationViewModel: AppNavigationViewModel,
    sharedTransitionScope: SharedTransitionScope,
) {
    composable(COMMAND_DECK_ROUTE) { backStackEntry ->
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) }
        val goalListViewModel: ContextHierarchyScreenViewModel = hiltViewModel(parentEntry)
        val commandDeckViewModel: CommandDeckViewModel = hiltViewModel(parentEntry)
        val navigationManager = appNavigationViewModel.navigationManager
        val scope = rememberCoroutineScope()

        MainScreenLayout(
            navController = navController,
            navigationManager = navigationManager,
            onNavigateToProjectHierarchy = {
                navigationManager.navigate(
                    target = NavTarget.ContextHierarchy,
                    recordInHistory = true,
                )
            },
            onNavigateToPresets = {
                navigationManager.navigate(target = NavTarget.StructurePresets)
            },
            onNavigateToCharacter = { navigationManager.navigate(route = CHARACTER_SCREEN_ROUTE) },
            onNavigateToGlobalSearch = { navigationManager.navigate(target = NavTarget.GlobalSearchHome) },
            onNavigateToSettings = { navigationManager.navigate(target = NavTarget.Settings) },
            onNavigateToInbox = {
                scope.launch {
                    val inboxId = goalListViewModel.getInboxProjectId()
                    if (inboxId != null) {
                        navigationManager.navigate(
                            target =
                                NavTarget.ContextDetail(
                                    contextId = inboxId,
                                    initialViewMode = "INBOX",
                                ),
                            recordInHistory = true,
                            historyTitle = "Inbox",
                        )
                    }
                }
            },
            onNavigateToTracker = { navigationManager.navigate(target = NavTarget.Tracker) },
            onNavigateToReminders = { navigationManager.navigate(target = NavTarget.Reminders) },
            onNavigateToAiChat = { navigationManager.navigate(target = NavTarget.Chat) },
            onNavigateToAiInsights = { navigationManager.navigate(target = NavTarget.AiInsights) },
            onNavigateToAiLifeManagement = { navigationManager.navigate(target = NavTarget.LifeState) },
            onExportToFile = { commandDeckViewModel.onEvent(CommandDeckEvent.ExportToFile) },
            onImportFromFileRequest = { uri ->
                commandDeckViewModel.onEvent(
                    CommandDeckEvent.ImportFromFileRequest(uri.toString()),
                )
            },
            onSelectiveImportFromFileRequest = { uri ->
                navigationManager.navigate(route = NavigationRoutes.selectiveImport(uri.toString()))
            },
            onExportAttachments = { commandDeckViewModel.onEvent(CommandDeckEvent.ExportAttachments) },
            onImportAttachmentsFromFileRequest = { uri ->
                commandDeckViewModel.onEvent(CommandDeckEvent.ImportAttachmentsFromFile(uri.toString()))
            },
            onWifiPush = { host -> commandDeckViewModel.onEvent(CommandDeckEvent.WifiPush(host)) },
            onShowWifiServer = { commandDeckViewModel.onShowWifiServerDialog() },
            onShowWifiImport = { commandDeckViewModel.onShowWifiImportDialog() },
            onNavigateToSyncScreenWithData = { json ->
                syncDataViewModel.jsonString = json
                navigationManager.navigate(target = NavTarget.Sync)
            },
            onNavigateToAttachments = { navigationManager.navigate(target = NavTarget.AttachmentsLibrary) },
            onNavigateToScripts = { navigationManager.navigate(target = NavTarget.ScriptsLibrary) },
            onNavigateToRecentItem = { item: RecentItem ->
                when (item.type) {
                    RecentItemType.PROJECT ->
                        navigationManager.navigate(
                            target = NavTarget.ContextDetail(contextId = item.target),
                            recordInHistory = true,
                            historyTitle = "Context",
                        )

                    RecentItemType.NOTE,
                    RecentItemType.NOTE_DOCUMENT,
                    ->
                        navigationManager.navigate(target = NavTarget.NoteDocument(id = item.target))

                    RecentItemType.CHECKLIST ->
                        navigationManager.navigate(target = NavTarget.Checklist(id = item.target))

                    RecentItemType.MUSIC_NOTE ->
                        navigationManager.navigate(target = NavTarget.MusicNote(id = item.target))

                    RecentItemType.OBSIDIAN_LINK -> {
                        // Поки що просто лог або нічого
                        Log.d("RecentItemNav", "Obsidian link clicked: ${item.target}")
                    }
                }
            },
            recentViewModel = hiltViewModel<RecentViewModel>(),
            commandDeckViewModel = commandDeckViewModel,
            contextHierarchyViewModel = goalListViewModel,
        )
    }

    composable(CHARACTER_SCREEN_ROUTE) {
        CharacterScreen()
    }

    composable(
        route = "${NavigationRoutes.PROJECT_STRUCTURE}/{projectId}",
        arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
    ) {
        ProjectStructureScreen(navController = navController)
    }

    composable(NavigationRoutes.STRUCTURE_PRESETS) {
        StructurePresetsScreen(
            navController = navController,
            navigationManager = appNavigationViewModel.navigationManager,
        )
    }

    composable(
        route = NavigationRoutes.STRUCTURE_PRESET_EDITOR_PATTERN,
        arguments =
            listOf(
                navArgument("presetId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("copyFromPresetId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) {
        StructurePresetEditorScreen(navController = navController)
    }

    composable(GOAL_LISTS_ROUTE) { backStackEntry ->
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) }
        val viewModel: ContextHierarchyScreenViewModel = hiltViewModel(parentEntry)

        viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager

        ProjectHierarchyScreen(
            navController = navController,
            syncDataViewModel = syncDataViewModel,
            viewModel = viewModel,
            navigationManager = appNavigationViewModel.navigationManager,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
        )
    }

    composable(
        route = NavigationRoutes.CONTEXT_DETAIL_PATTERN,
        arguments =
            listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("goalId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("itemIdToHighlight") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("inboxRecordIdToHighlight") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("initialViewMode") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("originContextId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) { backStackEntry -> // Add backStackEntry here
        val viewModel: ContextScreenViewModel = hiltViewModel()
        viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager

        // FIX: Extract the 'listId' argument from the route and assign it to a variable.
        val projectId = backStackEntry.arguments?.getString("listId")

        ProjectsScreen(
            navController = navController,
            viewModel = viewModel,
            projectId = projectId, // Now 'projectId' is a resolved reference.
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = this,
        )
    }

    composable(
        "${NavigationRoutes.GLOBAL_SEARCH}/{query}",
        arguments = listOf(navArgument("query") { type = NavType.StringType }),
    ) {
        val viewModel: GlobalSearchViewModel = hiltViewModel()
        viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager

        GlobalSearchScreen(viewModel = viewModel, navController = navController)
    }

    composable(NavigationRoutes.GLOBAL_SEARCH_HOME) {
        val viewModel: GlobalSearchViewModel = hiltViewModel()
        viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager

        GlobalSearchScreen(viewModel = viewModel, navController = navController)
    }

    composable(NavigationRoutes.ATTACHMENTS_LIBRARY) {
        AttachmentsLibraryScreen(
            navController = navController,
            navigationManager = appNavigationViewModel.navigationManager,
        )
    }

    composable(NavigationRoutes.SCRIPTS_LIBRARY) {
        ScriptsLibraryScreen(
            navController = navController,
            navigationManager = appNavigationViewModel.navigationManager,
        )
    }

    composable(NavigationRoutes.SCRIPT_CHOOSER) { ScriptChooserScreen(navController = navController) }

    composable(LIFE_STATE_ROUTE) { LifeStateScreen(navController = navController) }

    composable(NavigationRoutes.SETTINGS) { backStackEntry ->
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) }
        val goalListViewModel: ContextHierarchyScreenViewModel = hiltViewModel(parentEntry)

        val uiState by goalListViewModel.uiState.collectAsStateWithLifecycle()
        val reservedContextCount = uiState.allContexts.count { it.isReserved }

        SettingsScreen(
            planningSettings = uiState.planningSettings,
            initialVaultName = uiState.obsidianVaultName,
            reservedContextCount = reservedContextCount,
            onManageContextsClick = {
                appNavigationViewModel.navigationManager.navigate(target = NavTarget.ManageContexts)
            },
            onBack = { navController.popBackStack() },
            onSave = { settings ->
                goalListViewModel.onEvent(ContextHierarchyScreenEvent.SaveSettings(settings))
            },
        )
    }

    composable(NavigationRoutes.MANAGE_CONTEXTS) { backStackEntry ->
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) }
        val goalListViewModel: ContextHierarchyScreenViewModel = hiltViewModel(parentEntry)

        val uiState by goalListViewModel.uiState.collectAsStateWithLifecycle()

        ManageContextsScreen(
            initialContexts = uiState.allContexts,
            onBack = { navController.popBackStack() },
            onSave = { updatedContexts ->
                goalListViewModel.onEvent(ContextHierarchyScreenEvent.SaveAllContexts(updatedContexts))
                navController.popBackStack()
            },
        )
    }

    composable(NavigationRoutes.ACTIVITY_TRACKER) { ActivityTrackerScreen(navController = navController) }

    composable(
        route = NavigationRoutes.PROJECT_SETTINGS_PATTERN,
        arguments =
            listOf(
                navArgument("goalId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ProjectSettingsScreen(
            navController = navController,
            navigationManager = appNavigationViewModel.navigationManager,
            viewModel = hiltViewModel(),
        )
    }

    composable(
        route = "${NavigationRoutes.GOAL_SETTINGS}/{goalId}",
        arguments = listOf(navArgument("goalId") { type = NavType.StringType }),
    ) {
        GoalSettingsScreen(
            navController = navController,
            navigationManager = appNavigationViewModel.navigationManager,
            viewModel = hiltViewModel(),
        )
    }

    composable(NavigationRoutes.SYNC) {
        SyncScreen(
            syncDataViewModel = syncDataViewModel,
            onSyncComplete = { navController.popBackStack() },
        )
    }

    // Об'єднаний екран для перегляду/редагування існуючого списку
    composable(
        route = "${NavigationRoutes.NOTE_DOCUMENT}/{documentId}?startEdit={startEdit}",
        arguments =
            listOf(
                navArgument("documentId") { type = NavType.StringType },
                navArgument("startEdit") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) { backStackEntry ->
        val startEdit = backStackEntry.arguments?.getBoolean("startEdit") ?: false
        NoteDocumentEditorScreen(
            navController = navController,
            navigationManager = appNavigationViewModel.navigationManager,
            startEdit = startEdit,
        )
    }

    composable(
        route = "${NavigationRoutes.NOTE_DOCUMENT_CREATE}/{projectId}",
        arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
    ) {
        NoteDocumentScreen(
            navController = navController,
            navigationManager = appNavigationViewModel.navigationManager,
        )
    }

    composable(
        route = NavigationRoutes.NOTE_DOCUMENT_EDIT_PATTERN,
        arguments =
            listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("documentId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        NoteDocumentScreen(
            navController = navController,
            navigationManager = appNavigationViewModel.navigationManager,
        )
    }

    composable(
        route = NavigationRoutes.SCRIPT_EDITOR_PATTERN,
        arguments =
            listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("scriptId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) {
        ScriptEditorScreen(navController = navController)
    }

    composable(
        route = NavigationRoutes.CHECKLIST_PATTERN,
        arguments =
            listOf(
                navArgument("projectId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("checklistId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ChecklistScreen(
            navController = navController,
            navigationManager = appNavigationViewModel.navigationManager,
        )
    }

    composable(
        route = NavigationRoutes.MUSIC_NOTE_PATTERN,
        arguments =
            listOf(
                navArgument("musicNoteId") { type = NavType.StringType },
                navArgument("startEdit") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) { backStackEntry ->
        val startEdit = backStackEntry.arguments?.getBoolean("startEdit") ?: false
        MusicNoteScreen(navController = navController, startEdit = startEdit)
    }

    composable(
        route =
            "list_chooser_screen/{title}?currentParentId={currentParentId}&disabledIds={disabledIds}",
        arguments =
            listOf(
                navArgument("title") { type = NavType.StringType },
                navArgument("currentParentId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("disabledIds") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) { backStackEntry ->
        val viewModel: FilterableListChooserViewModel = hiltViewModel()
        val TAG = "MOVE_DEBUG"

        val title =
            backStackEntry.arguments?.getString("title")?.let { URLDecoder.decode(it, "UTF-8") }
                ?: "Select a list"

        val disabledIds =
            backStackEntry.arguments?.getString("disabledIds")?.split(",")?.toSet() ?: emptySet()
        val currentParentIdArg = backStackEntry.arguments?.getString("currentParentId")
        val currentParentId = if (currentParentIdArg == "root") null else currentParentIdArg

        Log.d(TAG, "[Nav] list_chooser_screen launched.")

        val chooserUiState by viewModel.chooserState.collectAsStateWithLifecycle()
        val filterText by viewModel.filterText.collectAsStateWithLifecycle()
        val expandedIds by viewModel.expandedIds.collectAsStateWithLifecycle()
        val showDescendants by viewModel.showDescendants.collectAsStateWithLifecycle()

        FilterableListChooserScreen(
            title = title,
            filterText = filterText,
            onFilterTextChanged = viewModel::updateFilterText,
            chooserUiState = chooserUiState,
            expandedIds = expandedIds,
            onToggleExpanded = viewModel::toggleExpanded,
            onNavigateBack = { navController.popBackStack() },
            onConfirm = { selectedId ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("list_chooser_result", selectedId ?: "root")

                navController.popBackStack()
            },
            currentParentId = currentParentId,
            disabledIds = disabledIds,
            onAddNewList = viewModel::addNewProject,
            showDescendants = showDescendants,
            onToggleShowDescendants = viewModel::toggleShowDescendants,
        )
    }
    chatScreen(navController, appNavigationViewModel.navigationManager)
    dayManagementGraph(navController, appNavigationViewModel.navigationManager)
    dayManagementScreen(navController, appNavigationViewModel.navigationManager)
    strategicManagementScreen(navController, appNavigationViewModel.navigationManager)

    composable(NavigationRoutes.TACTICAL_MANAGEMENT) { TacticalManagementScreen() }


    composable(AI_INSIGHTS_ROUTE) { AiInsightsScreen(navController = navController) }

    composable(NavigationRoutes.REMINDERS) {
        RemindersScreen(
            navController = navController,
            navigationManager = appNavigationViewModel.navigationManager,
        )
    }

    composable(
        route = "${NavigationRoutes.EDIT_TASK}/{taskId}",
        arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
    ) {
        EditTaskScreen(onNavigateUp = { navController.navigateUp() })
    }

    composable(
        route = "${NavigationRoutes.INBOX_EDITOR}/{inboxId}",
        arguments = listOf(navArgument("inboxId") { type = NavType.StringType }),
    ) {
        InboxEditorScreen(navController = navController)
    }

    composable(
        route = SELECTIVE_IMPORT_ROUTE,
        arguments =
            listOf(
                navArgument("fileUri") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) { backStackEntry ->
        val fileUri =
            backStackEntry.arguments?.getString("fileUri")?.let { URLDecoder.decode(it, "UTF-8") }
        if (fileUri != null) {
            SelectiveImportScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        } else {
            // Handle error: URI is missing. Maybe pop back or show an error message.
            navController.popBackStack()
        }
    }

    composable(KANBAN_ROUTE) { KanbanScreen() }
    composable(VET_CASE_SUMMARY_ROUTE) { VetCaseSummaryScreen() }
    composable(VET_CASE_HISTORY_ROUTE) { VetCaseHistoryScreen() }

    composable(
        route = NavigationRoutes.PLACEHOLDER_PATTERN,
        arguments =
            listOf(
                navArgument("viewId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("screenId") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) { backStackEntry ->
        val viewId = backStackEntry.arguments?.getString("viewId")
        val screenId = backStackEntry.arguments?.getString("screenId")
        PlaceholderScreen(viewId = viewId, screenId = screenId)
    }
}

fun mapTargetToRoute(target: NavTarget): String = NavTargetRouter.routeOf(target)
