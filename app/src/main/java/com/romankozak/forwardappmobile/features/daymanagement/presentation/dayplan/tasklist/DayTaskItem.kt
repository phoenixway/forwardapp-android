package com.romankozak.forwardappmobile.features.daymanagement.presentation.dayplan.tasklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2

import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.ui.reminders.components.ReminderBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
private fun SublistIconBadge(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .semantics { contentDescription = "Підсписок" }
                .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.SubdirectoryArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun ProjectLinkBadge(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Topic,
        contentDescription = "Підпроект в беклозі",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(18.dp),
    )
}

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
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
            color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )

        val hasStatusContent = hasStatusContent(task)
        AnimatedVisibility(visible = hasStatusContent) {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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
        (task.nextOccurrenceTime != null) ||
        (!task.description.isNullOrBlank()) ||
        (task.goalId != null) ||
        (task.projectId != null) ||
        (task.recurringTaskId != null)
}

@Composable
private fun FlowRowScope.RenderBadges(
    task: DayTask,
    currentTimeMillis: Long,
    reminder: Reminder?,
) {
    if (task.recurringTaskId != null) {
        Icon(
            imageVector = Icons.Default.Repeat,
            contentDescription = "Повторюване завдання",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp).align(Alignment.CenterVertically),
        )
    }
    task.nextOccurrenceTime?.let { time ->
        if (time > currentTimeMillis) {
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val nextTime = formatter.format(Date(time))
            Text("Наступне о $nextTime", style = MaterialTheme.typography.labelSmall)
        }
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
            modifier = Modifier.size(18.dp).align(Alignment.CenterVertically),
        )
    }
}


@Composable
private fun PriorityBadge(priority: TaskPriority) {
    when (priority) {
        TaskPriority.CRITICAL -> {
            val backgroundColor = MaterialTheme.colorScheme.error
            val contentColor = MaterialTheme.colorScheme.onError
            Surface(
                shape = MaterialTheme.shapes.small,
                color = backgroundColor,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Пріоритет",
                        tint = contentColor,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "Критичний",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                    )
                }
            }
        }
        TaskPriority.HIGH -> {
            val backgroundColor = MaterialTheme.colorScheme.errorContainer
            val contentColor = MaterialTheme.colorScheme.onErrorContainer
            Surface(
                shape = MaterialTheme.shapes.small,
                color = backgroundColor,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Пріоритет",
                        tint = contentColor,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = "Високий",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                    )
                }
            }
        }
        else -> {
            val (color, text) =
                when (priority) {
                    TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary to "Середній"
                    TaskPriority.LOW -> MaterialTheme.colorScheme.tertiary to "Низький"
                    else -> MaterialTheme.colorScheme.onSurfaceVariant to "Без пріоритету"
                }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.1f),
                border = BorderStroke(0.7.dp, color.copy(alpha = 0.3f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Пріоритет",
                        tint = color,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalLinkBadge(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.TrackChanges,
        contentDescription = "Ціль в беклозі",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(18.dp),
    )
}
