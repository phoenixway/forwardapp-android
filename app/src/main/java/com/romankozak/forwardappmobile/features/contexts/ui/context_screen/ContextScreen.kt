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
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Overlay
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.ModernInputPanel
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.topbar.AdaptiveTopBar
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs.EditLogEntryDialog
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs.GoalDetailDialogs
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs.ProjectDisplayPropertiesDialog
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.features.reminders.dialogs.RemindersDialog
import com.romankozak.forwardappmobile.ui.common.components.ShareDialog
import com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen
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
            var textValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue()) }

            LaunchedEffect(recordToEdit) {
                val record = recordToEdit
                if (record != null && record.text != textValue.text) {
                    textValue = androidx.compose.ui.text.input.TextFieldValue(record.text)
                }
            }

            com.romankozak.forwardappmobile.ui.common.editor.components.FullScreenTextEditor(
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
                navigationManager = viewModel.enhancedNavigationManager,
                viewModel = editorViewModel,
                contentFocusRequester = focusRequester,
                foldingPersistenceKey = "context_artifact:${artifact.id}",
            )
        }
        uiState.showNoteDocumentEditor -> {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                    editorViewModel.onContentChange(androidx.compose.ui.text.input.TextFieldValue(""))
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
                navigationManager = viewModel.enhancedNavigationManager,
                viewModel = editorViewModel,
                contentFocusRequester = focusRequester, // Додано кому для відповідності INFO
                startInEditMode = true,
                foldingPersistenceKey = projectId?.let { "new_note_document:$it" },
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
    val sessionState by viewModel.contextSessionState.collectAsStateWithLifecycle()
    val canPasteIntoCurrentBacklog by viewModel.itemActionHandler.canPasteIntoCurrentBacklog.collectAsStateWithLifecycle()
    val canPasteIntoCurrentDirection by viewModel.itemActionHandler.canPasteIntoCurrentDirection.collectAsStateWithLifecycle()
    val canPasteIntoCurrentAttachments by viewModel.itemActionHandler.canPasteIntoCurrentAttachments.collectAsStateWithLifecycle()
    val isCurrentContextFocused by viewModel.isCurrentContextFocused.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val inboxListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }
    var showRemindersListDialog by remember { mutableStateOf(false) }
    var selectedItemForReminders by remember { mutableStateOf<BacklogItemContent?>(null) }
    val canPasteIntoCurrentList =
        when (uiState.currentViewMode) {
            ContextViewMode.BACKLOG -> canPasteIntoCurrentBacklog
            ContextViewMode.DIRECTION -> canPasteIntoCurrentDirection
            ContextViewMode.CONNECTIONS -> canPasteIntoCurrentAttachments
            else -> false
        }

    val holdMenuController = rememberHoldMenu2()
    val navigationManager = viewModel.enhancedNavigationManager

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
                                .graphicsLayer {
                                    scaleX = glow
                                    scaleY = glow
                                },
                        shape = RoundedCornerShape(0.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = topBarContainerColor),
                    ) {
                        Column {
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
                                onPaste =
                                    if (canPasteIntoCurrentList) {
                                        { viewModel.itemActionHandler.onTransportPasteRequested(uiState.currentViewMode) }
                                    } else {
                                        null
                                    },
                                onInboxClick = {
                                    val today = System.currentTimeMillis()
                                    navigationManager.navigate(
                                        target = NavTarget.DayPlan(dayPlanId = today.toString(), startTab = "INBOX"),
                                    )
                                },
                                onMarkAsComplete = { viewModel.selectionHandler.markSelectedAsComplete(uiState.selectedItemIds) },
                                onMarkAsIncomplete = { viewModel.selectionHandler.markSelectedAsIncomplete(uiState.selectedItemIds) },
                                currentViewMode = uiState.currentViewMode,
                                enabledCapabilities = sessionState.enabledCapabilities,
                                windowInsets = WindowInsets.statusBars,
                            )

                            // Action panel removed to avoid duplicating ModernInputPanel controls.
                        }
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
                        isCurrentContextFocused = isCurrentContextFocused,
                        sessionState = sessionState,
                        lastOngoingActivity = lastOngoingActivity,
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        menuExpanded = menuExpanded,
                        onMenuExpandedChange = { menuExpanded = it },
                        project = project,
                        onShowDisplayPropertiesClick = viewModel::onShowDisplayPropertiesDialog,
                        navigationManager = navigationManager,
                        holdMenuController = holdMenuController,
                    )
                }
            },
        ) { paddingValues ->
            if (uiState.isContextSwitching) {
                Box(
                    modifier =
                        Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                )
            } else {
                GoalDetailContent(
                    modifier =
                        Modifier
                            .padding(paddingValues)
                            .glitch(trigger = sessionState.currentView),
                    viewModel = viewModel,
                    uiState = uiState,
                    currentViewMode = sessionState.currentView,
                    enabledCapabilities = sessionState.enabledCapabilities,
                    listState = listState,
                    inboxListState = inboxListState,
                    onEditLog = viewModel::onEditLogEntry,
                    onDeleteLog = viewModel::onDeleteLogEntry,
                    onSaveArtifact = viewModel::onSaveArtifact,
                    onEditArtifact = viewModel::onEditArtifact,
                    onRemindersClick = { item ->
                        // Використовуємо 'as?', щоб не падати, якщо прийшов не той тип
                        val safeItem = item as? BacklogItemContent

                        if (safeItem != null) {
                            selectedItemForReminders = safeItem
                            showRemindersListDialog = true
                        } else {
                            // Це допоможе вам побачити в логах, що саме прилітає насправді
                            Log.e("TYPE_ERROR", "Очікували BacklogItemContent, але прийшло: ${item::class.java.simpleName}")
                        }
                    },
                    onShowProjectProperties = {
                        menuExpanded = false
                        navigationManager.navigate(
                            target = NavTarget.ProjectSettings(projectId = project?.id),
                        )
                    },
                    onSwitchView = viewModel::onProjectViewChange,
                    onLinkDirectionRequest = { itemId ->
                        viewModel.onLinkDirectionItemRequest(itemId)
                    },
                    onUnlinkDirectionRequest = { itemId ->
                        viewModel.onUnlinkDirectionItem(itemId)
                    },
                    onOpenLinkedDirectionContext = { contextId ->
                        viewModel.openLinkedContext(contextId)
                    },
                    linkedContextNames = uiState.linkedContextNames,
                )
            }
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
    isCurrentContextFocused: Boolean,
    sessionState: com.romankozak.forwardappmobile.core.context.ContextSessionState,
    lastOngoingActivity: ActivityRecord?,
    canGoBack: Boolean,
    canGoForward: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    project: Context?,
    onShowDisplayPropertiesClick: () -> Unit,
    navigationManager: EnhancedNavigationManager,
    holdMenuController: com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller,
) {
    val indicatorState = remember { com.romankozak.forwardappmobile.ui.shared.InProgressIndicatorState(isInitiallyExpanded = true) }
    var showContextPicker by remember { mutableStateOf(false) }
    val capabilityViewActions =
        remember(sessionState.currentView, sessionState.enabledCapabilities) {
            viewModel.getAvailableCapabilityViewActions(
                currentView = sessionState.currentView,
                enabledCapabilities = sessionState.enabledCapabilities,
            )
        }
    val groupedContexts by viewModel.subprojectChildren.collectAsStateWithLifecycle()
    val contextOptions =
        remember(groupedContexts) {
            groupedContexts
                .values
                .flatten()
                .distinctBy { it.id }
                .map { context -> ProjectOption(id = context.id, name = context.name, parentId = context.parentId) }
        }

    Column {
        InProgressIndicator(
            ongoingActivity = lastOngoingActivity,
            onStopClick = viewModel::stopOngoingActivity,
            onReminderClick = viewModel::setReminderForOngoingActivity,
            onIndicatorClick = {
                val today = System.currentTimeMillis()
                navigationManager.navigate(
                    target = NavTarget.DayPlan(dayPlanId = today.toString(), startTab = "TRACK"),
                )
            },
            indicatorState = indicatorState,
            allowCollapse = false,
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
                onAddNestedProjectClick = { showContextPicker = true },
                onShowCurrentContextInHierarchyFocus = {
                    val contextIdToReveal = project?.id ?: return@ModernInputPanel
                    navigationManager.navigate(
                        target = NavTarget.ContextHierarchy,
                        builder = {
                            launchSingleTop = true
                            restoreState = true
                        },
                    )
                    runCatching {
                        navController.getBackStackEntry("goal_lists_screen").savedStateHandle.apply {
                            this["projectIdToReveal"] = contextIdToReveal
                            this["projectIdToRevealForceFocus"] = true
                        }
                    }
                },
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onBackClick = { viewModel.onBackPressed() },
                onForwardClick = { viewModel.onForwardPressed() },
                onShowProjectHierarchy = { navigationManager.navigate(target = NavTarget.GlobalSearchHome) },
                onNavigateHome = { navigationManager.navigate(target = NavTarget.CommandDeck) },
                onEditList = {
                    Log.d("EDIT_PROJECT_DEBUG", "LIST EDITING")
                    onMenuExpandedChange(false)
                    navigationManager.navigate(
                        target = NavTarget.ProjectSettings(projectId = project?.id),
                    )
                },
                onShareList = { viewModel.onExportBacklogToMarkdown() },
                onDeleteList = { viewModel.deleteCurrentProject() },
                onSetReminder = { viewModel.onSetReminderForProject() },
                onToggleFocusContext = viewModel::toggleCurrentContextFocus,
                isCurrentContextFocused = isCurrentContextFocused,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = onMenuExpandedChange,
                currentView = sessionState.currentView,
                onViewChange = { newView -> viewModel.onProjectViewChange(newView) },
                onImportFromMarkdown = viewModel::onImportFromMarkdownRequest,
                onExportToMarkdown = viewModel::onExportInboxToMarkdown,
                onImportBacklogFromMarkdown = viewModel::onShowImportBacklogFromMarkdownDialog,
                onExportBacklogToMarkdown = viewModel::onExportBacklogToMarkdown,
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
                enabledCapabilitiesOverride = sessionState.enabledCapabilities,
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
                onShowDisplayPropertiesClick = onShowDisplayPropertiesClick,
                capabilityViewActions = capabilityViewActions,
                onCapabilityViewActionClick = viewModel::onCapabilityViewActionClick,
            )
        }
    }

    if (showContextPicker) {
        LinkedTargetsPickerDialog(
            contextOptions = contextOptions,
            attachmentOptions = emptyList(),
            preselectedContextIds = emptySet(),
            preselectedAttachmentIds = emptySet(),
            initialTab = LinkPickerTab.CONTEXTS,
            allowedTabs = setOf(LinkPickerTab.CONTEXTS),
            onDismiss = { showContextPicker = false },
            onContextSelected = { id ->
                when (uiState.currentViewMode) {
                    ContextViewMode.DIRECTION -> viewModel.onDirectionContextLinkSelected(id)
                    ContextViewMode.BACKLOG -> viewModel.onBacklogContextLinkSelected(id)
                    else -> viewModel.onBacklogContextLinkSelected(id)
                }
                showContextPicker = false
            },
            onAttachmentSelected = {},
            onCreateRootContext = { name -> viewModel.createRootContextForPicker(name) },
            onCreateDocument = null,
        )
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
