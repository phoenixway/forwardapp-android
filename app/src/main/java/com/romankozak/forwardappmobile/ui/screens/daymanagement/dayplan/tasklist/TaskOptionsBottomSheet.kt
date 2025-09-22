
package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.DayTask
import com.romankozak.forwardappmobile.data.database.models.TaskPriority


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
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            ListItem(
                headlineContent = { Text("Редагувати") },
                leadingContent = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                    )
                },
                modifier =
                    Modifier.clickable {
                        onEdit(task)
                        
                    },
            )

            ListItem(
                headlineContent = { Text("Встановити нагадування") },
                leadingContent = {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                    )
                },
                modifier =
                    Modifier.clickable {
                        onSetReminder(task)
                        onDismiss()
                    },
            )

            ListItem(
                headlineContent = { Text("Видалити") },
                leadingContent = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                modifier =
                    Modifier.clickable {
                        onDelete(task)
                        onDismiss()
                    },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


fun TaskPriority.getDisplayName(): String =
    when (this) {
        TaskPriority.CRITICAL -> "Критичний"
        TaskPriority.HIGH -> "Високий"
        TaskPriority.MEDIUM -> "Середній"
        TaskPriority.LOW -> "Низький"
        TaskPriority.NONE -> "Без пріоритету"
    }



@Composable
private fun TaskInfoHeader(
    task: DayTask,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Опції завдання",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                task.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = "Пріоритет",
                            modifier = Modifier.size(14.dp),
                            tint = getPriorityColor(task.priority),
                        )
                        Text(
                            text = task.priority.getDisplayName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    
                    task.estimatedDurationMinutes?.takeIf { it > 0 }?.let { duration ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Тривалість",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "$duration хв",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    
                    task.dueTime?.let { dueTimestamp ->
                        val formattedTime =
                            remember(dueTimestamp) {
                                val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
                                Instant.ofEpochMilli(dueTimestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .format(formatter)
                            }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = "Термін",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "до $formattedTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    isDangerous: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color =
            if (isDangerous) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (enabled) {
                            contentColor.copy(alpha = 0.7f)
                        } else {
                            contentColor.copy(alpha = 0.3f)
                        },
                )
            }
        }
    }
}

@Composable
private fun getPriorityColor(priority: TaskPriority): Color {
    return when (priority) {
        TaskPriority.CRITICAL -> MaterialTheme.colorScheme.error
        TaskPriority.HIGH -> MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.primary
        TaskPriority.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
        TaskPriority.NONE -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    } 
}
