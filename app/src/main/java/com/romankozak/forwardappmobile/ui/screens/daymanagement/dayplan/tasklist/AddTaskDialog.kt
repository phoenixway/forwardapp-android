package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.data.database.models.TaskPriority
import com.romankozak.forwardappmobile.data.database.models.RecurrenceRule
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.components.AdvancedRecurrencePickerDialog
// import com.romankozak.forwardappmobile.ui.theme.FlowRow
import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        duration: Long?,
        priority: TaskPriority,
        recurrenceRule: RecurrenceRule?,
        points: Int
    ) -> Unit,
    initialPriority: TaskPriority = TaskPriority.MEDIUM,
) {
    // STATE
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

    //---------------------------------------------------------------------
    //  PREMIUM GLASS DIALOG
    //---------------------------------------------------------------------
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(16.dp),
        title = {
            Text(
                text = "Нове завдання",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {

                //-----------------------------------------------------------------
                // Title
                //-----------------------------------------------------------------
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Назва завдання") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Title, contentDescription = null)
                        },
                        isError = title.isBlank(),
                        supportingText = {
                            if (title.isBlank()) Text("Обов'язкове поле")
                        }
                    )
                }

                //-----------------------------------------------------------------
                // Description
                //-----------------------------------------------------------------
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Опис (необов'язково)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .heightIn(min = 80.dp),
                        maxLines = 4,
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        }
                    )
                }

                //-----------------------------------------------------------------
                // Points + Duration
                //-----------------------------------------------------------------
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        OutlinedTextField(
                            value = pointsText,
                            onValueChange = {
                                if (it.all { ch -> ch.isDigit() } || it.isEmpty()) pointsText = it
                            },
                            label = { Text("Бали") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                        )
                    }

                    ElevatedCard(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        OutlinedTextField(
                            value = durationText,
                            onValueChange = {
                                if (it.all { ch -> ch.isDigit() } || it.isEmpty()) durationText = it
                            },
                            label = { Text("Тривалість") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                            suffix = { if (durationText.isNotBlank()) Text("хв") }
                        )
                    }
                }

                //-----------------------------------------------------------------
                // Recurrence
                //-----------------------------------------------------------------
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                            .clickable { showRecurrencePicker = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = recurrenceRule?.let { "Повторення: ${it.frequency.name}" }
                                ?: "Без повторення",
                            fontSize = 15.sp
                        )

                        Icon(Icons.Default.Repeat, contentDescription = null)
                    }
                }

                //-----------------------------------------------------------------
                // Priority Chips — Animated
                //-----------------------------------------------------------------
                Column {
                    Text(
                        text = "Пріоритет",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TaskPriority.values().forEach { prio ->
                            val selected = priority == prio

                            AssistChip(
                                onClick = { priority = prio },
                                label = { Text(prio.getDisplayName()) },
                                leadingIcon = {
                                    if (selected)
                                        Icon(Icons.Default.Check, contentDescription = null)
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (selected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = if (selected)
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
                    val points = pointsText.toIntOrNull() ?: 0

                    onConfirm(title, description, duration, priority, recurrenceRule, points)
                },
                enabled = title.isNotBlank()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Додати")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text("Скасувати")
            }
        }
    )
}
