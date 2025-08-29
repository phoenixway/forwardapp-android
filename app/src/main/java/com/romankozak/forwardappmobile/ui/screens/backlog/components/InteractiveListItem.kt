package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent

@Composable
fun InteractiveListItem(
    item: ListItemContent,
    index: Int,
    dragDropState: SimpleDragDropState, // Змінено тип

    // Параметри для SwipeableListItem
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

    // Контент
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit,
) {
    val isDragging = dragDropState.draggedItemIndex == index

    // Використовуємо більш м'які анімації
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

    val itemModifier = modifier
        .pointerInput(dragDropState, item.item.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragDropState.onDragStart(item)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragDropState.onDrag(dragAmount.y)
                },
                onDragEnd = {
                    dragDropState.onDragEnd()
                },
                onDragCancel = {
                    dragDropState.onDragEnd()
                },
            )
        }
        .graphicsLayer {
            val offset = dragDropState.getItemOffset(item)
            translationY = offset
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
            shadowElevation = elevation

            // Додаткова стабілізація для великих зміщень
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
            onCreateInstanceRequest = onCreateInstanceRequest,
            onMoveInstanceRequest = onMoveInstanceRequest,
            onCopyGoalRequest = onCopyGoalRequest,
            backgroundColor = backgroundColor,
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        content(isDragging)
                    }
                    DragHandleIcon()
                }
            },
        )

        // Виправлена логіка показу індикатора
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

@Composable
private fun DropIndicator(isValidDrop: Boolean) {
    val color = if (isValidDrop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val infiniteTransition = rememberInfiniteTransition(label = "dropIndicatorPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dropIndicatorAlpha",
    )
    val height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dropIndicatorHeight",
    )
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isValidDrop) {
        haptic.performHapticFeedback(
            if (isValidDrop) HapticFeedbackType.LongPress
            else HapticFeedbackType.TextHandleMove,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .padding(horizontal = 12.dp)
            .shadow(4.dp, shape = MaterialTheme.shapes.medium)
            .border(0.5.dp, color.copy(alpha = alpha * 0.5f), MaterialTheme.shapes.medium)
            .semantics {
                contentDescription = if (isValidDrop) "Дозволена зона для скидання"
                else "Недозволена зона для скидання"
            }
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        color.copy(alpha = alpha),
                        Color.Transparent,
                    ),
                ),
            ),
    ) {
        if (!isValidDrop) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Недозволена зона",
                tint = color.copy(alpha = alpha),
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.Center),
            )
        }
    }
}

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