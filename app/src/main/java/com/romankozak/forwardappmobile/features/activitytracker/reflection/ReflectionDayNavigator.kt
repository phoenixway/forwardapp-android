package com.romankozak.forwardappmobile.features.activitytracker.reflection

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectionDayNavigator(
    selectedDayStart: Long?,
    availableDayStarts: List<Long>,
    hasPreviousDay: Boolean,
    hasNextDay: Boolean,
    isLatestDay: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onLatestDay: () -> Unit,
    onDaySelected: (Long) -> Unit,
) {
    var dragDistance = 0f
    var calendarVisible by remember { mutableStateOf(false) }
    val dayFormatter = remember { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()) }
    val timeFormatter = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }

    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .pointerInput(hasPreviousDay, hasNextDay) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onHorizontalDrag = { _, amount -> dragDistance += amount },
                        onDragEnd = {
                            when {
                                dragDistance > 64f && hasPreviousDay -> onPreviousDay()
                                dragDistance < -64f && hasNextDay -> onNextDay()
                            }
                        },
                    )
                },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onPreviousDay, enabled = hasPreviousDay) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Попередній день")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = selectedDayStart?.let { dayFormatter.format(Date(it)) } ?: "Немає початків днів",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    selectedDayStart?.let {
                        Text(
                            text = "День розпочато о ${timeFormatter.format(Date(it))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onNextDay, enabled = hasNextDay) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Наступний день")
                }
            }
            if (!isLatestDay) {
                TextButton(onClick = onLatestDay) {
                    Icon(Icons.Default.Today, contentDescription = null)
                    Text(" До поточного дня")
                }
            } else if (selectedDayStart != null) {
                Text(
                    text = "Поточний день",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (availableDayStarts.isNotEmpty()) {
                TextButton(onClick = { calendarVisible = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Text(" Обрати дату")
                }
            }
        }
    }

    if (calendarVisible) {
        val zone = ZoneId.systemDefault()
        val selectableEpochDays =
            remember(availableDayStarts) {
                availableDayStarts.mapTo(hashSetOf()) { start ->
                    Instant.ofEpochMilli(start).atZone(zone).toLocalDate().toEpochDay()
                }
            }
        val initialPickerMillis =
            selectedDayStart?.let { start ->
                Instant.ofEpochMilli(start)
                    .atZone(zone)
                    .toLocalDate()
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }
        val pickerState =
            androidx.compose.material3.rememberDatePickerState(
                initialSelectedDateMillis = initialPickerMillis,
                selectableDates =
                    object : androidx.compose.material3.SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                            Instant.ofEpochMilli(utcTimeMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toEpochDay() in selectableEpochDays
                    },
            )
        DatePickerDialog(
            onDismissRequest = { calendarVisible = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onDaySelected)
                        calendarVisible = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) { Text("Обрати") }
            },
            dismissButton = { TextButton(onClick = { calendarVisible = false }) { Text("Скасувати") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
