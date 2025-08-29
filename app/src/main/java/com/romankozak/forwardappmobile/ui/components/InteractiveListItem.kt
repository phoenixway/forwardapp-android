package com.romankozak.forwardappmobile.ui.components

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.backlog.components.SwipeableListItem

@Composable
fun InteractiveListItem(
    // Параметри для Drag-and-Drop
    item: ListItemContent,
    dragDropState: DragDropState<ListItemContent>,

    // Параметри, які будуть прокинуті напряму у ваш SwipeableListItem
    isAnyItemDragging: Boolean,
    swipeEnabled: Boolean,
    isAnotherItemSwiped: Boolean,
    resetTrigger: Int,
    backgroundColor: Color,
    onSwipeStart: () -> Unit,
    onDelete: () -> Unit,
    onMoreActionsRequest: () -> Unit,
    onCreateInstanceRequest: () -> Unit,
    onMoveInstanceRequest: () -> Unit,
    onCopyGoalRequest: () -> Unit,
    recompositionTrigger: Float,


    // Контент (GoalItem, SublistItemRow)
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val isDragging = dragDropState.draggedItemKey == item.item.id

    val elevation by animateFloatAsState(if (isDragging) 8f else 0f, label = "elevation")

    Box(
        modifier = modifier
            // --- ОСНОВНЕ ВИПРАВЛЕННЯ ТУТ ---
            // 1. Модифікатор жестів перенесено на весь Box
            .pointerInput(dragDropState, item) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragDropState.onDragStart(item, item.item.id) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDropState.onDrag(dragAmount)
                        // Тепер `change.position` буде в правильній системі координат
                        dragDropState.onPointerMove(change.position)
                    },
                    onDragEnd = { dragDropState.onDragEnd() },
                    onDragCancel = { dragDropState.onDragCanceled() },
                )
            }
            .graphicsLayer { // Візуальні ефекти під час перетягування
                translationY = if (isDragging) dragDropState.draggedItemOffset.y else 0f
                scaleX = if (isDragging) 1.05f else 1f
                scaleY = if (isDragging) 1.05f else 1f
                alpha = if (isDragging) 0.9f else 1f
                shadowElevation = elevation
            }
            // 2. Цей блок тепер буде працювати, оскільки координати збігаються
            .onGloballyPositioned { layoutCoordinates ->
                val currentBounds = layoutCoordinates.boundsInWindow()
                val pointerPos = dragDropState.pointerPosition

                if (item.item.id != dragDropState.draggedItemKey) {
                    if (dragDropState.isDragging && currentBounds.contains(pointerPos)) {
                        dragDropState.targetItem = item
                    }
                }
            }
    ) {
        // Використовуємо ваш існуючий SwipeableListItem
        SwipeableListItem(
            isDragging = isDragging,
            isAnyItemDragging = isAnyItemDragging,
            swipeEnabled = swipeEnabled,
            isAnotherItemSwiped = isAnotherItemSwiped,
            resetTrigger = resetTrigger,
            onSwipeStart = onSwipeStart,
            onDelete = onDelete,
            onMoreActionsRequest = onMoreActionsRequest,
            onCreateInstanceRequest = onCreateInstanceRequest,
            onMoveInstanceRequest = onMoveInstanceRequest,
            onCopyGoalRequest = onCopyGoalRequest,
            backgroundColor = backgroundColor,
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        content(isDragging)
                    }
                    // 3. Іконка-ручка тепер лише візуальний елемент, без обробника жестів
                    DragHandleIcon()
                }
            }
        )
    }
}

@Composable
private fun DragHandleIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Перетягнути",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}