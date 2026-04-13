package com.romankozak.forwardappmobile.domain.ner

import android.util.Log
import java.util.Calendar
import java.util.Locale

object NerReminderParser {
    private data class DateTimeDetectionState(
        val calendar: Calendar,
        var dateSet: Boolean = false,
        var timeSet: Boolean = false,
        val detectedDateTimeEntities: MutableList<DateTimeEntity> = mutableListOf(),
        val detectedOtherEntities: MutableList<Entity> = mutableListOf(),
        val timeRelatedEntities: MutableList<Entity> = mutableListOf(),
    )

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

        val detectionState = DateTimeDetectionState(calendar = Calendar.getInstance())
        expandedEntities.forEach { entity -> processEntity(entity, detectionState) }

        if (!detectionState.dateSet && !detectionState.timeSet) {
            return ReminderParseResult(
                originalText = text,
                dateTimeEntities = detectionState.detectedDateTimeEntities,
                otherEntities = detectionState.detectedOtherEntities,
                success = false,
                errorMessage = "No valid date/time found",
            )
        }

        applyMissingDateOrTimeDefaults(detectionState)

        val suggestion =
            detectionState.timeRelatedEntities
                .sortedBy { it.start }
                .joinToString(" ") { it.text }

        val goalText = extractGoalText(text, detectionState.timeRelatedEntities)
        if (goalText.isNotEmpty()) {
            detectionState.detectedOtherEntities.add(
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
            dateTimeEntities = detectionState.detectedDateTimeEntities,
            otherEntities = detectionState.detectedOtherEntities,
            success = true,
            calendar = detectionState.calendar,
            suggestionText = if (suggestion.isNotBlank()) suggestion else null,
            errorMessage = null,
        )
    }

    private fun processEntity(
        entity: Entity,
        state: DateTimeDetectionState,
    ) {
        val normalizedText = entity.text.lowercase(Locale.getDefault())
        when (entity.label.uppercase()) {
            "DATE" -> updateDetectionState(entity, state, parseDate(normalizedText, state.calendar), setDate = true)
            "TIME" -> updateDetectionState(entity, state, parseTime(normalizedText, state.calendar), setTime = true)
            "DURATION" ->
                updateDetectionState(
                    entity = entity,
                    state = state,
                    parsed = parseDuration(normalizedText, state.calendar),
                    setDate = true,
                    setTime = true,
                )
            else -> state.detectedOtherEntities.add(entity)
        }
    }

    private fun updateDetectionState(
        entity: Entity,
        state: DateTimeDetectionState,
        parsed: Boolean,
        setDate: Boolean = false,
        setTime: Boolean = false,
    ) {
        if (!parsed) {
            state.detectedOtherEntities.add(entity)
            return
        }
        if (setDate) state.dateSet = true
        if (setTime) state.timeSet = true
        state.timeRelatedEntities.add(entity)
        state.detectedDateTimeEntities.add(toDateTimeEntity(entity))
    }

    private fun applyMissingDateOrTimeDefaults(state: DateTimeDetectionState) {
        if (state.dateSet && !state.timeSet) {
            state.calendar.set(Calendar.HOUR_OF_DAY, 9)
            state.calendar.set(Calendar.MINUTE, 0)
            state.calendar.set(Calendar.SECOND, 0)
            state.calendar.set(Calendar.MILLISECOND, 0)
        }

        if (!state.dateSet && state.timeSet) {
            val now = Calendar.getInstance()
            if (state.calendar.timeInMillis < now.timeInMillis) {
                state.calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
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

        val result = applyDurationUnit(calendar, normalizedText, number, durationText)

        if (result) {
            Log.d("NerReminderParser", "Successfully parsed duration: $number from '$durationText'")
        }

        return result
    }

    private fun applyDurationUnit(
        calendar: Calendar,
        normalizedText: String,
        number: Int,
        durationText: String,
    ): Boolean {
        val calendarField =
            when {
                containsAny(normalizedText, "хвилину", "хвилини", "хвилин", "хв") -> Calendar.MINUTE
                containsAny(normalizedText, "годину", "години", "годин", "год") -> Calendar.HOUR_OF_DAY
                containsAny(normalizedText, "днів", "дні", "день", "дн") -> Calendar.DAY_OF_YEAR
                containsAny(normalizedText, "тижнів", "тижні", "тиждень", "тижн") -> Calendar.WEEK_OF_YEAR
                containsAny(normalizedText, "місяців", "місяці", "місяць") -> Calendar.MONTH
                containsAny(normalizedText, "років", "роки", "року", "рок") -> Calendar.YEAR
                else -> null
            }
        if (calendarField == null) {
            Log.w("NerReminderParser", "No time unit found in duration text: '$durationText'")
            return false
        }
        calendar.add(calendarField, number)
        return true
    }

    private fun containsAny(
        text: String,
        vararg variants: String,
    ): Boolean = variants.any(text::contains)

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
