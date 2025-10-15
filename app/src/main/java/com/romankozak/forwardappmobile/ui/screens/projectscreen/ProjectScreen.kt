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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.ui.common.editor.components.FullScreenTextEditor
import com.romankozak.forwardappmobile.ui.common.components.ShareDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.SimpleDragDropState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.inputpanel.ModernInputPanel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.topbar.AdaptiveTopBar
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.GoalDetailDialogs
import kotlinx.coroutines.delay

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectsScreen(
    navController: NavController,
    viewModel: BacklogViewModel = hiltViewModel(),
    // Додаємо projectId та скоупи для анімації
    projectId: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val topBarContainerColor = MaterialTheme.colorScheme.surfaceContainer
    val view = LocalView.current
    val isDarkTheme = isSystemInDarkTheme()

    if (!view.isInEditMode) {
        LaunchedEffect(Unit) {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    // Ідея №1: Фон із картки. Анімуємо колір фону.
    val targetBackgroundColor = MaterialTheme.colorScheme.surface
    val animatedBackgroundColor by animateColorAsState(
        targetValue = targetBackgroundColor,
        animationSpec = tween(600), // Тривалість анімації фону
        label = "background_color_animation"
    )

    // Ідея №2: Кіберпанкове світіння
    val transition = rememberInfiniteTransition(label = "glow_transition")
    val glow by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f, // Невелике збільшення для ефекту пульсації
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val list by viewModel.project.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    
    val lastOngoingActivity by viewModel.lastOngoingActivity.collectAsStateWithLifecycle()

    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    val suggestions by viewModel.autocompleteSuggestions.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val inboxListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(value = false) }

    LaunchedEffect(navController) {
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("refresh_needed")?.observeForever { isRefreshNeeded ->
            if (isRefreshNeeded) {
                viewModel.forceRefresh()
                navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("refresh_needed")
            }
        }
    }

    @Composable
    fun rememberSimpleDragDropState(
        lazyListState: LazyListState,
        onMove: (Int, Int) -> Unit,
    ): SimpleDragDropState {
        val scope = rememberCoroutineScope()
        return remember(lazyListState) {
            SimpleDragDropState(state = lazyListState, scope = scope, onMove = onMove)
        }
    }

    val recordToEdit by viewModel.inboxHandler.recordToEdit.collectAsStateWithLifecycle()

    if (recordToEdit != null) {
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
            onSave = {
                viewModel.inboxHandler.onInboxRecordEditConfirm(textValue.text)
            },
            onCancel = { viewModel.inboxHandler.onInboxRecordEditDismiss() }
        )
    } else if (uiState.artifactToEdit != null) {
        // NOTE: It's generally better to hoist hiltViewModel() calls outside of conditional blocks.
        // However, for this targeted change, we'll create it here.
        // This ViewModel will be scoped to the ProjectScreen's lifecycle.
        val editorViewModel: com.romankozak.forwardappmobile.ui.common.editor.viewmodel.UniversalEditorViewModel = hiltViewModel()
        val artifact = uiState.artifactToEdit!!

        // This effect will run when the artifact to edit changes, initializing the editor's content.
        // If the user cancels, their intermediate edits will be preserved in the editorViewModel
        // until they edit a different artifact or the screen is left.
        LaunchedEffect(artifact) {
            if (editorViewModel.uiState.value.content.text != artifact.content) {
                val newContent = artifact.content
                editorViewModel.onContentChange(
                    TextFieldValue(
                        newContent,
                        androidx.compose.ui.text.TextRange(newContent.length)
                    )
                )
            }
        }

        com.romankozak.forwardappmobile.ui.common.editor.UniversalEditorScreen(
            title = "Редагувати Артефакт",
            onSave = { content, _ ->
                viewModel.onSaveArtifact(content)
            },
            onNavigateBack = { viewModel.onDismissArtifactEditor() },
            navController = navController,
            viewModel = editorViewModel
        )
    } else {
        if (uiState.showShareDialog) {
            ShareDialog(
                onDismiss = { viewModel.onShareDialogDismiss() },
                onCopyToClipboard = { viewModel.onCopyToClipboardRequest() },
                onTransfer = {
                    viewModel.onTransferBacklogToServerRequest()
                },
                content = viewModel.getBacklogAsMarkdown()
            )
        }

        uiState.recordForReminderDialog?.let { record ->
            com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.ReminderPickerDialog(
                onDismiss = viewModel::onReminderDialogDismiss,
                onSetReminder = viewModel::onSetReminder,
                onRemoveReminder = { time -> viewModel.onRemoveReminder(time) },
                currentReminderTimes = uiState.remindersForDialog.map { it.reminderTime },
            )
        }

        val dragDropState = rememberSimpleDragDropState(
            lazyListState = listState,
            onMove = viewModel::moveItem,
        )

        val draggableItems = remember(listContent) {
            listContent.filterNot { it is ListItemContent.LinkItem }
        }

        GoalDetailEffects(
            navController = navController,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            listState = listState,
            inboxListState = inboxListState,
            dragDropState = dragDropState,
            coroutineScope = coroutineScope
        )

        GoalDetailDialogs(viewModel = viewModel)

        uiState.logEntryToEdit?.let { logEntry ->
            com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.EditLogEntryDialog(
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
                with(sharedTransitionScope) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sharedElement(
                                // FIX 1: Changed parameter name from 'state' to 'sharedContentState'
                                sharedContentState = rememberSharedContentState(key = "project-card-$projectId"), // FIX 2: Removed redundant curly braces
                                animatedVisibilityScope = animatedVisibilityScope,
                                boundsTransform = { initialBounds, targetBounds ->
                                    tween(durationMillis = 600, easing = FastOutSlowInEasing)
                                }
                           //     boundsTransform = MaterialContainerTransform()

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
                            isSelectionModeActive = isSelectionModeActive,
                            project = list,
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
                        visible = !isSelectionModeActive,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        ModernInputPanel(
                            inputValue = uiState.inputValue,
                            inputMode = uiState.inputMode,
                            onValueChange = {
                                viewModel.inputHandler.onInputTextChanged(
                                    it,
                                    uiState.inputMode
                                )
                            },
                            onSubmit = {
                                viewModel.inputHandler.submitInput(
                                    uiState.inputValue,
                                    uiState.inputMode
                                )
                            },
                            onInputModeSelected = {
                                viewModel.inputHandler.onInputModeSelected(
                                    it,
                                    uiState.inputValue
                                )
                            },
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
                                menuExpanded = false
                                Log.d("EDIT_PROJECT_DEBUG", "List id: ${list?.id}")
                                navController.navigate("edit_list_screen/${list?.id}")
                            },
                            onShareList = { viewModel.onExportBacklogToMarkdownRequest() },
                            onDeleteList = { viewModel.deleteCurrentProject() },
                            onSetReminder = { viewModel.onSetReminderForProject() },
                            menuExpanded = menuExpanded,
                            onMenuExpandedChange = { newStatus -> menuExpanded = newStatus },
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
                            isProjectManagementEnabled = list?.isProjectManagementEnabled == true,
                            modifier = Modifier
                                .navigationBarsPadding()
                                .imePadding(),
                            onToggleProjectManagement = viewModel::onToggleProjectManagement,
                            onExportProjectState = viewModel::onExportProjectStateRequest,
                            onAddProjectToDayPlan = viewModel::addCurrentProjectToDayPlan,
                            onRevealInExplorer = { viewModel.onRevealInExplorer(list?.id ?: "") },
                            onCloseSearch = viewModel::onCloseSearch,
                            onAddMilestone = viewModel::onAddMilestone,
                            isViewModePanelVisible = uiState.isViewModePanelVisible,
                            onToggleNavPanelMode = viewModel::onToggleNavPanelMode,
                            suggestions = suggestions,
                            onSuggestionClick = viewModel::onSuggestionClick
                        )
                    }
                }
            }
        ) { paddingValues ->
            // ВИДАЛЕНО: Тимчасові вкладки та текст
            // ЗАМІНЕНО: Ефект "глітч" застосовано до GoalDetailContent
            GoalDetailContent(
                modifier = Modifier
                    .padding(paddingValues)
                    .glitch(trigger = uiState.currentView), // Анімація при зміні вигляду
                viewModel = viewModel,
                uiState = uiState,
                listState = listState,
                inboxListState = inboxListState,
                dragDropState = dragDropState,
                onEditLog = viewModel::onEditLogEntry,
                onDeleteLog = viewModel::onDeleteLogEntry,
                onSaveArtifact = viewModel::onSaveArtifact,
                onEditArtifact = viewModel::onEditArtifact
            )
        }
    }
}

// Модифікатор для Glitch-ефекту
fun Modifier.glitch(trigger: Any): Modifier = composed {
    var glitchAmount by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(key1 = trigger) {
        val glitchDuration = 150L
        val startTime = withFrameNanos { it }

        while (withFrameNanos { it } < startTime + (glitchDuration * 1_000_000)) {
            glitchAmount = (Math.random() * 10 - 5).toFloat()
            delay(40) // Маленька затримка між "кадрами" глітчу
        }
        glitchAmount = 0f
    }

    this.graphicsLayer {
        translationX = glitchAmount
        translationY = (Math.random() * glitchAmount - glitchAmount / 2).toFloat()
    }
}


@Composable
private fun InProgressIndicator(
    ongoingActivity: com.romankozak.forwardappmobile.data.database.models.ActivityRecord?,
    onStopClick: () -> Unit,
    onReminderClick: () -> Unit,
    onIndicatorClick: () -> Unit
) {
    AnimatedVisibility(
        visible = ongoingActivity != null,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        if (ongoingActivity != null) {
            var elapsedTime by remember { mutableLongStateOf(System.currentTimeMillis() - (ongoingActivity.startTime ?: 0L)) }

            LaunchedEffect(key1 = ongoingActivity.id) {
                while (true) {
                    elapsedTime = System.currentTimeMillis() - (ongoingActivity.startTime ?: 0L)
                    kotlinx.coroutines.delay(1000L)
                }
            }
            val timeString = formatElapsedTime(elapsedTime)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onIndicatorClick),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.HourglassTop, contentDescription = "В процесі", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ongoingActivity.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(text = timeString, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = onReminderClick) {
                        Icon(Icons.Default.Notifications, contentDescription = "Встановити нагадування")
                    }
                    IconButton(onClick = onStopClick) {
                        Icon(Icons.Default.StopCircle, contentDescription = "Зупинити")
                    }
                    val context = androidx.compose.ui.platform.LocalContext.current
                    IconButton(onClick = {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = android.net.Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Налаштування")
                    }
                }
            }
        }
    }
}

@Composable
private fun formatElapsedTime(elapsedTime: Long): String {
    val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(elapsedTime)
    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60
    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
    return if (hours > 0) {
        String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}