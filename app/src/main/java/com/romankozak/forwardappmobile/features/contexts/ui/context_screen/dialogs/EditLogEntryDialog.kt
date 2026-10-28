package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.features.reminders.components.DateTimePickerDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EditLogEntryDialog(
    logEntry: ContextLog,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Long) -> Unit,
) {
    var description by rememberSaveable(logEntry.id) { mutableStateOf(logEntry.description) }
    var details by rememberSaveable(logEntry.id) { mutableStateOf(logEntry.details ?: "") }
    var timestamp by rememberSaveable(logEntry.id) { mutableStateOf(logEntry.timestamp) }
    var showDateTimePicker by rememberSaveable(logEntry.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Log Entry") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedButton(onClick = { showDateTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Дата й час: ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))}")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") },
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(description, details.ifBlank { null }, timestamp) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
    if (showDateTimePicker) {
        DateTimePickerDialog(
            initialDateTime = timestamp,
            enablePastValues = true,
            title = "Дата й час запису",
            summaryLabel = "Запис",
            onDismiss = { showDateTimePicker = false },
            onConfirm = { timestamp = it; showDateTimePicker = false },
        )
    }
}
