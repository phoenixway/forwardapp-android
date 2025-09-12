package com.romankozak.forwardappmobile.domain.ner

import android.util.Log
import java.util.Calendar
import java.util.Locale

object NerReminderParser {
    val numberMap =
        mapOf(
            "одна" to 1,
            "одну" to 1,
            "однією" to 1,
            "один" to 1,
            "два" to 2,
            "дві" to 2,
            "двох" to 2,
            "двома" to 2,
            "три" to 3,
            "трьох" to 3,
            "чотири" to 4,
            "чотирьох" to 4,
            "п'ять" to 5,
            "пять" to 5,
            "п'яти" to 5,
            "шість" to 6,
            "шести" to 6,
            "сім" to 7,
            "семи" to 7,
            "вісім" to 8,
            "восьми" to 8,
            "дев'ять" to 9,
            "девять" to 9,
            "дев'яти" to 9,
            "десять" to 10,
            "десяти" to 10,
        )

    fun parse(
        text: String,
        entities: List<Entity>,
    ): ReminderParseResult {
        Log.d("NerReminderParser", "Parsing text: '$text' with entities: $entities")

        val expandedEntities = expandDurationEntities(text, entities)
        Log.d("NerReminderParser", "Expanded entities: $expandedEntities")

        if (expandedEntities.isEmpty()) {
            Log.w("NerReminderParser", "No entities found for text: '$text'")
            return ReminderParseResult(
                originalText = text,
                dateTimeEntities = emptyList(),
                otherEntities = emptyList(),
                success = false,
                errorMessage = "No entities found",
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
                else -> detectedOtherEntities.add(entity)
            }
        }

        if (!dateSet && !timeSet) {
            return ReminderParseResult(
                originalText = text,
                dateTimeEntities = detectedDateTimeEntities,
                otherEntities = detectedOtherEntities,
                success = false,
                errorMessage = "No valid date/time found",
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

        val suggestion =
            timeRelatedEntities
                .sortedBy { it.start }
                .joinToString(" ") { it.text }

        val goalText = extractGoalText(text, timeRelatedEntities)
        if (goalText.isNotEmpty()) {
            detectedOtherEntities.add(
                Entity(
                    text = goalText,
                    label = "GOAL",
                    start = 0,
                    end = goalText.length,
                ),
            )
        }

        return ReminderParseResult(
            originalText = text,
            dateTimeEntities = detectedDateTimeEntities,
            otherEntities = detectedOtherEntities,
            success = true,
            calendar = calendar,
            suggestionText = if (suggestion.isNotBlank()) suggestion else null,
            errorMessage = null,
        )
    }

    private fun expandDurationEntities(
        text: String,
        entities: List<Entity>,
    ): List<Entity> {
        val result = mutableListOf<Entity>()
        val textLower = text.lowercase(Locale.getDefault())

        entities.forEach { entity ->
            if (entity.label.uppercase() == "DURATION") {
                val expandedEntity = expandDurationEntity(textLower, entity)
                result.add(expandedEntity)
                Log.d("NerReminderParser", "Expanded DURATION from '${entity.text}' to '${expandedEntity.text}'")
            } else {
                result.add(entity)
            }
        }

        return result
    }

    private fun expandDurationEntity(
        textLower: String,
        entity: Entity,
    ): Entity {
        val originalText = entity.text.lowercase()
        var newStart = entity.start
        var newEnd = entity.end

        val durationPattern =
            Regex(
                """(через|за)\s*(\d+|один|одну|одна|два|дві|три|чотири|п'ять|пять|шість|сім|вісім|дев'ять|девять|десять)\s*(хвилин[уи]?|хв|годин[уи]?|год|дн[іи]в?|день|тижн[іи]в?|тиждень|місяц[іи]в?|місяць|рок[иу]в?|року)""",
            )

        val matches = durationPattern.findAll(textLower)
        for (match in matches) {
            val matchStart = match.range.first
            val matchEnd = match.range.last + 1

            if (entity.start < matchEnd && entity.end > matchStart) {
                newStart = matchStart
                newEnd = matchEnd
                val newText = textLower.substring(newStart, newEnd)

                Log.d("NerReminderParser", "Found full duration pattern: '$newText' at $newStart-$newEnd")

                return Entity(
                    label = entity.label,
                    start = newStart,
                    end = newEnd,
                    text = newText,
                )
            }
        }

        val entityWords = entity.text.split(" ")
        val entityEnd = entity.end

        val nextWordMatch = Regex("""\s*([а-яії']+)""").find(textLower, entityEnd)
        if (nextWordMatch != null) {
            val nextWord = nextWordMatch.groups[1]?.value
            if (nextWord != null && isTimeUnit(nextWord)) {
                val newEndPos = nextWordMatch.range.last + 1
                val newText = textLower.substring(entity.start, newEndPos)

                Log.d("NerReminderParser", "Extended entity with next word: '$newText'")

                return Entity(
                    label = entity.label,
                    start = entity.start,
                    end = newEndPos,
                    text = newText,
                )
            }
        }

        Log.d("NerReminderParser", "No expansion found, keeping original entity")
        return entity
    }

    private fun isTimeUnit(word: String): Boolean =
        when {
            word.startsWith("хвилин") || word == "хв" -> true
            word.startsWith("годин") || word == "год" -> true
            word.startsWith("дн") || word == "день" -> true
            word.startsWith("тижн") -> true
            word.startsWith("місяц") -> true
            word.startsWith("рок") -> true
            else -> false
        }

    private fun parseDate(
        dateText: String,
        calendar: Calendar,
    ): Boolean =
        when {
            dateText.contains("сьогодні") -> true
            dateText.contains("завтра") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                true
            }
            dateText.contains("післязавтра") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 2)
                true
            }
            dateText.contains("понеділок") -> {
                setToNextDayOfWeek(calendar, Calendar.MONDAY)
                true
            }
            dateText.contains("вівторок") -> {
                setToNextDayOfWeek(calendar, Calendar.TUESDAY)
                true
            }
            dateText.contains("серед") -> {
                setToNextDayOfWeek(calendar, Calendar.WEDNESDAY)
                true
            }
            dateText.contains("четвер") -> {
                setToNextDayOfWeek(calendar, Calendar.THURSDAY)
                true
            }
            dateText.contains("п'ятниц") -> {
                setToNextDayOfWeek(calendar, Calendar.FRIDAY)
                true
            }
            dateText.contains("субот") -> {
                setToNextDayOfWeek(calendar, Calendar.SATURDAY)
                true
            }
            dateText.contains("неділ") -> {
                setToNextDayOfWeek(calendar, Calendar.SUNDAY)
                true
            }
            dateText.matches(Regex("\\d{1,2}[./]\\d{1,2}")) -> parseDatePattern(dateText, calendar)
            else -> false
        }

    private fun parseTime(
        timeText: String,
        calendar: Calendar,
    ): Boolean =
        when {
            timeText.matches(Regex("\\d{1,2}[:.]*\\d{0,2}")) -> parseNumericTime(timeText, calendar)
            timeText.contains("ранку") || timeText.contains("вранці") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
                true
            }
            timeText.contains("вдень") || timeText.contains("обід") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 14)
                calendar.set(Calendar.MINUTE, 0)
                true
            }
            timeText.contains("вечора") || timeText.contains("ввечері") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 19)
                calendar.set(Calendar.MINUTE, 0)
                true
            }
            timeText.contains("ночі") || timeText.contains("вночі") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 22)
                calendar.set(Calendar.MINUTE, 0)
                true
            }
            else -> false
        }

    private fun parseDuration(
        durationText: String,
        calendar: Calendar,
    ): Boolean {
        Log.d("NerReminderParser", "Parsing duration: '$durationText'")

        val normalizedText =
            durationText
                .replace("через", "")
                .replace("за", "")
                .trim()

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

        val result =
            when {
                normalizedText.contains("хвилину") || normalizedText.contains("хвилини") || normalizedText.contains("хвилин") ||
                    normalizedText.contains("хв") -> {
                    calendar.add(Calendar.MINUTE, number!!)
                    true
                }
                normalizedText.contains("годину") || normalizedText.contains("години") || normalizedText.contains("годин") ||
                    normalizedText.contains("год") -> {
                    calendar.add(Calendar.HOUR_OF_DAY, number!!)
                    true
                }
                normalizedText.contains("днів") || normalizedText.contains("дні") || normalizedText.contains("день") ||
                    normalizedText.contains("дн") -> {
                    calendar.add(Calendar.DAY_OF_YEAR, number!!)
                    true
                }
                normalizedText.contains("тижнів") || normalizedText.contains("тижні") || normalizedText.contains("тиждень") ||
                    normalizedText.contains("тижн") -> {
                    calendar.add(Calendar.WEEK_OF_YEAR, number!!)
                    true
                }
                normalizedText.contains("місяців") || normalizedText.contains("місяці") || normalizedText.contains("місяць") -> {
                    calendar.add(Calendar.MONTH, number!!)
                    true
                }
                normalizedText.contains("років") || normalizedText.contains("роки") || normalizedText.contains("року") ||
                    normalizedText.contains("рок") -> {
                    calendar.add(Calendar.YEAR, number!!)
                    true
                }
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

    private fun setToNextDayOfWeek(
        calendar: Calendar,
        targetDayOfWeek: Int,
    ) {
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = targetDayOfWeek - currentDayOfWeek
        if (daysToAdd <= 0) daysToAdd += 7
        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
    }

    private fun parseDatePattern(
        dateText: String,
        calendar: Calendar,
    ): Boolean {
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
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun parseNumericTime(
        timeText: String,
        calendar: Calendar,
    ): Boolean {
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
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun extractGoalText(
        originalText: String,
        timeRelatedEntities: List<Entity>,
    ): String {
        Log.d("NerReminderParser", "Extracting goal text from: '$originalText' with entities: $timeRelatedEntities")

        if (timeRelatedEntities.isEmpty()) return originalText.trim()

        val sortedEntities = timeRelatedEntities.sortedBy { it.start }

        val result = StringBuilder()
        var lastEnd = 0

        sortedEntities.forEach { entity ->
            Log.d("NerReminderParser", "Processing entity: '${entity.text}' at ${entity.start}-${entity.end}")

            if (entity.start > lastEnd) {
                val beforeText = originalText.substring(lastEnd, entity.start)
                Log.d("NerReminderParser", "Adding text before entity: '$beforeText'")
                result.append(beforeText)
            }
            lastEnd = entity.end
        }

        if (lastEnd < originalText.length) {
            val afterText = originalText.substring(lastEnd)
            Log.d("NerReminderParser", "Adding text after last entity: '$afterText'")
            result.append(afterText)
        }

        val finalResult = result.toString().trim()
        Log.d("NerReminderParser", "Final goal text: '$finalResult'")

        return finalResult
    }

    private fun toDateTimeEntity(entity: Entity): DateTimeEntity =
        DateTimeEntity(
            text = entity.text,
            label = entity.label,
            start = entity.start,
            end = entity.end,
            confidence = 1.0f,
        )
}
