package com.romankozak.forwardappmobile.features.reminders.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.ui.components.AdaptiveSegmentedControl
import com.romankozak.forwardappmobile.ui.components.SegmentedTab
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val DEFAULT_CUSTOM_VALUE = "2"
private const val QUICK_PRESET_COLUMNS = 3
private const val DIALOG_WIDTH_FRACTION = 0.94f
private const val DEFAULT_REMINDER_HOUR = 9
private const val ZERO_MINUTES = 0L
private const val ZERO_SECONDS = 0
private const val ZERO_MILLIS = 0
private const val HOURS_PER_DAY = 24
private const val MINUTES_PER_HOUR = 60
private const val DELETE_ICON_SIZE = 18
private const val BUTTON_ICON_SIZE = 20
private const val COMPACT_ICON_SIZE = 18
private const val INPUT_CARD_PADDING = 12
private const val SECTION_SPACING = 12
private const val CHIP_SPACING = 8
private const val DEFAULT_TIME_PATTERN = "%02d:%02d"
private const val DEFAULT_TIME_PLACEHOLDER = "14:30"
private const val INVALID_TIME_ERROR = "Невірний формат часу"
private const val OUT_OF_RANGE_TIME_ERROR = "Години 0-23, хвилини 0-59"
private const val REMINDER_TIME_PATTERN = "dd MMM, HH:mm"
private const val PRESET_FIVE_MINUTES = 5L
private const val PRESET_TEN_MINUTES = 10L
private const val PRESET_FIFTEEN_MINUTES = 15L
private const val PRESET_TWENTY_MINUTES = 20L
private const val PRESET_THIRTY_MINUTES = 30L
private const val PRESET_ONE_HOUR_MINUTES = 60L
private const val PRESET_THREE_HOURS_MINUTES = 180L
private val UK_LOCALE = java.util.Locale("uk")

private data class ReminderPreset(
    val label: String,
    val minutes: Long,
    val isDatePreset: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPropertiesDialog(
    onDismiss: () -> Unit,
    onSetReminder: (time: Long) -> Unit,
    onRemoveReminder: ((String) -> Unit)? = null,
    currentReminders: List<Reminder> = emptyList(),
) {
    val datePickerState = rememberDatePickerState()
    var timeText by remember {
        val now = Calendar.getInstance()
        mutableStateOf(
            String.format(
                DEFAULT_TIME_PATTERN,
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
            ),
        )
    }
    var timeError by remember { mutableStateOf<String?>(null) }

    var customValue by remember { mutableStateOf(DEFAULT_CUSTOM_VALUE) }
    var selectedUnit by remember { mutableStateOf(TimeUnit.MINUTES) }
    var selectedTab by remember { mutableStateOf(0) }

    val quickPresets = remember { buildQuickPresets() }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(DIALOG_WIDTH_FRACTION),
        title = {
            Text(
                text = stringResource(id = R.string.set_reminder_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            ReminderDialogContent(
                currentReminders = currentReminders,
                onRemoveReminder = onRemoveReminder,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                quickPresets = quickPresets,
                onQuickPresetSelect = { minutes, isDate ->
                    onSetReminder(calculatePresetTime(minutes, isDate))
                    onDismiss()
                },
                datePickerState = datePickerState,
                timeText = timeText,
                timeError = timeError,
                onTimeChange = {
                    timeText = it
                    timeError = null
                },
                onExactDateSet = { dateMillis, hour, minute ->
                    onSetReminder(buildReminderTime(dateMillis, hour, minute))
                    onDismiss()
                },
                onTimeValidationError = { timeError = it },
                customValue = customValue,
                onCustomValueChange = { customValue = it },
                selectedUnit = selectedUnit,
                onUnitChange = { selectedUnit = it },
                onIntervalSet = {
                    onSetReminder(calculateCustomTime(customValue, selectedUnit))
                    onDismiss()
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDialogContent(
    currentReminders: List<Reminder>,
    onRemoveReminder: ((String) -> Unit)?,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    quickPresets: List<ReminderPreset>,
    onQuickPresetSelect: (Long, Boolean) -> Unit,
    datePickerState: DatePickerState,
    timeText: String,
    timeError: String?,
    onTimeChange: (String) -> Unit,
    onExactDateSet: (Long, Int, Int) -> Unit,
    onTimeValidationError: (String) -> Unit,
    customValue: String,
    onCustomValueChange: (String) -> Unit,
    selectedUnit: TimeUnit,
    onUnitChange: (TimeUnit) -> Unit,
    onIntervalSet: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING.dp),
    ) {
        if (currentReminders.isNotEmpty()) {
            CurrentRemindersSection(
                reminders = currentReminders,
                onRemoveReminder = onRemoveReminder,
            )
            HorizontalDivider()
        }

        ReminderModeTabs(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
        )

        when (selectedTab) {
            0 ->
                QuickPresetsSection(
                    presets = quickPresets,
                    onSelect = onQuickPresetSelect,
                )
            1 ->
                ExactDateSection(
                    datePickerState = datePickerState,
                    timeText = timeText,
                    timeError = timeError,
                    onTimeChange = onTimeChange,
                    onSet = onExactDateSet,
                    onTimeValidationError = onTimeValidationError,
                )
            else ->
                IntervalSection(
                    customValue = customValue,
                    onCustomValueChange = onCustomValueChange,
                    selectedUnit = selectedUnit,
                    onUnitChange = onUnitChange,
                    onSet = onIntervalSet,
                )
        }
    }
}

@Composable
private fun ReminderModeTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    AdaptiveSegmentedControl(
        tabs =
            listOf(
                SegmentedTab("Швидко", Icons.Default.Timer),
                SegmentedTab("Дата і час", Icons.Default.CalendarMonth),
                SegmentedTab("Інтервал", Icons.Default.Timer),
            ),
        selectedTabIndex = selectedTab,
        onTabSelected = onTabSelected,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun QuickPresetsSection(
    presets: List<ReminderPreset>,
    onSelect: (minutes: Long, isDatePreset: Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp)) {
        Text(
            text = "Швидкий вибір",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        presets.chunked(QUICK_PRESET_COLUMNS).forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp),
            ) {
                rowPresets.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { onSelect(preset.minutes, preset.isDatePreset) },
                        label = { Text(preset.label, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(QUICK_PRESET_COLUMNS - rowPresets.size) {
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
    Column(verticalArrangement = Arrangement.spacedBy(SECTION_SPACING.dp)) {
        Text(
            text = "Точна дата і час",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(INPUT_CARD_PADDING.dp),
                verticalArrangement = Arrangement.spacedBy(SECTION_SPACING.dp),
            ) {
                var showDate by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showDate = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(BUTTON_ICON_SIZE.dp),
                    )
                    Spacer(Modifier.width(CHIP_SPACING.dp))
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
                    placeholder = { Text(DEFAULT_TIME_PLACEHOLDER) },
                    isError = timeError != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        val helper = timeError ?: "Вкажіть час у форматі 24г, напр. 08:15"
                        Text(
                            helper,
                            color =
                                if (timeError != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
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
                    modifier = Modifier.fillMaxWidth(),
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
    Column(verticalArrangement = Arrangement.spacedBy(SECTION_SPACING.dp)) {
        Text(
            text = "Власний інтервал",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        CustomIntervalSection(
            customValue = customValue,
            onCustomValueChange = onCustomValueChange,
            selectedUnit = selectedUnit,
            onUnitChange = onUnitChange,
            onSet = onSet,
            onCancel = { /* no-op */ },
        )
    }
}

@Composable
private fun CurrentRemindersSection(
    reminders: List<Reminder>,
    onRemoveReminder: ((String) -> Unit)?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp),
    ) {
        Text(
            text = "Активні нагадування",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        reminders.forEach { reminder ->
            ReminderChip(
                reminder = reminder,
                onRemove = { onRemoveReminder?.invoke(reminder.id) },
            )
        }
    }
}

@Composable
private fun ReminderChip(
    reminder: Reminder,
    onRemove: () -> Unit,
) {
    val timeText =
        remember(reminder.reminderTime, reminder.snoozeUntil) {
            val time = reminder.snoozeUntil ?: reminder.reminderTime
            val format = java.text.SimpleDateFormat(REMINDER_TIME_PATTERN, UK_LOCALE)
            val prefix = if (reminder.snoozeUntil != null) "Відкладено: " else ""
            prefix + format.format(time)
        }

    val chipColors =
        when (reminder.status) {
            "SNOOZED" ->
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            "SCHEDULED" ->
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
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
                modifier = Modifier.size(DELETE_ICON_SIZE.dp),
            )
        },
        colors = chipColors,
    )
}

@Composable
private fun CustomIntervalSection(
    customValue: String,
    onCustomValueChange: (String) -> Unit,
    selectedUnit: TimeUnit,
    onUnitChange: (TimeUnit) -> Unit,
    onSet: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SECTION_SPACING.dp),
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
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp),
        ) {
            FilterChip(
                selected = selectedUnit == TimeUnit.MINUTES,
                onClick = { onUnitChange(TimeUnit.MINUTES) },
                label = { Text("Хвилини") },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = selectedUnit == TimeUnit.HOURS,
                onClick = { onUnitChange(TimeUnit.HOURS) },
                label = { Text("Години") },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = selectedUnit == TimeUnit.DAYS,
                onClick = { onUnitChange(TimeUnit.DAYS) },
                label = { Text("Дні") },
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CHIP_SPACING.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Скасувати")
            }
            Button(
                onClick = onSet,
                enabled = customValue.toLongOrNull()?.let { it > 0 } == true,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    modifier = Modifier.size(COMPACT_ICON_SIZE.dp),
                )
                Spacer(Modifier.width((CHIP_SPACING / 2).dp))
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
    val hour = parts.getOrNull(0)?.toIntOrNull()
    val minute = parts.getOrNull(1)?.toIntOrNull()
    val error =
        when {
            parts.size != 2 -> INVALID_TIME_ERROR
            hour == null || minute == null -> INVALID_TIME_ERROR
            hour !in 0 until HOURS_PER_DAY || minute !in 0 until MINUTES_PER_HOUR -> OUT_OF_RANGE_TIME_ERROR
            else -> null
        }

    return if (error == null) {
        Triple(hour ?: 0, minute ?: 0, null)
    } else {
        Triple(0, 0, error)
    }
}

private fun buildQuickPresets(): List<ReminderPreset> =
    listOf(
        ReminderPreset("5 хв", PRESET_FIVE_MINUTES),
        ReminderPreset("10 хв", PRESET_TEN_MINUTES),
        ReminderPreset("15 хв", PRESET_FIFTEEN_MINUTES),
        ReminderPreset("20 хв", PRESET_TWENTY_MINUTES),
        ReminderPreset("30 хв", PRESET_THIRTY_MINUTES),
        ReminderPreset("1 год", PRESET_ONE_HOUR_MINUTES),
        ReminderPreset("3 год", PRESET_THREE_HOURS_MINUTES),
    )

private fun calculatePresetTime(
    minutes: Long,
    isDatePreset: Boolean = false,
): Long {
    return if (isDatePreset && minutes == TimeUnit.DAYS.toMinutes(1)) {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, DEFAULT_REMINDER_HOUR)
            set(Calendar.MINUTE, ZERO_SECONDS)
            set(Calendar.SECOND, ZERO_SECONDS)
        }.timeInMillis
    } else {
        System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(minutes)
    }
}

private fun calculateCustomTime(
    customValue: String,
    selectedUnit: TimeUnit,
): Long {
    val value = customValue.toLongOrNull() ?: ZERO_MINUTES
    return System.currentTimeMillis() + selectedUnit.toMillis(value)
}

private fun buildReminderTime(
    dateMillis: Long,
    hour: Int,
    minute: Int,
): Long =
    Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, ZERO_SECONDS)
        set(Calendar.MILLISECOND, ZERO_MILLIS)
    }.timeInMillis
