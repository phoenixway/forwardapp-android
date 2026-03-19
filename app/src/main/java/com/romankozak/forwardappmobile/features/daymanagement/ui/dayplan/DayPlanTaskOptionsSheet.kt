package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask

private const val TASK_OPTIONS_VERTICAL_PADDING_DP = 8
private const val TASK_OPTIONS_DIVIDER_PADDING_DP = 16

data class TaskOptionsActions(
    val onEdit: () -> Unit,
    val onDelete: (DayTaskWithReminder) -> Unit,
    val onSetReminder: () -> Unit,
    val onAddToToday: () -> Unit,
    val onAddToTacticalMissions: () -> Unit,
    val onShowInBacklog: (DayTask) -> Unit,
    val onMoveToTop: () -> Unit,
    val onMoveToTomorrow: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOptionsBottomSheet(
    taskWithReminder: DayTaskWithReminder,
    onDismiss: () -> Unit,
    actions: TaskOptionsActions,
    showAddToTodayOption: Boolean,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        TaskOptionsContent(
            task = taskWithReminder.dayTask,
            taskWithReminder = taskWithReminder,
            actions = actions,
            showAddToTodayOption = showAddToTodayOption,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun TaskOptionsContent(
    task: DayTask,
    taskWithReminder: DayTaskWithReminder,
    actions: TaskOptionsActions,
    showAddToTodayOption: Boolean,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = TASK_OPTIONS_VERTICAL_PADDING_DP.dp)) {
        PrimaryTaskOptions(actions = actions, onDismiss = onDismiss)
        TaskOptionsDivider()
        SecondaryTaskOptions(
            task = task,
            actions = actions,
            showAddToTodayOption = showAddToTodayOption,
            onDismiss = onDismiss,
        )
        TaskOptionsDivider()
        DeleteTaskOption(
            taskWithReminder = taskWithReminder,
            onDelete = actions.onDelete,
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun PrimaryTaskOptions(
    actions: TaskOptionsActions,
    onDismiss: () -> Unit,
) {
    TaskOptionsItem(
        label = "Редагувати",
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        onClick = actions.onEdit,
    )
    TaskOptionsItem(
        label = "Підняти на вершину списку",
        icon = { Icon(Icons.Outlined.VerticalAlignTop, contentDescription = null) },
        onClick = actions.onMoveToTop,
    )
    TaskOptionsItem(
        label = "Перенести на завтра",
        icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
        onClick = {
            actions.onMoveToTomorrow()
            onDismiss()
        },
    )
}

@Composable
private fun SecondaryTaskOptions(
    task: DayTask,
    actions: TaskOptionsActions,
    showAddToTodayOption: Boolean,
    onDismiss: () -> Unit,
) {
    TaskOptionsItem(
        label = "Встановити нагадування",
        icon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
        onClick = actions.onSetReminder,
    )
    TaskOptionsItem(
        label = "Додати в тактичні місії",
        icon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
        onClick = {
            actions.onAddToTacticalMissions()
            onDismiss()
        },
    )
    if (showAddToTodayOption) {
        TaskOptionsItem(
            label = "Додати в план на сьогодні",
            icon = { Icon(Icons.Outlined.Today, contentDescription = null) },
            onClick = {
                actions.onAddToToday()
                onDismiss()
            },
        )
    }
    if (task.hasBacklogTarget()) {
        TaskOptionsItem(
            label = "Показати в беклозі проекту",
            icon = { Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = null) },
            onClick = {
                logBacklogNavigationClick(task)
                actions.onShowInBacklog(task)
                onDismiss()
            },
        )
    }
}

@Composable
private fun DeleteTaskOption(
    taskWithReminder: DayTaskWithReminder,
    onDelete: (DayTaskWithReminder) -> Unit,
    onDismiss: () -> Unit,
) {
    TaskOptionsItem(
        label = "Видалити",
        color = MaterialTheme.colorScheme.error,
        icon = {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        onClick = {
            onDelete(taskWithReminder)
            onDismiss()
        },
    )
}

@Composable
private fun TaskOptionsDivider() {
    HorizontalDivider(
        modifier =
            Modifier.padding(
                horizontal = TASK_OPTIONS_DIVIDER_PADDING_DP.dp,
                vertical = TASK_OPTIONS_VERTICAL_PADDING_DP.dp,
            ),
    )
}

@Composable
private fun TaskOptionsItem(
    label: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    ListItem(
        headlineContent = { Text(label, color = color) },
        leadingContent = icon,
        modifier = Modifier.clickable(onClick = onClick),
        colors =
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
            ),
    )
}

private fun DayTask.hasBacklogTarget(): Boolean = projectId != null || goalId != null

private fun logBacklogNavigationClick(task: DayTask) {
    Log.d(TAG, "1. КЛІК: 'Показати в беклозі'.")
    Log.d(TAG, "   - Task Title: ${task.title}")
    Log.d(TAG, "   - Task ProjectID: ${task.projectId}")
    Log.d(TAG, "   - Task GoalID: ${task.goalId}")
    Log.d(TAG, "   - Task ID: ${task.id}")
}
