@file:OptIn(ExperimentalFoundationApi::class)
package com.romankozak.forwardappmobile.ui.features.backlog

import androidx.compose.animation.core.animate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import sh.calvin.reorderable.ReorderableCollectionItemScope
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun SwipeableBacklogItem(
    item: ListItemContent,
    reorderableScope: ReorderableCollectionItemScope,
    showCheckboxes: Boolean,
    isDragging: Boolean,
    onItemClick: (ListItemContent) -> Unit,
    onLongClick: (ListItemContent) -> Unit,
    onMoreClick: (ListItemContent) -> Unit,
    onCheckedChange: (ListItemContent, Boolean) -> Unit,
    onDelete: (ListItemContent) -> Unit,
    onDeleteEverywhere: (ListItemContent) -> Unit,
    onMoveToTop: (ListItemContent) -> Unit,
    onAddToDayPlan: (ListItemContent) -> Unit,
    onStartTracking: (ListItemContent) -> Unit,
    onShowGoalTransportMenu: (ListItemContent) -> Unit,
    onRelatedLinkClick: (com.romankozak.forwardappmobile.data.database.models.RelatedLink) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) }
    val rightActionWidth = 180.dp
    val leftActionWidth = 240.dp
    val rightActionWidthPx = with(density) { rightActionWidth.toPx() }
    val leftActionWidthPx = with(density) { leftActionWidth.toPx() }

    val draggableState = rememberDraggableState { delta ->
        offsetX = (offsetX + delta).coerceIn(-rightActionWidthPx, leftActionWidthPx)
    }

    fun animateTo(target: Float) {
        coroutineScope.launch {
            animate(initialValue = offsetX, targetValue = target) { value, _ ->
                offsetX = value
            }
        }
    }

    val isCompleted = when (item) {
        is ListItemContent.GoalItem -> item.goal.completed
        is ListItemContent.SublistItem -> item.project.isCompleted
        else -> false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Horizontal,
                state = draggableState,
                onDragStopped = { velocity ->
                    val leftThreshold = leftActionWidthPx * 0.12f
                    val rightThreshold = rightActionWidthPx * 0.12f
                    val velocityThreshold = 1200f
                    when {
                        velocity > velocityThreshold -> animateTo(leftActionWidthPx)
                        velocity < -velocityThreshold -> animateTo(-rightActionWidthPx)
                        offsetX > leftThreshold -> animateTo(leftActionWidthPx)
                        offsetX < -rightThreshold -> animateTo(-rightActionWidthPx)
                        else -> animateTo(0f)
                    }
                }
            )
    ) {
        if (offsetX > 0) {
            Row(
                modifier = Modifier
                    .width(leftActionWidth)
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
            ) {
                SwipeActionButton(
                    icon = Icons.Default.Share,
                    contentDescription = "Share",
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    onShowGoalTransportMenu(item)
                    animateTo(0f)
                }
                SwipeActionButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Move to top",
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    onMoveToTop(item)
                    animateTo(0f)
                }
                SwipeActionButton(
                    icon = Icons.Default.AddCircle,
                    contentDescription = "Add to day plan",
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    onAddToDayPlan(item)
                    animateTo(0f)
                }
                SwipeActionButton(
                    icon = Icons.Default.PlayCircleOutline,
                    contentDescription = "Start tracking",
                    color = MaterialTheme.colorScheme.inversePrimary,
                ) {
                    onStartTracking(item)
                    animateTo(0f)
                }
            }
        }

        if (offsetX < 0) {
            Row(
                modifier = Modifier
                    .width(rightActionWidth)
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp, start = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            ) {
                SwipeActionButton(
                    icon = Icons.Default.Done,
                    contentDescription = "Complete",
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    onCheckedChange(item, !isCompleted)
                    animateTo(0f)
                }
                SwipeActionButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete",
                    color = MaterialTheme.colorScheme.error,
                ) {
                    onDelete(item)
                    animateTo(0f)
                }
                SwipeActionButton(
                    icon = Icons.Default.DeleteForever,
                    contentDescription = "Delete everywhere",
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    onDeleteEverywhere(item)
                    animateTo(0f)
                }
            }
        }

        BacklogItem(
            item = item,
            reorderableScope = reorderableScope,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) },
            onItemClick = { onItemClick(item) },
            onLongClick = { onLongClick(item) },
            onMoreClick = { onMoreClick(item) },
            onCheckedChange = { onCheckedChange(item, it) },
            onRelatedLinkClick = onRelatedLinkClick,
            showCheckbox = showCheckboxes,
            isSelected = isDragging
        )
    }
}

@Composable
private fun SwipeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    color: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.9f),
        tonalElevation = 0.dp,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
