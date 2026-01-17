package com.romankozak.forwardappmobile.features.daymanagement.presentation.dayplan.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.RecurrenceFrequency
import com.romankozak.forwardappmobile.data.database.models.RecurrenceRule
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedRecurrencePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (RecurrenceRule?) -> Unit
) {
    var selectedFrequency by remember { mutableStateOf(RecurrenceFrequency.DAILY) }
    var interval by remember { mutableStateOf("1") }
    var selectedDays by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Налаштувати повторення") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Frequency Selector
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedFrequency.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Частота") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        RecurrenceFrequency.values().filter { it != RecurrenceFrequency.MONTHLY && it != RecurrenceFrequency.YEARLY }.forEach { frequency ->
                            DropdownMenuItem(text = { Text(frequency.name) }, onClick = {
                                selectedFrequency = frequency
                                expanded = false
                            })
                        }
                    }
                }

                // Interval Input
                OutlinedTextField(
                    value = interval,
                    onValueChange = { if (it.all { char -> char.isDigit() }) interval = it },
                    label = { Text("Інтервал (в ${getIntervalUnit(selectedFrequency)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Day of Week Selector
                if (selectedFrequency == RecurrenceFrequency.WEEKLY) {
                    DayOfWeekSelector(selectedDays = selectedDays, onDaySelected = { day ->
                        selectedDays = if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                    })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalInterval = interval.toIntOrNull() ?: 1
                val rule = RecurrenceRule(
                    frequency = selectedFrequency,
                    interval = finalInterval,
                    daysOfWeek = if (selectedFrequency == RecurrenceFrequency.WEEKLY) selectedDays.toList() else null
                )
                onConfirm(rule)
            }) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) {
                Text("Не повторювати")
            }
        }
    )
}

@Composable
private fun DayOfWeekSelector(
    selectedDays: Set<DayOfWeek>,
    onDaySelected: (DayOfWeek) -> Unit
) {
    val days = DayOfWeek.values()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            FilterChip(
                selected = selectedDays.contains(day),
                onClick = { onDaySelected(day) },
                label = { Text(day.name.first().toString()) },
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

private fun getIntervalUnit(frequency: RecurrenceFrequency): String {
    return when (frequency) {
        RecurrenceFrequency.HOURLY -> "годинах"
        RecurrenceFrequency.DAILY -> "днях"
        RecurrenceFrequency.WEEKLY -> "тижнях"
        else -> ""
    }
}
