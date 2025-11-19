package com.romankozak.forwardappmobile.features.projectscreen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.di.LocalAppComponent
import com.romankozak.forwardappmobile.features.projectscreen.components.topbar.AdaptiveTopBar
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.ModernInputPanel
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.InputMode
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.projectscreen.components.list.BacklogView
import com.romankozak.forwardappmobile.features.projectscreen.views.InboxView


@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    projectId: String?,
) {
    val appComponent = LocalAppComponent.current
    val viewModel: ProjectScreenViewModel = viewModel(
        factory = appComponent.viewModelFactory
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var inputValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var showInputPanelMenu by rememberSaveable { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val selectedProject = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Project>("selected_project")
        ?.observeAsState()

    val holdMenuController = rememberHoldMenu2()

    LaunchedEffect(selectedProject) {
        selectedProject?.value?.let { project: Project ->
            viewModel.onEvent(ProjectScreenViewModel.Event.LinkExistingProject(project))
            navController.currentBackStackEntry?.savedStateHandle?.remove<Project>("selected_project")
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.navigationBarsPadding().imePadding(),
            topBar = {
                AdaptiveTopBar(
                    isSelectionModeActive = state.isSelectionModeActive,
                    project = state.projectName?.let {
                        Project(
                            id = projectId ?: "",
                            name = it,
                            description = null,
                            parentId = null,
                            createdAt = 0,
                            updatedAt = 0,
                            isCompleted = false,
                            isExpanded = false,
                            goalOrder = 0,
                            projectStatus = null,
                            projectStatusText = null,
                            isProjectManagementEnabled = false,
                            showCheckboxes = false,
                            tags = null
                        )
                    },
                    selectedCount = state.selectedItemIds.size,
                    areAllSelected = false, // TODO: Implement
                    onClearSelection = { /* TODO */ },
                    onSelectAll = { /* TODO */ },
                    onDelete = { /* TODO */ },
                    onMarkAsComplete = { /* TODO */ },
                    onMarkAsIncomplete = { /* TODO */ },

                    onInboxClick = { Toast.makeText(context, "Inbox (не реалізовано)", Toast.LENGTH_SHORT).show() },
                    currentViewMode = state.currentView
                )
            },
            bottomBar = {
                ModernInputPanel(
                    holdMenuController = holdMenuController,
                    inputValue = inputValue,
                    onValueChange = { inputValue = it },
                    inputMode = state.inputMode,
                    onInputModeSelected = { viewModel.onEvent(ProjectScreenViewModel.Event.SwitchInputMode(it)) },
                    onSubmit = {
                        if (inputValue.text.isNotBlank()) {
                            when (state.inputMode) {
                                InputMode.AddNestedProject -> viewModel.onEvent(ProjectScreenViewModel.Event.AddNestedProject(inputValue.text))
                                InputMode.AddGoal -> viewModel.onEvent(ProjectScreenViewModel.Event.AddBacklogGoal(inputValue.text))
                                else -> viewModel.onEvent(ProjectScreenViewModel.Event.AddInboxRecord(inputValue.text))
                            }
                            inputValue = TextFieldValue("")
                        }
                    },
                    onRecentsClick = { /* TODO */ },
                    onLinkExistingProjectClick = { navController.navigate("project_chooser") },
                    onShowAddWebLinkDialog = { /* TODO */ },
                    onShowAddObsidianLinkDialog = { /* TODO */ },
                    onAddListShortcutClick = { viewModel.onEvent(ProjectScreenViewModel.Event.SwitchInputMode(InputMode.AddNestedProject)) },
                    canGoBack = false, // TODO
                    canGoForward = false, // TODO
                    onBackClick = { /* TODO */ },
                    onForwardClick = { /* TODO */ },
                    onHomeClick = { /* TODO */ },
                    onEditList = { /* TODO */ },
                    onShareList = { /* TODO */ },
                    onDeleteList = { /* TODO */ },
                    onSetReminder = { /* TODO */ },
                    menuExpanded = showInputPanelMenu,
                    onMenuExpandedChange = { showInputPanelMenu = it },
                    currentView = state.currentView,
                    onViewChange = { viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(it)) },
                    onImportFromMarkdown = { /* TODO */ },
                    onExportToMarkdown = { /* TODO */ },
                    onImportBacklogFromMarkdown = { /* TODO */ },
                    onExportBacklogToMarkdown = { /* TODO */ },
                    onExportProjectState = { /* TODO */ },
                    reminderParseResult = null, // TODO
                    onClearReminder = { /* TODO */ },
                    isNerActive = false, // TODO
                    onStartTrackingCurrentProject = { /* TODO */ },
                    isProjectManagementEnabled = false, // TODO
                    onToggleProjectManagement = { /* TODO */ },
                    onAddProjectToDayPlan = { /* TODO */ },
                    onRevealInExplorer = { /* TODO */ },
                    onCloseSearch = { /* TODO */ },
                    onAddMilestone = { /* TODO */ },
                    onShowCreateNoteDocumentDialog = { /* TODO */ },
                    onCreateChecklist = { /* TODO */ },
                    onShowDisplayPropertiesClick = { /* TODO */ },
                    suggestions = emptyList(), // TODO
                    onSuggestionClick = { /* TODO */ }
                )
            }
        ) { paddingValues ->
            when (state.currentView) {
                ProjectViewMode.Backlog -> BacklogView(
                    listContent = state.backlogItems,
                    viewModel = viewModel,
                    state = state,
                    onRemindersClick = { /* TODO */ },
                    modifier = Modifier
                        .padding(paddingValues),
                    listState = listState,
                    onMove = { from, to -> viewModel.onEvent(ProjectScreenViewModel.Event.MoveItem(from, to)) },
                    onDragEnd = { from, to -> viewModel.onEvent(ProjectScreenViewModel.Event.DragEnd(from, to)) },
                    onCopyContent = { /* TODO */ },
                )
                ProjectViewMode.Inbox -> {
                    InboxView(
                        modifier = Modifier.padding(paddingValues),
                        viewModel = viewModel,
                        inboxRecords = state.inboxItems,
                        listState = rememberLazyListState(),
                        highlightedRecordId = null,
                        navController = navController,
                    )
                }
                ProjectViewMode.Advanced -> Text(
                    text = "Advanced Content for ID: $projectId",
                    modifier = Modifier.padding(paddingValues).fillMaxWidth().padding(16.dp)
                )
                ProjectViewMode.Attachments -> Text(
                    text = "Attachments Content for ID: $projectId",
                    modifier = Modifier.padding(paddingValues).fillMaxWidth().padding(16.dp)
                )
            }
        }
        com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Overlay(
            controller = holdMenuController,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(999f)
        )
    }
}