@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.goaldetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.components.GoalInputBar
import com.romankozak.forwardappmobile.ui.components.MultiSelectTopAppBar
import com.romankozak.forwardappmobile.ui.components.SwipeableGoalItem
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val list by viewModel.goalList.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler(enabled = isSelectionModeActive) {
        viewModel.clearSelection()
    }

    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState = listState) { from, to ->
        viewModel.moveItem(from.index, to.index)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    coroutineScope.launch {
                        val result = snackbarHostState.showSnackbar(message = event.message, actionLabel = event.action)
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDelete()
                        }
                    }
                }
                is UiEvent.Navigate -> navController.navigate(event.route)
                is UiEvent.NavigateBackAndReveal -> {
                    navController.previousBackStackEntry?.savedStateHandle?.set("list_to_reveal", event.listId)
                    navController.popBackStack()
                }
                is UiEvent.ScrollTo -> coroutineScope.launch { listState.animateScrollToItem(event.index) }
                is UiEvent.ResetSwipeState -> { /* Handled by Recomposition Keys */ }
            }
        }
    }

    LaunchedEffect(listContent.size) {
        uiState.newlyAddedItemId?.let { id ->
            val index = listContent.indexOfFirst { it.item.id == id }
            if (index != -1) {
                listState.animateScrollToItem(index)
                viewModel.onScrolledToNewItem()
            }
        }
    }

    Scaffold(
        modifier = Modifier.navigationBarsPadding().imePadding(),
        topBar = {
            if (isSelectionModeActive) {
                MultiSelectTopAppBar(
                    selectedCount = uiState.selectedItemIds.size,
                    areAllSelected = listContent.isNotEmpty() && (uiState.selectedItemIds.size == listContent.size),
                    onClearSelection = viewModel::clearSelection,
                    onSelectAll = viewModel::selectAllItems,
                    onDelete = viewModel::deleteSelectedItems,
                    onToggleComplete = viewModel::toggleCompletionForSelectedGoals,
                    onMoreActions = { viewModel.onBulkActionRequest(it) },
                )
            } else {
                TopAppBar(
                    title = { Text(list?.name ?: "Loading...", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { list?.id?.let { viewModel.onRevealInExplorer(it) } }) {
                            Icon(Icons.Default.LocationSearching, contentDescription = "Reveal in lists")
                        }
                    },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(visible = !isSelectionModeActive) {
                GoalInputBar(
                    inputValue = uiState.inputValue,
                    onValueChange = viewModel::onInputTextChanged,
                    onSubmit = viewModel::submitInput,
                    modifier = Modifier.fillMaxWidth(),
                    inputMode = uiState.inputMode,
                    onInputModeSelected = viewModel::onInputModeSelected,
                    onRecentsClick = viewModel::onShowRecentLists,
                    // currentMode = uiState.inputMode,
                    // onModeSelected = viewModel::onInputModeSelected,
                )
            }
        },
    ) { paddingValues ->
        when {
            list == null -> CenteredContent { CircularProgressIndicator() }
            listContent.isEmpty() && uiState.localSearchQuery.isBlank() -> CenteredContent { Text("This list has no items yet.") }
            listContent.isEmpty() && uiState.localSearchQuery.isNotBlank() -> CenteredContent { Text("No results found.") }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    itemsIndexed(items = listContent, key = { _, item -> item.item.id }) { index, content ->
                        ReorderableItem(reorderableLazyListState, key = content.item.id) { isDragging ->
                            val isSelected = content.item.id in uiState.selectedItemIds
                            val backgroundColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                                label = "item_selection_color",
                            )

                            when (content) {
                                is ListItemContent.GoalItem -> {
                                    SwipeableGoalItem(
                                        modifier = Modifier
                                            .padding(vertical = 2.dp, horizontal = 8.dp)
                                            .animateItem(),
                                        goal = content.goal,
                                        backgroundColor = backgroundColor,
                                        isDragging = isDragging,
                                        obsidianVaultName = obsidianVaultName,
                                        onDelete = { viewModel.deleteItem(content) },
                                        onToggle = { viewModel.toggleGoalCompleted(content.goal) },
                                        onItemClick = { viewModel.onItemClick(content) },
                                        onLongClick = { viewModel.onItemLongClick(content.item.id) },
                                        onTagClick = viewModel::onTagClicked,
                                        onRelatedLinkClick = { link -> handleRelatedLinkClick(link, context, navController) },
                                        resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                                        onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                                        isAnotherItemSwiped = uiState.swipedItemId != null && uiState.swipedItemId != content.item.id,
                                        onMoreActionsRequest = { viewModel.onGoalActionInitiated(content) },
                                        onCreateInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.CreateInstance, content) },
                                        onMoveInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.MoveInstance, content) },
                                        onCopyGoalRequest = { viewModel.onGoalActionSelected(GoalActionType.CopyGoal, content) },
                                        contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                                    )
                                }
                                is ListItemContent.NoteItem, is ListItemContent.SublistItem -> {
                                    val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
                                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .graphicsLayer { scaleX = scale; scaleY = scale }
                                            .shadow(elevation, RoundedCornerShape(12.dp))
                                            .animateItem(),
                                    ) {
                                        when (content) {
                                            is ListItemContent.NoteItem -> NoteItemRow(
                                                noteContent = content,
                                                isSelected = isSelected,
                                                onClick = { viewModel.onItemClick(content) },
                                                onLongClick = { viewModel.onItemLongClick(content.item.id) }
                                            )
                                            is ListItemContent.SublistItem -> SublistItemRow(
                                                sublistContent = content,
                                                isSelected = isSelected,
                                                onClick = { viewModel.onItemClick(content) },
                                                onLongClick = { viewModel.onItemLongClick(content.item.id) }
                                            )
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItemRow(
    modifier: Modifier = Modifier,
    noteContent: ListItemContent.NoteItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        label = "note_color",
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = noteContent.note.content,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SublistItemRow(
    modifier: Modifier = Modifier,
    sublistContent: ListItemContent.SublistItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        label = "sublist_color",
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.List, "Sublist", tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.width(16.dp))
            Text(
                text = sublistContent.sublist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun handleRelatedLinkClick(link: RelatedLink, context: Context, navController: NavController) {
    when (link.type) {
        LinkType.URL -> {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.target))
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
            }
        }
        LinkType.GOAL_LIST -> navController.navigate("goal_detail_screen/${link.target}")
        LinkType.NOTE -> { /* TODO: Implement navigation to a specific note */ }
        LinkType.OBSIDIAN -> { /* TODO: Implement obsidian link handling */ }
    }
}