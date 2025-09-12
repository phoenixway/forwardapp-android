package com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ReminderDialog(
    record: ActivityRecord?,
    onDismiss: () -> Unit,
    onSetReminder: (Int, Int, Int, Int, Int) -> Unit,
    onClearReminder: () -> Unit,
) {
    if (record == null) return

    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val currentReminderTime = record.reminderTime

    LaunchedEffect(currentReminderTime) {
        if (currentReminderTime != null) {
            calendar.timeInMillis = currentReminderTime
            selectedDate = currentReminderTime
            selectedTime = calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Встановити нагадування") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Активність: ${record.text}")

                if (currentReminderTime != null) {
                    Text(
                        "Поточне нагадування: ${SimpleDateFormat(
                            "dd.MM.yyyy HH:mm",
                            Locale.getDefault(),
                        ).format(Date(currentReminderTime))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            selectedDate?.let {
                                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
                            } ?: "Дата",
                        )
                    }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            selectedTime?.let { (hour, minute) ->
                                String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                            } ?: "Час",
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (currentReminderTime != null) {
                    TextButton(onClick = onClearReminder) {
                        Text("Скасувати")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Button(
                    onClick = {
                        val date = selectedDate
                        val time = selectedTime
                        if (date != null && time != null) {
                            calendar.timeInMillis = date
                            onSetReminder(
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH),
                                time.first,
                                time.second,
                            )
                        }
                    },
                    enabled = selectedDate != null && selectedTime != null,
                ) {
                    Text("Встановити")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Відмінити")
            }
        },
    )

    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = selectedDate ?: System.currentTimeMillis(),
            )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Скасувати")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val currentTime = selectedTime ?: (calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE))
        val timePickerState =
            rememberTimePickerState(
                initialHour = currentTime.first,
                initialMinute = currentTime.second,
                is24Hour = true,
            )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = timePickerState.hour to timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Скасувати")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }
}
