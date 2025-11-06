package com.romankozak.forwardappmobile.ui.reminders.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.shared.features.reminders.data.model.Reminder
import java.util.Calendar
import java.util.concurrent.TimeUnit

 @OptIn(ExperimentalMaterial3Api::class)
 @Composable
fun ReminderPropertiesDialog(
    onDismiss: () -> Unit,
    onSetReminder: (time: Long) -> Unit,
    onRemoveReminder: (() -> Unit)? = null,
    currentReminders: List<Reminder> = emptyList()
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    var customValue by remember { mutableStateOf("15") }
    var selectedUnit by remember { mutableStateOf(TimeUnit.MINUTES) }

    val quickPresets = listOf(
        "5 хв" to 5L,
        "15 хв" to 15L,
        "30 хв" to 30L,
        "1 год" to 60L,
        "3 год" to 180L,
    )
    
    val datePresets = listOf(
        "Завтра" to TimeUnit.DAYS.toMinutes(1),
        "Тиждень" to TimeUnit.DAYS.toMinutes(7)
    )

    fun calculatePresetTime(minutes: Long): Long {
        return System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes)
    }

    fun calculateCustomTime(): Long {
        val value = customValue.toLongOrNull() ?: 0
        val now = System.currentTimeMillis()
        return now + selectedUnit.toMillis(value)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = stringResource(id = R.string.set_reminder_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick presets
                Text(
                    text = "Швидкий вибір",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                FlowRow(
                    mainAxisSpacing = 6.dp,
                    crossAxisSpacing = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    quickPresets.forEach { (label, minutes) ->
                        FilterChip(
                            selected = false,
                            onClick = { 
                                onSetReminder(calculatePresetTime(minutes))
                                onDismiss()
                            },
                            label = { Text(label) }
                        )
                    }
                }

                // Date presets
                FlowRow(
                    mainAxisSpacing = 6.dp,
                    crossAxisSpacing = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    datePresets.forEach { (label, minutes) ->
                        FilterChip(
                            selected = false,
                            onClick = { 
                                onSetReminder(calculatePresetTime(minutes))
                                onDismiss()
                            },
                            label = { Text(label) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Custom date & time
                Text(
                    text = "Вибрати дату",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                FilledTonalButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Обрати дату і час")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Custom interval
                Text(
                    text = "Власний інтервал",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customValue,
                        onValueChange = { customValue = it },
                        label = { Text("Значення") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedUnit == TimeUnit.MINUTES,
                        onClick = { selectedUnit = TimeUnit.MINUTES },
                        label = { Text("Хвилини") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedUnit == TimeUnit.HOURS,
                        onClick = { selectedUnit = TimeUnit.HOURS },
                        label = { Text("Години") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedUnit == TimeUnit.DAYS,
                        onClick = { selectedUnit = TimeUnit.DAYS },
                        label = { Text("Дні") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                FilledTonalButton(
                    onClick = { 
                        onSetReminder(calculateCustomTime())
                        onDismiss()
                    },
                    enabled = customValue.toLongOrNull() != null && customValue.toLongOrNull()!! > 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Встановити")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Далі") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Скасувати") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Виберіть час") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = dateMillis
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                            }
                            onSetReminder(calendar.timeInMillis)
                            onDismiss()
                        }
                    },
                ) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Скасувати") }
            },
        )
    }
}
