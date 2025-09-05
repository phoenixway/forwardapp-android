// ReminderParser.kt

package com.romankozak.forwardappmobile.domain

import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

// Змінюємо назву поля для ясності
data class ReminderParseResult(
    val originalText: String, // Текст без змін
    val calendar: Calendar?,     // Розпізнаний час, якщо є
    val suggestionText: String? // Текст для підказки-чіпа
)

object ReminderParser {

    fun parse(text: String): ReminderParseResult {
        // Регулярний вираз для розпізнавання часу (напр. "о 15:00", "в 9")
        val timeRegex = "(\\sо\\s|\\sв\\s)?(\\d{1,2}:\\d{2}|\\d{1,2})".toRegex(RegexOption.IGNORE_CASE)

        // Патерни для розпізнавання дат
        val patterns = mapOf(
            "завтра" to "завтра",
            "післязавтра" to "післязавтра",
            "через (\\d+) годин" to "через (\\d+) годин(и|у)?",
            "через (\\d+) хвилин" to "через (\\d+) хвилин(и|у)?"
        )

        for ((key, patternString) in patterns) {
            val pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE)
            val matcher = pattern.matcher(text)

            if (matcher.find()) {
                val foundDateText = matcher.group(0) ?: ""
                // --- ЗМІНА: Не видаляємо розпізнаний текст ---
                // var remainingText = text.replace(foundDateText, "", true).trim()
                val calendar = Calendar.getInstance()

                // Встановлюємо дату
                when {
                    key.startsWith("завтра") -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                    key.startsWith("післязавтра") -> calendar.add(Calendar.DAY_OF_YEAR, 2)
                    key.startsWith("через (\\d+) годин") -> calendar.add(Calendar.HOUR_OF_DAY, matcher.group(1)?.toIntOrNull() ?: 0)
                    key.startsWith("через (\\d+) хвилин") -> calendar.add(Calendar.MINUTE, matcher.group(1)?.toIntOrNull() ?: 0)
                }

                // Встановлюємо час
                val timeMatch = timeRegex.find(text)
                var suggestion = foundDateText.replaceFirstChar { it.uppercase() }

                if (timeMatch != null) {
                    val timeString = timeMatch.groupValues[2]
                    val timeParts = timeString.split(":")
                    val hour = timeParts[0].toIntOrNull() ?: 9
                    val minute = if (timeParts.size > 1) timeParts[1].toIntOrNull() ?: 0 else 0

                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)

                    val fullTimeText = timeMatch.value
                    suggestion += fullTimeText

                    // --- ЗМІНА: Не видаляємо час з тексту ---
                    // remainingText = remainingText.replace(fullTimeText, "", true).trim()

                } else {
                    // Якщо час не вказано, за замовчуванням ставимо на 9 ранку
                    calendar.set(Calendar.HOUR_OF_DAY, 9)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                }

                // Повертаємо оригінальний текст і розпізнані дані
                return ReminderParseResult(text, calendar, suggestion)
            }
        }

        // Якщо нічого не знайдено, повертаємо текст як є
        return ReminderParseResult(text, null, null)
    }
}