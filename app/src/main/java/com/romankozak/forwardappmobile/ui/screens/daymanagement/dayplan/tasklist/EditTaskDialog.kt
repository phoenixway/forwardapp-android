package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.TaskPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: DayTask,
    onDismissRequest: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        duration: Long?,
        priority: TaskPriority,
    ) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description ?: "") }
    var durationText by remember {
        mutableStateOf(task.estimatedDurationMinutes?.toString() ?: "")
    }
    var priority by remember { mutableStateOf(task.priority) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp),
        title = {
            Text(
                text = "Редагувати завдання",
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
                    onConfirm(title, description, duration, priority)
                },
                enabled = title.isNotBlank(),
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Зберегти")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Видалити")
                }
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Скасувати")
                }
            }
        },
    )
}
