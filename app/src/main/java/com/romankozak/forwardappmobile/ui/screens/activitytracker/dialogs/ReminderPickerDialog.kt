package com.romankozak.forwardappmobile.ui.screens.activitytracker.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

data class ReminderDuration(
    val label: String,
    val minutes: Int
)

enum class ReminderType {
    QUICK_DURATION,
    CUSTOM_DURATION,
    SPECIFIC_DATETIME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPickerDialog(
    onDismiss: () -> Unit,
    onSetReminder: (Long) -> Unit,
    onClearReminder: (() -> Unit)? = null,
    currentReminderTime: Long? = null
) {
    var selectedType by remember { mutableStateOf(ReminderType.QUICK_DURATION) }
    var selectedDuration by remember { mutableStateOf<ReminderDuration?>(null) }
    var customMinutes by remember { mutableStateOf("") }
    var selectedDateTime by remember { mutableStateOf<Long?>(null) }
    var showDateTimePicker by remember { mutableStateOf(false) }

    val quickDurations = remember {
        listOf(
            ReminderDuration("5 хв", 5),
            ReminderDuration("10 хв", 10),
            ReminderDuration("15 хв", 15),
            ReminderDuration("30 хв", 30),
            ReminderDuration("45 хв", 45),
            ReminderDuration("1 год", 60)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Встановити нагадування",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Показуємо поточне нагадування якщо є
                currentReminderTime?.let { time ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Поточне нагадування:\n${formatDateTime(time)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Типи нагадувань
                ReminderTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )

                // Контент залежно від типу
                when (selectedType) {
                    ReminderType.QUICK_DURATION -> {
                        QuickDurationPicker(
                            durations = quickDurations,
                            selectedDuration = selectedDuration,
                            onDurationSelected = { selectedDuration = it }
                        )
                    }
                    ReminderType.CUSTOM_DURATION -> {
                        CustomDurationPicker(
                            minutes = customMinutes,
                            onMinutesChanged = { customMinutes = it }
                        )
                    }
                    ReminderType.SPECIFIC_DATETIME -> {
                        DateTimePicker(
                            selectedDateTime = selectedDateTime,
                            onDateTimeClicked = { showDateTimePicker = true }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onClearReminder?.let {
                    if (currentReminderTime != null) {
                        TextButton(onClick = it) {
                            Text("Скасувати")
                        }
                    }
                }

                Button(
                    onClick = {
                        val reminderTime = when (selectedType) {
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
                    enabled = when (selectedType) {
                        ReminderType.QUICK_DURATION -> selectedDuration != null
                        ReminderType.CUSTOM_DURATION -> customMinutes.toIntOrNull()?.let { it > 0 } == true
                        ReminderType.SPECIFIC_DATETIME -> selectedDateTime != null && selectedDateTime!! > System.currentTimeMillis()
                    }
                ) {
                    Text("Встановити")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Відмінити")
            }
        }
    )

    // Діалог вибору дати і часу
    if (showDateTimePicker) {
        DateTimePickerDialog(
            initialDateTime = selectedDateTime ?: (System.currentTimeMillis() + 60 * 60 * 1000L), // +1 година за замовчуванням
            onDismiss = { showDateTimePicker = false },
            onConfirm = { dateTime ->
                selectedDateTime = dateTime
                showDateTimePicker = false
            }
        )
    }
}

@Composable
private fun ReminderTypeSelector(
    selectedType: ReminderType,
    onTypeSelected: (ReminderType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Тип нагадування:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ReminderTypeItem(
                title = "Швидкий вибір",
                subtitle = "Типові інтервали часу",
                isSelected = selectedType == ReminderType.QUICK_DURATION,
                onClick = { onTypeSelected(ReminderType.QUICK_DURATION) }
            )

            ReminderTypeItem(
                title = "Власний інтервал",
                subtitle = "Вказати хвилини вручну",
                isSelected = selectedType == ReminderType.CUSTOM_DURATION,
                onClick = { onTypeSelected(ReminderType.CUSTOM_DURATION) }
            )

            ReminderTypeItem(
                title = "Конкретна дата і час",
                subtitle = "Вибрати точний момент",
                isSelected = selectedType == ReminderType.SPECIFIC_DATETIME,
                onClick = { onTypeSelected(ReminderType.SPECIFIC_DATETIME) }
            )
        }
    }
}

@Composable
private fun ReminderTypeItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickDurationPicker(
    durations: List<ReminderDuration>,
    selectedDuration: ReminderDuration?,
    onDurationSelected: (ReminderDuration) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Через скільки нагадати:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        // Розбиваємо на рядки по 3 елементи
        val chunkedDurations = durations.chunked(3)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chunkedDurations.forEach { rowDurations ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowDurations.forEach { duration ->
                        DurationChip(
                            duration = duration,
                            isSelected = selectedDuration == duration,
                            onClick = { onDurationSelected(duration) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Додаємо пустий простір якщо в останньому рядку менше 3 елементів
                    repeat(3 - rowDurations.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationChip(
    duration: ReminderDuration,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = { Text(duration.label) },
        selected = isSelected,
        leadingIcon = if (isSelected) {
            { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        modifier = modifier
    )
}

@Composable
private fun CustomDurationPicker(
    minutes: String,
    onMinutesChanged: (String) -> Unit
) {
    val currentMinutes = minutes.toIntOrNull() ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Через скільки хвилин нагадати:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        // Швидкі кнопки налаштування
        // File: ReminderPickerDialog.kt

        FlowRow( // Використовуємо FlowRow для автоматичного перенесення
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp), // Відступи між кнопками
            verticalArrangement = Arrangement.spacedBy(8.dp)   // Відступи між рядами кнопок
        ) {
            listOf(1, 2, 3, 5, 10, 30).forEach { mins ->
                OutlinedButton(
                    onClick = {
                        val newValue = maxOf(0, currentMinutes + mins)
                        onMinutesChanged(newValue.toString())
                    },
                    // ВИДАЛЕНО: modifier = Modifier.weight(1f)
                    // Дозволяємо кнопці займати лише потрібну ширину
                ) {
                    Text(
                        text = "+$mins",
                        softWrap = false
                    )
                }
            }
        }

        // Слайдер для точного налаштування
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Хвилин: $currentMinutes",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Row {
                        IconButton(
                            onClick = {
                                val newValue = maxOf(0, currentMinutes - 1)
                                onMinutesChanged(newValue.toString())
                            },
                            enabled = currentMinutes > 0
                        ) {
                            Text("−", style = MaterialTheme.typography.headlineSmall)
                        }

                        IconButton(
                            onClick = {
                                val newValue = minOf(1440, currentMinutes + 1) // максимум 24 години
                                onMinutesChanged(newValue.toString())
                            },
                            enabled = currentMinutes < 1440
                        ) {
                            Text("+", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }

                Slider(
                    value = currentMinutes.toFloat(),
                    onValueChange = { onMinutesChanged(it.toInt().toString()) },
                    valueRange = 1f..240f, // від 1 хвилини до 4 годин
                    steps = 239,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 хв", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("4 год", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Показуємо час нагадування
        if (currentMinutes > 0) {
            val future = System.currentTimeMillis() + currentMinutes * 60 * 1000L
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Нагадування: ${formatDateTime(future)}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Швидкі пресети для великих значень
        Text(
            "Або виберіть:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val presets = listOf(
            "2 год" to 120,
            "3 год" to 180,
            "6 год" to 360,
            "12 год" to 720,
            "24 год" to 1440
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            presets.take(3).forEach { (label, mins) ->
                FilterChip(
                    onClick = { onMinutesChanged(mins.toString()) },
                    label = { Text(label) },
                    selected = currentMinutes == mins,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            presets.drop(3).forEach { (label, mins) ->
                FilterChip(
                    onClick = { onMinutesChanged(mins.toString()) },
                    label = { Text(label) },
                    selected = currentMinutes == mins,
                    modifier = Modifier.weight(1f)
                )
            }
            // Заповнюємо пустий простір
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DateTimePicker(
    selectedDateTime: Long?,
    onDateTimeClicked: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Дата і час нагадування:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )

        OutlinedCard(
            onClick = onDateTimeClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = selectedDateTime?.let { formatDateTime(it) } ?: "Обрати дату і час",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selectedDateTime != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(
    initialDateTime: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    var selectedDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                timeInMillis = initialDateTime
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var selectedTime by remember {
        mutableStateOf(
            calendar.apply { timeInMillis = initialDateTime }.let {
                it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE)
            }
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(selectedDate)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                String.format(Locale.getDefault(), "%02d:%02d", selectedTime.first, selectedTime.second),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                val finalDateTime = Calendar.getInstance().apply {
                    timeInMillis = selectedDate
                    set(Calendar.HOUR_OF_DAY, selectedTime.first)
                    set(Calendar.MINUTE, selectedTime.second)
                }.timeInMillis

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "Нагадування: ${formatDateTime(finalDateTime)}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalDateTime = Calendar.getInstance().apply {
                        timeInMillis = selectedDate
                        set(Calendar.HOUR_OF_DAY, selectedTime.first)
                        set(Calendar.MINUTE, selectedTime.second)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    onConfirm(finalDateTime)
                },
                enabled = Calendar.getInstance().apply {
                    timeInMillis = selectedDate
                    set(Calendar.HOUR_OF_DAY, selectedTime.first)
                    set(Calendar.MINUTE, selectedTime.second)
                }.timeInMillis > System.currentTimeMillis()
            ) {
                Text("Підтвердити")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
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
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.first,
            initialMinute = selectedTime.second,
            is24Hour = true
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
            }
        )
    }
}

private fun formatDateTime(timeMillis: Long): String {
    return SimpleDateFormat("dd.MM.yyyy 'о' HH:mm", Locale.getDefault()).format(Date(timeMillis))
}