package com.romankozak.forwardappmobile.ui.screens.backlog.components.dnd

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.backlog.components.SwipeableListItem


@Composable
fun InteractiveListItem(
    item: ListItemContent,
    index: Int,
    dragDropState: SimpleDragDropState,
    isSelected: Boolean, // Додано для визначення фону
    isHighlighted: Boolean, // Додано для визначення фону

    // Параметри для SwipeableListItem
    swipeEnabled: Boolean,
    isAnotherItemSwiped: Boolean,
    resetTrigger: Int,
    // backgroundColor: Color, // <-- ВИДАЛЕНО, будемо розраховувати тут
    onSwipeStart: () -> Unit,
    onDelete: () -> Unit,
    onMoreActionsRequest: () -> Unit,
    onCreateInstanceRequest: () -> Unit,
    onMoveInstanceRequest: () -> Unit,
    onCopyGoalRequest: () -> Unit,
    onGoalTransportRequest: () -> Unit,
    onCopyContentRequest: () -> Unit,

    // Контент
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

    // --- ПОЧАТОК КЛЮЧОВИХ ЗМІН ---
    // 1. Визначаємо, чи елемент завершено
    val isCompleted = when (item) {
        is ListItemContent.GoalItem -> item.goal.completed
        is ListItemContent.SublistItem -> item.sublist.isCompleted
        else -> false
    }

    // 2. Розраховуємо колір фону тут, на основі всіх станів
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHighlighted -> MaterialTheme.colorScheme.tertiaryContainer
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isCompleted -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = spring(), label = "interactive_item_background"
    )
    // --- КІНЕЦЬ КЛЮЧОВИХ ЗМІН ---


    val isDraggable = item is ListItemContent.GoalItem || item is ListItemContent.SublistItem

    val itemModifier = modifier
        .pointerInput(dragDropState, item.item.id, isDraggable) {
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
        }
        .graphicsLayer {
            val offset = dragDropState.getItemOffset(item)
            translationY = offset
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
            shadowElevation = elevation
            clip = false
        }

    Box(modifier = itemModifier) {
        SwipeableListItem(
            isDragging = isDragging,
            isAnyItemDragging = dragDropState.isDragging,
            swipeEnabled = swipeEnabled,
            isAnotherItemSwiped = isAnotherItemSwiped,
            resetTrigger = resetTrigger,
            onSwipeStart = onSwipeStart,
            onDelete = onDelete,
            onMoreActionsRequest = onMoreActionsRequest,
            onGoalTransportRequest = onGoalTransportRequest,
            onCopyContentRequest = onCopyContentRequest,
            backgroundColor = backgroundColor, // <-- Передаємо наш розрахований колір
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        content(isDragging)
                    }
                    // Ручка тепер завжди тут, і фон під нею буде правильним
                    if (isDraggable) {
                        DragHandleIcon()
                    }
                }
            },
        )
        val isTarget = dragDropState.isDragging &&
                dragDropState.targetIndexOfDraggedItem == index &&
                dragDropState.initialIndexOfDraggedItem != index

        if (isTarget) {
            val isDraggingDown = dragDropState.initialIndexOfDraggedItem < dragDropState.targetIndexOfDraggedItem
            val align = if (isDraggingDown) Alignment.BottomCenter else Alignment.TopCenter

            Box(modifier = Modifier.align(align)) {
                //DropIndicator(isValidDrop = true)
            }
        }
    }
}

// ... (решта файлу без змін) ...
@Composable
private fun DragHandleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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