

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.projectscreen

import android.util.Log
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.ui.common.components.FullScreenTextEditor
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.SimpleDragDropState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.inputpanel.ModernInputPanel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.topbar.AdaptiveTopBar
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.CreateCustomListDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.ExportTransferDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.GoalDetailDialogs

@Composable
fun ProjectsScreen(
    navController: NavController,
    viewModel: BacklogViewModel = hiltViewModel(),
) {
    Log.d("ViewModelInitTest", "ProjectsScreen: Спроба створити GoalDetailViewModel. Результат: $viewModel")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val list by viewModel.project.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val desktopAddress by viewModel.desktopAddress.collectAsStateWithLifecycle()

    
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    val suggestions by viewModel.autocompleteSuggestions.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(navController, lifecycleOwner) {
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("refresh_needed")?.observe(lifecycleOwner) { isRefreshNeeded ->
            if (isRefreshNeeded) {
                Log.d("CUSTOM_LIST_DEBUG", "'refresh_needed' event received")
                viewModel.forceRefresh()
                navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("refresh_needed")
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val inboxListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(value = false) }

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
        FullScreenTextEditor(
            initialText = recordToEdit!!.text,
            onSave = { newText -> viewModel.inboxHandler.onInboxRecordEditConfirm(newText) },
            onCancel = { viewModel.inboxHandler.onInboxRecordEditDismiss() },
            title = "Редагувати запис",
        )
    } else {
        if (uiState.showCreateCustomListDialog) {
            CreateCustomListDialog(
                onDismiss = { viewModel.onDismissCreateCustomListDialog() },
                onConfirm = { title -> viewModel.onCreateCustomList(title) }
            )
        }

        if (uiState.showExportTransferDialog) {
            val transferUrl =
                remember(desktopAddress) {
                    val ip = desktopAddress
                    if (ip.isNotBlank() && !ip.startsWith("http")) {
                        "http://$ip:8000"
                    } else {
                        ip
                    }
                }

            ExportTransferDialog(
                onDismiss = { viewModel.onExportTransferDialogDismiss() },
                onCopyToClipboard = { viewModel.onCopyToClipboardRequest() },
                onTransfer = { url -> viewModel.onTransferBacklogViaWifi(url) },
                desktopUrl = transferUrl,
                transferStatus = uiState.transferStatus,
            )
        }

        val dragDropState =
            rememberSimpleDragDropState(
                lazyListState = listState,
                onMove = viewModel::moveItem,
            )

        val draggableItems =
            remember(listContent) {
                listContent.filterNot { it is ListItemContent.LinkItem }
            }

        GoalDetailEffects(
            navController = navController,
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            listState = listState,
            inboxListState = inboxListState,
            dragDropState = dragDropState,
            coroutineScope = coroutineScope,
        )

        GoalDetailDialogs(viewModel = viewModel)

        
        
        
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
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AdaptiveTopBar(
                    isSelectionModeActive = isSelectionModeActive,
                    project = list,
                    selectedCount = uiState.selectedItemIds.size,
                    areAllSelected = draggableItems.isNotEmpty() && (uiState.selectedItemIds.size == draggableItems.size),
                    onClearSelection = { viewModel.selectionHandler.clearSelection() },
                    onSelectAll = { viewModel.selectionHandler.selectAllItems() },
                    onDelete = { viewModel.selectionHandler.deleteSelectedItems(uiState.selectedItemIds) },
                    onMoreActions = { actionType -> viewModel.selectionHandler.onBulkActionRequest(actionType, uiState.selectedItemIds) },
                    onMarkAsComplete = { viewModel.selectionHandler.markSelectedAsComplete(uiState.selectedItemIds) },
                    onMarkAsIncomplete = { viewModel.selectionHandler.markSelectedAsIncomplete(uiState.selectedItemIds) },
                    currentViewMode = uiState.currentView,
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
                        onValueChange = {
                            viewModel.inputHandler.onInputTextChanged(
                                it,
                                uiState.inputMode,
                            )
                        },
                        onSubmit = {
                            viewModel.inputHandler.submitInput(
                                uiState.inputValue,
                                uiState.inputMode,
                            )
                        },
                        onInputModeSelected = {
                            viewModel.inputHandler.onInputModeSelected(
                                it,
                                uiState.inputValue,
                            )
                        },
                        onRecentsClick = { viewModel.inputHandler.onShowRecentLists() },
                        onAddListLinkClick = { viewModel.inputHandler.onAddListLinkRequest() },
                        onShowAddWebLinkDialog = { viewModel.inputHandler.onShowAddWebLinkDialog() },
                        onShowAddObsidianLinkDialog = { viewModel.inputHandler.onShowAddObsidianLinkDialog() },
                        onAddListShortcutClick = { viewModel.inputHandler.onAddListShortcutRequest() },
                        
                        canGoBack = canGoBack,
                        canGoForward = canGoForward,
                        onBackClick = { viewModel.onBackPressed() },
                        onForwardClick = { viewModel.onForwardPressed() },
                        onHomeClick = viewModel::onHomeClick,
                        isAttachmentsExpanded = list?.isAttachmentsExpanded ?: false,
                        onToggleAttachments = { viewModel.toggleAttachmentsVisibility() },
                        onEditList = {
                            Log.d("EDIT_PROJECT_DEBUG", "LIST EDITING")
                            menuExpanded = false
                            Log.d("EDIT_PROJECT_DEBUG", "List id: ${list?.id}")
                            navController.navigate("edit_list_screen/${list?.id}")
                        },
                        onShareList = { viewModel.onExportBacklogToMarkdownRequest() },
                        onDeleteList = { viewModel.deleteCurrentProject() },
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
                        modifier = Modifier.navigationBarsPadding().imePadding(),
                        onToggleProjectManagement = viewModel::onToggleProjectManagement,
                        onExportProjectState = viewModel::onExportProjectStateRequest,
                        onAddProjectToDayPlan = viewModel::addCurrentProjectToDayPlan,
                        onRevealInExplorer = { viewModel.onRevealInExplorer(list?.id ?: "") },
                        onCloseSearch = viewModel::onCloseSearch,
                        isViewModePanelVisible = uiState.isViewModePanelVisible,
                        onToggleNavPanelMode = viewModel::onToggleNavPanelMode,
                        suggestions = suggestions,
                        onSuggestionClick = viewModel::onSuggestionClick,
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
                dragDropState = dragDropState,
            )
        }
    }
}
