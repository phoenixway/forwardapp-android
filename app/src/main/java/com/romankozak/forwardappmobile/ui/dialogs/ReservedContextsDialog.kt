package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ReservedContextsDialog(
    initialContexts: Map<String, Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (newContexts: Map<String, Pair<String, String>>) -> Unit
) {
    val tempContexts = remember { mutableStateOf(initialContexts) }
    val contextKeys = initialContexts.keys.sorted()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reserved Contexts") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Manage tags and emojis for reserved contexts.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))

                contextKeys.forEach { contextKey ->
                    val (tag, emoji) = tempContexts.value[contextKey] ?: ("" to "")

                    Text(
                        text = contextKey.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { newValue ->
                                val currentMap = tempContexts.value.toMutableMap()
                                currentMap[contextKey] = tag to newValue
                                tempContexts.value = currentMap
                            },
                            label = { Text("Emoji") },
                            modifier = Modifier.width(90.dp),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
//                            keyboardOptions = KeyboardOptions(autoCorrect = false),

                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Unspecified, autoCorrectEnabled = false, keyboardType = KeyboardType.Unspecified, imeAction = ImeAction.Unspecified),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = tag,
                            onValueChange = { newValue ->
                                val currentMap = tempContexts.value.toMutableMap()
                                currentMap[contextKey] = newValue to emoji
                                tempContexts.value = currentMap
                            },
                            label = { Text("Corresponding Tag") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(tempContexts.value)
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