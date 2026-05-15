package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.features.activitytracker.dialogs.TimePickerDialog
import com.romankozak.forwardappmobile.features.daymanagement.utils.formatDayTime

@Composable
fun TaskExecutionPolicyEditor(
    dayAnchorTime: Long,
    durationMinutes: Long?,
    scheduledTime: Long?,
    dueTime: Long?,
    resolvedScheduledTime: Long?,
    resolvedDueTime: Long?,
    strictness: TaskExecutionStrictness,
    onDurationChange: (Long?) -> Unit,
    onScheduledTimeChange: (Long?) -> Unit,
    onDueTimeChange: (Long?) -> Unit,
    onStrictnessChange: (TaskExecutionStrictness) -> Unit,
    modifier: Modifier = Modifier,
) {
    var activePicker by remember { mutableStateOf<ExecutionTimeField?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TaskExecutionDurationField(
            durationMinutes = durationMinutes,
            onDurationChange = onDurationChange,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TaskExecutionTimeButton(
                modifier = Modifier.weight(1f),
                label = "Старт",
                icon = Icons.Outlined.PlayArrow,
                explicitTime = scheduledTime,
                resolvedTime = resolvedScheduledTime,
                onClick = { activePicker = ExecutionTimeField.START },
                onClear = { onScheduledTimeChange(null) },
            )
            TaskExecutionTimeButton(
                modifier = Modifier.weight(1f),
                label = "Дедлайн",
                icon = Icons.Outlined.Alarm,
                explicitTime = dueTime,
                resolvedTime = resolvedDueTime,
                onClick = { activePicker = ExecutionTimeField.DEADLINE },
                onClear = { onDueTimeChange(null) },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Жорсткість",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskExecutionStrictness.values().forEach { option ->
                    FilterChip(
                        selected = option == strictness,
                        onClick = { onStrictnessChange(option) },
                        label = { Text(option.displayName()) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Flag,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }

    val initialPickerTime =
        when (activePicker) {
            ExecutionTimeField.START -> scheduledTime ?: resolvedScheduledTime ?: dayAnchorTime
            ExecutionTimeField.DEADLINE -> dueTime ?: resolvedDueTime ?: dayAnchorTime
            null -> null
        }
    if (activePicker != null && initialPickerTime != null) {
        TimePickerDialog(
            initialTime = initialPickerTime,
            onDismiss = { activePicker = null },
            onConfirm = { selectedTime ->
                when (activePicker) {
                    ExecutionTimeField.START -> onScheduledTimeChange(selectedTime)
                    ExecutionTimeField.DEADLINE -> onDueTimeChange(selectedTime)
                    null -> Unit
                }
                activePicker = null
            },
        )
    }
}

private enum class ExecutionTimeField {
    START,
    DEADLINE,
}

@Composable
private fun TaskExecutionDurationField(
    durationMinutes: Long?,
    onDurationChange: (Long?) -> Unit,
) {
    NumericField(
        config =
            NumericFieldConfig(
                label = "Тривалість",
                value = durationMinutes?.toString().orEmpty(),
                placeholder = "0",
                tint = MaterialTheme.colorScheme.tertiary,
                icon = Icons.Outlined.Timer,
                suffixText = "хв",
                onValueChange = { onDurationChange(it.toLongOrNull()) },
            ),
    )
}

@Composable
private fun TaskExecutionTimeButton(
    label: String,
    icon: ImageVector,
    explicitTime: Long?,
    resolvedTime: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(imageVector = icon, contentDescription = null)
                    Text(
                        text =
                            explicitTime?.let(::formatDayTime)
                                ?: resolvedTime?.let { "${formatDayTime(it)} авто" }
                                ?: "Не задано",
                    )
                }
                if (explicitTime != null) {
                    Text(
                        text = "Очистити",
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .clickable(onClick = onClear),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (explicitTime == null && resolvedTime != null) {
            Text(
                text = "Авто: ${formatDayTime(resolvedTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun TaskExecutionStrictness.displayName(): String =
    when (this) {
        TaskExecutionStrictness.SOFT -> "М'яко"
        TaskExecutionStrictness.NORMAL -> "Нормально"
        TaskExecutionStrictness.HARD -> "Жорстко"
    }
