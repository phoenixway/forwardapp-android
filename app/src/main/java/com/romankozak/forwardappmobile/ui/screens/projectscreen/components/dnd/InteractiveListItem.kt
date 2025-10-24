package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd

import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.SwipeableListItem
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.DraggableItem
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import com.romankozak.forwardappmobile.data.database.models.ListItemContent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable

@Composable
fun InteractiveListItem(
    item: ListItemContent,
    index: Int,
    dragDropState: SimpleDragDropState,
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
    val isCompleted =
        when (item) {
            is ListItemContent.GoalItem -> item.goal.completed
            is ListItemContent.SublistItem -> item.project.isCompleted
            else -> false
        }

    val backgroundColor by animateColorAsState(
        targetValue =
            when {
                isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.surface
            },
        animationSpec = spring(),
        label = "interactive_item_background",
    )

    val isDraggable = item is ListItemContent.GoalItem || item is ListItemContent.SublistItem

    DraggableItem(
        item = item,
        index = index,
        dragDropState = dragDropState,
        modifier = modifier,
    ) {
        key(swipeEnabled) {
            SwipeableListItem(
                isDragging = it,
                isAnyItemDragging = dragDropState.isDragging,
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
                            content(it)
                        }
                    }
                },
            )
        }


    }
}
