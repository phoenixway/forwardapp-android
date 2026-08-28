package com.romankozak.forwardappmobile.features.reminders.components

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
import com.romankozak.forwardappmobile.features.reminders.util.ReminderTextUtil.formatDateTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val DateTimePickerIconSize = 16.dp

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
    enablePastValues: Boolean = false,
    title: String = "Виберіть дату і час",
    summaryLabel: String = "Нагадування",
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
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateTimeSelectionButtons(
                    selectedDate = selectedDate,
                    selectedTime = selectedTime,
                    onDateClick = { showDatePicker = true },
                    onTimeClick = { showTimePicker = true },
                )

                val finalDateTime = buildDateTime(selectedDate, selectedTime)

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                ) {
                    Text(
                        text = "$summaryLabel: ${formatDateTime(finalDateTime)}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(buildDateTime(selectedDate, selectedTime)) },
                enabled = enablePastValues || buildDateTime(selectedDate, selectedTime) > System.currentTimeMillis(),
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
        DateSelectionDialog(
            selectedDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate = it },
        )
    }

    if (showTimePicker) {
        TimeSelectionDialog(
            selectedTime = selectedTime,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { selectedTime = it },
        )
    }
}

@Composable
private fun DateTimeSelectionButtons(
    selectedDate: Long,
    selectedTime: Pair<Int, Int>,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedButton(
            onClick = onDateClick,
            modifier = Modifier.weight(1f),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(DateTimePickerIconSize),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(selectedDate)),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        OutlinedButton(
            onClick = onTimeClick,
            modifier = Modifier.weight(1f),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(DateTimePickerIconSize),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        selectedTime.first,
                        selectedTime.second,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectionDialog(
    selectedDate: Long,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let(onDateSelected)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectionDialog(
    selectedTime: Pair<Int, Int>,
    onDismiss: () -> Unit,
    onTimeSelected: (Pair<Int, Int>) -> Unit,
) {
    val timePickerState =
        rememberTimePickerState(
            initialHour = selectedTime.first,
            initialMinute = selectedTime.second,
            is24Hour = true,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(timePickerState.hour to timePickerState.minute)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        },
        text = { TimePicker(state = timePickerState) },
    )
}

private fun buildDateTime(
    selectedDate: Long,
    selectedTime: Pair<Int, Int>,
): Long =
    Calendar
        .getInstance()
        .apply {
            timeInMillis = selectedDate
            set(Calendar.HOUR_OF_DAY, selectedTime.first)
            set(Calendar.MINUTE, selectedTime.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
