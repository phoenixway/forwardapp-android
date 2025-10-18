package com.romankozak.forwardappmobile.ui.dialogs.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.dialogs.reminders.formatDateTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    selectedDateTime: Long?,
    onDateTimeClicked: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Дата і час нагадування:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )

        OutlinedCard(
            onClick = onDateTimeClicked,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = selectedDateTime?.let { formatDateTime(it) } ?: "Обрати дату і час",
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                    if (selectedDateTime != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initialDateTime: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val calendar = Calendar.getInstance()
    var selectedDate by remember {
        mutableStateOf(
            Calendar
                .getInstance()
                .apply {
                    timeInMillis = initialDateTime
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis,
        )
    }
    var selectedTime by remember {
        mutableStateOf(
            calendar.apply { timeInMillis = initialDateTime }.let {
                it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE)
            },
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Виберіть дату і час") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(selectedDate)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                String.format(Locale.getDefault(), "%02d:%02d", selectedTime.first, selectedTime.second),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                val finalDateTime =
                    Calendar
                        .getInstance()
                        .apply {
                            timeInMillis = selectedDate
                            set(Calendar.HOUR_OF_DAY, selectedTime.first)
                            set(Calendar.MINUTE, selectedTime.second)
                        }.timeInMillis

                Card(
                    colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ) {
                    Text(
                        text = "Нагадування: ${formatDateTime(finalDateTime)}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDateTime =
                        Calendar
                            .getInstance()
                            .apply {
                                timeInMillis = selectedDate
                                set(Calendar.HOUR_OF_DAY, selectedTime.first)
                                set(Calendar.MINUTE, selectedTime.second)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis

                    onConfirm(finalDateTime)
                },
                enabled =
                Calendar
                    .getInstance()
                    .apply {
                        timeInMillis = selectedDate
                        set(Calendar.HOUR_OF_DAY, selectedTime.first)
                        set(Calendar.MINUTE, selectedTime.second)
                    }.timeInMillis > System.currentTimeMillis(),
            ) {
                Text("Підтвердити")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        },
    )

    if (showDatePicker) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis = selectedDate,
            )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
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
        val timePickerState =
            rememberTimePickerState(
                initialHour = selectedTime.first,
                initialMinute = selectedTime.second,
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
