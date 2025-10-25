@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.projectscreen

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Note
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Link
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.ui.common.components.ShareDialog
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen
import com.romankozak.forwardappmobile.ui.common.editor.components.FullScreenTextEditor
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel
import com.romankozak.forwardappmobile.ui.reminders.dialogs.ReminderPropertiesDialog

import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.inputpanel.ModernInputPanel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.topbar.AdaptiveTopBar
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.EditLogEntryDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.GoalDetailDialogs
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.ProjectDisplayPropertiesDialog
import com.romankozak.forwardappmobile.ui.shared.InProgressIndicator
import kotlinx.coroutines.delay




@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectsScreen(
    navController: NavController,
    viewModel: BacklogViewModel = hiltViewModel(),
    projectId: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    TransparentSystemBars()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recordToEdit by viewModel.inboxHandler.recordToEdit.collectAsStateWithLifecycle()
    val editorViewModel: UniversalEditorViewModel = hiltViewModel()

    // Router logic to decide which screen to show
    when {
        recordToEdit != null -> {
            var textValue by remember { mutableStateOf(TextFieldValue()) }

            LaunchedEffect(recordToEdit) {
                val record = recordToEdit
                if (record != null && record.text != textValue.text) {
                    textValue = TextFieldValue(record.text)
                }
            }

            FullScreenTextEditor(
                title = "Редагувати запис",
                value = textValue,
                onValueChange = { textValue = it },
                onSave = { viewModel.inboxHandler.onInboxRecordEditConfirm(textValue.text) },
                onCancel = { viewModel.inboxHandler.onInboxRecordEditDismiss() }
            )
        }
        uiState.artifactToEdit != null -> {
            val artifact = uiState.artifactToEdit!!
            LaunchedEffect(artifact) {
                if (editorViewModel.uiState.value.content.text != artifact.content) {
                    val newContent = artifact.content
                    editorViewModel.onContentChange(
                        TextFieldValue(newContent, androidx.compose.ui.text.TextRange(newContent.length))
                    )
                }
            }
            UniversalEditorScreen(
                title = "Редагувати Артефакт",
                onSave = { content, _ -> viewModel.onSaveArtifact(content) },
                onNavigateBack = { viewModel.onDismissArtifactEditor() },
                navController = navController,
                viewModel = editorViewModel
            )
        }
        uiState.showUniversalEditorForCustomList -> {
            LaunchedEffect(Unit) {
                editorViewModel.onContentChange(TextFieldValue(""))
            }
            UniversalEditorScreen(
                title = "Створити новий список",
                onSave = { content, _ -> viewModel.onSaveCustomList(content) },
                onNavigateBack = { viewModel.onDismissCustomListEditor() },
                navController = navController,
                viewModel = editorViewModel
            )
        }
        else -> {
            ProjectScaffold(
                navController = navController,
                viewModel = viewModel,
                projectId = projectId,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProjectScaffold(
    navController: NavController,
    viewModel: BacklogViewModel,
    projectId: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val project by viewModel.project.collectAsStateWithLifecycle()
    val lastOngoingActivity by viewModel.lastOngoingActivity.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    val suggestions by viewModel.autocompleteSuggestions.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val inboxListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    val targetBackgroundColor = MaterialTheme.colorScheme.surface
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(600),
        label = "background_color_animation"
    )

    val transition = rememberInfiniteTransition(label = "glow_transition")
    val glow by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    LaunchedEffect(navController) {
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("refresh_needed")?.observeForever { isRefreshNeeded ->
            if (isRefreshNeeded) {
                viewModel.forceRefresh()
                navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("refresh_needed")
            }
        }
    }

    if (uiState.showShareDialog) {
        ShareDialog(
            onDismiss = { viewModel.onShareDialogDismiss() },
            onCopyToClipboard = { viewModel.onCopyToClipboardRequest() },
            onTransfer = { viewModel.onTransferBacklogToServerRequest() },
            content = viewModel.getBacklogAsMarkdown()
        )
    }

    uiState.recordForReminderDialog?.let {
        ReminderPropertiesDialog(
            onDismiss = viewModel::onReminderDialogDismiss,
            onSetReminder = viewModel::onSetReminder,
            onRemoveReminder = null,
            currentReminders = uiState.remindersForDialog,
        )
    }

    

    val draggableItems = remember(listContent) {
        listContent.filterNot { it is ListItemContent.LinkItem }
    }

    GoalDetailEffects(
        navController = navController,
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        listState = listState,
        inboxListState = inboxListState,
        coroutineScope = coroutineScope
    )

    GoalDetailDialogs(viewModel = viewModel)

    if (uiState.showDisplayPropertiesDialog) {
        ProjectDisplayPropertiesDialog(
            onDismiss = viewModel::onDismissDisplayPropertiesDialog
        )
    }

    uiState.logEntryToEdit?.let { logEntry ->
        EditLogEntryDialog(
            logEntry = logEntry,
            onDismiss = viewModel::onDismissEditLogEntryDialog,
            onConfirm = { description, details ->
                viewModel.onUpdateLogEntry(description, details)
            }
        )
    }

    BackHandler(enabled = true) {
        val wasConsumed = viewModel.onBackPressed()
        if (!wasConsumed) {
            navController.popBackStack()
        }
    }

    val reminderParseResult =
        if ((uiState.detectedReminderCalendar != null) &&
            (uiState.detectedReminderSuggestion != null) &&
            (uiState.inputValue.text.isNotBlank())
        ) {
            ReminderParseResult(
                originalText = uiState.inputValue.text,
                calendar = uiState.detectedReminderCalendar,
                suggestionText = uiState.detectedReminderSuggestion,
                dateTimeEntities = emptyList(),
                otherEntities = emptyList(),
                success = true,
                errorMessage = null,
            )
        } else {
            null
        }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackgroundColor),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val topBarContainerColor = MaterialTheme.colorScheme.surfaceContainer
            with(sharedTransitionScope) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "project-card-$projectId"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                tween(durationMillis = 600, easing = FastOutSlowInEasing)
                            }
                        )
                        .graphicsLayer {
                            scaleX = glow
                            scaleY = glow
                        },
                    shape = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = topBarContainerColor),
                ) {
                    AdaptiveTopBar(
                        isSelectionModeActive = uiState.isSelectionModeActive,
                        project = project,
                        selectedCount = uiState.selectedItemIds.size,
                        areAllSelected = draggableItems.isNotEmpty() && (uiState.selectedItemIds.size == draggableItems.size),
                        onClearSelection = { viewModel.selectionHandler.clearSelection() },
                        onSelectAll = { viewModel.selectionHandler.selectAllItems() },
                        onDelete = { viewModel.selectionHandler.deleteSelectedItems(uiState.selectedItemIds) },
                        onMoreActions = { actionType ->
                            viewModel.selectionHandler.onBulkActionRequest(
                                actionType,
                                uiState.selectedItemIds
                            )
                        },
                        onMarkAsComplete = { viewModel.selectionHandler.markSelectedAsComplete(uiState.selectedItemIds) },
                        onMarkAsIncomplete = { viewModel.selectionHandler.markSelectedAsIncomplete(uiState.selectedItemIds) },
                        currentViewMode = uiState.currentView,
                        windowInsets = WindowInsets.statusBars,
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ProjectBottomBar(
                viewModel = viewModel,
                navController = navController,
                uiState = uiState,
                lastOngoingActivity = lastOngoingActivity,
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                reminderParseResult = reminderParseResult,
                suggestions = suggestions,
                project = project,
                onShowDisplayPropertiesClick = viewModel::onShowDisplayPropertiesDialog
            )
        }
    ) { paddingValues ->
        GoalDetailContent(
            modifier = Modifier
                .padding(paddingValues)
                .glitch(trigger = uiState.currentView),
            viewModel = viewModel,
            uiState = uiState,
            listState = listState,
            inboxListState = inboxListState,
            onEditLog = viewModel::onEditLogEntry,
            onDeleteLog = viewModel::onDeleteLogEntry,
            onSaveArtifact = viewModel::onSaveArtifact,
            onEditArtifact = viewModel::onEditArtifact
        )
    }
}

@Composable
private fun ProjectBottomBar(
    viewModel: BacklogViewModel,
    navController: NavController,
    uiState: UiState,
    lastOngoingActivity: ActivityRecord?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    reminderParseResult: ReminderParseResult?,
    suggestions: List<String>,
    project: Project?,
    onShowDisplayPropertiesClick: () -> Unit
) {
    Column {
        InProgressIndicator(
            ongoingActivity = lastOngoingActivity,
            onStopClick = viewModel::stopOngoingActivity,
            onReminderClick = viewModel::setReminderForOngoingActivity,
            onIndicatorClick = {
                val today = System.currentTimeMillis()
                navController.navigate("day_plan_screen/$today?startTab=TRACK")
            }
        )
        AnimatedVisibility(
            visible = !uiState.isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            ModernInputPanel(
                inputValue = uiState.inputValue,
                inputMode = uiState.inputMode,
                onValueChange = { viewModel.inputHandler.onInputTextChanged(it, uiState.inputMode) },
                onSubmit = { viewModel.inputHandler.submitInput(uiState.inputValue, uiState.inputMode) },
                onInputModeSelected = { viewModel.inputHandler.onInputModeSelected(it, uiState.inputValue) },
                onRecentsClick = {
                    Log.d("Recents_Debug", "onRecentsClick called from ProjectScreen")
                    viewModel.inputHandler.onShowRecentLists()
                },
                onAddListLinkClick = { viewModel.inputHandler.onAddListLinkRequest() },
                onShowAddWebLinkDialog = { viewModel.inputHandler.onShowAddWebLinkDialog() },
                onShowAddObsidianLinkDialog = { viewModel.inputHandler.onShowAddObsidianLinkDialog() },
                onAddListShortcutClick = { viewModel.inputHandler.onAddListShortcutRequest() },
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onBackClick = { viewModel.onBackPressed() },
                onForwardClick = { viewModel.onForwardPressed() },
                onHomeClick = viewModel::onHomeClick,
                onEditList = {
                    Log.d("EDIT_PROJECT_DEBUG", "LIST EDITING")
                    onMenuExpandedChange(false)
                    Log.d("EDIT_PROJECT_DEBUG", "List id: ${project?.id}")
                    navController.navigate("project_settings_screen?projectId=${project?.id}")
                },
                onShareList = { viewModel.onExportBacklogToMarkdownRequest() },
                onDeleteList = { viewModel.deleteCurrentProject() },
                onSetReminder = { viewModel.onSetReminderForProject() },
                menuExpanded = menuExpanded,
                onMenuExpandedChange = onMenuExpandedChange,
                currentView = uiState.currentView,
                onViewChange = { newView -> viewModel.onProjectViewChange(newView) },
                onImportFromMarkdown = viewModel::onImportFromMarkdownRequest,
                onExportToMarkdown = viewModel::onExportToMarkdownRequest,
                onImportBacklogFromMarkdown = viewModel::onImportBacklogFromMarkdownRequest,
                onExportBacklogToMarkdown = viewModel::onExportBacklogRequest,
                reminderParseResult = reminderParseResult,
                onClearReminder = viewModel::onClearReminder,
                isNerActive = uiState.nerState is NerState.Ready,
                onStartTrackingCurrentProject = viewModel::onStartTrackingCurrentProject,
                isProjectManagementEnabled = project?.isProjectManagementEnabled == true,
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                onToggleProjectManagement = viewModel::onToggleProjectManagement,
                onExportProjectState = viewModel::onExportProjectStateRequest,
                onAddProjectToDayPlan = viewModel::addCurrentProjectToDayPlan,
                onRevealInExplorer = { viewModel.onRevealInExplorer(project?.id ?: "") },
                onCloseSearch = viewModel::onCloseSearch,
                onAddMilestone = viewModel::onAddMilestone,
                onShowCreateCustomListDialog = viewModel::onShowCreateCustomListDialog,
                isViewModePanelVisible = uiState.isViewModePanelVisible,
                onToggleNavPanelMode = viewModel::onToggleNavPanelMode,
                suggestions = suggestions,
                onSuggestionClick = viewModel::onSuggestionClick,
                onShowDisplayPropertiesClick = onShowDisplayPropertiesClick
            )
        }
    }
}

@Composable
private fun TransparentSystemBars(isDarkTheme: Boolean = isSystemInDarkTheme()) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        LaunchedEffect(isDarkTheme) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme
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