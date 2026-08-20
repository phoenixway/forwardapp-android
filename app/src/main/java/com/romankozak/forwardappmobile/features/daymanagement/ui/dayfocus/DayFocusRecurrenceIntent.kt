package com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceDayOfWeek
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule

sealed interface DayFocusRecurrenceIntent {
    data object OneOff : DayFocusRecurrenceIntent

    data class CreateSeries(
        val rule: RecurrenceRule,
    ) : DayFocusRecurrenceIntent

    data object CurrentOccurrence : DayFocusRecurrenceIntent

    data object WholeSeries : DayFocusRecurrenceIntent

    data class SplitFromHere(
        val rule: RecurrenceRule,
    ) : DayFocusRecurrenceIntent
}

internal fun defaultDayFocusRecurrenceRule(): RecurrenceRule =
    RecurrenceRule(
        frequency = RecurrenceFrequency.DAILY,
        interval = 1,
        daysOfWeek = null,
    )

@Composable
internal fun DayFocusRecurrenceIntentEditor(
    existingCanonicalRecurring: Boolean,
    intent: DayFocusRecurrenceIntent,
    onIntentChange: (DayFocusRecurrenceIntent) -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (existingCanonicalRecurring) {
                Text(
                    text = "Редагування повторення",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = intent == DayFocusRecurrenceIntent.CurrentOccurrence,
                        onClick = { onIntentChange(DayFocusRecurrenceIntent.CurrentOccurrence) },
                        label = { Text("Лише цей день") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = intent == DayFocusRecurrenceIntent.WholeSeries,
                        onClick = { onIntentChange(DayFocusRecurrenceIntent.WholeSeries) },
                        label = { Text("Вся серія") },
                        modifier = Modifier.weight(1f),
                    )
                }
                FilterChip(
                    selected = intent is DayFocusRecurrenceIntent.SplitFromHere,
                    onClick = {
                        onIntentChange(
                            DayFocusRecurrenceIntent.SplitFromHere(
                                rule =
                                    (intent as? DayFocusRecurrenceIntent.SplitFromHere)?.rule
                                        ?: defaultDayFocusRecurrenceRule(),
                            ),
                        )
                    },
                    label = { Text("З цього дня змінити повторення") },
                    modifier = Modifier.fillMaxWidth(),
                )

                when (intent) {
                    DayFocusRecurrenceIntent.CurrentOccurrence ->
                        Text(
                            text = "Зміниться лише цей конкретний день",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                    DayFocusRecurrenceIntent.WholeSeries ->
                        Text(
                            text = "Оновиться шаблон серії та незмінені інстанси",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                    is DayFocusRecurrenceIntent.SplitFromHere -> {
                        Text(
                            text = "Стара серія завершиться вчора, а з цього дня почнеться нова",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        CanonicalFocusRecurrenceRuleEditor(
                            rule = intent.rule,
                            onRuleChange = { nextRule ->
                                onIntentChange(DayFocusRecurrenceIntent.SplitFromHere(nextRule))
                            },
                        )
                    }

                    else -> Unit
                }
            } else {
                Text(
                    text = "Повторення",
                    style = MaterialTheme.typography.titleSmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = intent == DayFocusRecurrenceIntent.OneOff,
                        onClick = { onIntentChange(DayFocusRecurrenceIntent.OneOff) },
                        label = { Text("Один раз") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = intent is DayFocusRecurrenceIntent.CreateSeries,
                        onClick = {
                            onIntentChange(
                                DayFocusRecurrenceIntent.CreateSeries(
                                    rule =
                                        (intent as? DayFocusRecurrenceIntent.CreateSeries)?.rule
                                            ?: defaultDayFocusRecurrenceRule(),
                                ),
                            )
                        },
                        label = { Text("Повторювати") },
                        modifier = Modifier.weight(1f),
                    )
                }

                val recurringIntent = intent as? DayFocusRecurrenceIntent.CreateSeries
                if (recurringIntent != null) {
                    CanonicalFocusRecurrenceRuleEditor(
                        rule = recurringIntent.rule,
                        onRuleChange = { nextRule ->
                            onIntentChange(DayFocusRecurrenceIntent.CreateSeries(nextRule))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CanonicalFocusRecurrenceRuleEditor(
    rule: RecurrenceRule,
    onRuleChange: (RecurrenceRule) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Частота",
            style = MaterialTheme.typography.labelLarge,
        )

        RecurrenceFrequency.entries.chunked(2).forEach { frequencies ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                frequencies.forEach { frequency ->
                    FilterChip(
                        selected = rule.frequency == frequency,
                        onClick = {
                            onRuleChange(
                                rule.copy(
                                    frequency = frequency,
                                    daysOfWeek = if (frequency == RecurrenceFrequency.WEEKLY) rule.daysOfWeek else null,
                                ),
                            )
                        },
                        label = { Text(frequency.title()) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(2 - frequencies.size) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }

        OutlinedTextField(
            value = rule.interval.toString(),
            onValueChange = { rawValue ->
                val value = rawValue.filter(Char::isDigit).take(3).toIntOrNull() ?: 1
                onRuleChange(rule.copy(interval = value.coerceAtLeast(1)))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Кожні N періодів") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        if (rule.frequency == RecurrenceFrequency.WEEKLY) {
            Text(
                text = "Дні тижня",
                style = MaterialTheme.typography.labelLarge,
            )

            RecurrenceDayOfWeek.entries.chunked(4).forEach { days ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    days.forEach { day ->
                        val selectedDays = rule.daysOfWeek.orEmpty()
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                val nextDays =
                                    if (day in selectedDays) {
                                        selectedDays - day
                                    } else {
                                        selectedDays + day
                                    }
                                onRuleChange(
                                    rule.copy(
                                        daysOfWeek =
                                            nextDays
                                                .distinct()
                                                .sortedBy { it.ordinal }
                                                .takeIf { it.isNotEmpty() },
                                    ),
                                )
                            },
                            label = { Text(day.shortTitle()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(4 - days.size) {
                        Column(modifier = Modifier.weight(1f)) {}
                    }
                }
            }

            Text(
                text = "День створення серії завжди буде включений у перший тиждень.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun RecurrenceFrequency.title(): String =
    when (this) {
        RecurrenceFrequency.DAILY -> "Щодня"
        RecurrenceFrequency.WEEKLY -> "Щотижня"
        RecurrenceFrequency.MONTHLY -> "Щомісяця"
        RecurrenceFrequency.YEARLY -> "Щороку"
    }

private fun RecurrenceDayOfWeek.shortTitle(): String =
    when (this) {
        RecurrenceDayOfWeek.MONDAY -> "Пн"
        RecurrenceDayOfWeek.TUESDAY -> "Вт"
        RecurrenceDayOfWeek.WEDNESDAY -> "Ср"
        RecurrenceDayOfWeek.THURSDAY -> "Чт"
        RecurrenceDayOfWeek.FRIDAY -> "Пт"
        RecurrenceDayOfWeek.SATURDAY -> "Сб"
        RecurrenceDayOfWeek.SUNDAY -> "Нд"
    }
