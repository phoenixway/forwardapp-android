package com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreateCustomListDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String) -> Unit,
) {
    var listTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Створити новий список") },
        text = {
            Column {
                OutlinedTextField(
                    value = listTitle,
                    onValueChange = { listTitle = it },
                    label = { Text("Назва списку") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Новий список буде додано до поточного проекту.")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(listTitle)
                    onDismiss()
                },
                enabled = listTitle.isNotBlank()
            ) {
                Text("Створити")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}