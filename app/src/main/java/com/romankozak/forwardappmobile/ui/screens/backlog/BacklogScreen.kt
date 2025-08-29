@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.backlog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.components.*
import com.romankozak.forwardappmobile.ui.dialogs.GoalActionChoiceDialog
import com.romankozak.forwardappmobile.ui.screens.backlog.components.*
import com.romankozak.forwardappmobile.ui.screens.backlog.dialogs.AddLinkDialog
import com.romankozak.forwardappmobile.ui.shared.NavigationResultViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking


@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val draggableItems by viewModel.draggableItems.collectAsStateWithLifecycle()
    val attachmentItems by viewModel.attachmentItems.collectAsStateWithLifecycle()

    val list by viewModel.goalList.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val showRecentListsSheet by viewModel.showRecentListsSheet.collectAsStateWithLifecycle()
    val recentLists by viewModel.recentLists.collectAsStateWithLifecycle()
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val goalActionState by viewModel.goalActionDialogState.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()
    val currentListContextEmojiToHide by viewModel.currentListContextEmojiToHide.collectAsStateWithLifecycle()

    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        onMove = { from, to -> viewModel.moveItem(from, to) }
    )

    LaunchedEffect(uiState.needsStateRefresh) {
        if (uiState.needsStateRefresh) {
            dragDropState.resetState()
            viewModel.onStateRefreshed()
        }
    }


    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val navGraphEntry = remember(currentBackStackEntry) {
        navController.getBackStackEntry("app_graph")
    }
    val resultViewModel: NavigationResultViewModel = viewModel(navGraphEntry)

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            resultViewModel.consumeResult<String>("picked_goal_id")?.let {
                viewModel.onExistingItemSelected(it)
            }
            resultViewModel.consumeResult<String>("selectedListId")?.let {
                viewModel.onListChooserResult(it)
            }
            if (resultViewModel.consumeResult<Boolean>("refresh_needed") == true) {
                viewModel.forceRefresh()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.Navigate -> navController.navigate(event.route)
                is UiEvent.ShowSnackbar -> {
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.action,
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete()
                        }
                    }
                }
                is UiEvent.NavigateBackAndReveal -> {
                    navController.getBackStackEntry("goal_lists_screen")
                        .savedStateHandle["list_to_reveal"] = event.listId
                    navController.popBackStack("goal_lists_screen", inclusive = false)
                }
                is UiEvent.HandleLinkClick -> {
                    handleRelatedLinkClick(event.link, obsidianVaultName, localContext, navController)
                }
                is UiEvent.ResetSwipeState -> viewModel.onSwipeStateReset(event.itemId)
                is UiEvent.ScrollTo -> listState.animateScrollToItem(event.index)
            }
        }
    }

    LaunchedEffect(uiState.goalToHighlight, uiState.itemToHighlight, draggableItems, attachmentItems, list?.isAttachmentsExpanded) {
        val goalId = uiState.goalToHighlight
        val itemId = uiState.itemToHighlight

        if ((goalId == null && itemId == null) || (draggableItems.isEmpty() && attachmentItems.isEmpty())) {
            return@LaunchedEffect
        }

        val headerItemsCount = 1
        val expandedAttachmentsCount = if (list?.isAttachmentsExpanded == true) attachmentItems.size else 0

        val indexToScroll = when {
            goalId != null -> draggableItems
                .indexOfFirst { it is ListItemContent.GoalItem && it.goal.id == goalId }
                .takeIf { it != -1 }
                ?.let { it + headerItemsCount + expandedAttachmentsCount }

            itemId != null -> {
                val attachmentIndex = attachmentItems.indexOfFirst { it.item.id == itemId }
                if (attachmentIndex != -1 && list?.isAttachmentsExpanded == true) {
                    headerItemsCount + attachmentIndex
                } else {
                    draggableItems
                        .indexOfFirst { it.item.id == itemId }
                        .takeIf { it != -1 }
                        ?.let { it + headerItemsCount + expandedAttachmentsCount }
                }
            }
            else -> null
        }

        if (indexToScroll != null) {
            listState.animateScrollToItem(indexToScroll)
            delay(2500L)
        }
        viewModel.onHighlightShown()
    }

    LaunchedEffect(uiState.newlyAddedItemId, list?.isAttachmentsExpanded) {
        val newItemId = uiState.newlyAddedItemId
        if (newItemId != null) {
            val attachmentIndex = attachmentItems.indexOfFirst { it.item.id == newItemId }
            if (attachmentIndex != -1 && list?.isAttachmentsExpanded == true) {
                listState.animateScrollToItem(1)
            } else {
                val draggableIndex = draggableItems.indexOfFirst { it.item.id == newItemId }
                if (draggableIndex != -1) {
                    val headerSize = 1 + if (list?.isAttachmentsExpanded == true) attachmentItems.size else 0
                    listState.animateScrollToItem(headerSize + draggableIndex)
                }
            }
            viewModel.onScrolledToNewItem()
        }
    }

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelection()
    }

    BackHandler(enabled = !isSelectionModeActive) {
        viewModel.flushPendingMoves() // Зберігаємо перед виходом
        navController.popBackStack()
    }

    // Також можете додати DisposableEffect для збереження при закритті екрану
    DisposableEffect(Unit) {
        onDispose {
            viewModel.flushPendingMoves()
        }
    }

    //val listVersion by viewModel.listVersion.collectAsStateWithLifecycle()


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isSelectionModeActive) {
                MultiSelectTopAppBar(
                    selectedCount = uiState.selectedItemIds.size,
                    areAllSelected = draggableItems.isNotEmpty() && (uiState.selectedItemIds.size == draggableItems.size),
                    onClearSelection = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAllItems() },
                    onDelete = { viewModel.deleteSelectedItems() },
                    onToggleComplete = { viewModel.toggleCompletionForSelectedGoals() },
                    onMoreActions = { actionType -> viewModel.onBulkActionRequest(actionType) },
                )
            } else {
                TopAppBar(
                    title = { Text(list?.name ?: stringResource(R.string.loading)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button_description))
                        }
                    },
                    actions = {
                        list?.let {
                            IconButton(onClick = { viewModel.toggleAttachmentsVisibility() }) {
                                Icon(
                                    imageVector = Icons.Default.Attachment,
                                    contentDescription = "Додатки",
                                    tint = if (it.isAttachmentsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.onRevealInExplorer(list?.id ?: "") }) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = stringResource(R.string.reveal_in_backlogs),
                            )
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = !isSelectionModeActive,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                GoalInputBar(
                    inputValue = uiState.inputValue,
                    inputMode = uiState.inputMode,
                    onValueChange = { viewModel.onInputTextChanged(it) },
                    onSubmit = { viewModel.submitInput() },
                    onInputModeSelected = { viewModel.onInputModeSelected(it) },
                    onRecentsClick = { viewModel.onShowRecentLists() },
                    onAddListLinkClick = { viewModel.onAddListLinkRequest() },
                    onShowAddWebLinkDialog = { viewModel.onShowAddWebLinkDialog() },
                    onShowAddObsidianLinkDialog = { viewModel.onShowAddObsidianLinkDialog() },
                    onAddListShortcutClick = { viewModel.onAddListShortcutRequest() },
                    modifier = Modifier.navigationBarsPadding().imePadding()
                )
            }
        },
    ) { paddingValues ->
        // У файлі BacklogScreen.kt
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),

            ) {
            item(key = "attachments_section") {
                list?.let {
                    AttachmentsSection(
                        attachments = attachmentItems,
                        isExpanded = it.isAttachmentsExpanded,
                        onAddAttachment = { type ->
                            when (type) {
                                AttachmentType.NOTE -> viewModel.onAddNewNoteRequested()
                                AttachmentType.WEB_LINK -> viewModel.onShowAddWebLinkDialog()
                                AttachmentType.OBSIDIAN_LINK -> viewModel.onShowAddObsidianLinkDialog()
                                AttachmentType.LIST_LINK -> viewModel.onAddListLinkRequest()
                            }
                        },
                        onDeleteItem = { item -> viewModel.deleteItem(item) },
                        onItemClick = { item -> viewModel.onItemClick(item) }
                    )
                }
            }

            // --- УВАГА: УСЯ МАГІЯ ВІДБУВАЄТЬСЯ ТУТ ---

            // 1. Переконайтеся, що тут саме `itemsIndexed`, а не `items`.
            itemsIndexed(
                items = draggableItems,
                key = { index, item -> "${item.item.id}_$index" } // Include index in key
            ) { index, content -> // 2. Переконайтеся, що ви отримуєте `index` як перший параметр.

                val isHighlighted = (uiState.itemToHighlight == content.item.id) ||
                        (content is ListItemContent.GoalItem && content.goal.id == uiState.goalToHighlight)

                val backgroundColor by animateColorAsState(
                    targetValue = when {
                        isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                        content.item.id in uiState.selectedItemIds -> MaterialTheme.colorScheme.primaryContainer
                        (content as? ListItemContent.GoalItem)?.goal?.completed == true -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "bgAnim"
                )

                InteractiveListItem(
                    item = content,
                    index = index, // 3. Переконайтеся, що ви передаєте `index` сюди.
                    dragDropState = dragDropState.apply { this.itemsProvider = { draggableItems } },
                    swipeEnabled = !isSelectionModeActive && !dragDropState.isDragging,
                    isAnotherItemSwiped = (uiState.swipedItemId != null) && (uiState.swipedItemId != content.item.id),
                    resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                    backgroundColor = backgroundColor,
                    onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                    onDelete = { viewModel.deleteItem(content) },
                    onMoreActionsRequest = { viewModel.onGoalActionInitiated(content) },
                    onCreateInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.CreateInstance, content) },
                    onMoveInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.MoveInstance, content) },
                    onCopyGoalRequest = { viewModel.onGoalActionSelected(GoalActionType.CopyGoal, content) },
                    modifier = Modifier
                ) {
                    when (content) {
                        is ListItemContent.GoalItem -> {
                            GoalItem(
                                goal = content.goal,
                                obsidianVaultName = obsidianVaultName,
                                onToggle = { isChecked -> viewModel.toggleGoalCompletedWithState(content.goal, isChecked) },
                                onItemClick = { viewModel.onItemClick(content) },
                                onLongClick = { viewModel.onItemLongClick(content.item.id) },
                                onTagClick = { tag -> viewModel.onTagClicked(tag) },
                                onRelatedLinkClick = { link -> viewModel.onLinkItemClick(link) },
                                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                                emojiToHide = currentListContextEmojiToHide
                            )
                        }
                        is ListItemContent.SublistItem -> {
                            SublistItemRow(
                                sublistContent = content,
                                isSelected = content.item.id in uiState.selectedItemIds,
                                isHighlighted = isHighlighted,
                                onClick = { viewModel.onItemClick(content) },
                                onLongClick = { viewModel.onItemLongClick(content.item.id) }
                            )
                        }
                        else -> {
                            Log.w("BacklogScreen", "Unsupported draggable content type: ${content::class.simpleName}")
                        }
                    }
                }
            }
        }

        RecentListsSheet(
            showSheet = showRecentListsSheet,
            recentLists = recentLists,
            onDismiss = { viewModel.onDismissRecentLists() },
            onListClick = { listId -> viewModel.onRecentListSelected(listId) },
        )

        when (val state = goalActionState) {
            is GoalActionDialogState.Hidden -> {}
            is GoalActionDialogState.AwaitingActionChoice -> {
                GoalActionChoiceDialog(
                    itemContent = state.itemContent,
                    onDismiss = { viewModel.onDismissGoalActionDialogs() },
                    onActionSelected = { actionType ->
                        viewModel.onGoalActionSelected(actionType, state.itemContent)
                    },
                )
            }
        }

        if (uiState.showAddWebLinkDialog) {
            AddLinkDialog(
                title = stringResource(R.string.add_link_dialog_title),
                namePlaceholder = stringResource(R.string.dialog_placeholder_name_optional),
                targetPlaceholder = stringResource(R.string.url_placeholder),
                onDismiss = { viewModel.onDismissLinkDialogs() },
                onConfirm = { name, url ->
                    viewModel.onAddWebLinkConfirm(url, name.takeIf { it.isNotBlank() })
                },
            )
        }

        if (uiState.showAddObsidianLinkDialog) {
            AddLinkDialog(
                title = stringResource(R.string.add_obsidian_link_dialog_title),
                namePlaceholder = stringResource(R.string.dialog_placeholder_note_name),
                isTargetVisible = false,
                onDismiss = { viewModel.onDismissLinkDialogs() },
                onConfirm = { name, _ ->
                    viewModel.onAddObsidianLinkConfirm(name)
                },
            )
        }
    }


}

private fun handleRelatedLinkClick(
    link: RelatedLink,
    obsidianVaultName: String,
    context: Context,
    navController: NavController,
) {
    try {
        when (link.type) {
            LinkType.URL -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.target))
                context.startActivity(intent)
            }
            LinkType.GOAL_LIST -> {
                navController.navigate("goal_detail_screen/${link.target}")
            }
            LinkType.OBSIDIAN -> {
                if (obsidianVaultName.isNotBlank()) {
                    val encodedVault = URLEncoder.encode(obsidianVaultName, "UTF-8")
                    val encodedFile = URLEncoder.encode(link.target, "UTF-8")
                    val obsidianUri = "obsidian://open?vault=$encodedVault&file=$encodedFile"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(obsidianUri))
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, context.getString(R.string.error_obsidian_vault_not_set), Toast.LENGTH_LONG).show()
                }
            }
            LinkType.NOTE -> { /* TODO */ }
        }
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.error_link_open_failed), Toast.LENGTH_LONG).show()
    }
}
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (ListItemContent, ListItemContent) -> Unit
): DragDropState {
    val scope = rememberCoroutineScope()
    // ЗМІНА: Ключ для remember тепер lazyListState, щоб уникнути зайвих рекомпозицій
    return remember(lazyListState) { DragDropState(state = lazyListState, scope = scope, onMove = onMove) }
}

// ... (код екрану та функція rememberDragDropState залишаються без змін) ...

// --- ЗАМІНІТЬ ПОВНІСТЮ ЦЕЙ КЛАС ---
class DragDropState(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (ListItemContent, ListItemContent) -> Unit
) {
    lateinit var itemsProvider: () -> List<ListItemContent>

    private var draggedDistance by mutableStateOf(0f)
    var draggedItem by mutableStateOf<ListItemContent?>(null)
        private set
    private var draggedItemInfo by mutableStateOf<LazyListItemInfo?>(null)

    var initialIndexOfDraggedItem by mutableStateOf<Int?>(null)
    var targetIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set

    private var overscrollJob by mutableStateOf<Job?>(null)

    val isDragging: Boolean get() = draggedItem != null

    fun onDragStart(item: ListItemContent) {
        if (isDragging) return
        val index = itemsProvider().indexOf(item)
        if (index == -1) return

        draggedItem = item
        initialIndexOfDraggedItem = index
        targetIndexOfDraggedItem = index
        draggedItemInfo = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }

    fun onDrag(offset: Float) {
        if (!isDragging) return
        draggedDistance += offset

        val initialInfo = draggedItemInfo ?: return
        val initialIndex = initialIndexOfDraggedItem ?: return
        val currentTargetIndex = targetIndexOfDraggedItem ?: return

        val draggedItemTop = initialInfo.offset + draggedDistance
        var newTargetIndex = currentTargetIndex

        // Нова, більш надійна логіка визначення цілі
        if (draggedDistance > 0) { // Рух вниз
            state.layoutInfo.visibleItemsInfo
                .firstOrNull {
                    it.index > currentTargetIndex && draggedItemTop + initialInfo.size > it.offset + it.size / 2
                }
                ?.also { newTargetIndex = it.index }
        } else if (draggedDistance < 0) { // Рух вгору
            state.layoutInfo.visibleItemsInfo
                .lastOrNull {
                    it.index < currentTargetIndex && draggedItemTop < it.offset + it.size / 2
                }
                ?.also { newTargetIndex = it.index }
        }

        if (newTargetIndex != currentTargetIndex) {
            val fromItem = itemsProvider().getOrNull(initialIndex)
            val toItem = itemsProvider().getOrNull(newTargetIndex)
            if (fromItem != null && toItem != null) {
                onMove(fromItem, toItem)
                targetIndexOfDraggedItem = newTargetIndex
            }
        }

        if (overscrollJob?.isActive != true) {
            checkForOverscroll()
        }
    }

    fun onDragEnd() {
        val fromIndex = initialIndexOfDraggedItem
        val toIndex = targetIndexOfDraggedItem

        if (fromIndex != null && toIndex != null && fromIndex != toIndex) {
            val fromItem = itemsProvider().getOrNull(fromIndex)
            val toItem = itemsProvider().getOrNull(toIndex)
            if (fromItem != null && toItem != null) {
                onMove(fromItem, toItem)
                // Не скидаємо стан тут - чекаємо на команду від ViewModel
                return
            }
        }
        resetState()
    }



    fun resetState() {
        draggedDistance = 0f
        draggedItem = null
        draggedItemInfo = null
        initialIndexOfDraggedItem = null
        targetIndexOfDraggedItem = null
        overscrollJob?.cancel()
    }
    fun getOffset(item: ListItemContent): Float {
        if (!isDragging) return 0f

        val initialIndex = initialIndexOfDraggedItem ?: return 0f
        val targetIndex = targetIndexOfDraggedItem ?: return 0f
        val itemIndex = itemsProvider().indexOf(item)

        val draggedItemSize = draggedItemInfo?.size?.toFloat() ?: 0f

        return when {
            itemIndex == initialIndex -> draggedDistance
            itemIndex > initialIndex && itemIndex <= targetIndex -> -draggedItemSize
            itemIndex < initialIndex && itemIndex >= targetIndex -> draggedItemSize
            else -> 0f
        }
    }

    fun canDrag(item: ListItemContent): Boolean {
        return !isDragging
    }

    private fun checkForOverscroll() {
        if (overscrollJob?.isActive == true) return
        overscrollJob = scope.launch {
            while (isDragging) {
                val initialElement = draggedItemInfo ?: break
                val viewportHeight = state.layoutInfo.viewportSize.height
                val draggedItemTop = initialElement.offset + draggedDistance
                val draggedItemBottom = draggedItemTop + initialElement.size

                val scrollAmount = when {
                    draggedItemBottom > viewportHeight - 200 -> 20f
                    draggedItemTop < 200 -> -20f
                    else -> 0f
                }

                if (scrollAmount != 0f) {
                    state.scrollBy(scrollAmount)
                }
                delay(16)
            }
        }
    }
}