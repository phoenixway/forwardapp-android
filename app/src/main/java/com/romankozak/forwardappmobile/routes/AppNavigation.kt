package com.romankozak.forwardappmobile.routes

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
import com.romankozak.forwardappmobile.data.database.models.RecentItem
import com.romankozak.forwardappmobile.data.database.models.RecentItemType
import com.romankozak.forwardappmobile.features.attachments.ui.library.AttachmentsLibraryScreen
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalManagementScreen
import com.romankozak.forwardappmobile.ui.navigation.AppNavigationViewModel
import com.romankozak.forwardappmobile.ui.navigation.NavigationCommand
import com.romankozak.forwardappmobile.ui.recent.RecentViewModel
import com.romankozak.forwardappmobile.ui.reminders.list.RemindersScreen
import com.romankozak.forwardappmobile.ui.screens.ManageContextsScreen
import com.romankozak.forwardappmobile.ui.screens.activitytracker.ActivityTrackerScreen
import com.romankozak.forwardappmobile.ui.screens.commanddeck.SharedCommandDeckLayout
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.EditTaskScreen
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchScreen
import com.romankozak.forwardappmobile.ui.screens.globalsearch.GlobalSearchViewModel
import com.romankozak.forwardappmobile.ui.screens.inbox.InboxEditorScreen
import com.romankozak.forwardappmobile.ui.screens.insights.AiInsightsScreen
import com.romankozak.forwardappmobile.ui.screens.lifestate.LifeStateScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserScreen
import com.romankozak.forwardappmobile.ui.screens.listchooser.FilterableListChooserViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.ProjectHierarchyScreen
import com.romankozak.forwardappmobile.ui.screens.mainscreen.ProjectHierarchyScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectHierarchyScreenEvent
import com.romankozak.forwardappmobile.ui.screens.notedocument.NoteDocumentEditorScreen
import com.romankozak.forwardappmobile.ui.screens.notedocument.NoteDocumentScreen
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.ProjectsScreen
import com.romankozak.forwardappmobile.ui.screens.projectsettings.ProjectSettingsScreen
import com.romankozak.forwardappmobile.ui.screens.script.ScriptChooserScreen
import com.romankozak.forwardappmobile.ui.screens.script.ScriptEditorScreen
import com.romankozak.forwardappmobile.ui.screens.script.ScriptsLibraryScreen
import com.romankozak.forwardappmobile.ui.screens.settings.SettingsScreen
import com.romankozak.forwardappmobile.ui.screens.sync.SyncScreen
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import java.net.URLDecoder
import kotlinx.coroutines.launch
import com.romankozak.forwardappmobile.ui.navigation.NavTarget


const val MAIN_GRAPH_ROUTE = "main_graph"
const val COMMAND_DECK_ROUTE = "command_deck_screen"
const val GOAL_LISTS_ROUTE = "goal_lists_screen"
const val AI_INSIGHTS_ROUTE = "ai_insights_screen"
const val LIFE_STATE_ROUTE = "life_state_screen"
const val SELECTIVE_IMPORT_ROUTE = "selective_import_screen/{fileUri}"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(syncDataViewModel: SyncDataViewModel) {
  val navController = rememberNavController()
  val appNavigationViewModel: AppNavigationViewModel = hiltViewModel()

  val navigationManager = appNavigationViewModel.navigationManager

  LaunchedEffect(navigationManager, navController) {
    navigationManager.navigationCommandFlow.collect { command ->
      when (command) {
        is NavigationCommand.Navigate -> {
          val options = command.builder
          navController.navigate(command.route, options ?: {})
        }

        is NavigationCommand.NavigateTarget -> {
          val route = mapTargetToRoute(command.target)
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
    val viewModel: ProjectHierarchyScreenViewModel = hiltViewModel(parentEntry)
    val scope = rememberCoroutineScope()

    SharedCommandDeckLayout(
      navController = navController,
      onNavigateToProjectHierarchy = { navController.navigate(GOAL_LISTS_ROUTE) },
      onNavigateToGlobalSearch = { navController.navigate("global_search") },
      onNavigateToSettings = { navController.navigate("settings_screen") },
      onNavigateToInbox = {
        scope.launch {
          val inboxId = viewModel.getInboxProjectId()
          if (inboxId != null) {
            navController.navigate("goal_detail_screen/$inboxId?initialViewMode=INBOX")
          }
        }
      },
      onNavigateToTracker = { navController.navigate("activity_tracker_screen") },
      onNavigateToReminders = { navController.navigate("reminders_screen") },
      onNavigateToAiChat = { navController.navigate(AI_INSIGHTS_ROUTE) },
      onNavigateToAiLifeManagement = { navController.navigate(LIFE_STATE_ROUTE) },
      onNavigateToImportExport = {
        navController.navigate(SELECTIVE_IMPORT_ROUTE.replace("/{fileUri}", ""))
      },
      onNavigateToAttachments = { navController.navigate("attachments_library_screen") },
      onNavigateToScripts = { navController.navigate("scripts_library_screen") },
      onNavigateToRecentItem = { item: RecentItem ->
        when (item.type) {
          RecentItemType.PROJECT -> navController.navigate("goal_detail_screen/${item.target}")

          RecentItemType.NOTE,
          RecentItemType.NOTE_DOCUMENT ->
            navController.navigate("note_document_screen/${item.target}")

          RecentItemType.CHECKLIST ->
            navController.navigate("checklist_screen?checklistId=${item.target}")

          RecentItemType.OBSIDIAN_LINK -> {
            // Поки що просто лог або нічого
            Log.d("RecentItemNav", "Obsidian link clicked: ${item.target}")
          }
        }
      },
      recentViewModel = hiltViewModel<RecentViewModel>(),
    )
  }

  composable(GOAL_LISTS_ROUTE) { backStackEntry ->
    val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) }
    val viewModel: ProjectHierarchyScreenViewModel = hiltViewModel(parentEntry)

    viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager

    ProjectHierarchyScreen(
      navController = navController,
      syncDataViewModel = syncDataViewModel,
      viewModel = viewModel,
      sharedTransitionScope = sharedTransitionScope,
      animatedVisibilityScope = this,
    )
  }

  composable(
    route =
      "goal_detail_screen/{listId}?goalId={goalId}&itemIdToHighlight={itemIdToHighlight}&inboxRecordIdToHighlight={inboxRecordIdToHighlight}&initialViewMode={initialViewMode}",
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
      ),
  ) { backStackEntry -> // Add backStackEntry here
    val viewModel: BacklogViewModel = hiltViewModel()
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
    "global_search_screen/{query}",
    arguments = listOf(navArgument("query") { type = NavType.StringType }),
  ) {
    val viewModel: GlobalSearchViewModel = hiltViewModel()
    viewModel.enhancedNavigationManager = appNavigationViewModel.navigationManager

    GlobalSearchScreen(viewModel = viewModel, navController = navController)
  }

  composable("attachments_library_screen") {
    AttachmentsLibraryScreen(navController = navController)
  }

  composable("scripts_library_screen") { ScriptsLibraryScreen(navController = navController) }

  composable("script_chooser_screen") { ScriptChooserScreen(navController = navController) }

  composable(LIFE_STATE_ROUTE) { LifeStateScreen(navController = navController) }

  composable("settings_screen") { backStackEntry ->
    val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) }
    val goalListViewModel: ProjectHierarchyScreenViewModel = hiltViewModel(parentEntry)

    val uiState by goalListViewModel.uiState.collectAsStateWithLifecycle()
    val reservedContextCount = uiState.allContexts.count { it.isReserved }

    SettingsScreen(
      planningSettings = uiState.planningSettings,
      initialVaultName = uiState.obsidianVaultName,
      reservedContextCount = reservedContextCount,
      onManageContextsClick = { navController.navigate("manage_contexts_screen") },
      onBack = { navController.popBackStack() },
      onSave = { settings ->
        goalListViewModel.onEvent(ProjectHierarchyScreenEvent.SaveSettings(settings))
      },
    )
  }

  composable("manage_contexts_screen") { backStackEntry ->
    val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) }
    val goalListViewModel: ProjectHierarchyScreenViewModel = hiltViewModel(parentEntry)

    val uiState by goalListViewModel.uiState.collectAsStateWithLifecycle()

    ManageContextsScreen(
      initialContexts = uiState.allContexts,
      onBack = { navController.popBackStack() },
      onSave = { updatedContexts ->
        goalListViewModel.onEvent(ProjectHierarchyScreenEvent.SaveAllContexts(updatedContexts))
        navController.popBackStack()
      },
    )
  }

  composable("activity_tracker_screen") { ActivityTrackerScreen(navController = navController) }

  composable(
    route = "project_settings_screen?goalId={goalId}&projectId={projectId}",
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
    ProjectSettingsScreen(navController = navController, viewModel = hiltViewModel())
  }

  composable(
    route = "goal_settings_screen/{goalId}",
    arguments = listOf(navArgument("goalId") { type = NavType.StringType }),
  ) {
    com.romankozak.forwardappmobile.ui.screens.goalsettings.GoalSettingsScreen(
      navController = navController,
      viewModel = hiltViewModel(),
    )
  }

  composable("sync_screen") {
    SyncScreen(
      syncDataViewModel = syncDataViewModel,
      onSyncComplete = { navController.popBackStack() },
    )
  }

  // Об'єднаний екран для перегляду/редагування існуючого списку
  composable(
    route = "note_document_screen/{documentId}?startEdit={startEdit}",
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
    NoteDocumentEditorScreen(navController = navController, startEdit = startEdit)
  }

  composable(
    route = "note_document_create_screen/{projectId}",
    arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
  ) {
    NoteDocumentScreen(navController = navController)
  }

  composable(
    route = "note_document_edit_screen?projectId={projectId}&documentId={documentId}",
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
    NoteDocumentScreen(navController = navController)
  }

  composable(
    route = "script_editor_screen?projectId={projectId}&scriptId={scriptId}",
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
    route = "checklist_screen?projectId={projectId}&checklistId={checklistId}",
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
    com.romankozak.forwardappmobile.ui.screens.checklist.ChecklistScreen(
      navController = navController
    )
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
  chatScreen(navController)
  dayManagementGraph(navController)
  dayManagementScreen(navController)
  strategicManagementScreen(navController)

  composable("tactical_management_screen") { TacticalManagementScreen() }

  composable(AI_INSIGHTS_ROUTE) { AiInsightsScreen(navController = navController) }

  composable("reminders_screen") { RemindersScreen(navController = navController) }

  composable(
    route = "edit_task_screen/{taskId}",
    arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
  ) {
    EditTaskScreen(onNavigateUp = { navController.navigateUp() })
  }

  composable(
    route = "inbox_editor_screen/{inboxId}",
    arguments = listOf(navArgument("inboxId") { type = NavType.StringType }),
  ) {
    InboxEditorScreen(navController = navController)
  }

  composable(
    route = SELECTIVE_IMPORT_ROUTE,
    arguments = listOf(navArgument("fileUri") { type = NavType.StringType }),
  ) { backStackEntry ->
    val fileUri =
      backStackEntry.arguments?.getString("fileUri")?.let { URLDecoder.decode(it, "UTF-8") }
    if (fileUri != null) {
      com.romankozak.forwardappmobile.ui.screens.selectiveimport.SelectiveImportScreen(
        onNavigateBack = { navController.popBackStack() }
      )
    } else {
      // Handle error: URI is missing. Maybe pop back or show an error message.
      navController.popBackStack()
    }
  }
}

fun mapTargetToRoute(target: NavTarget): String =
    when (target) {
        NavTarget.ProjectHierarchy -> "goal_lists_screen"

        is NavTarget.ProjectDetail ->
            if (target.initialViewMode != null)
                "goal_detail_screen/${target.projectId}?initialViewMode=${target.initialViewMode}"
            else
                "goal_detail_screen/${target.projectId}"

        is NavTarget.NoteDocument ->
            "note_document_screen/${target.id}"

        is NavTarget.Checklist ->
            "checklist_screen?checklistId=${target.id}"

        is NavTarget.GlobalSearch ->
            "global_search_screen/${target.query}"

        NavTarget.Settings -> "settings_screen"
        NavTarget.Reminders -> "reminders_screen"
        NavTarget.Tracker -> "activity_tracker_screen"
        NavTarget.AiInsights -> "ai_insights_screen"
        NavTarget.LifeState -> "life_state_screen"
        NavTarget.AttachmentsLibrary -> "attachments_library_screen"
        NavTarget.ScriptsLibrary -> "scripts_library_screen"

        is NavTarget.ImportExport ->
            if (target.uri != null)
                "selective_import_screen/${java.net.URLEncoder.encode(target.uri, "UTF-8")}"
            else
                "selective_import_screen"
    }
