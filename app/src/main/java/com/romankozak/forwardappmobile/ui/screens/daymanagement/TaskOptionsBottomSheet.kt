// TaskOptionsBottomSheet.kt
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.romankozak.forwardappmobile.data.database.models.DayTask
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOptionsBottomSheet(
    task: DayTask,
    onDismiss: () -> Unit,
    onEdit: (DayTask) -> Unit,
    onDelete: (DayTask) -> Unit,
    onSetReminder: (DayTask) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(2.dp)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Заголовок з інформацією про завдання
            TaskInfoHeader(
                task = task,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Список опцій
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Опція редагування
                OptionItem(
                    icon = Icons.Default.Edit,
                    text = "Редагувати завдання",
                    description = "Змінити назву, опис та інші параметри",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEdit(task)
                        onDismiss()
                    }
                )

                // Опція нагадування
                OptionItem(
                    icon = Icons.Default.Notifications,
                    text = "Встановити нагадування",
                    description = "Додати сповіщення для цього завдання",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSetReminder(task)
                        onDismiss()
                    },
                    enabled = !task.completed
                )

                // Опція дублювання завдання
                OptionItem(
                    icon = Icons.Default.ContentCopy,
                    text = "Дублювати завдання",
                    description = "Створити копію цього завдання",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        // TODO: Implement duplicate functionality
                        onDismiss()
                    }
                )

                // Розділювач
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Небезпечна опція - видалення
                OptionItem(
                    icon = Icons.Default.Delete,
                    text = "Видалити завдання",
                    description = "Остаточно видалити це завдання",
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteConfirmation = true
                    },
                    contentColor = MaterialTheme.colorScheme.error,
                    isDangerous = true
                )
            }
        }
    }

    // Діалог підтвердження видалення
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "Видалити завдання?",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    Text(
                        text = "Ви впевнені, що хочете видалити це завдання?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"${task.title}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Цю дію неможливо скасувати.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete(task)
                        showDeleteConfirmation = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("Скасувати")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TaskInfoHeader(
    task: DayTask,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Опції завдання",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Інформація про завдання
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                task.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Метаінформація
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Пріоритет
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = "Пріоритет",
                            modifier = Modifier.size(14.dp),
                            tint = getPriorityColor(task.priority)
                        )
                        Text(
                            text = task.priority.getDisplayName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Тривалість
                    task.estimatedDurationMinutes?.takeIf { it > 0 }?.let { duration ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Тривалість",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${duration} хв",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Термін виконання
                    task.dueTime?.let { dueTimestamp ->
                        val formattedTime = remember(dueTimestamp) {
                            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                            Instant.ofEpochMilli(dueTimestamp)
                                .atZone(ZoneId.systemDefault())
                                .format(formatter)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Термін",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "до $formattedTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionItem(
    icon: ImageVector,
    text: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    isDangerous: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (isDangerous) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        contentColor.copy(alpha = 0.7f)
                    } else {
                        contentColor.copy(alpha = 0.3f)
                    }
                )
            }
        }
    }
}

@Composable
private fun getPriorityColor(priority: com.romankozak.forwardappmobile.data.database.models.TaskPriority): androidx.compose.ui.graphics.Color {
    return when (priority) {
        com.romankozak.forwardappmobile.data.database.models.TaskPriority.CRITICAL -> MaterialTheme.colorScheme.error
        com.romankozak.forwardappmobile.data.database.models.TaskPriority.HIGH -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        com.romankozak.forwardappmobile.data.database.models.TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        com.romankozak.forwardappmobile.data.database.models.TaskPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
        com.romankozak.forwardappmobile.data.database.models.TaskPriority.NONE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }}