package com.romankozak.forwardappmobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    initialVaultName: String,
    onDismiss: () -> Unit,
    onSave: (vaultName: String) -> Unit
) {
    var vaultName by remember { mutableStateOf(initialVaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Налаштування") },
        text = {
            Column {
                Text("Вкажіть точну назву вашого Obsidian Vault для інтеграції посилань.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = vaultName,
                    onValueChange = { vaultName = it },
                    label = { Text("Назва Obsidian Vault") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(vaultName) }) {
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