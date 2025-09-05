package com.romankozak.forwardappmobile.domain

import com.romankozak.forwardappmobile.domain.ner.NerManager
import com.romankozak.forwardappmobile.domain.ner.NerReminderParser
import java.util.Calendar
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

// ВИЗНАЧЕННЯ DATA CLASS ДОДАНО СЮДИ
data class ReminderParseResult(
    val originalText: String,
    val calendar: Calendar?,
    val suggestionText: String?
)

@Singleton
class ReminderParser @Inject constructor(
    private val nerManager: NerManager
) {

    /**
     * Головний метод парсингу, який діє як фасад.
     * Спочатку намагається розпізнати дату/час за допомогою NER-моделі.
     * Якщо не вдається, використовує резервний парсер на основі регулярних виразів.
     */
    fun parse(text: String): ReminderParseResult {
        // 1. Спроба розпізнавання через NER
        val nerEntities = nerManager.predict(text)
        if (nerEntities != null && nerEntities.isNotEmpty()) {
            val nerResult = NerReminderParser.parse(text, nerEntities)
            // Якщо NER-парсер успішно знайшов і розібрав дату/час, повертаємо його результат
            if (nerResult.calendar != null) {
                return nerResult
            }
        }

        // 2. Резервний варіант: парсинг за допомогою регулярних виразів
        return parseWithRegex(text)
    }

    /**
     * Резервний парсер, що використовує регулярні вирази.
     */
    private fun parseWithRegex(text: String): ReminderParseResult {
        val timeRegex = "(\\sо\\s|\\sв\\s)?(\\d{1,2}:\\d{2}|\\d{1,2})".toRegex(RegexOption.IGNORE_CASE)

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
                val calendar = Calendar.getInstance()

                when {
                    key.startsWith("завтра") -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                    key.startsWith("післязавтра") -> calendar.add(Calendar.DAY_OF_YEAR, 2)
                    key.startsWith("через (\\d+) годин") -> calendar.add(Calendar.HOUR_OF_DAY, matcher.group(1)?.toIntOrNull() ?: 0)
                    key.startsWith("через (\\d+) хвилин") -> calendar.add(Calendar.MINUTE, matcher.group(1)?.toIntOrNull() ?: 0)
                }

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
                    suggestion += timeMatch.value
                } else {
                    calendar.set(Calendar.HOUR_OF_DAY, 9)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                }
                return ReminderParseResult(text, calendar, suggestion)
            }
        }
        return ReminderParseResult(text, null, null)
    }
}