// AddTaskDialog.kt
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.data.database.models.TaskPriority

// Допоміжна функція для отримання назви пріоритету
fun TaskPriority.getDisplayName(): String {
    return when (this) {
        TaskPriority.CRITICAL -> "Критичний"
        TaskPriority.HIGH -> "Високий"
        TaskPriority.MEDIUM -> "Середній"
        TaskPriority.LOW -> "Низький"
        TaskPriority.NONE -> "Без пріоритету"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (title: String, description: String, duration: Long?, priority: TaskPriority) -> Unit,
    initialPriority: TaskPriority = TaskPriority.MEDIUM,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var estimatedDuration by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(initialPriority) }
    var isValidTitle by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    // Валідація заголовка
    LaunchedEffect(title) {
        isValidTitle = title.trim().isNotBlank() && title.length <= 100
    }

    // Список пріоритетів у логічному порядку
    val priorities = remember {
        listOf(
            TaskPriority.CRITICAL,
            TaskPriority.HIGH,
            TaskPriority.MEDIUM,
            TaskPriority.LOW,
            TaskPriority.NONE
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Заголовок діалогу
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Додати завдання",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Закрити",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Поле назви завдання
                OutlinedTextField(
                    value = title,
                    onValueChange = { newTitle ->
                        if (newTitle.length <= 100) title = newTitle
                    },
                    label = { Text("Назва завдання*") },
                    placeholder = { Text("Введіть назву завдання...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = title.isNotBlank() && !isValidTitle,
                    supportingText = {
                        val charCount = title.length
                        val textColor = if (charCount > 90) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = "$charCount/100",
                            color = textColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                // Поле опису
                OutlinedTextField(
                    value = description,
                    onValueChange = { newDescription ->
                        if (newDescription.length <= 300) description = newDescription
                    },
                    label = { Text("Опис") },
                    placeholder = { Text("Додатковий опис завдання...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    supportingText = {
                        Text(
                            text = "${description.length}/300",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                // Рядок з тривалістю і пріоритетом
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Поле тривалості
                    OutlinedTextField(
                        value = estimatedDuration,
                        onValueChange = { newDuration ->
                            if (newDuration.all { it.isDigit() } && (newDuration.isEmpty() || newDuration.toIntOrNull()?.let { it <= 1440 } == true)) {
                                estimatedDuration = newDuration
                            }
                        },
                        label = { Text("Тривалість (хв)") },
                        placeholder = { Text("60") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = true,
                        supportingText = {
                            Text(
                                text = "До 24 год",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    )

                    // Випадаючий список пріоритетів
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = priority.getDisplayName(),
                            onValueChange = {},
                            label = { Text("Пріоритет") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            priorities.forEach { priorityOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            priorityOption.getDisplayName(),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        priority = priorityOption
                                        expanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Кнопки дій
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Скасувати")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (isValidTitle) {
                                onConfirm(
                                    title.trim(),
                                    description.trim(),
                                    estimatedDuration.toLongOrNull(),
                                    priority
                                )
                            }
                        },
                        enabled = isValidTitle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Додати")
                    }
                }
            }
        }
    }
}