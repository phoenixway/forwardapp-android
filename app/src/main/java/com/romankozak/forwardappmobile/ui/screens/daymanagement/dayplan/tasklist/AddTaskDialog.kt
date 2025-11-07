
package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.core.database.models.TaskPriority

import com.romankozak.forwardappmobile.core.database.models.RecurrenceRule
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.components.AdvancedRecurrencePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (title: String, description: String, duration: Long?, priority: TaskPriority, recurrenceRule: RecurrenceRule?, points: Int) -> Unit,
    initialPriority: TaskPriority = TaskPriority.MEDIUM,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var pointsText by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(initialPriority) }
    var recurrenceRule by remember { mutableStateOf<RecurrenceRule?>(null) }
    var showRecurrencePicker by remember { mutableStateOf(false) }

    if (showRecurrencePicker) {
        AdvancedRecurrencePickerDialog(
            onDismiss = { showRecurrencePicker = false },
            onConfirm = { rule ->
                recurrenceRule = rule
                showRecurrencePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp),
        title = {
            Text(
                text = "Додати завдання",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва завдання") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Title,
                            contentDescription = null,
                        )
                    },
                    isError = title.isBlank(),
                    supportingText = {
                        if (title.isBlank()) {
                            Text("Обов'язкове поле")
                        }
                    },
                )

                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Опис (необов'язково)") },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                    maxLines = 4,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                        )
                    },
                )

                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            pointsText = newValue
                        }
                    },
                    label = { Text("Бали") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                        )
                    },
                )

                
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            durationText = newValue
                        }
                    },
                    label = { Text("Тривалість (хвилини)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                        )
                    },
                    suffix = {
                        if (durationText.isNotBlank()) {
                            Text("хв")
                        }
                    },
                )

                Button(onClick = { showRecurrencePicker = true }) {
                    Text(text = recurrenceRule?.let { "Повторення: ${it.frequency.name}" } ?: "Без повторення")
                }

                
                Column {
                    Text(
                        text = "Пріоритет:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TaskPriority.values().forEach { taskPriority ->
                            val isSelected = priority == taskPriority
                            AssistChip(
                                onClick = { priority = taskPriority },
                                label = {
                                    Text(
                                        text = taskPriority.getDisplayName(),
                                        maxLines = 1,
                                    )
                                },
                                colors =
                                    AssistChipDefaults.assistChipColors(
                                        containerColor =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            },
                                    ),
                                border =
                                    if (isSelected) {
                                        BorderStroke(
                                            2.dp,
                                            MaterialTheme.colorScheme.primary,
                                        )
                                    } else {
                                        null
                                    },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val duration = durationText.toLongOrNull()
                    val points = pointsText.toIntOrNull() ?: 0
                    onConfirm(title, description, duration, priority, recurrenceRule, points)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.padding(end = 8.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Додати")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismissRequest,
            ) {
                Text("Скасувати")
            }
        },
    )
}
