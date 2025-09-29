package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.SwipeableListItem

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
    onStartTrackingRequest: () -> Unit,
    onAddToDayPlanRequest: () -> Unit,
    onShowGoalTransportMenu: (ListItemContent) -> Unit,
    onCopyContentRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit,
) {
    val isDragging = dragDropState.draggedItemIndex == index

    val elevation by animateFloatAsState(
        targetValue = if (isDragging) 16f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "elevation",
    )
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
        label = "scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (isDragging) 0.8f else 1f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 500f),
        label = "alpha",
    )

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

    val itemModifier =
        modifier
            .pointerInput(dragDropState, item.listItem.id, isDraggable) {
                if (isDraggable) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { dragDropState.onDragStart(item) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDropState.onDrag(dragAmount.y)
                        },
                        onDragEnd = { dragDropState.onDragEnd() },
                        onDragCancel = { dragDropState.onDragEnd() },
                    )
                }
            }.graphicsLayer {
                val offset = dragDropState.getItemOffset(item)
                translationY = offset
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                shadowElevation = elevation
                clip = false
            }

    Box(modifier = itemModifier) {
        key(swipeEnabled) {
            SwipeableListItem(
                isDragging = isDragging,
                isAnyItemDragging = dragDropState.isDragging,
                swipeEnabled = swipeEnabled,
                isAnotherItemSwiped = isAnotherItemSwiped,
                resetTrigger = resetTrigger,
                onSwipeStart = onSwipeStart,
                onDelete = onDelete,
                onMoreActionsRequest = onMoreActionsRequest,
                onGoalTransportRequest = { onShowGoalTransportMenu(item) },
                onStartTrackingRequest = onStartTrackingRequest,
                onAddToDayPlanRequest = onAddToDayPlanRequest,
                onCopyContentRequest = onCopyContentRequest,
                backgroundColor = backgroundColor,
                content = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            content(isDragging)
                        }
                        if (isDraggable) {
                            DragHandleIcon()
                        }
                    }
                },
            )
        }
        val isTarget =
            dragDropState.isDragging &&
                dragDropState.targetIndexOfDraggedItem == index &&
                dragDropState.initialIndexOfDraggedItem != index

        if (isTarget) {
            val isDraggingDown = dragDropState.initialIndexOfDraggedItem < dragDropState.targetIndexOfDraggedItem
            val align = if (isDraggingDown) Alignment.BottomCenter else Alignment.TopCenter

            Box(modifier = Modifier.align(align)) {
            }
        }
    }
}

@Composable
private fun DragHandleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Перетягнути",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}
