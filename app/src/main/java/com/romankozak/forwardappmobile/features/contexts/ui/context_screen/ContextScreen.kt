@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import android.app.Activity
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Overlay
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.ModernInputPanel
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.topbar.AdaptiveTopBar
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs.EditLogEntryDialog
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs.GoalDetailDialogs
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs.ProjectDisplayPropertiesDialog
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState
import com.romankozak.forwardappmobile.features.reminders.dialogs.RemindersDialog
import com.romankozak.forwardappmobile.ui.common.components.ShareDialog
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen
import com.romankozak.forwardappmobile.ui.common.editor.components.FullScreenTextEditor
import com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel
import com.romankozak.forwardappmobile.ui.shared.InProgressIndicator
import kotlinx.coroutines.delay

private const val TAG = "BacklogVM_DEBUG"


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectsScreen(
    navController: NavController,
    viewModel: ContextScreenViewModel = hiltViewModel(),
    projectId: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    TransparentSystemBars()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recordToEdit by viewModel.inboxHandler.recordToEdit.collectAsStateWithLifecycle()
    val editorViewModel: UniversalEditorViewModel = hiltViewModel()
    val currentProjectArtifact by viewModel.contextArtifact.collectAsStateWithLifecycle()

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
                onCancel = { viewModel.inboxHandler.onInboxRecordEditDismiss() },
            )
        }
        uiState.artifactToEdit != null -> {
            val artifact = uiState.artifactToEdit!!
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(artifact) {
                if (editorViewModel.uiState.value.content.text != artifact.content) {
                    val newContent = artifact.content
                    editorViewModel.onContentChange(
                        TextFieldValue(newContent, androidx.compose.ui.text.TextRange(newContent.length)),
                    )
                }
            }
            UniversalEditorScreen(
                title = "Редагувати Артефакт",
                onSave = { content, _ ->
                    if (projectId != null) {
                        viewModel.onSaveArtifact(projectId, content)
                    } else {
                        // Handle error or show a snackbar
                        // For now, let's just log it
                        Log.e(TAG, "projectId is null when trying to save artifact")
                    }
                },
                onAutoSave = { content, _ -> viewModel.onAutoSaveArtifact(content) },
                onNavigateBack = { viewModel.onDismissArtifactEditor() },
                navController = navController,
                viewModel = editorViewModel,
                contentFocusRequester = focusRequester,
            )
        }
        uiState.showNoteDocumentEditor -> {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                editorViewModel.onContentChange(TextFieldValue(""))
            }
            // File: ContextScreen.kt

            UniversalEditorScreen(
                title = "Створити новий документ",
                onSave = { content, _ ->
                    // Ми ігноруємо Int від редактора і передаємо null,
                    // щоб ViewModel використала поточний contextIdFlow.value
                    viewModel.onSaveNoteDocument(content, null)
                },
                onAutoSave = null,
                onNavigateBack = { viewModel.onDismissNoteDocumentEditor() },
                navController = navController,
                viewModel = editorViewModel,
                contentFocusRequester = focusRequester, // Додано кому для відповідності INFO
            )
        }
        else -> {
            ProjectScaffold(
                navController = navController,
                viewModel = viewModel,
                projectId = projectId,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ProjectScaffold(
    navController: NavController,
    viewModel: ContextScreenViewModel,
    projectId: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val reminderViewModel: com.romankozak.forwardappmobile.features.reminders.viewmodel.ReminderViewModel = hiltViewModel()
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
    var showRemindersListDialog by remember { mutableStateOf(false) }
    var selectedItemForReminders by remember { mutableStateOf<BacklogItemContent?>(null) }

    val holdMenuController = rememberHoldMenu2()

    val targetBackgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(600),
        label = "background_color_animation",
    )

    val transition = rememberInfiniteTransition(label = "glow_transition")
    val glow by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "glow_scale",
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
            onDismiss = { viewModel.onDismissShareDialog() },
            onCopyToClipboard = { viewModel.onCopyToClipboardRequest() },
            onTransfer = { viewModel.onTransferBacklogToServerRequest() },
            content = viewModel.getBacklogAsMarkdown(),
        )
    }

    val draggableItems =
        remember(listContent) {
            listContent.filterNot { it is BacklogItemContent.LinkItem }
        }

    GoalDetailEffects(
        navController = navController,
        viewModel = viewModel,
        snackbarHostState = snackbarHostState,
        listState = listState,
        inboxListState = inboxListState,
        coroutineScope = coroutineScope,
    )

    GoalDetailDialogs(viewModel = viewModel)

    if (uiState.showDisplayPropertiesDialog) {
        ProjectDisplayPropertiesDialog(
            isProjectManagementEnabled = uiState.isProjectManagementEnabled,
            onToggleProjectManagement = viewModel::onToggleProjectManagement,
            onDismiss = viewModel::onDismissDisplayPropertiesDialog,
        )
    }

    uiState.logEntryToEdit?.let { logEntry ->
        EditLogEntryDialog(
            logEntry = logEntry,
            onDismiss = viewModel::onDismissEditLogEntryDialog,
            onConfirm = { description, details ->
                viewModel.onUpdateLogEntry(description, details)
            },
        )
    }

    BackHandler(enabled = true) {
        val wasConsumed = viewModel.onBackPressed()
        // If wasConsumed is false OR null, it will trigger the popBackStack
        if (wasConsumed != true) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = animatedBackgroundColor,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                val topBarContainerColor = MaterialTheme.colorScheme.surfaceContainer
                with(sharedTransitionScope) {
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "project-card-$projectId"),
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    boundsTransform = { _, _ ->
                                        tween(durationMillis = 600, easing = FastOutSlowInEasing)
                                    },
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
                                    uiState.selectedItemIds,
                                )
                            },
                            onInboxClick = {
                                val today = System.currentTimeMillis()
                                navController.navigate("day_plan_screen/$today?startTab=INBOX")
                            },
                            onMarkAsComplete = { viewModel.selectionHandler.markSelectedAsComplete(uiState.selectedItemIds) },
                            onMarkAsIncomplete = { viewModel.selectionHandler.markSelectedAsIncomplete(uiState.selectedItemIds) },
                            currentViewMode = uiState.currentViewMode,
                            windowInsets = WindowInsets.statusBars,
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Surface(color = animatedBackgroundColor) {
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
                        onShowDisplayPropertiesClick = viewModel::onShowDisplayPropertiesDialog,
                        holdMenuController = holdMenuController,
                    )
                }
            },
        ) { paddingValues ->
            GoalDetailContent(
                modifier =
                    Modifier
                        .padding(paddingValues)
                        .glitch(trigger = uiState.currentViewMode),
                viewModel = viewModel,
                uiState = uiState,
                listState = listState,
                inboxListState = inboxListState,
                onEditLog = viewModel::onEditLogEntry,
                onDeleteLog = viewModel::onDeleteLogEntry,
                onSaveArtifact = viewModel::onSaveArtifact,
                onEditArtifact = viewModel::onEditArtifact,
                onRemindersClick = { item ->
                    selectedItemForReminders = item
                    showRemindersListDialog = true
                },
                onShowProjectProperties = {
                    menuExpanded = false
                    navController.navigate("project_settings_screen?projectId=${project?.id}")
                },
                onSwitchView = viewModel::onProjectViewChange,
            )
        }

        HoldMenu2Overlay(
            controller = holdMenuController,
            modifier = Modifier.fillMaxSize().zIndex(10f),
        )
    }

    if (showRemindersListDialog && selectedItemForReminders != null) {
        RemindersDialog(
            viewModel = reminderViewModel,
            item = selectedItemForReminders!!,
            onDismiss = { showRemindersListDialog = false },
        )
    }
}

@Composable
private fun ProjectBottomBar(
    viewModel: ContextScreenViewModel,
    navController: NavController,
    uiState: ContextUiState,
    lastOngoingActivity: ActivityRecord?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    reminderParseResult: ReminderParseResult?,
    suggestions: List<String>,
    project: Context?,
    onShowDisplayPropertiesClick: () -> Unit,
    holdMenuController: com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller,
) {
    val indicatorState = remember { com.romankozak.forwardappmobile.ui.shared.InProgressIndicatorState(isInitiallyExpanded = false) }

    Column {
        InProgressIndicator(
            ongoingActivity = lastOngoingActivity,
            onStopClick = viewModel::stopOngoingActivity,
            onReminderClick = viewModel::setReminderForOngoingActivity,
            onIndicatorClick = {
                val today = System.currentTimeMillis()
                navController.navigate("day_plan_screen/$today?startTab=TRACK")
            },
            indicatorState = indicatorState,
        )
        AnimatedVisibility(
            visible = !uiState.isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            ModernInputPanel(
                holdMenuController = holdMenuController,
                inputValue = uiState.inputValue,
                inputMode = uiState.inputMode,
                onValueChange = { viewModel.inputHandler.onInputTextChanged(it, uiState.inputMode) },
                onSubmit = { viewModel.inputHandler.submitInput(uiState.inputValue, uiState.inputMode) },
                onInputModeSelected = { viewModel.inputHandler.onInputModeSelected(it, uiState.inputValue) },
                onRecentsClick = {
                    Log.d("Recents_Debug", "onRecentsClick called from ProjectScreen")
                    viewModel.inputHandler.onShowRecentLists()
                },
                onAddNestedProjectClick = { viewModel.inputHandler.onAddListLinkRequest() },
                onShowAddWebLinkDialog = { viewModel.inputHandler.onShowAddWebLinkDialog() },
                onShowAddObsidianLinkDialog = { viewModel.inputHandler.onShowAddObsidianLinkDialog() },
                onAddListShortcutClick = { viewModel.inputHandler.onAddListShortcutRequest() },
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onBackClick = { viewModel.onBackPressed() },
                onForwardClick = { viewModel.onForwardPressed() },
                onShowProjectHierarchy = viewModel::onHomeClick,
                onNavigateHome = { navController.navigate("command_deck_screen") },
                onEditList = {
                    Log.d("EDIT_PROJECT_DEBUG", "LIST EDITING")
                    onMenuExpandedChange(false)
                    navController.navigate("project_settings_screen?projectId=${project?.id}")
                },
                onShareList = { viewModel.onExportBacklogToMarkdownRequest() },
                onDeleteList = { viewModel.deleteCurrentProject() },
                onSetReminder = { viewModel.onSetReminderForProject() },
                menuExpanded = menuExpanded,
                onMenuExpandedChange = onMenuExpandedChange,
                currentView = uiState.currentViewMode,
                onViewChange = { newView -> viewModel.onProjectViewChange(newView) },
                onImportFromMarkdown = viewModel::onImportFromMarkdownRequest,
                onExportToMarkdown = viewModel::onExportToMarkdownRequest,
                onImportBacklogFromMarkdown = viewModel::onImportBacklogFromMarkdownRequest,
                onExportBacklogToMarkdown = viewModel::onExportBacklogToMarkdownRequest,
                reminderParseResult = reminderParseResult,
                onClearReminder = viewModel::onClearReminder,
                isNerActive = uiState.nerState is NerState.Ready,
                onStartTrackingCurrentProject = viewModel::onStartTrackingCurrentProject,
                // --- ОНОВЛЕНА ЛОГІКА CAPABILITIES ---
                isProjectManagementEnabled = uiState.isProjectManagementEnabled,
                experimentalCapabilityIds = uiState.experimentalCapabilityIds,
                enableInbox = uiState.enableInbox,
                enableLog = uiState.enableLog,
                enableArtifact = uiState.enableArtifact,
                enableBacklog = uiState.enableBacklog,
                enableDashboard = uiState.enableDashboard,
                enableAttachments = uiState.enableAttachments,
                // ------------------------------------
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                onToggleProjectManagement = viewModel::onToggleProjectManagement,
                onExportProjectState = viewModel::onExportProjectStateRequest,
                onAddProjectToDayPlan = viewModel::addCurrentProjectToDayPlan,
                onCloseSearch = viewModel::onCloseSearch,
                onAddMilestone = viewModel::onAddMilestone,
                onShowCreateNoteDocumentDialog = viewModel::onShowCreateNoteDocumentDialog,
                onCreateChecklist = viewModel::onCreateChecklist,
                suggestions = suggestions,
                onSuggestionClick = viewModel::onSuggestionClick,
                onShowDisplayPropertiesClick = onShowDisplayPropertiesClick,
                onAddScript =
                    if (FeatureToggles.isEnabled(FeatureFlag.ScriptsLibrary)) {
                        {
                            val route =
                                project?.id?.let { id -> "script_editor_screen?projectId=$id" }
                                    ?: "script_editor_screen"
                            navController.navigate(route)
                        }
                    } else {
                        null
                    },
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

fun Modifier.glitch(trigger: Any): Modifier =
    composed {
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
