package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TodayDatePickerDialogHost(
    visible: Boolean,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) {
        return
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let(onDateSelected)
                    onDismiss()
                },
            ) {
                Text("Обрати")
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
