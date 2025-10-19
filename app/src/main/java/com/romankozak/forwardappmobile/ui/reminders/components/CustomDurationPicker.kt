package com.romankozak.forwardappmobile.ui.reminders.components

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
import com.romankozak.forwardappmobile.ui.reminders.util.ReminderTextUtil.formatDateTime

@Composable
fun CustomDurationPicker(
    minutes: String,
    onMinutesChanged: (String) -> Unit,
) {
    var hours by remember { mutableStateOf(0) }
    var mins by remember { mutableStateOf(0) }

    val totalMinutes = hours * 60 + mins

    LaunchedEffect(totalMinutes) {
        onMinutesChanged(totalMinutes.toString())
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Через скільки нагадати:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )

        // Quick selection chips
        com.google.accompanist.flowlayout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp,
        ) {
            listOf(1, 2, 4, 8).forEach { h ->
                OutlinedButton(
                    onClick = { hours = h; mins = 0 },
                ) {
                    Text("$h год")
                }
            }
        }

        // Sliders for hours and minutes
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Hours slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Годин: $hours",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Row {
                        IconButton(
                            onClick = { if (hours > 0) hours-- },
                            enabled = hours > 0,
                        ) {
                            Text("−", style = MaterialTheme.typography.headlineSmall)
                        }
                        IconButton(
                            onClick = { if (hours < 23) hours++ },
                            enabled = hours < 23,
                        ) {
                            Text("+", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
                Slider(
                    value = hours.toFloat(),
                    onValueChange = { hours = it.toInt() },
                    valueRange = 0f..23f,
                    steps = 22,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Minutes slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Хвилин: $mins",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Row {
                        IconButton(
                            onClick = { if (mins > 0) mins-- },
                            enabled = mins > 0,
                        ) {
                            Text("−", style = MaterialTheme.typography.headlineSmall)
                        }
                        IconButton(
                            onClick = { if (mins < 59) mins++ },
                            enabled = mins < 59,
                        ) {
                            Text("+", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
                Slider(
                    value = mins.toFloat(),
                    onValueChange = { mins = it.toInt() },
                    valueRange = 0f..59f,
                    steps = 58,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (totalMinutes > 0) {
            val future = System.currentTimeMillis() + totalMinutes * 60 * 1000L
            Card(
                colors = CardDefaults.cardColors(
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
    }
}
