package com.romankozak.forwardappmobile.features.projectscreen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.ModernInputPanel
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.InputMode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.projectscreen.components.list.BacklogView
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItemContent
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
                inputValue = inputValue,
                onValueChange = { inputValue = it },
                inputMode = state.inputMode,
                onInputModeSelected = { viewModel.onEvent(ProjectScreenViewModel.Event.SwitchInputMode(it)) },
                onSubmit = {
                    if (inputValue.text.isNotBlank()) {
                        when (state.currentView) {
                            ProjectViewMode.Backlog -> viewModel.onEvent(ProjectScreenViewModel.Event.AddBacklogGoal(inputValue.text))
                            else -> viewModel.onEvent(ProjectScreenViewModel.Event.AddInboxRecord(inputValue.text))
                        }
                        inputValue = TextFieldValue("")
                    }
                },
                onRecentsClick = { /* TODO */ },
                onAddListLinkClick = { /* TODO */ },
                onShowAddWebLinkDialog = { /* TODO */ },
                onShowAddObsidianLinkDialog = { /* TODO */ },
                onAddListShortcutClick = { /* TODO */ },
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
                    .padding(paddingValues)
                    .glitch(trigger = uiState.currentView),
                listState = listState,
                onMove = { _, _ -> /* TODO */ },
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