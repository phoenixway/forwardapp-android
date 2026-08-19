package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedRecurrencePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (RecurrenceRule?) -> Unit,
) {
    var selectedFrequency by remember { mutableStateOf(RecurrenceFrequency.DAILY) }
    var interval by remember { mutableStateOf("1") }
    var selectedDays by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Налаштувати повторення") },
        text = {
            AdvancedRecurrenceDialogContent(
                selectedFrequency = selectedFrequency,
                interval = interval,
                selectedDays = selectedDays,
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                onDismissDropdown = { expanded = false },
                onFrequencySelected = { frequency ->
                    selectedFrequency = frequency
                    expanded = false
                },
                onIntervalChange = { value ->
                    if (value.all { char -> char.isDigit() }) {
                        interval = value
                    }
                },
                onDaySelected = { day ->
                    selectedDays =
                        if (selectedDays.contains(day)) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                },
            )
        },
        confirmButton = {
            AdvancedRecurrenceConfirmButton(
                selectedFrequency = selectedFrequency,
                interval = interval,
                selectedDays = selectedDays,
                onConfirm = onConfirm,
            )
        },
        dismissButton = {
            TextButton(onClick = { onConfirm(null) }) {
                Text("Не повторювати")
            }
        },
    )
}

@Composable
private fun AdvancedRecurrenceDialogContent(
    selectedFrequency: RecurrenceFrequency,
    interval: String,
    selectedDays: Set<DayOfWeek>,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onDismissDropdown: () -> Unit,
    onFrequencySelected: (RecurrenceFrequency) -> Unit,
    onIntervalChange: (String) -> Unit,
    onDaySelected: (DayOfWeek) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FrequencySelector(
            selectedFrequency = selectedFrequency,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            onDismiss = onDismissDropdown,
            onFrequencySelected = onFrequencySelected,
        )
        IntervalField(
            interval = interval,
            selectedFrequency = selectedFrequency,
            onValueChange = onIntervalChange,
        )
        if (selectedFrequency == RecurrenceFrequency.WEEKLY) {
            DayOfWeekSelector(
                selectedDays = selectedDays,
                onDaySelected = onDaySelected,
            )
        }
    }
}

@Composable
private fun AdvancedRecurrenceConfirmButton(
    selectedFrequency: RecurrenceFrequency,
    interval: String,
    selectedDays: Set<DayOfWeek>,
    onConfirm: (RecurrenceRule?) -> Unit,
) {
    Button(
        onClick = {
            onConfirm(
                buildRecurrenceRule(
                    selectedFrequency = selectedFrequency,
                    interval = interval,
                    selectedDays = selectedDays,
                ),
            )
        },
    ) {
        Text("Зберегти")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelector(
    selectedFrequency: RecurrenceFrequency,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    onDismiss: () -> Unit,
    onFrequencySelected: (RecurrenceFrequency) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange() },
    ) {
        OutlinedTextField(
            value = selectedFrequency.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Частота") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
        ) {
            supportedRecurrenceFrequencies().forEach { frequency ->
                DropdownMenuItem(
                    text = { Text(frequency.name) },
                    onClick = { onFrequencySelected(frequency) },
                )
            }
        }
    }
}

@Composable
private fun IntervalField(
    interval: String,
    selectedFrequency: RecurrenceFrequency,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = interval,
        onValueChange = onValueChange,
        label = { Text("Інтервал (в ${getIntervalUnit(selectedFrequency)})") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DayOfWeekSelector(
    selectedDays: Set<DayOfWeek>,
    onDaySelected: (DayOfWeek) -> Unit,
) {
    val days = DayOfWeek.values()
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            FilterChip(
                selected = selectedDays.contains(day),
                onClick = { onDaySelected(day) },
                label = { Text(day.name.first().toString()) },
                modifier = Modifier.padding(horizontal = 2.dp),
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

private fun supportedRecurrenceFrequencies(): List<RecurrenceFrequency> =
    RecurrenceFrequency.values().filter { frequency ->
        frequency != RecurrenceFrequency.MONTHLY &&
            frequency != RecurrenceFrequency.YEARLY
    }

private fun buildRecurrenceRule(
    selectedFrequency: RecurrenceFrequency,
    interval: String,
    selectedDays: Set<DayOfWeek>,
): RecurrenceRule {
    val finalInterval = interval.toIntOrNull() ?: 1
    val daysOfWeek =
        if (selectedFrequency == RecurrenceFrequency.WEEKLY) {
            selectedDays.toList()
        } else {
            null
        }

    return RecurrenceRule(
        frequency = selectedFrequency,
        interval = finalInterval,
        daysOfWeek = daysOfWeek,
    )
}
