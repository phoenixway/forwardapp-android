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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import androidx.compose.foundation.lazy.LazyListItemInfo
import kotlinx.coroutines.Job

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
        onMove = { from, to -> viewModel.moveItem(from, to) },
        itemsProvider = { draggableItems } // Надаємо доступ до списку
    )
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .dragContainer(dragDropState),
            // Додано пропущену кому
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
                        onItemClick = { item -> viewModel.onItemClick(item) },
                        // Додано пропущену кому
                    )
                }
            }

            items(
                items = draggableItems,
                key = { item -> item.item.id },
                // Додано пропущену кому
            ) { content ->
                val isHighlighted = (uiState.itemToHighlight == content.item.id) ||
                        (content is ListItemContent.GoalItem && content.goal.id == uiState.goalToHighlight)
                // Дужки для читабельності не обов’язкові, але логічно згруповано за змістом

                val backgroundColor by animateColorAsState(
                    targetValue = when {
                        isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                        content.item.id in uiState.selectedItemIds -> MaterialTheme.colorScheme.primaryContainer
                        (content as? ListItemContent.GoalItem)?.goal?.completed == true ->
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "bgAnim",
                    // Додано пропущену кому
                )

                // --- CORRECTED COMPONENT CALL ---
                InteractiveListItem(
                    item = content,
                    //isAnythingDragging = dragDropState.isDragging,
                    dragDropState = dragDropState,
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
                    modifier = Modifier,
                    // Якщо animateItemPlacement() не вирішується — перевірте, чи підключено:
                    // import androidx.compose.foundation.gestures.animateItemPlacement
                    // Або, можливо, ви мали на увазі: Modifier.animateItemChange() або використовується бібліотека, наприклад, Accompanist?
                ){
                    // Контент, який буде відображено всередині
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
            }}

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
    onMove: (ListItemContent, ListItemContent) -> Unit,
    itemsProvider: () -> List<ListItemContent>
): DragDropState {
    val scope = rememberCoroutineScope()
    return remember(lazyListState) {
        DragDropState(
            state = lazyListState,
            scope = scope,
            onMove = onMove,
            itemsProvider = itemsProvider
        )
    }
}

// Вставте цей код в кінець файлу BacklogScreen.kt, замінивши старий клас DragDropState

class DragDropState(
    private val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (ListItemContent, ListItemContent) -> Unit,
    private val itemsProvider: () -> List<ListItemContent>
) {
    private var draggedDistance by mutableStateOf(0f)

    // --- ЗМІНА 1: Зробіть ці поля публічними (приберіть 'private') ---
    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)
    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

    private var overscrollJob by mutableStateOf<Job?>(null)

    val isDragging: Boolean get() = initiallyDraggedElement != null

    // --- ЗМІНА 2: Додайте ці 3 нові властивості ---
    val initialDraggedItemIndex: Int? get() = initiallyDraggedElement?.index
    val draggedItem: ListItemContent? get() = currentIndexOfDraggedItem?.let { currentItems.getOrNull(it) }
    fun getItemIndex(item: ListItemContent): Int = currentItems.indexOf(item)

    private val currentItems: List<ListItemContent> get() = itemsProvider()
    fun onDragStart(item: ListItemContent) {
        val index = currentItems.indexOf(item)
        if (index == -1) return

        currentIndexOfDraggedItem = index
        initiallyDraggedElement = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }

    fun onDrag(offset: Float) {
        if (!isDragging) return
        draggedDistance += offset

        val initialOffset = initiallyDraggedElement?.offset ?: return
        val currentOffset = initialOffset + draggedDistance

        val draggedItemIndex = currentIndexOfDraggedItem ?: return
        val draggedItem = currentItems[draggedItemIndex]

        val hoveredItem = state.layoutInfo.visibleItemsInfo
            .filterNot { it.key == draggedItem.item.id }
            .find {
                val start = it.offset
                val end = it.offset + it.size
                val center = start + (end - start) / 2
                // Перевіряємо, чи перетнув центр перетягуваного елемента центр цільового
                if (draggedDistance > 0) { // Рух вниз
                    currentOffset + (initiallyDraggedElement?.size ?: 0) / 2 > center
                } else { // Рух вгору
                    currentOffset + (initiallyDraggedElement?.size ?: 0) / 2 < center
                }
            }

        if (hoveredItem != null) {
            val fromIndex = currentIndexOfDraggedItem ?: return
            val toIndex = hoveredItem.index

            // Запобігаємо виклику, якщо індекси однакові
            if (fromIndex != toIndex) {
                val fromItem = currentItems.getOrNull(fromIndex)
                val toItem = currentItems.getOrNull(toIndex)

                if (fromItem != null && toItem != null && fromItem.item.id != toItem.item.id) {
                    onMove(fromItem, toItem)
                    // Оновлюємо стан ПІСЛЯ переміщення
                    currentIndexOfDraggedItem = toIndex
                    draggedDistance = 0f
                    initiallyDraggedElement = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == toIndex }
                }
            }
        }

        if (overscrollJob?.isActive != true) {
            checkForOverscroll()
        }
    }

    fun onDragEnd() {
        initiallyDraggedElement = null
        draggedDistance = 0f
        currentIndexOfDraggedItem = null

        overscrollJob?.cancel()
    }

    fun getOffset(item: ListItemContent): Float {
        if (!isDragging) return 0f
        val draggedInitialIndex = initiallyDraggedElement?.index ?: return 0f
        val currentDraggedIndex = currentIndexOfDraggedItem ?: return 0f
        val currentItemIndex = currentItems.indexOf(item)

        if (draggedInitialIndex == -1 || currentItemIndex == -1) return 0f

        val draggedItemSize = (initiallyDraggedElement?.size?.toFloat() ?: 0f)

        return when {
            currentItemIndex == draggedInitialIndex -> draggedDistance
            // Елемент був між старою і новою позицією, і ми його "проштовхнули"
            (currentItemIndex > draggedInitialIndex && currentItemIndex <= currentDraggedIndex) -> -draggedItemSize
            (currentItemIndex < draggedInitialIndex && currentItemIndex >= currentDraggedIndex) -> draggedItemSize
            else -> 0f
        }
    }

    fun canDrag(item: ListItemContent): Boolean {
        return !isDragging
    }

    private fun checkForOverscroll() {
        scope.launch {
            while (isDragging) {
                val draggedItemInfo = initiallyDraggedElement ?: break
                val listVisibleHeight = state.layoutInfo.viewportSize.height
                val draggedItemCenter = draggedItemInfo.offset + draggedDistance + draggedItemInfo.size / 2

                val scrollAmount = when {
                    draggedItemCenter > listVisibleHeight - 200 -> 20f
                    draggedItemCenter < 200 -> -20f
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

fun Modifier.dragContainer(dragDropState: DragDropState): Modifier {
    return this
}