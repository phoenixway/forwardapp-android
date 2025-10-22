package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.CompactDayPlanHeader
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.GoalItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<DayTaskWithReminder>,
    dayPlan: DayPlan?,
    totalPointsEarned: Int,
    totalPointsAvailable: Int,
    completedTasks: Int,
    totalTasks: Int,
    onTaskLongPress: (DayTaskWithReminder) -> Unit,
    onTasksReordered: (List<DayTaskWithReminder>) -> Unit,
    onToggleTask: (String) -> Unit,
    onNavigateToPreviousDay: () -> Unit,
    onNavigateToNextDay: () -> Unit,
    isNextDayNavigationEnabled: Boolean,
    onAddTaskClick: () -> Unit,
    onSublistClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var internalTasks by remember(tasks) { mutableStateOf(tasks) }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        internalTasks = internalTasks.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onTasksReordered(internalTasks)
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(modifier = modifier.fillMaxSize()) {
        CompactDayPlanHeader(
            dayPlan = dayPlan,
            totalPointsEarned = totalPointsEarned,
            totalPointsAvailable = totalPointsAvailable,
            completedTasks = completedTasks,
            totalTasks = totalTasks,
            onNavigateToPreviousDay = onNavigateToPreviousDay,
            onNavigateToNextDay = onNavigateToNextDay,
            isNextDayNavigationEnabled = isNextDayNavigationEnabled,
            onSettingsClick = onSettingsClick,
            onAddTaskClick = onAddTaskClick,
        )

        if (internalTasks.isEmpty()) {
            EmptyTasksState(
                modifier = Modifier.weight(1f),
                onAddTaskClick = onAddTaskClick,
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(internalTasks, key = { it.dayTask.id }) { taskWithReminder ->
                    ReorderableItem(reorderableState, key = taskWithReminder.dayTask.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = taskContainerColor(
                                    dayTask = taskWithReminder.dayTask,
                                ),
                            ),
                        ) {
                            TaskItem(
                                taskWithReminder = taskWithReminder,
                                onToggle = { onToggleTask(taskWithReminder.dayTask.id) },
                                onLongPress = { onTaskLongPress(taskWithReminder) },
                                dragHandle = {
                                    Icon(
                                        Icons.Rounded.DragHandle,
                                        contentDescription = "Перетягнути",
                                        modifier = Modifier
                                            .draggableHandle()
                                            .padding(end = 8.dp)
                                            .size(24.dp),
                                        tint = MaterialTheme.colorScheme.outline,
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTasksState(
    modifier: Modifier = Modifier,
    onAddTaskClick: () -> Unit,
) {
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
        FilledTonalButton(onClick = onAddTaskClick) {
            Text("Додати перше завдання")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    taskWithReminder: DayTaskWithReminder,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit,
) {
    val task = taskWithReminder.dayTask
    val priorityColor = task.priority.priorityIndicatorColor()
    val contentAlpha = if (task.completed) 0.6f else 1f

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        priorityColor,
                                        priorityColor.copy(alpha = 0.6f),
                                    ),
                            ),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                    ),
        )

        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                TaskMetaInfo(
                    taskWithReminder = taskWithReminder,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                dragHandle()
                IconButton(onClick = onLongPress, modifier = Modifier.size(44.dp)) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Більше опцій",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskMetaInfo(taskWithReminder: DayTaskWithReminder, modifier: Modifier = Modifier) {
    val task = taskWithReminder.dayTask

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        task.points.takeIf { it > 0 }?.let { points ->
            MetaInfoChip(
                icon = Icons.Filled.Star,
                text = "$points балів",
                contentColor = MaterialTheme.colorScheme.secondary,
            )
        }

        task.estimatedDurationMinutes?.takeIf { it > 0 }?.let {
            MetaInfoChip(
                icon = Icons.Outlined.Timer,
                text = "$it хв",
            )
        }

        if (task.priority != TaskPriority.NONE) {
            MetaInfoChip(
                icon = Icons.Outlined.Flag,
                text = task.priority.name.lowercase().replaceFirstChar { it.titlecase() },
                contentColor = task.priority.priorityIndicatorColor(),
            )
        }

        if (task.recurringTaskId != null) {
            MetaInfoChip(
                icon = Icons.Outlined.Repeat,
                text = "Повторюється",
            )
        }

        if (taskWithReminder.reminder != null) {
            MetaInfoChip(
                icon = Icons.Outlined.Notifications,
                text = "Нагадування",
            )
        }
    }
}

@Composable
private fun MetaInfoChip(
    icon: ImageVector,
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun taskContainerColor(dayTask: DayTask): Color {
    val colorScheme = MaterialTheme.colorScheme
    return if (dayTask.completed) {
        colorScheme.surfaceContainerHigh
    } else {
        colorScheme.surfaceContainerLow
    }
}

@Composable
private fun TaskPriority.priorityIndicatorColor(): Color {
    val colorScheme = MaterialTheme.colorScheme
    return when (this) {
        TaskPriority.CRITICAL -> colorScheme.error
        TaskPriority.HIGH -> colorScheme.tertiary
        TaskPriority.MEDIUM -> colorScheme.primary
        TaskPriority.LOW -> colorScheme.secondary
        TaskPriority.NONE -> colorScheme.outline
    }
}
