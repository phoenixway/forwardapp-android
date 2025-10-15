
package com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.Reminder
import com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs.ReminderPickerDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.viewmodel.RemindersViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemindersDialog(
    viewModel: RemindersViewModel,
    item: ListItemContent,
    onDismiss: () -> Unit,
) {
    val reminders by viewModel.reminders.collectAsState()
    val showTimePickerDialog by viewModel.showTimePickerDialog.collectAsState()

    val editingReminder by viewModel.editingReminder.collectAsState()

    if (editingReminder != null) {
        ReminderPickerDialog(
            onDismiss = { viewModel.onEditReminderDismiss() },
            onSetReminder = { time ->
                editingReminder?.let {
                    viewModel.updateReminder(it.copy(reminderTime = time))
                }
                viewModel.onEditReminderDismiss()
            },
            onRemoveReminder = null,
            currentReminders = listOfNotNull(editingReminder)
        )
    }

    if (showTimePickerDialog) {
        ReminderPickerDialog(
            onDismiss = { viewModel.onAddTimePickerDismiss() },
            onSetReminder = { time ->
                viewModel.addReminder(time)
                viewModel.onAddTimePickerDismiss()
            },
            onRemoveReminder = null,
            currentReminders = emptyList()
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
                Text("Reminders", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(reminders) { reminder ->
                        ReminderItem(
                            reminder = reminder,
                            viewModel = viewModel
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.onAddTimePickerShow() }) {
                    Text("Add Reminder")
                }
            }
        }
    }
}

@Composable
private fun ReminderItem(
    reminder: Reminder,
    viewModel: RemindersViewModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(reminder.reminderTime)),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { viewModel.onEditReminder(reminder) }) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
        }
        IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}
