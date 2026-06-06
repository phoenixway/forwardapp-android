package com.romankozak.forwardappmobile.features.mainscreen.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp

@Composable
fun MainBeaconGroupEditorDialog(
    group: MainBeaconGroupUi?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember(group) { mutableStateOf(group?.title.orEmpty()) }
    var description by remember(group) { mutableStateOf(group?.description.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (group == null) "Нова група" else "Група") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.trim().isNotBlank(),
                onClick = { onSave(title, description.ifBlank { null }) },
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onDelete?.let {
                    TextButton(onClick = it) {
                        Text("Видалити")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Скасувати")
                }
            }
        },
    )
}
