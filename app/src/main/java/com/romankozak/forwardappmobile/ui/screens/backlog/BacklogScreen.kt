// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goaldetail/BacklogScreen.kt

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.backlog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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
import com.romankozak.forwardappmobile.ui.components.GoalInputBar
import com.romankozak.forwardappmobile.ui.components.MultiSelectTopAppBar
import com.romankozak.forwardappmobile.ui.components.RecentListsSheet
import com.romankozak.forwardappmobile.ui.components.listItemsRenderers.*
import com.romankozak.forwardappmobile.ui.dialogs.GoalActionChoiceDialog
import com.romankozak.forwardappmobile.ui.shared.NavigationResultViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.net.URLEncoder

@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val list by viewModel.goalList.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val showRecentListsSheet by viewModel.showRecentListsSheet.collectAsStateWithLifecycle()
    val recentLists by viewModel.recentLists.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    val emojiToHide by viewModel.currentListContextEmojiToHide.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val goalActionState by viewModel.goalActionDialogState.collectAsStateWithLifecycle()

    val listId = remember {
        navController.currentBackStackEntry?.arguments?.getString("listId") ?: ""
    }

    // Блок для отримання результатів від інших екранів (включно з list_chooser_screen)
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val navGraphEntry = remember(currentBackStackEntry) {
        navController.getBackStackEntry("app_graph")
    }
    val resultViewModel: NavigationResultViewModel = viewModel(navGraphEntry)

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Перевірка результату від вибору списку
            val selectedListId = resultViewModel.consumeResult<String>("selectedListId")
            if (selectedListId != null) {
                viewModel.onListChooserResult(selectedListId)
            }

            // Перевірка, чи потрібно оновити екран після редагування
            val needsRefresh = resultViewModel.consumeResult<Boolean>("refresh_needed")
            if (needsRefresh == true) {
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
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete()
                        }
                    }
                }
                is UiEvent.ScrollTo -> listState.animateScrollToItem(event.index)
                is UiEvent.NavigateBackAndReveal -> {
                    navController.getBackStackEntry("goal_lists_screen")
                        .savedStateHandle
                        .set("list_to_reveal", event.listId)
                    navController.popBackStack("goal_lists_screen", inclusive = false)
                }
                is UiEvent.HandleLinkClick -> {
                    handleRelatedLinkClick(event.link, obsidianVaultName, localContext, navController)
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(listContent) {
        val newItemId = uiState.newlyAddedItemId
        if (newItemId != null && listContent.firstOrNull()?.item?.id == newItemId) {
            coroutineScope.launch { listState.animateScrollToItem(0) }
            viewModel.onScrolledToNewItem()
        }
    }

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelection()
    }

    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
    ) { from, to ->
        viewModel.moveItem(from.index, to.index)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    val isAnyItemDragging = reorderableLazyListState.isAnyItemDragging

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        topBar = {
            if (isSelectionModeActive) {
                MultiSelectTopAppBar(
                    selectedCount = uiState.selectedItemIds.size,
                    areAllSelected = listContent.isNotEmpty() && uiState.selectedItemIds.size == listContent.size,
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
                        IconButton(onClick = { viewModel.onRevealInExplorer(listId) }) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = stringResource(R.string.reveal_in_backlogs)
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                AnimatedVisibility(
                    visible = !isSelectionModeActive,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
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
                        onShowAddObsidianLinkDialog = { viewModel.onShowAddObsidianLinkDialog() }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                itemsIndexed(listContent, key = { _, item -> item.item.id }) { index, content ->
                    ReorderableItem(reorderableLazyListState, key = content.item.id) { isDragging ->
                        val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                        val itemModifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(elevation, RoundedCornerShape(12.dp))

                        when (content) {
                            is ListItemContent.GoalItem -> {
                                val isSelected = content.item.id in uiState.selectedItemIds
                                SwipeableListItem(
                                    modifier = itemModifier,
                                    isDragging = isDragging,
                                    isAnyItemDragging = isAnyItemDragging,
                                    resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                                    backgroundColor = Color.Transparent, // Змінено
                                    onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                                    isAnotherItemSwiped = uiState.swipedItemId != null && uiState.swipedItemId != content.item.id,
                                    onDelete = { viewModel.deleteItem(content) },
                                    onMoreActionsRequest = { viewModel.onGoalActionInitiated(content) },
                                    onCreateInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.CreateInstance, content) },
                                    onMoveInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.MoveInstance, content) },
                                    onCopyGoalRequest = { viewModel.onGoalActionSelected(GoalActionType.CopyGoal, content) }
                                ) {
                                    GoalItem(
                                        goal = content.goal,
                                        isSelected = isSelected, // Додано
                                        obsidianVaultName = obsidianVaultName,
                                        onToggle = { viewModel.toggleGoalCompletedWithState(content.goal, it) },
                                        onItemClick = { viewModel.onItemClick(content) },
                                        onLongClick = { viewModel.onItemLongClick(content.item.id) },
                                        onTagClick = { viewModel.onTagClicked(it) },
                                        onRelatedLinkClick = {
                                            handleRelatedLinkClick(it, obsidianVaultName, localContext, navController)
                                        },
                                        dragHandleModifier = Modifier.longPressDraggableHandle(
                                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                        ),
                                        contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                                        emojiToHide = emojiToHide
                                    )
                                }
                            }
                            is ListItemContent.NoteItem -> {
                                val isSelected = content.item.id in uiState.selectedItemIds
                                SwipeableListItem(
                                    modifier = itemModifier,
                                    isDragging = isDragging,
                                    isAnyItemDragging = isAnyItemDragging,
                                    resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                                    backgroundColor = Color.Transparent, // Змінено
                                    onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                                    isAnotherItemSwiped = uiState.swipedItemId != null && uiState.swipedItemId != content.item.id,
                                    onDelete = { viewModel.deleteItem(content) },
                                    onMoreActionsRequest = { viewModel.onGoalActionInitiated(content) },
                                    onCreateInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.CreateInstance, content) },
                                    onMoveInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.MoveInstance, content) },
                                    onCopyGoalRequest = { viewModel.onGoalActionSelected(GoalActionType.CopyGoal, content) }
                                ) {
                                    NoteItemRow(
                                        noteContent = content,
                                        isSelected = isSelected,
                                        onClick = { viewModel.onItemClick(content) },
                                        onLongClick = { viewModel.onItemLongClick(content.item.id) },
                                        dragHandleModifier = Modifier.longPressDraggableHandle(
                                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                        )
                                    )
                                }
                            }
                            is ListItemContent.SublistItem -> {
                                SublistItemRow(
                                    modifier = itemModifier,
                                    sublistContent = content,
                                    isSelected = content.item.id in uiState.selectedItemIds,
                                    onClick = { viewModel.onItemClick(content) },
                                    onLongClick = { viewModel.onItemLongClick(content.item.id) }
                                )
                            }
                            is ListItemContent.LinkItem -> {
                                val isSelected = content.item.id in uiState.selectedItemIds
                                SwipeableListItem(
                                    modifier = itemModifier,
                                    isDragging = isDragging,
                                    isAnyItemDragging = isAnyItemDragging,
                                    resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                                    backgroundColor = Color.Transparent,
                                    onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                                    isAnotherItemSwiped = uiState.swipedItemId != null && uiState.swipedItemId != content.item.id,
                                    onDelete = { viewModel.deleteItem(content) },
                                    onMoreActionsRequest = { viewModel.onGoalActionInitiated(content) },
                                    onCreateInstanceRequest = { /* Not applicable for links */ },
                                    onMoveInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.MoveInstance, content) },
                                    onCopyGoalRequest = { /* Not applicable for links */ }
                                ) {
                                    LinkItemRow(
                                        link = content.link.linkData,
                                        isSelected = isSelected,
                                        onItemClick = { viewModel.onItemClick(content) },
                                        onLongClick = { viewModel.onItemLongClick(content.item.id) },
                                        dragHandleModifier = Modifier.longPressDraggableHandle(
                                            onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    RecentListsSheet(
        showSheet = showRecentListsSheet,
        recentLists = recentLists,
        onDismiss = { viewModel.onDismissRecentLists() },
        onListClick = { listId -> viewModel.onRecentListSelected(listId) }
    )

    when (val state = goalActionState) {
        is GoalActionDialogState.Hidden -> {}
        is GoalActionDialogState.AwaitingActionChoice -> {
            GoalActionChoiceDialog(
                onDismiss = { viewModel.onDismissGoalActionDialogs() },
                onActionSelected = { actionType ->
                    viewModel.onGoalActionSelected(actionType, state.itemContent)
                }
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
            }
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
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinkItemRow(
    link: RelatedLink,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier
) {
    val targetColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val background by animateColorAsState(
        targetValue = targetColor,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "link_bg_anim"
    )
    var isPressed by remember { mutableStateOf(false) }
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 1.dp,
        label = "link_elevation"
    )
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(200),
        label = "link_border_color_anim"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.elevatedCardElevation(elevation),
        border = BorderStroke(2.dp, animatedBorderColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(onItemClick, onLongClick) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                },
                                onLongPress = { onLongClick() },
                                onTap = { onItemClick() }
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (link.type) {
                            LinkType.GOAL_LIST -> Icons.AutoMirrored.Filled.ListAlt
                            LinkType.URL -> Icons.Default.Language
                            LinkType.OBSIDIAN -> Icons.AutoMirrored.Filled.Note
                            else -> Icons.Default.Link
                        },
                        contentDescription = stringResource(R.string.link_icon_description),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = link.displayName ?: link.target,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when (link.type) {
                                LinkType.GOAL_LIST -> stringResource(R.string.link_type_goal_list)
                                LinkType.URL -> link.target
                                LinkType.OBSIDIAN -> stringResource(R.string.link_type_obsidian_note)
                                else -> stringResource(R.string.link_type_generic)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                val dragHandleDescription = stringResource(R.string.drag_handle_description)

                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = 4.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.semantics { contentDescription = dragHandleDescription },
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = dragHandleModifier
                                    .size(24.dp)
                                    .padding(4.dp)
                                    .pointerInput(Unit) { detectTapGestures { } }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun handleRelatedLinkClick(
    link: RelatedLink,
    obsidianVaultName: String,
    context: Context,
    navController: NavController
) {
    try {
        when (link.type) {
            LinkType.URL -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.target))
                context.startActivity(intent)
            }
            LinkType.GOAL_LIST -> {
                navController.navigate("goal_detail_screen/${link.target}") {
                    popUpTo(navController.currentDestination!!.id) {
                        inclusive = true
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLinkDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, target: String) -> Unit,
    namePlaceholder: String = stringResource(R.string.dialog_placeholder_name),
    targetPlaceholder: String = stringResource(R.string.dialog_placeholder_address),
    isTargetVisible: Boolean = true
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    val isConfirmEnabled = if (isTargetVisible) name.isNotBlank() && target.isNotBlank() else name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(namePlaceholder) },
                    singleLine = true
                )
                if (isTargetVisible) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text(targetPlaceholder) },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, target) },
                enabled = isConfirmEnabled
            ) {
                Text(stringResource(R.string.add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}