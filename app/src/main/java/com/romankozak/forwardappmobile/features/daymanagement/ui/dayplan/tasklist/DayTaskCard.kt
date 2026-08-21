package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.backlogitems.AnimatedContextEmoji
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayTaskWithReminder
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentInfo
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.ParentType
import com.romankozak.forwardappmobile.features.daymanagement.utils.formatDayTime
import com.romankozak.forwardappmobile.ui.common.rememberParsedText
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedCheckboxStyle
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemCheckbox
import com.romankozak.forwardappmobile.ui.components.listitem.unifiedCheckboxColors
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedMetaChip
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusChipSpec
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow

private const val COMPLETED_TASK_CONTENT_ALPHA = 0.6f

data class DayTaskCardActions(
    val onClick: () -> Unit,
    val onToggle: () -> Unit,
    val onLongPress: () -> Unit,
    val onParentInfoClick: (ParentInfo) -> Unit,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayTaskCard(
    taskWithReminder: DayTaskWithReminder,
    linkedProjectTitles: Map<String, String>,
    contextMarkerToEmojiMap: Map<String, String>,
    actions: DayTaskCardActions,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
) {
    val task = taskWithReminder.dayTask
    val contentAlpha = if (task.completed) COMPLETED_TASK_CONTENT_ALPHA else 1f

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UnifiedItemCheckbox(
            checked = task.completed,
            onCheckedChange = { actions.onToggle() },
            style = UnifiedCheckboxStyle.Square,
            modifier = dragHandleModifier,
            colors =
                unifiedCheckboxColors(
                    checked = MaterialTheme.colorScheme.primary,
                    uncheckedBorder = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                ),
        )

        Spacer(modifier = Modifier.width(6.dp))

        DayTaskCardContent(
            taskWithReminder = taskWithReminder,
            linkedProjectTitles = linkedProjectTitles,
            contextMarkerToEmojiMap = contextMarkerToEmojiMap,
            contentAlpha = contentAlpha,
            onClick = actions.onClick,
            onParentInfoClick = actions.onParentInfoClick,
        )

        IconButton(
            onClick = actions.onLongPress,
            modifier = dragHandleModifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Більше опцій",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun RowScope.DayTaskCardContent(
    taskWithReminder: DayTaskWithReminder,
    linkedProjectTitles: Map<String, String>,
    contextMarkerToEmojiMap: Map<String, String>,
    contentAlpha: Float,
    onClick: () -> Unit,
    onParentInfoClick: (ParentInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val task = taskWithReminder.dayTask
    val parsedTitle = rememberParsedText(task.title, contextMarkerToEmojiMap)
    val parsedDescription = rememberParsedText(task.description.orEmpty(), contextMarkerToEmojiMap)
    val parsedTaskText =
        rememberParsedText(
            buildString {
                append(task.title)
                task.description?.takeIf { it.isNotBlank() }?.let {
                    append('\n')
                    append(it)
                }
            },
            contextMarkerToEmojiMap,
        )
    val displayTime =
        task.scheduledTime
            ?: taskWithReminder.reminder?.reminderTime
            ?: task.dueTime

    Column(
        modifier =
            modifier
                .weight(1f)
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            task.priority.takeIf { it != TaskPriority.NONE }?.let { priority ->
                DayTaskPriorityLabel(
                    priority = priority,
                    contentAlpha = contentAlpha,
                )
            }

            if (task.recurrenceSeriesId != null) {
                if (task.priority != TaskPriority.NONE) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                UnifiedMetaChip(
                    text = "",
                    icon = Icons.Outlined.Repeat,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            displayTime?.let { timestamp ->
                Text(
                    text = formatDayTime(timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f * contentAlpha),
                )
            }
        }

        DayPlanMarkdownText(
            text = parsedTitle.mainText,
            style =
                MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                ),
            isCompleted = task.completed,
            maxLines = 4,
        )

        parsedDescription.mainText.takeIf { it.isNotBlank() }?.let { description ->
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
            linkedProjectTitles = linkedProjectTitles,
            contextIcons = parsedTaskText.icons,
            modifier = Modifier.padding(top = 8.dp),
            onParentInfoClick = onParentInfoClick,
        )
    }
}

@Composable
private fun DayTaskPriorityLabel(
    priority: TaskPriority,
    contentAlpha: Float,
) {
    val color = priority.priorityIndicatorColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(7.dp)
                    .background(color.copy(alpha = contentAlpha), CircleShape),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = priority.name.lowercase(),
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = contentAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DayTaskMetaRow(
    taskWithReminder: DayTaskWithReminder,
    linkedProjectTitles: Map<String, String>,
    contextIcons: List<String>,
    modifier: Modifier = Modifier,
    onParentInfoClick: (ParentInfo) -> Unit,
) {
    val task = taskWithReminder.dayTask
    val metaItems =
        buildList<UnifiedStatusChipSpec> {
            if (task.title.contains("#focus", ignoreCase = true) || task.description?.contains("#focus", ignoreCase = true) == true) {
                add(
                    UnifiedStatusChipSpec(
                        icon = Icons.Outlined.Flag,
                        text = "Focus",
                        contentColor = MaterialTheme.colorScheme.tertiary,
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
                    parentInfo.toStatusChipSpec(onParentInfoClick),
                )
            }
            task.linkedProjectIds.orEmpty()
                .map(String::trim)
                .filter { linkedProjectId -> linkedProjectId.isNotBlank() && linkedProjectId != "root" }
                .distinct()
                .forEach { linkedProjectId ->
                    val title = linkedProjectTitles[linkedProjectId] ?: linkedProjectId
                    add(
                        ParentInfo(
                            id = linkedProjectId,
                            title = title,
                            type = ParentType.PROJECT,
                            projectId = linkedProjectId,
                        ).toStatusChipSpec(onParentInfoClick),
                    )
                }
            task.estimatedDurationMinutes?.takeIf { it > 0 }?.let { minutes ->
                add(UnifiedStatusChipSpec(icon = Icons.Outlined.Timer, text = "$minutes хв"))
            }
            if (taskWithReminder.reminder != null) {
                add(UnifiedStatusChipSpec(icon = Icons.Outlined.Notifications, text = "Нагадування"))
            }
        }

    if (metaItems.isEmpty() && contextIcons.isEmpty()) return
    UnifiedStatusRow(modifier = modifier) {
        contextIcons.forEach { icon ->
            AnimatedContextEmoji(emoji = icon)
        }
        metaItems.forEach { item ->
            UnifiedMetaChip(
                text = item.text,
                icon = item.icon,
                contentColor = item.contentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = item.onClick,
            )
        }
    }
}

@Composable
private fun ParentInfo.toStatusChipSpec(
    onParentInfoClick: (ParentInfo) -> Unit,
): UnifiedStatusChipSpec =
    UnifiedStatusChipSpec(
        icon =
            if (type == ParentType.GOAL) {
                Icons.Default.TrackChanges
            } else {
                Icons.Default.Topic
            },
        text = title,
        contentColor =
            if (type == ParentType.GOAL) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            },
        onClick = { onParentInfoClick(this) },
    )

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
