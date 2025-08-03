package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
    // ✨ ПОВЕРНУВ: Головна функція збереження
    onSave: (vaultName: String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    var vaultName by remember { mutableStateOf(initialVaultName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Налаштування") },
        text = {
            Column {
                // Секція Obsidian Vault
                Text("Вкажіть точну назву вашого Obsidian Vault для інтеграції посилань.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = vaultName,
                    onValueChange = { vaultName = it },
                    label = { Text("Назва Obsidian Vault") },
                    singleLine = true
                )

                // Секція керування даними
                Divider(modifier = Modifier.padding(vertical = 24.dp))

                Text("Резервне копіювання та відновлення даних.")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onExport, modifier = Modifier.weight(1f)) {
                        Text("Експорт")
                    }
                    Button(onClick = onImport, modifier = Modifier.weight(1f)) {
                        Text("Імпорт")
                    }
                }
            }
        },
        confirmButton = {
            // ✨ ВИПРАВЛЕНО: Кнопка "Зберегти" тепер викликає onSave і закриває діалог
            TextButton(onClick = {
                onSave(vaultName)
                onDismiss()
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