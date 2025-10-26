package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import com.romankozak.forwardappmobile.ui.dnd.DragAndDropState
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.SwipeableListItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.InteractiveListItem(
    item: ListItemContent,
    index: Int,
    dragAndDropState: DragAndDropState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isSelected: Boolean,
    isHighlighted: Boolean,
    swipeEnabled: Boolean,
    isAnotherItemSwiped: Boolean,
    resetTrigger: Int,
    onSwipeStart: () -> Unit,
    onDelete: () -> Unit,
    onMoreActionsRequest: () -> Unit,
    onMoveToTopRequest: () -> Unit,
    onStartTrackingRequest: () -> Unit,
    onAddToDayPlanRequest: () -> Unit,
    onShowGoalTransportMenu: (ListItemContent) -> Unit,
    onCopyContentRequest: () -> Unit,
    onToggleCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit,
) {
    val isCompleted = when (item) {
        is ListItemContent.GoalItem -> item.goal.completed
        is ListItemContent.SublistItem -> item.project.isCompleted
        else -> false
    }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isCompleted -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(),
        label = "interactive_item_background",
    )

    val isDraggingThisItem = dragAndDropState.dragInProgress && dragAndDropState.draggedItemIndex == index

    val itemModifier = if (isDraggingThisItem) {
        modifier.graphicsLayer { alpha = 0f }  // Invisible, but occupies space
    } else {
        modifier.animateItem()
    }

    SwipeableListItem(
        modifier = itemModifier,
        isDragging = isDraggingThisItem,
        isAnyItemDragging = dragAndDropState.dragInProgress,
        swipeEnabled = swipeEnabled,
        isAnotherItemSwiped = isAnotherItemSwiped,
        resetTrigger = resetTrigger,
        onSwipeStart = onSwipeStart,
        onDelete = onDelete,
        onMoreActionsRequest = onMoreActionsRequest,
        onMoveToTopRequest = onMoveToTopRequest,
        onGoalTransportRequest = { onShowGoalTransportMenu(item) },
        onStartTrackingRequest = onStartTrackingRequest,
        onAddToDayPlanRequest = onAddToDayPlanRequest,
        onCopyContentRequest = onCopyContentRequest,
        onToggleCompleted = onToggleCompleted,
        backgroundColor = backgroundColor,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    content(isDraggingThisItem)
                }
            }
        },
    )
}