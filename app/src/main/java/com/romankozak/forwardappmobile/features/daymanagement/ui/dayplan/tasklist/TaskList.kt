package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentInfo
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentType
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedCheckboxStyle
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemCheckbox
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemTokens
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusChipSpec
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedTrailingActionButton
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    tasks: List<DayTaskWithReminder>,
    onTaskLongPress: (DayTaskWithReminder) -> Unit,
    onTasksReordered: (List<DayTaskWithReminder>) -> Unit,
    onToggleTask: (String) -> Unit,
    onSublistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onParentInfoClick: (ParentInfo) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var internalTasks by remember(tasks) { mutableStateOf(tasks) }

    val lazyListState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            internalTasks =
                internalTasks.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            onTasksReordered(internalTasks)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    Column(modifier = modifier.fillMaxSize()) {
        if (internalTasks.isEmpty()) {
            EmptyTasksState(
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(UnifiedListItemTokens.OuterVerticalSpacing * 2),
            ) {
                items(internalTasks, key = { it.dayTask.id }) { taskWithReminder ->
                    ReorderableItem(reorderableState, key = taskWithReminder.dayTask.id) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = elevation / 8),
                        ) {
                            val colorScheme = MaterialTheme.colorScheme
                            val itemState =
                                if (taskWithReminder.dayTask.completed) {
                                    UnifiedItemState.COMPLETED
                                } else {
                                    UnifiedItemState.DEFAULT
                                }
                            val dayTaskContainerColor =
                                when (itemState) {
                                    UnifiedItemState.COMPLETED -> colorScheme.secondaryContainer.copy(alpha = 0.28f)
                                    UnifiedItemState.DEFAULT -> colorScheme.primaryContainer.copy(alpha = 0.34f)
                                    UnifiedItemState.SELECTED -> colorScheme.surfaceContainerHighest
                                    UnifiedItemState.OVERDUE -> colorScheme.errorContainer.copy(alpha = 0.50f)
                                    UnifiedItemState.DISABLED -> colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                            val dayTaskBorderColor =
                                when (itemState) {
                                    UnifiedItemState.COMPLETED -> colorScheme.outlineVariant.copy(alpha = 0.55f)
                                    UnifiedItemState.DEFAULT -> colorScheme.primary.copy(alpha = 0.30f)
                                    UnifiedItemState.SELECTED -> colorScheme.primary.copy(alpha = 0.4f)
                                    UnifiedItemState.OVERDUE -> colorScheme.error.copy(alpha = 0.45f)
                                    UnifiedItemState.DISABLED -> colorScheme.outlineVariant.copy(alpha = 0.35f)
                                }
                            UnifiedListItemSurface(
                                isSelected = isDragging,
                                state = itemState,
                                containerColorOverride = dayTaskContainerColor,
                                borderColorOverride = dayTaskBorderColor,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                TaskItem(
                                    taskWithReminder = taskWithReminder,
                                    onToggle = { onToggleTask(taskWithReminder.dayTask.id) },
                                    onLongPress = { onTaskLongPress(taskWithReminder) },
                                    dragHandleModifier = Modifier.draggableHandle(),
                                    onParentInfoClick = onParentInfoClick,
                                )
                            }
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
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    taskWithReminder: DayTaskWithReminder,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    onParentInfoClick: (ParentInfo) -> Unit, // Add this line
) {
    val task = taskWithReminder.dayTask

    val contentAlpha = if (task.completed) 0.6f else 1f

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            UnifiedItemCheckbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                style = UnifiedCheckboxStyle.Square,
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                DayPlanMarkdownText(
                    text = task.title,
                    style =

                        MaterialTheme.typography.titleMedium.copy(
                            fontSize = MaterialTheme.typography.titleSmall.fontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                        ),
                    isCompleted = task.completed,
                    maxLines = 2,
                )

                task.description?.takeIf { it.isNotBlank() }?.let { description ->

                    Spacer(modifier = Modifier.height(2.dp))

                    DayPlanMarkdownText(
                        text = description,
                        style =

                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                            ),
                        isCompleted = task.completed,
                        maxLines = 2,
                    )
                }

                TaskMetaInfo(
                    taskWithReminder = taskWithReminder,
                    modifier = Modifier.padding(top = 4.dp),
                    onParentInfoClick = onParentInfoClick, // Add this line
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = dragHandleModifier) {
                UnifiedTrailingActionButton(
                    icon = Icons.Filled.MoreVert,
                    contentDescription = "Більше опцій",
                    onClick = onLongPress,
                )
            }
    }
}

@Composable
private fun TaskMetaInfo(
    taskWithReminder: DayTaskWithReminder,
    modifier: Modifier = Modifier,
    onParentInfoClick: (ParentInfo) -> Unit, // Add this line
) {
    val task = taskWithReminder.dayTask
    val metaItems =
        buildList<UnifiedStatusChipSpec> {
            if (task.priority != TaskPriority.NONE) {
                add(
                    UnifiedStatusChipSpec(
                        icon = Icons.Outlined.Flag,
                        text =
                            task.priority
                                .name
                                .lowercase()
                                .replaceFirstChar { it.titlecase() },
                        contentColor = task.priority.priorityIndicatorColor(),
                    ),
                )
            }
            task.points.takeIf { it > 0 }?.let { points ->
                add(
                    UnifiedStatusChipSpec(
                        icon = Icons.Filled.Star,
                        text = "$points балів",
                        contentColor = MaterialTheme.colorScheme.secondary,
                    ),
                )
            }
            taskWithReminder.parentInfo?.let { parentInfo ->
                add(
                    UnifiedStatusChipSpec(
                        icon = if (parentInfo.type == ParentType.GOAL) Icons.Default.TrackChanges else Icons.Default.Topic,
                        text = parentInfo.title,
                        contentColor = if (parentInfo.type == ParentType.GOAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        onClick = { onParentInfoClick(parentInfo) }, // Add this line
                    ),
                )
            }
            task.estimatedDurationMinutes?.takeIf { it > 0 }?.let { minutes ->
                add(UnifiedStatusChipSpec(icon = Icons.Outlined.Timer, text = "$minutes хв"))
            }
            if (task.recurringTaskId != null) {
                add(UnifiedStatusChipSpec(icon = Icons.Outlined.Repeat, text = "Повторюється"))
            }
            if (taskWithReminder.reminder != null) {
                add(UnifiedStatusChipSpec(icon = Icons.Outlined.Notifications, text = "Нагадування"))
            }
        }

    if (metaItems.isEmpty()) return

    UnifiedStatusRow(items = metaItems, modifier = modifier)
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
