package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd

import androidx.compose.ui.platform.LocalDensity
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.ui.dnd.DragAndDropState
import com.romankozak.forwardappmobile.ui.dnd.DnDVisualState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.SwipeableListItem
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.DraggableItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import com.romankozak.forwardappmobile.data.database.models.ListItemContent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity

@Composable
fun InteractiveListItem(
    item: ListItemContent,
    index: Int,
    dragAndDropState: DragAndDropState,
    dndVisualState: StateFlow<DnDVisualState>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isSelected: Boolean,
    isHighlighted: Boolean,
    swipeEnabled: Boolean,
    isDragHandleActive: Boolean,
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
    val density = LocalDensity.current
    val currentDnDVisualState by dndVisualState.collectAsStateWithLifecycle()
    
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

    val isDraggingThisItem = currentDnDVisualState.isDragging && dragAndDropState.draggedItemIndex == index
    val yOffset = currentDnDVisualState.itemOffsets[index] ?: 0f

    DraggableItem(
        isDragging = isDraggingThisItem,
        yOffset = yOffset,
        modifier = if (isDraggingThisItem) modifier.height(with(density) { currentDnDVisualState.draggedItemHeight.toDp() }) else modifier,
    ) { isDragging ->
        SwipeableListItem(
            modifier = Modifier,
            isDragging = isDraggingThisItem,
            isAnyItemDragging = currentDnDVisualState.isDragging,
            swipeEnabled = swipeEnabled && !isDragHandleActive,
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
                        content(isDragging)
                    }
                }
            },
        )
    }
}