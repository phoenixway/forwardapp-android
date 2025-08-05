package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.romankozak.forwardappmobile.data.database.models.GoalList

// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goallist/EditListDialog.kt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListDialog(
    list: GoalList,
    onDismiss: () -> Unit,
    onConfirm: (newName: String, newTags: List<String>) -> Unit
) {
    var name by remember { mutableStateOf(list.name) }
    var tags by remember { mutableStateOf(list.tags ?: emptyList()) }
    var currentTagInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit List") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List name") },
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))

                // ✨ Блок для додавання нового тегу ✨
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Add new tag", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = currentTagInput,
                            onValueChange = { currentTagInput = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (currentTagInput.isNotBlank()) {
                                        tags = tags + currentTagInput.trim()
                                        currentTagInput = ""
                                    }
                                }
                            ),
                            label = { Text("Tag name") }
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (currentTagInput.isNotBlank()) {
                                    tags = tags + currentTagInput.trim()
                                    currentTagInput = ""
                                }
                            },
                            enabled = currentTagInput.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // ✨ Блок для відображення поточних тегів ✨
                if (tags.isNotEmpty()) {
                    Text("Current tags", style = MaterialTheme.typography.labelMedium)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        mainAxisSpacing = 4.dp,
                        crossAxisSpacing = 4.dp
                    ) {
                        tags.forEach { tag ->
                            TagChip(
                                text = tag,
                                onDismiss = { tags = tags - tag }
                            )
                        }
                    }
                } else {
                    Text("No tags added yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, tags) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TagChip(text: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = "Remove tag",
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onDismiss)
        )
    }
}

