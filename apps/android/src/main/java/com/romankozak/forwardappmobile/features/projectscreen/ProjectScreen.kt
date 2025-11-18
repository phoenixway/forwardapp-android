package com.romankozak.forwardappmobile.features.projectscreen

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.ModernInputPanel
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.InputMode
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.zIndex
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.MinimalInputPanel
import com.romankozak.forwardappmobile.features.projectscreen.components.list.BacklogView
import com.romankozak.forwardappmobile.ui.holdmenu.HoldMenuOverlay
import com.romankozak.forwardappmobile.ui.holdmenu.HoldMenuState
import kotlinx.coroutines.delay


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

    val holdMenuState = remember { mutableStateOf(HoldMenuState()) }


    val selectedProject = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Project>("selected_project")
        ?.observeAsState()


    val onHoldMenuSelect: (Int) -> Unit = { index ->
        when(index) {
            0 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Backlog))
            1 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Advanced))
            2 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Inbox))
            3 -> viewModel.onEvent(ProjectScreenViewModel.Event.SwitchViewMode(ProjectViewMode.Attachments))
        }
    }


    LaunchedEffect(selectedProject) {
        selectedProject?.value?.let { project: Project ->
            viewModel.onEvent(ProjectScreenViewModel.Event.LinkExistingProject(project))
            navController.currentBackStackEntry?.savedStateHandle?.remove<Project>("selected_project")
        }
    }
    Box(Modifier.fillMaxSize()) {

        Scaffold(
            modifier = Modifier.navigationBarsPadding().imePadding()
                .onGloballyPositioned {
                    Log.e("HOLDMENU", "📏 ROOT coords = ${it.positionInWindow()}, size=${it.size}")
                },
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

                    onInboxClick = {
                        Toast.makeText(
                            context,
                            "Inbox (не реалізовано)",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    currentViewMode = state.currentView
                )
            },
            bottomBar = {
                /*  ModernInputPanel(
                inputValue = inputValue,
                inputMode = state.inputMode,
                onValueChange = { inputValue = it },
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
                onInputModeSelected = { viewModel.onEvent(ProjectScreenViewModel.Event.SwitchInputMode(it)) },
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
                isViewModePanelVisible = true, // TODO
                onToggleNavPanelMode = { /* TODO */ },
                onRevealInExplorer = { /* TODO */ },
                onCloseSearch = { /* TODO */ },
                onAddMilestone = { /* TODO */ },
                onShowCreateNoteDocumentDialog = { /* TODO */ },
                onCreateChecklist = { /* TODO */ },
                onShowDisplayPropertiesClick = { /* TODO */ },
                suggestions = emptyList(), // TODO
                onSuggestionClick = { /* TODO */ },
                holdMenuState = holdMenuState
            )*/

                MinimalInputPanel(
                    inputMode = state.inputMode,
                    onInputModeSelected = {
                        viewModel.onEvent(
                            ProjectScreenViewModel.Event.SwitchInputMode(
                                it
                            )
                        )
                    },

                    holdMenuState = holdMenuState,
                    onHoldMenuSelect = onHoldMenuSelect,
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
                        .padding(paddingValues)
                        .glitch(trigger = uiState.currentView),
                    listState = listState,
                    onMove = { from, to ->
                        viewModel.onEvent(
                            ProjectScreenViewModel.Event.MoveItem(
                                from,
                                to
                            )
                        )
                    },
                    onDragEnd = { from, to ->
                        viewModel.onEvent(
                            ProjectScreenViewModel.Event.DragEnd(
                                from,
                                to
                            )
                        )
                    },
                    onCopyContent = { /* TODO */ },
                )

                ProjectViewMode.Inbox -> {
                    LazyColumn(modifier = Modifier.padding(paddingValues)) {
                        items(state.inboxItems) { item ->
                            Text(
                                text = item.text,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                    }
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
        HoldMenuOverlay(
            state = holdMenuState.value,
            onChangeState = { holdMenuState.value = it },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(999f)   // ← ОБОВ’ЯЗКОВО!

        )








        /*HoldMenuOverlay(
        state = holdMenuState.value,
        onDismiss = { holdMenuState.value = holdMenuState.value.copy(isOpen = false) }
    )*/

    }
}


fun Modifier.glitch(trigger: Any): Modifier = composed {
    var glitchAmount by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(key1 = trigger) {
        val glitchDuration = 150L
        val startTime = withFrameNanos { it }

        while (withFrameNanos { it } < startTime + (glitchDuration * 1_000_000)) {
            glitchAmount = (Math.random() * 10 - 5).toFloat()
            delay(40)
        }
        glitchAmount = 0f
    }

    this.graphicsLayer {
        translationX = glitchAmount
        translationY = (Math.random() * glitchAmount - glitchAmount / 2).toFloat()
    }
}