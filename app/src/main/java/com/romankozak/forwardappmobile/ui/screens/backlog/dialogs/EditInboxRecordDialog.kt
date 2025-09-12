package com.romankozak.forwardappmobile.ui.screens.backlog.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.data.database.models.InboxRecord

@Composable
fun EditInboxRecordDialog(
    record: InboxRecord,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(record) { mutableStateOf(record.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати запис") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        },
    )
}
