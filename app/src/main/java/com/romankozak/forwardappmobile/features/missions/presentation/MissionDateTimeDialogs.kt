package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeadlinePickerDialog(
    initialTime: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val initialCalendar =
        remember(initialTime) {
            Calendar.getInstance().apply { timeInMillis = initialTime }
        }
    var selectedDate by remember { mutableStateOf(initialCalendar.timeInMillis) }
    var showTimePicker by remember { mutableStateOf(false) }

    if (showTimePicker) {
        TimePickerDialog(
            initialCalendar = Calendar.getInstance().apply { timeInMillis = selectedDate },
            onDismiss = onDismiss,
            onConfirm = { hour, minute ->
                val calendar =
                    Calendar.getInstance().apply {
                        timeInMillis = selectedDate
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                    }
                onConfirm(calendar.timeInMillis)
            },
        )
        return
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialCalendar.timeInMillis)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Deadline") },
        text = { DatePicker(state = datePickerState) },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedDate = datePickerState.selectedDateMillis ?: initialCalendar.timeInMillis
                    showTimePicker = true
                },
            ) {
                Text("Next")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialCalendar: Calendar,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = initialCalendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCalendar.get(Calendar.MINUTE),
            is24Hour = true,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

internal fun missionDialogFormatDate(ts: Long): String {
    if (ts == NO_DEADLINE) return "Без дедлайну"
    val months =
        listOf(
            "січ",
            "лют",
            "бер",
            "квіт",
            "трав",
            "черв",
            "лип",
            "серп",
            "вер",
            "жовт",
            "лист",
            "груд",
        )
    val calendar = Calendar.getInstance().apply { timeInMillis = ts }
    val monthLabel = months[calendar.get(Calendar.MONTH)]
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val year = calendar.get(Calendar.YEAR)
    return "$day $monthLabel $year"
}
