package com.romankozak.forwardappmobile.features.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.reminders.util.ReminderTextUtil.formatDateTime

private const val MINUTES_PER_HOUR = 60
private const val MIN_HOURS = 0
private const val MAX_HOURS = 23
private const val HOUR_SLIDER_STEPS = 22
private const val MIN_MINUTES = 0
private const val MAX_MINUTES = 59
private const val MINUTE_SLIDER_STEPS = 58
private const val QUICK_OPTION_ONE_HOUR = 1
private const val QUICK_OPTION_TWO_HOURS = 2
private const val QUICK_OPTION_FOUR_HOURS = 4
private const val QUICK_OPTION_EIGHT_HOURS = 8
private val QuickHourOptions =
    listOf(
        QUICK_OPTION_ONE_HOUR,
        QUICK_OPTION_TWO_HOURS,
        QUICK_OPTION_FOUR_HOURS,
        QUICK_OPTION_EIGHT_HOURS,
    )
private const val INITIAL_TOTAL_MINUTES_RADIX = 10
private const val MILLIS_PER_SECOND = 1000L

@Composable
fun CustomDurationPicker(
    minutes: String,
    onMinutesChanged: (String) -> Unit,
) {
    val initialTotalMinutes =
        remember(minutes) {
            minutes.toIntOrNull(INITIAL_TOTAL_MINUTES_RADIX)?.coerceAtLeast(0) ?: 0
        }
    var hours by remember(initialTotalMinutes) { mutableStateOf(initialTotalMinutes / MINUTES_PER_HOUR) }
    var mins by remember(initialTotalMinutes) { mutableStateOf(initialTotalMinutes % MINUTES_PER_HOUR) }

    val totalMinutes = hours * MINUTES_PER_HOUR + mins

    LaunchedEffect(totalMinutes) {
        onMinutesChanged(totalMinutes.toString())
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Через скільки нагадати:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )

        QuickHourSelection(
            onHourSelected = { selectedHours ->
                hours = selectedHours
                mins = 0
            },
        )
        DurationSliders(
            hours = hours,
            mins = mins,
            onHoursChanged = { hours = it },
            onMinutesChanged = { mins = it },
        )

        if (totalMinutes > 0) {
            val future = System.currentTimeMillis() + totalMinutes * MINUTES_PER_HOUR * MILLIS_PER_SECOND
            ReminderPreviewCard(future)
        }
    }
}

@Composable
private fun QuickHourSelection(
    onHourSelected: (Int) -> Unit,
) {
    com.google.accompanist.flowlayout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp,
    ) {
        QuickHourOptions.forEach { h ->
            OutlinedButton(onClick = { onHourSelected(h) }) {
                Text("$h год")
            }
        }
    }
}

@Composable
private fun DurationSliders(
    hours: Int,
    mins: Int,
    onHoursChanged: (Int) -> Unit,
    onMinutesChanged: (Int) -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DurationSliderRow(
                title = "Годин",
                value = hours,
                minValue = MIN_HOURS,
                maxValue = MAX_HOURS,
                steps = HOUR_SLIDER_STEPS,
                onValueChanged = onHoursChanged,
            )
            DurationSliderRow(
                title = "Хвилин",
                value = mins,
                minValue = MIN_MINUTES,
                maxValue = MAX_MINUTES,
                steps = MINUTE_SLIDER_STEPS,
                onValueChanged = onMinutesChanged,
            )
        }
    }
}

@Composable
private fun DurationSliderRow(
    title: String,
    value: Int,
    minValue: Int,
    maxValue: Int,
    steps: Int,
    onValueChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$title: $value",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Row {
            IconButton(
                onClick = { if (value > minValue) onValueChanged(value - 1) },
                enabled = value > minValue,
            ) {
                Text("−", style = MaterialTheme.typography.headlineSmall)
            }
            IconButton(
                onClick = { if (value < maxValue) onValueChanged(value + 1) },
                enabled = value < maxValue,
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChanged(it.toInt()) },
        valueRange = minValue.toFloat()..maxValue.toFloat(),
        steps = steps,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ReminderPreviewCard(future: Long) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Text(
            text = "Нагадування: ${formatDateTime(future)}",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
