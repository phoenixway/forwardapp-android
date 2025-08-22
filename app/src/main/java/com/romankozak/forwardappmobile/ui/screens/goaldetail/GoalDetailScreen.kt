@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.goaldetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.components.listItemsRenderers.SwipeableGoalItem
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.lang.Integer.max
import java.lang.Integer.min

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
    val haptic = LocalHapticFeedback.current
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // --- Reorderable state ---
    val reorderableLazyListState = rememberReorderableLazyListState(
        lazyListState = listState,
        scrollThresholdPadding = WindowInsets.systemBars.asPaddingValues(),
    ) { from, to ->
        viewModel.moveGoal(from.index, to.index, afterApiUpdate = false)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        val scrollThreshold = 2
        coroutineScope.launch {
            val firstVisible = listState.firstVisibleItemIndex
            val lastVisible = firstVisible + listState.layoutInfo.visibleItemsInfo.size - 1

            if (to.index < (firstVisible + scrollThreshold)) {
                listState.animateScrollToItem(max(0, to.index - scrollThreshold))
            } else if (to.index > (lastVisible - scrollThreshold)) {
                listState.animateScrollToItem(
                    min(
                        listState.layoutInfo.totalItemsCount - 1,
                        to.index + scrollThreshold,
                    ),
                )
            }
        }
    }

    val isAnyItemDragging = reorderableLazyListState.isAnyItemDragging

    Scaffold(
        topBar = {
            if (isSelectionModeActive) {
                // твій MultiSelectTopAppBar
            } else {
                TopAppBar(
                    title = { Text(list?.name ?: "Loading...") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { /* GoalInputBar */ }
    ) { paddingValues ->

        LazyColumn(
            state = listState, // Використовуємо оригінальний listState
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            itemsIndexed(listContent, key = { _, item -> item.item.id }) { index, content ->
                ReorderableItem(reorderableLazyListState, key = content.item.id) { isDragging ->
                    // 1. Анімація для масштабування (збільшення)
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.05f else 1f,
                        label = "scale_animation"
                    )

                    // 2. Анімація для тіні (elevation)
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 8.dp else 0.dp,
                        label = "elevation_animation"
                    )
                    if (content is ListItemContent.GoalItem) {
                        SwipeableGoalItem(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .shadow(
                                    elevation = elevation,
                                    shape = RoundedCornerShape(12.dp) // Використовуйте заокруглення, яке відповідає вашому дизайну
                                ),
                            goalContent = content,
                            isAnyItemDragging = isAnyItemDragging,

                            isDragging = isDragging,
                            resetTrigger = uiState.resetTriggers[content.item.id] ?: 0,
                            obsidianVaultName = obsidianVaultName,
                            onToggle = { viewModel.toggleGoalCompletedWithState(content.goal, it) },
                            onItemClick = { viewModel.onItemClick(content) },
                            onLongClick = { viewModel.onItemLongClick(content.item.id) },
                            onDelete = { viewModel.deleteItem(content) },
                            onTagClick = { viewModel.onTagClicked(it) },
                            onRelatedLinkClick = { /* handle related link */ },
                            backgroundColor = MaterialTheme.colorScheme.surface,
                            onSwipeStart = { viewModel.onSwipeStart(content.item.id) },
                            isAnotherItemSwiped = uiState.swipedItemId != null && uiState.swipedItemId != content.item.id,
                            dragHandleModifier = if (!isSelectionModeActive) {
                                Modifier.draggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                            } else {
                                Modifier
                            },
                            onMoreActionsRequest = { viewModel.onGoalActionInitiated(content) },
                            onCreateInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.CreateInstance, content) },
                            onMoveInstanceRequest = { viewModel.onGoalActionSelected(GoalActionType.MoveInstance, content) },
                            onCopyGoalRequest = { viewModel.onGoalActionSelected(GoalActionType.CopyGoal, content) },
                            contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                        )
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