package com.romankozak.forwardappmobile.ui.reminders.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.components.AdaptiveSegmentedControl
import com.romankozak.forwardappmobile.ui.components.SegmentedTab
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.Reminder
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class ReminderPreset(
    val label: String,
    val minutes: Long,
    val isDatePreset: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPropertiesDialog(
    onDismiss: () -> Unit,
    onSetReminder: (time: Long) -> Unit,
    onRemoveReminder: ((String) -> Unit)? = null,
    currentReminders: List<Reminder> = emptyList()
) {
    val datePickerState = rememberDatePickerState()
    var timeText by remember {
        val now = Calendar.getInstance()
        mutableStateOf(String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)))
    }
    var timeError by remember { mutableStateOf<String?>(null) }

    var customValue by remember { mutableStateOf("2") }
    var selectedUnit by remember { mutableStateOf(TimeUnit.MINUTES) }
    var showCustomInput by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    // Об'єднані пресети для кращого UX
    val quickPresets = remember {
        listOf(
            ReminderPreset("5 хв", 5L),
            ReminderPreset("10 хв", 10L),
            ReminderPreset("15 хв", 15L),
            ReminderPreset("20 хв", 20L),
            ReminderPreset("30 хв", 30L),
            ReminderPreset("1 год", 60L),
            ReminderPreset("3 год", 180L),
        )
    }

    fun calculatePresetTime(minutes: Long, isDatePreset: Boolean = false): Long {
        return if (isDatePreset && minutes == TimeUnit.DAYS.toMinutes(1)) {
            // Завтра о 9:00
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        } else {
            System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes)
        }
    }

    fun calculateCustomTime(): Long {
        val value = customValue.toLongOrNull() ?: 0
        return System.currentTimeMillis() + selectedUnit.toMillis(value)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.94f),
        title = { 
            Text(
                text = stringResource(id = R.string.set_reminder_title),
                style = MaterialTheme.typography.titleLarge
            ) 
        },
    text = {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                // Відображення поточних нагадувань
                if (currentReminders.isNotEmpty()) {
                    CurrentRemindersSection(
                        reminders = currentReminders,
                        onRemoveReminder = onRemoveReminder
                    )
                    HorizontalDivider()
                }

                // Перемикач режимів
                AdaptiveSegmentedControl(
                    tabs = listOf(
                        SegmentedTab("Швидко", Icons.Default.Timer),
                        SegmentedTab("Дата і час", Icons.Default.CalendarMonth),
                        SegmentedTab("Інтервал", Icons.Default.Timer),
                    ),
                    selectedTabIndex = selectedTab,
                    onTabSelected = { index ->
                        selectedTab = index
                        if (index != 2) {
                            showCustomInput = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                when (selectedTab) {
                    0 -> QuickPresetsSection(
                        presets = quickPresets,
                        onSelect = { minutes, isDate ->
                            onSetReminder(calculatePresetTime(minutes, isDate))
                            onDismiss()
                        }
                    )
                    1 -> ExactDateSection(
                        datePickerState = datePickerState,
                        timeText = timeText,
                        timeError = timeError,
                        onTimeChange = {
                            timeText = it
                            timeError = null
                        },
                        onSet = { dateMillis, hour, minute ->
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = dateMillis
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onSetReminder(calendar.timeInMillis)
                            onDismiss()
                        },
                        onTimeValidationError = { timeError = it }
                    )
                    2 -> IntervalSection(
                        customValue = customValue,
                        onCustomValueChange = { customValue = it },
                        selectedUnit = selectedUnit,
                        onUnitChange = { selectedUnit = it },
                        onSet = {
                            onSetReminder(calculateCustomTime())
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )

}

@Composable
private fun QuickPresetsSection(
    presets: List<ReminderPreset>,
    onSelect: (minutes: Long, isDatePreset: Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Швидкий вибір",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        presets.chunked(3).forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPresets.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { onSelect(preset.minutes, preset.isDatePreset) },
                        label = { Text(preset.label, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowPresets.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExactDateSection(
    datePickerState: DatePickerState,
    timeText: String,
    timeError: String?,
    onTimeChange: (String) -> Unit,
    onSet: (dateMillis: Long, hour: Int, minute: Int) -> Unit,
    onTimeValidationError: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Точна дата і час",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Опційний вибір іншої дати
                var showDate by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showDate = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Вибрати дату (не сьогодні)")
                }

                if (showDate) {
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false,
                        headline = { Text("Оберіть дату", style = MaterialTheme.typography.titleMedium) },
                    )
                }

                OutlinedTextField(
                    value = timeText,
                    onValueChange = onTimeChange,
                    label = { Text("Час (HH:MM)") },
                    placeholder = { Text("14:30") },
                    isError = timeError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        val helper = timeError ?: "Вкажіть час у форматі 24г, напр. 08:15"
                        Text(helper, color = if (timeError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Button(
                    onClick = {
                        val dateMillis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        val (h, m, error) = parseHourMinute(timeText)
                        if (error != null) {
                            onTimeValidationError(error)
                            return@Button
                        }
                        onSet(dateMillis, h, m)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Встановити")
                }
            }
        }
    }
}

@Composable
private fun IntervalSection(
    customValue: String,
    onCustomValueChange: (String) -> Unit,
    selectedUnit: TimeUnit,
    onUnitChange: (TimeUnit) -> Unit,
    onSet: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Власний інтервал",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CustomIntervalSection(
            customValue = customValue,
            onCustomValueChange = onCustomValueChange,
            selectedUnit = selectedUnit,
            onUnitChange = onUnitChange,
            onSet = onSet,
            onCancel = { /* no-op */ }
        )
    }
}

@Composable
private fun CurrentRemindersSection(
    reminders: List<Reminder>,
    onRemoveReminder: ((String) -> Unit)?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Активні нагадування",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        reminders.forEach { reminder ->
            ReminderChip(
                reminder = reminder,
                onRemove = { onRemoveReminder?.invoke(reminder.id) }
            )
        }
    }
}

@Composable
private fun ReminderChip(
    reminder: Reminder,
    onRemove: () -> Unit
) {
    val timeText = remember(reminder.reminderTime, reminder.snoozeUntil) {
        val time = reminder.snoozeUntil ?: reminder.reminderTime
        val format = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale("uk"))
        val prefix = if (reminder.snoozeUntil != null) "Відкладено: " else ""
        prefix + format.format(time)
    }

    val chipColors = when (reminder.status) {
        "SNOOZED" -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        "SCHEDULED" -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        else -> AssistChipDefaults.assistChipColors()
    }

    AssistChip(
        onClick = onRemove,
        label = { Text(timeText) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Видалити нагадування",
                modifier = Modifier.size(18.dp)
            )
        },
        colors = chipColors
    )
}

@Composable
private fun CustomIntervalSection(
    customValue: String,
    onCustomValueChange: (String) -> Unit,
    selectedUnit: TimeUnit,
    onUnitChange: (TimeUnit) -> Unit,
    onSet: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = customValue,
            onValueChange = onCustomValueChange,
            label = { Text("Введіть значення") },
            placeholder = { Text("15") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = {
                Text("Мінімум 1 ${getUnitName(selectedUnit)}")
            }
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedUnit == TimeUnit.MINUTES,
                onClick = { onUnitChange(TimeUnit.MINUTES) },
                label = { Text("Хвилини") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedUnit == TimeUnit.HOURS,
                onClick = { onUnitChange(TimeUnit.HOURS) },
                label = { Text("Години") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedUnit == TimeUnit.DAYS,
                onClick = { onUnitChange(TimeUnit.DAYS) },
                label = { Text("Дні") },
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Скасувати")
            }
            Button(
                onClick = onSet,
                enabled = customValue.toLongOrNull()?.let { it > 0 } == true,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Встановити")
            }
        }
    }
}

private fun getUnitName(unit: TimeUnit): String {
    return when (unit) {
        TimeUnit.MINUTES -> "хвилину"
        TimeUnit.HOURS -> "годину"
        TimeUnit.DAYS -> "день"
        else -> ""
    }
}

private fun parseHourMinute(input: String): Triple<Int, Int, String?> {
    val trimmed = input.trim()
    val parts = trimmed.split(":")
    if (parts.size != 2) return Triple(0, 0, "Невірний формат часу")
    val hour = parts[0].toIntOrNull() ?: return Triple(0, 0, "Невірний формат часу")
    val minute = parts[1].toIntOrNull() ?: return Triple(0, 0, "Невірний формат часу")
    if (hour !in 0..23 || minute !in 0..59) return Triple(0, 0, "Години 0-23, хвилини 0-59")
    return Triple(hour, minute, null)
}
