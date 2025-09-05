package com.romankozak.forwardappmobile.domain.ner

import com.romankozak.forwardappmobile.domain.ReminderParseResult
import java.util.Calendar
import java.util.Locale

object NerReminderParser {

    fun parse(text: String, entities: List<Entity>): ReminderParseResult {
        if (entities.isEmpty()) {
            return ReminderParseResult(originalText = text, calendar = null, suggestionText = null)
        }

        val calendar = Calendar.getInstance()
        var dateSet = false
        var timeSet = false

        entities.forEach { entity ->
            when (entity.label.uppercase()) {
                "DATE" -> {
                    if (parseDate(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        dateSet = true
                    }
                }
                "TIME" -> {
                    if (parseTime(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        timeSet = true
                    }
                }
                "DURATION" -> {
                    if (parseDuration(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        // Duration sets both date and time relative to now
                        dateSet = true
                        timeSet = true
                    }
                }
            }
        }

        if (!dateSet && !timeSet) {
            return ReminderParseResult(originalText = text, calendar = null, suggestionText = null)
        }

        // Якщо встановлено лише дату, ставимо час за замовчуванням на 9 ранку
        if (dateSet && !timeSet) {
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
        }

        // Якщо встановлено лише час, визначаємо день (сьогодні або завтра)
        if (!dateSet && timeSet) {
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // Якщо час вже минув сьогодні, переносимо на завтра
            }
        }

        val suggestion = entities.sortedBy { it.start }.joinToString(" ") { it.text }

        return ReminderParseResult(originalText = text, calendar = calendar, suggestionText = suggestion)
    }

    private fun parseDate(dateText: String, calendar: Calendar): Boolean {
        return when {
            dateText.contains("сьогодні") -> true // Поточний день вже встановлено
            dateText.contains("завтра") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1); true
            }
            dateText.contains("післязавтра") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 2); true
            }
            // Тут можна додати складнішу логіку для розбору дат типу "5 вересня"
            else -> false
        }
    }

    private fun parseTime(timeText: String, calendar: Calendar): Boolean {
        val digits = timeText.filter { it.isDigit() }
        return when {
            digits.isNotEmpty() -> {
                val parts = digits.split("(?<=\\d{2})".toRegex()).filter { it.isNotEmpty() }
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: return false
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                true
            }
            timeText.contains("ранку") || timeText.contains("вранці") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 9); calendar.set(Calendar.MINUTE, 0); true
            }
            timeText.contains("вдень") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 14); calendar.set(Calendar.MINUTE, 0); true
            }
            timeText.contains("вечора") || timeText.contains("ввечері") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 19); calendar.set(Calendar.MINUTE, 0); true
            }
            else -> false
        }
    }

    private fun parseDuration(durationText: String, calendar: Calendar): Boolean {
        val number = durationText.filter { it.isDigit() }.toIntOrNull() ?: return false
        return when {
            durationText.contains("хвилин") -> {
                calendar.add(Calendar.MINUTE, number); true
            }
            durationText.contains("годин") -> {
                calendar.add(Calendar.HOUR_OF_DAY, number); true
            }
            durationText.contains("днів") || durationText.contains("дня") -> {
                calendar.add(Calendar.DAY_OF_YEAR, number); true
            }
            else -> false
        }
    }
}