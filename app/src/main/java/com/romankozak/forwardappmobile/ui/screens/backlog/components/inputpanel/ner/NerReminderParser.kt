// File: NerReminderParser.kt
package com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ner

import android.util.Log
import java.util.Calendar
import java.util.Locale

object NerReminderParser {

    // Єдина мапа для числівників
    val numberMap = mapOf(
        "одна" to 1, "одну" to 1, "однією" to 1, "один" to 1,
        "два" to 2, "дві" to 2, "двох" to 2, "двома" to 2,
        "три" to 3, "трьох" to 3,
        "чотири" to 4, "чотирьох" to 4,
        "п'ять" to 5, "пять" to 5, "п'яти" to 5,
        "шість" to 6, "шести" to 6,
        "сім" to 7, "семи" to 7,
        "вісім" to 8, "восьми" to 8,
        "дев'ять" to 9, "девять" to 9, "дев'яти" to 9,
        "десять" to 10, "десяти" to 10
    )

    fun parse(text: String, entities: List<Entity>): ReminderParseResult {
        Log.d("NerReminderParser", "Parsing text: '$text' with entities: $entities")

        // ДОДАНО: Спочатку спробуємо розширити DURATION entities
        val expandedEntities = expandDurationEntities(text, entities)
        Log.d("NerReminderParser", "Expanded entities: $expandedEntities")

        if (expandedEntities.isEmpty()) {
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

        expandedEntities.forEach { entity ->
            when (entity.label.uppercase()) {
                "DATE" -> {
                    if (parseDate(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        dateSet = true
                        timeRelatedEntities.add(entity)
                        detectedDateTimeEntities.add(toDateTimeEntity(entity))
                    } else detectedOtherEntities.add(entity)
                }
                "TIME" -> {
                    if (parseTime(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        timeSet = true
                        timeRelatedEntities.add(entity)
                        detectedDateTimeEntities.add(toDateTimeEntity(entity))
                    } else detectedOtherEntities.add(entity)
                }
                "DURATION" -> {
                    if (parseDuration(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        dateSet = true
                        timeSet = true
                        timeRelatedEntities.add(entity)
                        detectedDateTimeEntities.add(toDateTimeEntity(entity))
                    } else detectedOtherEntities.add(entity)
                }
                else -> detectedOtherEntities.add(entity)
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

        val suggestion = timeRelatedEntities.sortedBy { it.start }
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

    // НОВА ФУНКЦІЯ: Розширює DURATION entities, шукаючи повні фрази
    private fun expandDurationEntities(text: String, entities: List<Entity>): List<Entity> {
        val result = mutableListOf<Entity>()
        val textLower = text.lowercase(Locale.getDefault())

        entities.forEach { entity ->
            if (entity.label.uppercase() == "DURATION") {
                // Спробуємо розширити entity, щоб включити повну фразу
                val expandedEntity = expandDurationEntity(textLower, entity)
                result.add(expandedEntity)
                Log.d("NerReminderParser", "Expanded DURATION from '${entity.text}' to '${expandedEntity.text}'")
            } else {
                result.add(entity)
            }
        }

        return result
    }

    // НОВА ФУНКЦІЯ: Розширює конкретний DURATION entity
    private fun expandDurationEntity(textLower: String, entity: Entity): Entity {
        val originalText = entity.text.lowercase()
        var newStart = entity.start
        var newEnd = entity.end

        // Регулярний вираз для пошуку повної фрази з тривалістю
        val durationPattern = Regex(
            """(через|за)\s*(\d+|один|одну|одна|два|дві|три|чотири|п'ять|шість|сім|вісім|дев'ять|десять)\s*(хв|хвилин|хвилину|год|годин|годину|дні|днів|день|тижн|тижні|тиждень|місяць|місяці|року|років)"""
        )

        // Шукаємо найближчу повну фразу, що включає наш entity
        val matches = durationPattern.findAll(textLower)
        for (match in matches) {
            val matchStart = match.range.first
            val matchEnd = match.range.last + 1

            // Перевіряємо, чи перетинається наш entity з цим match
            if (entity.start < matchEnd && entity.end > matchStart) {
                // Розширюємо entity до повної фрази
                newStart = matchStart
                newEnd = matchEnd
                val newText = textLower.substring(newStart, newEnd)

                return Entity(
                    label = entity.label,
                    start = newStart,
                    end = newEnd,
                    text = newText
                )
            }
        }

        // Якщо не знайшли повного match, повертаємо оригінальний entity
        return entity
    }

    private fun parseDate(dateText: String, calendar: Calendar): Boolean {
        return when {
            dateText.contains("сьогодні") -> true
            dateText.contains("завтра") -> { calendar.add(Calendar.DAY_OF_YEAR, 1); true }
            dateText.contains("післязавтра") -> { calendar.add(Calendar.DAY_OF_YEAR, 2); true }
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
        Log.d("NerReminderParser", "Parsing duration: '$durationText'")

        val normalizedText = durationText.replace("через", "")
            .replace("за", "").trim()

        var number: Int? = Regex("\\d+").find(normalizedText)?.value?.toIntOrNull()

        if (number == null) {
            normalizedText.split(" ").forEach { word ->
                val cleanWord = word.trim()
                numberMap[cleanWord]?.let {
                    number = it
                    Log.d("NerReminderParser", "Found number word '$cleanWord' = $it")
                    return@forEach
                }
            }
        }

        if (number == null) {
            Log.w("NerReminderParser", "No number found in duration text: '$durationText'")
            return false
        }

        val result = when {
            normalizedText.contains("хв") -> { calendar.add(Calendar.MINUTE, number!!); true }
            normalizedText.contains("год") -> { calendar.add(Calendar.HOUR_OF_DAY, number!!); true }
            normalizedText.contains("дн") || normalizedText.contains("день") -> { calendar.add(Calendar.DAY_OF_YEAR, number!!); true }
            normalizedText.contains("тижн") -> { calendar.add(Calendar.WEEK_OF_YEAR, number!!); true }
            normalizedText.contains("місяц") -> { calendar.add(Calendar.MONTH, number!!); true }
            normalizedText.contains("рок") -> { calendar.add(Calendar.YEAR, number!!); true }
            else -> {
                Log.w("NerReminderParser", "No time unit found in duration text: '$durationText'")
                false
            }
        }

        if (result) {
            Log.d("NerReminderParser", "Successfully parsed duration: $number from '$durationText'")
        }

        return result
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
        } catch (e: Exception) { false }
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
        } catch (e: Exception) { false }
    }

    // НОВА ФУНКЦІЯ: Витягує goal text, видаляючи time-related entities
    private fun extractGoalText(originalText: String, timeRelatedEntities: List<Entity>): String {
        if (timeRelatedEntities.isEmpty()) return originalText.trim()

        // Сортуємо entities за позицією
        val sortedEntities = timeRelatedEntities.sortedBy { it.start }

        val result = StringBuilder()
        var lastEnd = 0

        sortedEntities.forEach { entity ->
            // Додаємо текст до поточного entity
            if (entity.start > lastEnd) {
                result.append(originalText.substring(lastEnd, entity.start))
            }
            // Пропускаємо сам entity
            lastEnd = entity.end
        }

        // Додаємо решту тексту після останнього entity
        if (lastEnd < originalText.length) {
            result.append(originalText.substring(lastEnd))
        }

        return result.toString().trim()
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