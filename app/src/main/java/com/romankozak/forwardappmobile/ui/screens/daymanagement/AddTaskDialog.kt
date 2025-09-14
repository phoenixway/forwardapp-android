// File: AddTaskDialog.kt
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.TaskPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismissRequest: () -> Unit,
    // ВИПРАВЛЕНО: Сигнатура onConfirm тепер приймає всі чотири параметри
    onConfirm: (title: String, description: String, duration: Long?, priority: TaskPriority) -> Unit,
    initialPriority: TaskPriority
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(initialPriority) }
    var description by remember { mutableStateOf("") }
    var estimatedDuration by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Додати завдання", style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва завдання") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = title.isBlank()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Опис (необов'язково)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = estimatedDuration,
                        onValueChange = { if (it.all { char -> char.isDigit() }) estimatedDuration = it },
                        label = { Text("Тривалість (хв)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = when (priority) {
                                TaskPriority.HIGH -> "Високий"
                                TaskPriority.MEDIUM -> "Середній"
                                TaskPriority.LOW -> "Низький"
                                TaskPriority.CRITICAL -> "Критичний"
                                TaskPriority.NONE -> "Без пріоритету"
                            },
                            onValueChange = {},
                            label = { Text("Пріоритет") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Високий") },
                                onClick = {
                                    priority = TaskPriority.HIGH
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Середній") },
                                onClick = {
                                    priority = TaskPriority.MEDIUM
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Низький") },
                                onClick = {
                                    priority = TaskPriority.LOW
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Скасувати")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // ВИПРАВЛЕНО: Передаємо всі зібрані дані
                            onConfirm(
                                title,
                                description,
                                estimatedDuration.toLongOrNull(),
                                priority
                            )
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Додати")
                    }
                }
            }
        }
    }
}
