package com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.model.ProjectExecutionLog

@Composable
fun EditLogEntryDialog(
    logEntry: ProjectExecutionLog,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var description by remember { mutableStateOf(logEntry.description) }
    var details by remember { mutableStateOf(logEntry.details ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Log Entry") },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text("Details") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(description, details.ifBlank { null }) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
