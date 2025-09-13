// file: ui/screens/backlog/GoalDetailScreen.kt

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.backlog

// import android.util.Log // <--- ВИДАЛЕНО НЕПОТРІБНИЙ ІМПОРТ
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.ui.screens.backlog.components.dnd.SimpleDragDropState
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ModernInputPanel
import com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar.AdaptiveTopBar
// import kotlinx.coroutines.CoroutineScope // <--- ВИДАЛЕНО НЕПОТРІБНИЙ ІМПОРТ

@Composable
fun ProjectsScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val list by viewModel.goalList.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val inboxListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(value = false) } // <--- Додано іменований аргумент для ясності

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

    BackHandler(enabled = uiState.inputValue.text.isNotEmpty()) {
        viewModel.inputHandler.onInputTextChanged(TextFieldValue(""), uiState.inputMode)
    }

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.selectionHandler.clearSelection()
    }

    BackHandler(enabled = !isSelectionModeActive) {
        viewModel.flushPendingMoves()
        navController.popBackStack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.flushPendingMoves()
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
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AdaptiveTopBar(
                isSelectionModeActive = isSelectionModeActive,
                goalList = list,
                selectedCount = uiState.selectedItemIds.size,
                areAllSelected = draggableItems.isNotEmpty() && (uiState.selectedItemIds.size == draggableItems.size),
                onClearSelection = { viewModel.selectionHandler.clearSelection() },
                onSelectAll = { viewModel.selectionHandler.selectAllItems() },
                onDelete = { viewModel.selectionHandler.deleteSelectedItems(uiState.selectedItemIds) },
                onMoreActions = { actionType -> viewModel.selectionHandler.onBulkActionRequest(actionType, uiState.selectedItemIds) },
                onMarkAsComplete = { viewModel.selectionHandler.markSelectedAsComplete(uiState.selectedItemIds) },
                onMarkAsIncomplete = { viewModel.selectionHandler.markSelectedAsIncomplete(uiState.selectedItemIds) },
                // 👇 ДОДАНО РЯДОК
                currentViewMode = uiState.currentView
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = !isSelectionModeActive,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                ModernInputPanel(
                    inputValue = uiState.inputValue,
                    inputMode = uiState.inputMode,
                    // ПОМИЛКУ ВИПРАВЛЕНО: onValue-Change -> onValueChange
                    onValueChange = { viewModel.inputHandler.onInputTextChanged(it, uiState.inputMode) },
                    onSubmit = { viewModel.inputHandler.submitInput(uiState.inputValue, uiState.inputMode) },
                    onInputModeSelected = { viewModel.inputHandler.onInputModeSelected(it, uiState.inputValue) },
                    onRecentsClick = { viewModel.inputHandler.onShowRecentLists() },
                    onAddListLinkClick = { viewModel.inputHandler.onAddListLinkRequest() },
                    onShowAddWebLinkDialog = { viewModel.inputHandler.onShowAddWebLinkDialog() },
                    onShowAddObsidianLinkDialog = { viewModel.inputHandler.onShowAddObsidianLinkDialog() },
                    onAddListShortcutClick = { viewModel.inputHandler.onAddListShortcutRequest() },
                    canGoBack = navController.previousBackStackEntry != null,
                    onBackClick = {
                        viewModel.flushPendingMoves()
                        navController.popBackStack()
                    },
                    onForwardClick = { /* TODO */ },
                    onHomeClick = { viewModel.onRevealInExplorer(list?.id ?: "") },
                    isAttachmentsExpanded = list?.isAttachmentsExpanded ?: false,
                    onToggleAttachments = { viewModel.toggleAttachmentsVisibility() },
                    onEditList = {
                        menuExpanded = false
                        navController.navigate("edit_list_screen/${list?.id}")
                    },
                    onShareList = { /* TODO */ },
                    onDeleteList = { viewModel.deleteCurrentList() },
                    menuExpanded = menuExpanded,
                    onMenuExpandedChange = { newStatus -> menuExpanded = newStatus },
                    currentView = uiState.currentView,
                    onViewChange = { newView -> viewModel.onProjectViewChange(newView) },
                    onImportFromMarkdown = viewModel::onImportFromMarkdownRequest,
                    onExportToMarkdown = viewModel::onExportToMarkdownRequest,
                    onImportBacklogFromMarkdown = viewModel::onImportBacklogFromMarkdownRequest,
                    onExportBacklogToMarkdown = viewModel::onExportBacklogToMarkdownRequest,
                    reminderParseResult = reminderParseResult,
                    onClearReminder = viewModel::onClearReminder,
                    isNerActive = uiState.nerState is NerState.Ready,
                    onStartTrackingCurrentProject = viewModel::onStartTrackingCurrentProject,
                    isProjectManagementEnabled = list?.isProjectManagementEnabled == true,
                    modifier = Modifier.navigationBarsPadding().imePadding(),
                    onToggleProjectManagement = viewModel::onToggleProjectManagement,
                    onExportProjectState = viewModel::onExportProjectStateRequest,
                )
            }
        },
    ) { paddingValues ->
        GoalDetailContent(
            modifier = Modifier.padding(paddingValues),
            viewModel = viewModel,
            uiState = uiState,
            listState = listState,
            inboxListState = inboxListState,
            dragDropState = dragDropState
        )
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