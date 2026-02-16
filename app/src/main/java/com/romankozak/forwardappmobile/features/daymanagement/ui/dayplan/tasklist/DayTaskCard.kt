package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentInfo
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentType
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedCheckboxStyle
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemCheckbox
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusChipSpec
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedTrailingActionButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayTaskCard(
    taskWithReminder: DayTaskWithReminder,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    onParentInfoClick: (ParentInfo) -> Unit,
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

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
        ) {
            DayPlanMarkdownText(
                text = task.title,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleSmall.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    ),
                isCompleted = task.completed,
                maxLines = 1,
            )

            task.description?.takeIf { it.isNotBlank() }?.let { description ->
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

            DayTaskMetaRow(
                taskWithReminder = taskWithReminder,
                modifier = Modifier.padding(top = 2.dp),
                onParentInfoClick = onParentInfoClick,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

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
private fun DayTaskMetaRow(
    taskWithReminder: DayTaskWithReminder,
    modifier: Modifier = Modifier,
    onParentInfoClick: (ParentInfo) -> Unit,
) {
    val task = taskWithReminder.dayTask
    val metaItems =
        buildList<UnifiedStatusChipSpec> {
            if (task.recurringTaskId != null) {
                add(
                    UnifiedStatusChipSpec(
                        icon = Icons.Outlined.Repeat,
                        text = "",
                    ),
                )
            }
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
                        onClick = { onParentInfoClick(parentInfo) },
                    ),
                )
            }
            task.estimatedDurationMinutes?.takeIf { it > 0 }?.let { minutes ->
                add(UnifiedStatusChipSpec(icon = Icons.Outlined.Timer, text = "$minutes хв"))
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
