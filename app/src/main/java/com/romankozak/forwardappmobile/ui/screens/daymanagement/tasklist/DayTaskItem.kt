// file: ui/screens/daymanagement/tasklist/DayTaskItem.kt
package com.romankozak.forwardappmobile.ui.screens.daymanagement.tasklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Link
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
import com.romankozak.forwardappmobile.data.database.models.ListItemType // Припускаємо, що цей enum існує
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems.EnhancedCustomCheckbox
import com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems.EnhancedReminderBadge
import com.romankozak.forwardappmobile.ui.screens.backlog.components.backlogitems.MarkdownText

// --- Примітка: Додайте цей enum у відповідний файл з моделями ---
// enum class ListItemType { GOAL, SUBLIST, LINK }
// ----------------------------------------------------------------

/**
 * Іконка для підсписку, аналогічна тій, що в беклозі.
 */
@Composable
private fun SublistIconBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .semantics { contentDescription = "Підсписок" }
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
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
        imageVector = Icons.Default.Topic, // <-- Нова іконка
        contentDescription = "Прив'язано до проєкту",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(18.dp)
    )
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayTaskAsGoalItem(task: DayTask, currentTimeMillis: Long) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DayPlanMarkdownText(
            text = task.title,
            isCompleted = task.completed,
            style = MaterialTheme.typography.bodyLarge,
        )

        val hasStatusContent = hasStatusContent(task)
        AnimatedVisibility(visible = hasStatusContent, /* ... */) {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // --- ОНОВЛЕНИЙ ПОРЯДОК ---
                    // 1. Іконка проєкту (якщо є)
                    if (task.projectId != null) {
                        ProjectLinkBadge(modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    // 2. Іконка цілі (якщо є)
                    if (task.goalId != null) {
                        GoalLinkBadge(modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    // 3. Решта бейджів (пріоритет, нагадування і т.д.)
                    RenderBadges(task, currentTimeMillis)
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayTaskAsSublistItem(task: DayTask, currentTimeMillis: Long) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (task.completed) TextDecoration.LineThrough else null,
            color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )

        val hasStatusContent = hasStatusContent(task)
        AnimatedVisibility(visible = hasStatusContent, /* ... */) {
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // --- ОНОВЛЕНИЙ ПОРЯДОК ---
                    // 1. Іконка підсписку
                    SublistIconBadge(modifier = Modifier.align(Alignment.CenterVertically))
                    // 2. Іконка проєкту
                    if (task.projectId != null) {
                        ProjectLinkBadge(modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    // 3. Іконка цілі
                    if (task.goalId != null) {
                        GoalLinkBadge(modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    // 4. Решта бейджів
                    RenderBadges(task, currentTimeMillis)
                }
            }
        }
    }
}



private fun hasStatusContent(task: DayTask): Boolean {
    return (task.priority != TaskPriority.NONE) ||
            (task.reminderTime != null) ||
            (!task.description.isNullOrBlank()) ||
            (task.goalId != null) ||
            (task.projectId != null)
}

@Composable
private fun FlowRowScope.RenderBadges(task: DayTask, currentTimeMillis: Long) {
    // Тепер ця функція не відповідає за іконки зв'язків
    task.reminderTime?.let { time ->
        EnhancedReminderBadge(
            reminderTime = time,
            currentTimeMillis = currentTimeMillis,
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
            modifier = Modifier.size(18.dp).align(Alignment.CenterVertically)
        )
    }
}

@Composable
private fun PriorityBadge(priority: TaskPriority) {
    val (color, text) = when (priority) {
        TaskPriority.CRITICAL -> MaterialTheme.colorScheme.error to "Критичний"
        TaskPriority.HIGH -> MaterialTheme.colorScheme.errorContainer to "Високий"
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary to "Середній"
        TaskPriority.LOW -> MaterialTheme.colorScheme.tertiary to "Низький"
        TaskPriority.NONE -> MaterialTheme.colorScheme.onSurfaceVariant to "Без пріоритету"
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(0.7.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Flag,
                contentDescription = "Пріоритет",
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

@Composable
private fun GoalLinkBadge(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.TrackChanges,
        contentDescription = "Прив'язано до цілі",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(18.dp)
    )
}
