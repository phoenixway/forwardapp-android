package com.romankozak.forwardappmobile.features.daymanagement.presentation.dayplan.tasklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onConfirm: (title: String, description: String, duration: Long?, priority: TaskPriority) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description ?: "") }
    var durationText by remember { mutableStateOf(task.estimatedDurationMinutes?.toString() ?: "") }
    var priority by remember { mutableStateOf(task.priority) }

    //---------------------------------------------------------------------
    // DIALOG
    //---------------------------------------------------------------------
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(16.dp),
        title = {
            Text(
                text = "Редагувати завдання",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        },

        //-----------------------------------------------------------------
        // CONTENT
        //-----------------------------------------------------------------
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ---------------------------------------------------------
                // TITLE
                // ---------------------------------------------------------
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Назва") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                        isError = title.isBlank(),
                        supportingText = { if (title.isBlank()) Text("Обов'язкове поле") }
                    )
                }

                // ---------------------------------------------------------
                // DESCRIPTION
                // ---------------------------------------------------------
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    )
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Опис") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .heightIn(80.dp),
                        maxLines = 5,
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                    )
                }

                // ---------------------------------------------------------
                // DURATION
                // ---------------------------------------------------------
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    )
                ) {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = {
                            if (it.all(Char::isDigit) || it.isEmpty()) durationText = it
                        },
                        label = { Text("Тривалість") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                        suffix = { if (durationText.isNotEmpty()) Text("хв") }
                    )
                }

                // ---------------------------------------------------------
                // PRIORITY
                // ---------------------------------------------------------
                Column {
                    Text(
                        "Пріоритет",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TaskPriority.values().forEach { p ->
                            val selected = p == priority

                            AssistChip(
                                onClick = { priority = p },
                                label = { Text(p.getDisplayName()) },
                                leadingIcon = {
                                    if (selected)
                                        Icon(Icons.Default.Check, contentDescription = null)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor =
                                        if (selected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f)
                                ),
                                border =
                                    if (selected)
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    else null
                            )
                        }
                    }
                }
            }
        },

        //---------------------------------------------------------------------
        // BUTTONS
        //---------------------------------------------------------------------
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    val duration = durationText.toLongOrNull()
                    onConfirm(title, description, duration, priority)
                },
                enabled = title.isNotBlank()
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Зберегти")
            }
        },

        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DELETE button with danger accent
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Видалити")
                }

                OutlinedButton(onClick = onDismissRequest) {
                    Text("Скасувати")
                }
            }
        }
    )
}
