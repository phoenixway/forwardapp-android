
package com.romankozak.forwardappmobile.ui.reminders.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.ui.dialogs.reminders.ReminderPropertiesDialog
import com.romankozak.forwardappmobile.ui.reminders.viewmodel.ReminderViewModel
import com.romankozak.forwardappmobile.ui.reminders.util.ReminderTextUtil
import java.util.*

@Composable
fun RemindersDialog(
    viewModel: com.romankozak.forwardappmobile.ui.reminders.viewmodel.ReminderViewModel,
    item: ListItemContent,
    onDismiss: () -> Unit,
) {
    val reminders by viewModel.reminders.collectAsState()
    val showPropertiesDialog by viewModel.showPropertiesDialog.collectAsState()

    val editingReminder by viewModel.editingReminder.collectAsState()

    if (showPropertiesDialog) {
        ReminderPropertiesDialog(
            onDismiss = { viewModel.onDismissPropertiesDialog() },
            onSetReminder = { time ->
                val reminderToUpdate = editingReminder
                if (reminderToUpdate != null) {
                    viewModel.updateReminder(reminderToUpdate.copy(reminderTime = time))
                } else {
                    viewModel.addReminder(time)
                }
                viewModel.onDismissPropertiesDialog()
            },
            onRemoveReminder = null, // Add logic if needed
            currentReminders = editingReminder?.let { listOf(it) } ?: emptyList()
        )
    }

    LaunchedEffect(item) {
        val entityId = when (item) {
            is ListItemContent.GoalItem -> item.goal.id
            is ListItemContent.SublistItem -> item.project.id
            else -> null
        }
        val entityType = when (item) {
            is ListItemContent.GoalItem -> "GOAL"
            is ListItemContent.SublistItem -> "PROJECT"
            else -> null
        }

        if (entityId != null && entityType != null) {
            viewModel.loadReminders(entityId, entityType)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                val title = when (item) {
                    is ListItemContent.GoalItem -> item.goal.text
                    is ListItemContent.SublistItem -> item.project.name
                    else -> "Reminders"
                }
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(reminders) { reminderItem ->
                        ReminderItem(
                            reminder = reminderItem.reminder,
                            viewModel = viewModel
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.onShowPropertiesDialog() }) {
                    Text("Add Reminder")
                }
            }
        }
    }
}

@Composable
private fun ReminderItem(
    reminder: Reminder,
    viewModel: com.romankozak.forwardappmobile.ui.reminders.viewmodel.ReminderViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isPast = reminder.reminderTime < System.currentTimeMillis()
        val statusIcon = when (reminder.status) {
            "COMPLETED" -> Icons.Default.CheckCircle
            "SNOOZED" -> Icons.Default.Snooze
            "DISMISSED" -> Icons.Default.Cancel
            else -> if (isPast) Icons.Default.Warning else Icons.Default.Schedule
        }
        val statusColor = when (reminder.status) {
            "COMPLETED" -> MaterialTheme.colorScheme.primary
            "SNOOZED" -> MaterialTheme.colorScheme.secondary
            "DISMISSED" -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        }
        val statusText = when (reminder.status) {
            "COMPLETED" -> "Виконано"
            "SNOOZED" -> "Відкладено"
            "DISMISSED" -> "Пропущено"
            else -> if (isPast) "Прострочено" else "Заплановано"
        }

        Icon(
            imageVector = statusIcon,
            contentDescription = statusText,
            tint = statusColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ReminderTextUtil.formatReminderTime(reminder.reminderTime, System.currentTimeMillis()),
            )
            Text(
                text = statusText,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall
            )
        }
        IconButton(onClick = { viewModel.onEditReminder(reminder) }) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}
