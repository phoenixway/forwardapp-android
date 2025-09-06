package com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ner

import android.util.Log
import java.util.Calendar
import java.util.Locale

object NerReminderParser {

    // Мапа для числівників
    private val numberMap = mapOf(
        "одна" to 1, "одну" to 1, "однією" to 1,
        "два" to 2, "дві" to 2, "двох" to 2, "двома" to 2,
        "три" to 3, "трьох" to 3,
        "чотири" to 4, "чотирьох" to 4,
        "п’ять" to 5, "пять" to 5, "п’яти" to 5,
        "шість" to 6, "шести" to 6,
        "сім" to 7, "семи" to 7,
        "вісім" to 8, "восьми" to 8,
        "дев’ять" to 9, "девять" to 9, "дев’яти" to 9,
        "десять" to 10, "десяти" to 10
    )

    fun parse(text: String, entities: List<Entity>): ReminderParseResult {
        Log.d("NerReminderParser", "Parsing text: '$text' with entities: $entities")
        if (entities.isEmpty()) {
            Log.w("NerReminderParser", "No entities found for text: '$text'")
            return ReminderParseResult(
                originalText = text,
                dateTimeEntities = emptyList(),
                otherEntities = emptyList(),
                success = false,
                errorMessage = "No entities found"
            )
        }

        val calendar = Calendar.getInstance()
        var dateSet = false
        var timeSet = false
        val detectedDateTimeEntities = mutableListOf<DateTimeEntity>()
        val detectedOtherEntities = mutableListOf<Entity>()

        val timeRelatedEntities = mutableListOf<Entity>()

        entities.forEach { entity ->
            when (entity.label.uppercase()) {
                "DATE" -> {
                    if (parseDate(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        dateSet = true
                        timeRelatedEntities.add(entity)
                        detectedDateTimeEntities.add(toDateTimeEntity(entity))
                    } else {
                        detectedOtherEntities.add(entity)
                    }
                }
                "TIME" -> {
                    if (parseTime(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        timeSet = true
                        timeRelatedEntities.add(entity)
                        detectedDateTimeEntities.add(toDateTimeEntity(entity))
                    } else {
                        detectedOtherEntities.add(entity)
                    }
                }
                "DURATION" -> {
                    if (parseDuration(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        dateSet = true
                        timeSet = true
                        timeRelatedEntities.add(entity)
                        detectedDateTimeEntities.add(toDateTimeEntity(entity))
                    } else {
                        detectedOtherEntities.add(entity)
                    }
                }
                else -> {
                    detectedOtherEntities.add(entity)
                }
            }
        }

        if (!dateSet && !timeSet) {
            return ReminderParseResult(
                originalText = text,
                dateTimeEntities = detectedDateTimeEntities,
                otherEntities = detectedOtherEntities,
                success = false,
                errorMessage = "No valid date/time found"
            )
        }

        if (dateSet && !timeSet) {
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        if (!dateSet && timeSet) {
            val now = Calendar.getInstance()
            if (calendar.timeInMillis < now.timeInMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val suggestion = timeRelatedEntities
            .sortedBy { it.start }
            .joinToString(" ") { it.text }

        return ReminderParseResult(
            originalText = text,
            dateTimeEntities = detectedDateTimeEntities,
            otherEntities = detectedOtherEntities,
            success = true,
            calendar = calendar,
            suggestionText = if (suggestion.isNotBlank()) suggestion else null,
            errorMessage = null
        )
    }

    private fun parseDate(dateText: String, calendar: Calendar): Boolean {
        return when {
            dateText.contains("сьогодні") -> true
            dateText.contains("завтра") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                true
            }
            dateText.contains("післязавтра") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 2)
                true
            }
            dateText.contains("понеділок") -> { setToNextDayOfWeek(calendar, Calendar.MONDAY); true }
            dateText.contains("вівторок") -> { setToNextDayOfWeek(calendar, Calendar.TUESDAY); true }
            dateText.contains("серед") -> { setToNextDayOfWeek(calendar, Calendar.WEDNESDAY); true }
            dateText.contains("четвер") -> { setToNextDayOfWeek(calendar, Calendar.THURSDAY); true }
            dateText.contains("п'ятниц") -> { setToNextDayOfWeek(calendar, Calendar.FRIDAY); true }
            dateText.contains("субот") -> { setToNextDayOfWeek(calendar, Calendar.SATURDAY); true }
            dateText.contains("неділ") -> { setToNextDayOfWeek(calendar, Calendar.SUNDAY); true }
            dateText.matches(Regex("\\d{1,2}[./]\\d{1,2}")) -> parseDatePattern(dateText, calendar)
            else -> false
        }
    }

    private fun parseTime(timeText: String, calendar: Calendar): Boolean {
        return when {
            timeText.matches(Regex("\\d{1,2}[:.]*\\d{0,2}")) -> parseNumericTime(timeText, calendar)
            timeText.contains("ранку") || timeText.contains("вранці") -> { calendar.set(Calendar.HOUR_OF_DAY, 9); calendar.set(Calendar.MINUTE, 0); true }
            timeText.contains("вдень") || timeText.contains("обід") -> { calendar.set(Calendar.HOUR_OF_DAY, 14); calendar.set(Calendar.MINUTE, 0); true }
            timeText.contains("вечора") || timeText.contains("ввечері") -> { calendar.set(Calendar.HOUR_OF_DAY, 19); calendar.set(Calendar.MINUTE, 0); true }
            timeText.contains("ночі") || timeText.contains("вночі") -> { calendar.set(Calendar.HOUR_OF_DAY, 22); calendar.set(Calendar.MINUTE, 0); true }
            else -> false
        }
    }

    private fun parseDuration(durationText: String, calendar: Calendar): Boolean {
        val cleanText = durationText.lowercase(Locale.forLanguageTag("uk-UA"))
        val normalizedText = cleanText.replace("через", "").replace("за", "").trim()

        // 1. Пробуємо знайти число в цифрах
        var number: Int? = Regex("\\d+").find(normalizedText)?.value?.toIntOrNull()

        // 2. Якщо числа немає, пробуємо словесний варіант
        if (number == null) {
            val tokens = normalizedText.split(" ")
            for (t in tokens) {
                val candidate = numberMap[t.trim()]
                if (candidate != null) {
                    number = candidate
                    break
                }
            }
        }

        if (number == null) return false

        return when {
            normalizedText.contains("хв") -> { calendar.add(Calendar.MINUTE, number); true }
            normalizedText.contains("год") -> { calendar.add(Calendar.HOUR_OF_DAY, number); true }
            normalizedText.contains("дн") -> { calendar.add(Calendar.DAY_OF_YEAR, number); true }
            normalizedText.contains("тижн") -> { calendar.add(Calendar.WEEK_OF_YEAR, number); true }
            normalizedText.contains("місяц") -> { calendar.add(Calendar.MONTH, number); true }
            else -> false
        }
    }

    private fun setToNextDayOfWeek(calendar: Calendar, targetDayOfWeek: Int) {
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = targetDayOfWeek - currentDayOfWeek
        if (daysToAdd <= 0) daysToAdd += 7
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
    }

    private fun parseDatePattern(dateText: String, calendar: Calendar): Boolean {
        return try {
            val parts = dateText.split(Regex("[./]"))
            if (parts.size >= 2) {
                val day = parts[0].toIntOrNull() ?: return false
                val month = parts[1].toIntOrNull() ?: return false
                calendar.set(Calendar.DAY_OF_MONTH, day)
                calendar.set(Calendar.MONTH, month - 1)
                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    calendar.add(Calendar.YEAR, 1)
                }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun parseNumericTime(timeText: String, calendar: Calendar): Boolean {
        return try {
            val cleanTime = timeText.replace(Regex("[^\\d:]"), "")
            val parts = cleanTime.split(":")
            val hour = parts[0].toIntOrNull() ?: return false
            val minute = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
            if (hour in 0..23 && minute in 0..59) {
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun toDateTimeEntity(entity: Entity): DateTimeEntity {
        return DateTimeEntity(
            text = entity.text,
            label = entity.label,
            start = entity.start,
            end = entity.end,
            confidence = 1.0f
        )
    }
}
