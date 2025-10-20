package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Timer
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
    totalPoints: Int,
    onTaskLongPress: (DayTaskWithReminder) -> Unit,
    onTasksReordered: (List<DayTaskWithReminder>) -> Unit,
    onToggleTask: (String) -> Unit,
    onNavigateToPreviousDay: () -> Unit,
    onNavigateToNextDay: () -> Unit,
    isNextDayNavigationEnabled: Boolean,
    onSublistClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    headerContainerColor: Color
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
            totalPoints = totalPoints,
            onNavigateToPreviousDay = onNavigateToPreviousDay,
            onNavigateToNextDay = onNavigateToNextDay,
            isNextDayNavigationEnabled = isNextDayNavigationEnabled,
            onSettingsClick = onSettingsClick,
            containerColor = headerContainerColor
        )

        if (internalTasks.isEmpty()) {
            EmptyTasksState(modifier = Modifier.weight(1f))
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
                        Surface(shadowElevation = elevation, shape = MaterialTheme.shapes.medium) {
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
    val alpha = if (task.completed) 0.6f else 1f

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                TaskMetaInfo(task = task, modifier = Modifier.padding(top = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                dragHandle()
                IconButton(onClick = onLongPress, modifier = Modifier.size(48.dp)) {
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

@Composable
private fun TaskMetaInfo(task: DayTask, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        task.estimatedDurationMinutes?.takeIf { it > 0 }?.let {
            MetaInfoChip(icon = Icons.Outlined.Timer, text = "$it хв")
        }
        if (task.priority != TaskPriority.NONE) {
            MetaInfoChip(icon = Icons.Outlined.Flag, text = task.priority.name.lowercase().replaceFirstChar { it.titlecase() }, color = task.priority.toColor())
        }
    }
}

@Composable
private fun MetaInfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

fun TaskPriority.toColor(): Color {
    return when (this) {
        TaskPriority.CRITICAL -> Color.Red
        TaskPriority.HIGH -> Color(0xFFFFA500) // Orange
        TaskPriority.MEDIUM -> Color.Blue
        TaskPriority.LOW -> Color.Gray
        TaskPriority.NONE -> Color.Transparent
    }
}