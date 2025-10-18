package com.romankozak.forwardappmobile.ui.dialogs.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.dialogs.reminders.components.CustomDurationPicker
import com.romankozak.forwardappmobile.ui.dialogs.reminders.components.DateTimePicker
import com.romankozak.forwardappmobile.ui.dialogs.reminders.components.DateTimePickerDialog
import com.romankozak.forwardappmobile.ui.dialogs.reminders.components.QuickDurationPicker
import com.romankozak.forwardappmobile.ui.dialogs.reminders.components.ReminderTypeSelector
import com.romankozak.forwardappmobile.ui.dialogs.reminders.formatDateTime
import com.romankozak.forwardappmobile.ui.dialogs.reminders.models.ReminderDuration
import com.romankozak.forwardappmobile.ui.dialogs.reminders.models.ReminderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPropertiesDialog(
    onDismiss: () -> Unit,
    onSetReminder: (Long) -> Unit,
    onRemoveReminder: ((Long) -> Unit)? = null,
    currentReminders: List<com.romankozak.forwardappmobile.data.database.models.Reminder> = emptyList(),
) {
    var selectedType by remember { mutableStateOf(ReminderType.QUICK_DURATION) }
    var selectedDuration by remember { mutableStateOf<ReminderDuration?>(null) }
    var customMinutes by remember { mutableStateOf("") }
    var selectedDateTime by remember { mutableStateOf<Long?>(null) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    val quickDurations =
        remember {
            listOf(
                ReminderDuration("5 хв", 5),
                ReminderDuration("10 хв", 10),
                ReminderDuration("15 хв", 15),
                ReminderDuration("30 хв", 30),
                ReminderDuration("45 хв", 45),
                ReminderDuration("1 год", 60),
            )
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Встановити нагадування",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (currentReminders.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Поточні нагадування:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        currentReminders.forEach { reminder ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = formatDateTime(reminder.reminderTime),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Text(
                                            text = "Статус: ${reminder.status}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                    onRemoveReminder?.let {
                                        IconButton(onClick = { it(reminder.reminderTime) }, modifier = Modifier.size(24.dp)) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove reminder",
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ReminderTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it },
                )

                when (selectedType) {
                    ReminderType.QUICK_DURATION -> {
                        QuickDurationPicker(
                            durations = quickDurations,
                            selectedDuration = selectedDuration,
                            onDurationSelected = { selectedDuration = it },
                        )
                    }
                    ReminderType.CUSTOM_DURATION -> {
                        CustomDurationPicker(
                            minutes = customMinutes,
                            onMinutesChanged = { customMinutes = it },
                        )
                    }
                    ReminderType.SPECIFIC_DATETIME -> {
                        DateTimePicker(
                            selectedDateTime = selectedDateTime,
                            onDateTimeClicked = { showDateTimePicker = true },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val reminderTime =
                            when (selectedType) {
                                ReminderType.QUICK_DURATION -> {
                                    selectedDuration?.let { duration ->
                                        System.currentTimeMillis() + duration.minutes * 60 * 1000L
                                    }
                                }
                                ReminderType.CUSTOM_DURATION -> {
                                    customMinutes.toIntOrNull()?.let { minutes ->
                                        System.currentTimeMillis() + minutes * 60 * 1000L
                                    }
                                }
                                ReminderType.SPECIFIC_DATETIME -> selectedDateTime
                            }

                        reminderTime?.let { onSetReminder(it) }
                    },
                    enabled =
                    when (selectedType) {
                        ReminderType.QUICK_DURATION -> selectedDuration != null
                        ReminderType.CUSTOM_DURATION -> customMinutes.toIntOrNull()?.let { it > 0 } == true
                        ReminderType.SPECIFIC_DATETIME -> selectedDateTime != null && selectedDateTime!! > System.currentTimeMillis()
                    },
                ) {
                    Text("Додати")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        },
    )

    if (showDateTimePicker) {
        DateTimePickerDialog(
            initialDateTime = selectedDateTime ?: (System.currentTimeMillis() + 60 * 60 * 1000L),
            onDismiss = { showDateTimePicker = false },
            onConfirm = { dateTime ->
                selectedDateTime = dateTime
                showDateTimePicker = false
            },
        )
    }
}
