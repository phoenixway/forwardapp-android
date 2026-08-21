package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.features.reminders.components.ReminderBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PRIORITY_BADGE_ALPHA = 0.1f
private const val PRIORITY_BADGE_BORDER_ALPHA = 0.3f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayTaskAsGoalItem(
    task: DayTask,
    currentTimeMillis: Long,
    reminder: Reminder? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DayPlanMarkdownText(
            text = task.title,
            isCompleted = task.completed,
            style = MaterialTheme.typography.bodyLarge,
        )

        val hasStatusContent = hasStatusContent(task)
        AnimatedVisibility(visible = hasStatusContent) {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    mainAxisSpacing = 4.dp,
                    crossAxisSpacing = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RenderBadges(task, currentTimeMillis, reminder)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayTaskAsSublistItem(
    task: DayTask,
    currentTimeMillis: Long,
    reminder: Reminder? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (task.completed) TextDecoration.LineThrough else null,
            color =
                if (task.completed) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )

        val hasStatusContent = hasStatusContent(task)
        AnimatedVisibility(visible = hasStatusContent) {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    mainAxisSpacing = 4.dp,
                    crossAxisSpacing = 4.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RenderBadges(task, currentTimeMillis, reminder)
                }
            }
        }
    }
}

private fun hasStatusContent(task: DayTask): Boolean {
    return (task.priority != TaskPriority.NONE) ||
        (!task.description.isNullOrBlank()) ||
        (task.goalId != null) ||
        (task.projectId != null) ||
        (task.recurrenceSeriesId != null)
}

@Composable
private fun RenderBadges(
    task: DayTask,
    currentTimeMillis: Long,
    reminder: Reminder?,
) {
    if (task.recurrenceSeriesId != null) {
        Icon(
            imageVector = Icons.Default.Repeat,
            contentDescription = "Повторюване завдання",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
    reminder?.let {
        ReminderBadge(
            reminder = it,
        )
    }
    if (task.priority != TaskPriority.NONE) {
        PriorityBadge(priority = task.priority)
    }
    if (!task.description.isNullOrBlank()) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
            contentDescription = "Містить нотатку",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun PriorityBadge(priority: TaskPriority) {
    val badge = priority.badgeStyle()
    Surface(
        shape = MaterialTheme.shapes.small,
        color = badge.backgroundColor,
        border = badge.borderStroke,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = "Пріоритет",
                tint = badge.contentColor,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = badge.label,
                style = MaterialTheme.typography.labelSmall,
                color = badge.contentColor,
            )
        }
    }
}

@Composable
private fun TaskPriority.badgeStyle(): PriorityBadgeStyle =
    when (this) {
        TaskPriority.CRITICAL ->
            PriorityBadgeStyle(
                label = "Критичний",
                backgroundColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
        TaskPriority.HIGH ->
            PriorityBadgeStyle(
                label = "Високий",
                backgroundColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        TaskPriority.MEDIUM ->
            priorityBadgeStyle(
                label = "Середній",
                color = MaterialTheme.colorScheme.primary,
            )
        TaskPriority.LOW ->
            priorityBadgeStyle(
                label = "Низький",
                color = MaterialTheme.colorScheme.tertiary,
            )
        TaskPriority.NONE ->
            priorityBadgeStyle(
                label = "Без пріоритету",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }

@Composable
private fun priorityBadgeStyle(
    label: String,
    color: androidx.compose.ui.graphics.Color,
): PriorityBadgeStyle =
    PriorityBadgeStyle(
        label = label,
        backgroundColor = color.copy(alpha = PRIORITY_BADGE_ALPHA),
        contentColor = color,
        borderStroke =
            BorderStroke(
                width = 0.7.dp,
                color = color.copy(alpha = PRIORITY_BADGE_BORDER_ALPHA),
            ),
    )

private data class PriorityBadgeStyle(
    val label: String,
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val borderStroke: BorderStroke? = null,
)
