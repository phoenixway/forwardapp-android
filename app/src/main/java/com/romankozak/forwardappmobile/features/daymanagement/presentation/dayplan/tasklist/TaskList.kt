package com.romankozak.forwardappmobile.features.daymanagement.presentation.dayplan.tasklist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.romankozak.forwardappmobile.features.daymanagement.presentation.dayplan.ParentType
import com.romankozak.forwardappmobile.features.daymanagement.presentation.dayplan.ParentInfo
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.TaskPriority

import com.romankozak.forwardappmobile.features.daymanagement.presentation.dayplan.DayTaskWithReminder
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
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        internalTasks = internalTasks.toMutableList().apply {
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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

@Composable
private fun EmptyTasksState(
    modifier: Modifier = Modifier,
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



    Surface(

        modifier = modifier.fillMaxWidth(),

        shape = RoundedCornerShape(20.dp),

        color = taskContainerColor(task),

        tonalElevation = if (task.completed) 0.dp else 4.dp,

    ) {

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .padding(horizontal = 12.dp, vertical = 10.dp),

            verticalAlignment = Alignment.CenterVertically,

        ) {

            IconToggleButton(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(32.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (task.completed)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Transparent,
                    border = if (!task.completed)
                        BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    else null,
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (task.completed) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Checkbox",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }



            Spacer(modifier = Modifier.width(12.dp))



            Column(modifier = Modifier.weight(1f)) {

                DayPlanMarkdownText(

                    text = task.title,

                    style =

                        MaterialTheme.typography.titleMedium.copy(

                            fontWeight = FontWeight.SemiBold,

                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),

                            ),

                    isCompleted = task.completed,
                    maxLines = 3,

                    )



                task.description?.takeIf { it.isNotBlank() }?.let { description ->

                    Spacer(modifier = Modifier.height(4.dp))

                    DayPlanMarkdownText(

                        text = description,

                        style =

                            MaterialTheme.typography.bodyMedium.copy(

                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),

                                ),

                        isCompleted = task.completed,

                        )

                }



                TaskMetaInfo(
                    taskWithReminder = taskWithReminder,
                    modifier = Modifier.padding(top = 6.dp),
                    onParentInfoClick = onParentInfoClick, // Add this line
                )

            }



            Spacer(modifier = Modifier.width(12.dp))



            IconButton(
                onClick = onLongPress,
                modifier = Modifier
                    .size(40.dp)
                    .then(dragHandleModifier),
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = "Більше опцій",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

        }

    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskMetaInfo(
    taskWithReminder: DayTaskWithReminder,
    modifier: Modifier = Modifier,
    onParentInfoClick: (ParentInfo) -> Unit, // Add this line
) {
    val task = taskWithReminder.dayTask
    val metaItems =
        buildList<MetaInfoItem> {
            if (task.priority != TaskPriority.NONE) {
                add(
                    MetaInfoItem(
                        icon = Icons.Outlined.Flag,
                        text = task.priority
                            .name
                            .lowercase()
                            .replaceFirstChar { it.titlecase() },
                        tint = task.priority.priorityIndicatorColor(),
                    ),
                )
            }
            task.points.takeIf { it > 0 }?.let { points ->
                add(
                    MetaInfoItem(
                        icon = Icons.Filled.Star,
                        text = "$points балів",
                        tint = MaterialTheme.colorScheme.secondary,
                    ),
                )
            }
            taskWithReminder.parentInfo?.let { parentInfo ->
                add(
                    MetaInfoItem(
                        icon = if (parentInfo.type == ParentType.GOAL) Icons.Default.TrackChanges else Icons.Default.Topic,
                        text = parentInfo.title,
                        tint = if (parentInfo.type == ParentType.GOAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        onClick = { onParentInfoClick(parentInfo) } // Add this line
                    )
                )
            }
            task.estimatedDurationMinutes?.takeIf { it > 0 }?.let { minutes ->
                add(MetaInfoItem(icon = Icons.Outlined.Timer, text = "$minutes хв"))
            }
            if (task.recurringTaskId != null) {
                add(MetaInfoItem(icon = Icons.Outlined.Repeat, text = "Повторюється"))
            }
            if (taskWithReminder.reminder != null) {
                add(MetaInfoItem(icon = Icons.Outlined.Notifications, text = "Нагадування"))
            }
        }

    if (metaItems.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        metaItems.forEach { item ->
            MetaInfoChip(
                icon = item.icon,
                text = item.text,
                contentColor = item.tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = item.onClick, // Add this line
            )
        }
    }
}

@Composable
private fun MetaInfoChip(
    icon: ImageVector,
    text: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null, // Add this line
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = contentColor,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onClick ?: {}, // Add this line
        enabled = onClick != null // Add this line
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = contentColor,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class MetaInfoItem(
    val icon: ImageVector,
    val text: String,
    val tint: Color? = null,
    val onClick: (() -> Unit)? = null, // Add this line
)

@Composable
private fun taskContainerColor(dayTask: DayTask): Color {
    val colorScheme = MaterialTheme.colorScheme
    return if (dayTask.completed) {
        colorScheme.surfaceContainerHighest
    } else {
        colorScheme.surfaceContainer
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
