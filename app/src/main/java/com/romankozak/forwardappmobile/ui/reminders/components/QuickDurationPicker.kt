package com.romankozak.forwardappmobile.ui.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.reminders.model.ReminderDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickDurationPicker(
    durations: List<ReminderDuration>,
    selectedDuration: ReminderDuration?,
    onDurationSelected: (ReminderDuration) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Через скільки нагадати:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )

        val chunkedDurations = durations.chunked(3)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chunkedDurations.forEach { rowDurations ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowDurations.forEach { duration ->
                        DurationChip(
                            duration = duration,
                            isSelected = selectedDuration == duration,
                            onClick = { onDurationSelected(duration) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(3 - rowDurations.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationChip(
    duration: ReminderDuration,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        onClick = onClick,
        label = { Text(duration.name) },
        selected = isSelected,
        leadingIcon =
        if (isSelected) {
            { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else {
            null
        },
        modifier = modifier,
    )
}
