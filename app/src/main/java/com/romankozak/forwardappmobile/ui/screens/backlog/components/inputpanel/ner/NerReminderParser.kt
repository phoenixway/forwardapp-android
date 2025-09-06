package com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ner

import android.util.Log
import java.util.Calendar
import java.util.Locale

object NerReminderParser {

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

        // Збираємо всі сущності, що стосуються часу
        val timeRelatedEntities = mutableListOf<Entity>()

        entities.forEach { entity ->
            when (entity.label.uppercase()) {
                "DATE" -> {
                    if (parseDate(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        dateSet = true
                        timeRelatedEntities.add(entity)
                        detectedDateTimeEntities.add(
                            DateTimeEntity(
                                text = entity.text,
                                label = entity.label,
                                start = entity.start,
                                end = entity.end,
                                confidence = 1.0f
                            )
                        )
                    } else {
                        detectedOtherEntities.add(entity)
                    }
                }
                "TIME" -> {
                    if (parseTime(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        timeSet = true
                        timeRelatedEntities.add(entity)
                        detectedDateTimeEntities.add(
                            DateTimeEntity(
                                text = entity.text,
                                label = entity.label,
                                start = entity.start,
                                end = entity.end,
                                confidence = 1.0f
                            )
                        )
                    } else {
                        detectedOtherEntities.add(entity)
                    }
                }
                "DURATION" -> {
                    if (parseDuration(entity.text.lowercase(Locale.getDefault()), calendar)) {
                        dateSet = true
                        timeSet = true
                        timeRelatedEntities.add(entity)
                        detectedDateTimeEntities.add(
                            DateTimeEntity(
                                text = entity.text,
                                label = entity.label,
                                start = entity.start,
                                end = entity.end,
                                confidence = 1.0f
                            )
                        )
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

        // Якщо встановлено лише дату, ставимо час за замовчуванням на 9 ранку
        if (dateSet && !timeSet) {
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        // Якщо встановлено лише час, визначаємо день (сьогодні або завтра)
        if (!dateSet && timeSet) {
            val now = Calendar.getInstance()
            if (calendar.timeInMillis < now.timeInMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // Якщо час вже минув сьогодні, переносимо на завтра
            }
        }

        // Створюємо suggestion text з усіх знайдених часових сущностей
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
            dateText.contains("сьогодні") -> true // Поточний день вже встановлено
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
            dateText.contains("середу") -> {
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
            // Тут можна додати складнішу логіку для розбору дат типу "5 вересня"
            dateText.matches(Regex("\\d{1,2}[./]\\d{1,2}")) -> {
                parseDatePattern(dateText, calendar)
            }
            else -> false
        }
    }

    private fun parseTime(timeText: String, calendar: Calendar): Boolean {
        return when {
            // Парсинг числового часу (наприклад, "15:30", "9", "21.45")
            timeText.matches(Regex("\\d{1,2}[:.]*\\d{0,2}")) -> {
                parseNumericTime(timeText, calendar)
            }
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
    }

    private fun parseDuration(durationText: String, calendar: Calendar): Boolean {
        val cleanText = durationText.lowercase(Locale.forLanguageTag("uk-UA"))
        // Remove "через" and normalize text
        val normalizedText = cleanText.replace("через", "").trim()

        // Extract number using regex
        val numberMatch = Regex("\\d+").find(normalizedText)?.value?.toIntOrNull() ?: return false

        return when {
            normalizedText.contains("хвилин") || normalizedText.contains("хв") -> {
                calendar.add(Calendar.MINUTE, numberMatch)
                true
            }
            normalizedText.contains("годин") || normalizedText.contains("год") -> {
                calendar.add(Calendar.HOUR_OF_DAY, numberMatch)
                true
            }
            normalizedText.contains("днів") || normalizedText.contains("дні") || normalizedText.contains("день") -> {
                calendar.add(Calendar.DAY_OF_YEAR, numberMatch)
                true
            }
            normalizedText.contains("тижн") -> {
                calendar.add(Calendar.WEEK_OF_YEAR, numberMatch)
                true
            }
            normalizedText.contains("місяц") -> {
                calendar.add(Calendar.MONTH, numberMatch)
                true
            }
            else -> false
        }
    }

    private fun setToNextDayOfWeek(calendar: Calendar, targetDayOfWeek: Int) {
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = targetDayOfWeek - currentDayOfWeek

        if (daysToAdd <= 0) {
            daysToAdd += 7 // Наступний тиждень
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
    }

    private fun parseDatePattern(dateText: String, calendar: Calendar): Boolean {
        try {
            val parts = dateText.split(Regex("[./]"))
            if (parts.size >= 2) {
                val day = parts[0].toIntOrNull() ?: return false
                val month = parts[1].toIntOrNull() ?: return false

                calendar.set(Calendar.DAY_OF_MONTH, day)
                calendar.set(Calendar.MONTH, month - 1) // Calendar months are 0-based

                // Якщо дата в минулому, переносимо на наступний рік
                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    calendar.add(Calendar.YEAR, 1)
                }
                return true
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    private fun parseNumericTime(timeText: String, calendar: Calendar): Boolean {
        try {
            val cleanTime = timeText.replace(Regex("[^\\d:]"), "")
            val parts = cleanTime.split(":")

            val hour = parts[0].toIntOrNull() ?: return false
            val minute = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0

            if (hour in 0..23 && minute in 0..59) {
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return true
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
}