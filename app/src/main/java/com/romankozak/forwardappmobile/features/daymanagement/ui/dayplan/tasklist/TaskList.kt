package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentInfo
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemColors
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurfaceLayout
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemTokens
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<DayTaskWithReminder>,
    linkedProjectTitles: Map<String, String>,
    contextMarkerToEmojiMap: Map<String, String>,
    actions: TaskListActions,
    lazyListState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var internalTasks by remember(tasks) { mutableStateOf(tasks) }

    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            internalTasks =
                internalTasks.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            actions.onTasksReordered(internalTasks)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    Column(modifier = modifier.fillMaxSize()) {
        if (internalTasks.isEmpty()) {
            EmptyTasksState(
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            TaskListContent(
                tasks = internalTasks,
                linkedProjectTitles = linkedProjectTitles,
                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                lazyListState = lazyListState,
                reorderableState = reorderableState,
                actions = actions,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskListContent(
    tasks: List<DayTaskWithReminder>,
    linkedProjectTitles: Map<String, String>,
    contextMarkerToEmojiMap: Map<String, String>,
    lazyListState: LazyListState,
    reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
    actions: TaskListActions,
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(UnifiedListItemTokens.OuterVerticalSpacing * 2),
    ) {
        items(tasks, key = { it.dayTask.id }) { taskWithReminder ->
            TaskListItem(
                taskWithReminder = taskWithReminder,
                linkedProjectTitles = linkedProjectTitles,
                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                reorderableState = reorderableState,
                actions = actions,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.TaskListItem(
    taskWithReminder: DayTaskWithReminder,
    linkedProjectTitles: Map<String, String>,
    contextMarkerToEmojiMap: Map<String, String>,
    reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
    actions: TaskListActions,
) {
    ReorderableItem(reorderableState, key = taskWithReminder.dayTask.id) { isDragging ->
        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = elevation / 8),
        ) {
            val itemState =
                if (taskWithReminder.dayTask.completed) {
                    UnifiedItemState.COMPLETED
                } else {
                    UnifiedItemState.DEFAULT
                }
            val borderColor = taskWithReminder.dayTask.priorityBorderColor()
            UnifiedListItemSurface(
                isSelected = isDragging,
                state = itemState,
                layout =
                    UnifiedListItemSurfaceLayout(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    ),
                colors =
                    UnifiedListItemColors(
                        container = taskWithReminder.dayTask.priorityContainerColor(borderColor),
                        border = borderColor,
                    ),
            ) {
                DayTaskCard(
                    taskWithReminder = taskWithReminder,
                    linkedProjectTitles = linkedProjectTitles,
                    contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                    actions =
                        DayTaskCardActions(
                            onClick = { actions.onTaskClick(taskWithReminder) },
                            onToggle = { actions.onToggleTask(taskWithReminder.dayTask.id) },
                            onLongPress = { actions.onTaskLongPress(taskWithReminder) },
                            onParentInfoClick = actions.onParentInfoClick,
                        ),
                    dragHandleModifier = Modifier.draggableHandle(),
                )
            }
        }
    }
}

@Composable
private fun com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask.priorityBorderColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (priority) {
        TaskPriority.CRITICAL -> colorScheme.error.copy(alpha = if (completed) 0.42f else 0.64f)
        TaskPriority.HIGH -> colorScheme.tertiary.copy(alpha = if (completed) 0.40f else 0.58f)
        TaskPriority.MEDIUM -> colorScheme.primary.copy(alpha = if (completed) 0.36f else 0.52f)
        TaskPriority.LOW -> colorScheme.secondary.copy(alpha = if (completed) 0.34f else 0.46f)
        TaskPriority.NONE -> colorScheme.outline.copy(alpha = if (completed) 0.28f else 0.38f)
    }
}

@Composable
private fun com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask.priorityContainerColor(
    borderColor: Color,
): Color = borderColor.copy(alpha = if (completed) 0.08f else 0.12f)

@Composable
private fun EmptyTasksState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Checklist,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Завдань ще немає",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Натисніть кнопку '+' для додавання",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
