package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReservedContextsDialog(
    initialContextTags: Map<String, String>,
    onDismiss: () -> Unit,
    onSave: (newContextTags: Map<String, String>) -> Unit
) {
    val tempContextTags = remember { mutableStateOf(initialContextTags) }
    // Отримуємо відсортований список ключів для стабільного порядку
    val contextKeys = initialContextTags.keys.sorted()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reserved Contexts") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Manage tags for reserved contexts. Goals with these @contexts will be automatically instanced in lists with the corresponding tag.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))

                contextKeys.forEach { contextKey ->
                    OutlinedTextField(
                        value = tempContextTags.value[contextKey] ?: "",
                        onValueChange = { newValue ->
                            val currentMap = tempContextTags.value.toMutableMap()
                            currentMap[contextKey] = newValue
                            tempContextTags.value = currentMap
                        },
                        label = { Text("${contextKey.replaceFirstChar { it.uppercase() }} Tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(tempContextTags.value)
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