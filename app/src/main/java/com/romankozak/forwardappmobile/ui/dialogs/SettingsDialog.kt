package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.screens.goallist.PlanningSettingsState

@Composable
fun SettingsDialog(
    // State for planning modes
    planningSettings: PlanningSettingsState,
    // State for Obsidian vault
    initialVaultName: String,
    initialContextTags: Map<String, String>,    // Callbacks
    onDismiss: () -> Unit,
    // ✨ ОНОВЛЕНО: Сигнатура onSave для передачі нових даних
    onSave: (
        showModes: Boolean, dailyTag: String, mediumTag: String, longTag: String,
        vaultName: String, contextTags: Map<String, String>
    )  -> Unit,
) {
    // --- Temporary states for edits within the dialog ---
    var tempShowModes by remember(planningSettings.showModes) { mutableStateOf(planningSettings.showModes) }
    var tempDailyTag by remember(planningSettings.dailyTag) { mutableStateOf(planningSettings.dailyTag) }
    var tempMediumTag by remember(planningSettings.mediumTag) { mutableStateOf(planningSettings.mediumTag) }
    var tempLongTag by remember(planningSettings.longTag) { mutableStateOf(planningSettings.longTag) }
    var tempVaultName by remember(initialVaultName) { mutableStateOf(initialVaultName) }

    val tempContextTags = remember { mutableStateOf(initialContextTags) }

    val contextKeys = listOf("buy", "pm", "paper", "mental", "providence", "manual", "research", "device")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            // Додано verticalScroll, щоб вміст прокручувався, якщо не вміщується на екрані
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // --- Planning Modes Section ---
                Text("Planning Modes", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Show planning scale modes", modifier = Modifier.weight(1f))
                    Switch(
                        checked = tempShowModes,
                        onCheckedChange = { tempShowModes = it }
                    )
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = tempDailyTag,
                    onValueChange = { tempDailyTag = it },
                    label = { Text("Daily Mode Tag") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tempMediumTag,
                    onValueChange = { tempMediumTag = it },
                    label = { Text("Medium Mode Tag") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = tempLongTag,
                    onValueChange = { tempLongTag = it },
                    label = { Text("Long Mode Tag") },
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(modifier = Modifier.padding(vertical = 24.dp))

                // --- Integrations Section (Obsidian) ---
                Text("Integrations", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Specify the exact name of your Obsidian Vault for link integration.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = tempVaultName,
                    onValueChange = { tempVaultName = it },
                    label = { Text("Obsidian Vault Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(modifier = Modifier.padding(vertical = 24.dp))
                Text("Context Tags", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Set tags to identify lists for specific contexts (e.g., @{manual}).")
                Spacer(Modifier.height(16.dp))

                contextKeys.forEach { contextKey ->
                    OutlinedTextField(
                        value = tempContextTags.value[contextKey] ?: "",
                        onValueChange = { newValue ->
                            val currentMap = tempContextTags.value.toMutableMap()
                            currentMap[contextKey] = newValue
                            tempContextTags.value = currentMap
                        },
                        label = { Text("${contextKey.replaceFirstChar { it.uppercase() }} Context Tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    tempShowModes, tempDailyTag, tempMediumTag, tempLongTag,
                    tempVaultName, tempContextTags.value
                )
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
