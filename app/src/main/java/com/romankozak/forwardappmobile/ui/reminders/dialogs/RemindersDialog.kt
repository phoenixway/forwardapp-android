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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.core.database.models.ListItemContent
import com.romankozak.forwardappmobile.shared.features.reminders.data.model.Reminder
import com.romankozak.forwardappmobile.ui.reminders.dialogs.ReminderPropertiesDialog
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
            onRemoveReminder = null,
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
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val title = when (item) {
                        is ListItemContent.GoalItem -> item.goal.text
                        is ListItemContent.SublistItem -> item.project.name
                        else -> "Нагадування"
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.onShowPropertiesDialog() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Додати нагадування",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Reminders list
                if (reminders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Нагадувань поки немає",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(reminders) { reminderItem ->
                            ReminderItem(
                                reminder = reminderItem.reminder,
                                viewModel = viewModel
                            )
                        }
                    }
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                modifier = Modifier.size(18.dp)
            )
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ReminderTextUtil.formatReminderTime(reminder.reminderTime, System.currentTimeMillis()),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            IconButton(
                onClick = { viewModel.onEditReminder(reminder) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редагувати",
                    modifier = Modifier.size(18.dp)
                )
            }
            
            IconButton(
                onClick = { viewModel.deleteReminder(reminder) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Видалити",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
