@file:OptIn(ExperimentalMaterial3Api::class)

package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.RecurrenceFrequency
import com.romankozak.forwardappmobile.data.database.models.RecurrenceRule
import java.time.DayOfWeek

@Composable
fun RecurrencePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (RecurrenceRule) -> Unit
) {
    var frequency by remember { mutableStateOf(RecurrenceFrequency.DAILY) }
    var selectedDays by remember { mutableStateOf(emptyList<DayOfWeek>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Налаштування повторення") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = false, onExpandedChange = {})
                 {
                    OutlinedTextField(
                        value = frequency.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Частота") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = false)
                        },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = false, onDismissRequest = { }) {
                        RecurrenceFrequency.values().forEach {
                            DropdownMenuItem(text = { Text(it.name) }, onClick = { frequency = it })
                        }
                    }
                }

                if (frequency == RecurrenceFrequency.WEEKLY) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Дні тижня:")
                    DayOfWeekSelector(selectedDays = selectedDays, onDaySelected = { day ->
                        selectedDays = if (selectedDays.contains(day)) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                    })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val rule = RecurrenceRule(
                    frequency = frequency,
                    daysOfWeek = if (frequency == RecurrenceFrequency.WEEKLY) selectedDays else null
                )
                onConfirm(rule)
            }) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}

@Composable
fun DayOfWeekSelector(
    selectedDays: List<DayOfWeek>,
    onDaySelected: (DayOfWeek) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        DayOfWeek.values().forEach { day ->
            FilterChip(
                selected = selectedDays.contains(day),
                onClick = { onDaySelected(day) },
                label = { Text(day.name.take(2)) }
            )
        }
    }
}
