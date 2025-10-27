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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
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
    onRelatedLinkClick: (RelatedLink) -> Unit,
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
        offsetX += delta
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
                onDragStopped = {
                    coroutineScope.launch {
                        if (offsetX > leftActionWidthPx / 2) {
                            animate(initialValue = offsetX, targetValue = leftActionWidthPx) { value, _ ->
                                offsetX = value
                            }
                        } else if (offsetX < -rightActionWidthPx / 2) {
                            animate(initialValue = offsetX, targetValue = -rightActionWidthPx) { value, _ ->
                                offsetX = value
                            }
                        } else {
                            animate(initialValue = offsetX, targetValue = 0f) { value, _ ->
                                offsetX = value
                            }
                        }
                    }
                }
            )
    ) {
        if (offsetX > 0) {
            Row(
                modifier = Modifier
                    .width(leftActionWidth)
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = {
                    onShowGoalTransportMenu(item)
                    coroutineScope.launch {
                        animate(initialValue = offsetX, targetValue = 0f) { value, _ ->
                            offsetX = value
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                IconButton(onClick = {
                    onMoveToTop(item)
                    coroutineScope.launch {
                        animate(initialValue = offsetX, targetValue = 0f) { value, _ ->
                            offsetX = value
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move to top",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = {
                    onAddToDayPlan(item)
                    coroutineScope.launch {
                        animate(initialValue = offsetX, targetValue = 0f) { value, _ ->
                            offsetX = value
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add to day plan",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = {
                    onStartTracking(item)
                    coroutineScope.launch {
                        animate(initialValue = offsetX, targetValue = 0f) { value, _ ->
                            offsetX = value
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = "Start tracking",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                            }
        }

        if (offsetX < 0) {
            Row(
                modifier = Modifier
                    .width(rightActionWidth)
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    onCheckedChange(item, !isCompleted)
                    coroutineScope.launch {
                        animate(initialValue = offsetX, targetValue = 0f) { value, _ ->
                            offsetX = value
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Complete",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = {
                    onDelete(item)
                    coroutineScope.launch {
                        animate(initialValue = offsetX, targetValue = 0f) { value, _ ->
                            offsetX = value
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                IconButton(onClick = {
                    onDeleteEverywhere(item)
                    coroutineScope.launch {
                        animate(initialValue = offsetX, targetValue = 0f) { value, _ ->
                            offsetX = value
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Delete everywhere",
                        tint = MaterialTheme.colorScheme.error
                    )
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
            showCheckbox = showCheckboxes,
            onRelatedLinkClick = onRelatedLinkClick,
            isSelected = isDragging
        )
    }
}