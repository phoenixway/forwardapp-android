package com.romankozak.forwardappmobile.domain.ner

import java.util.Locale

/**
 * Cheap negative preflight, not a date parser. Keep this a superset of the
 * expressions accepted by NerReminderParser and ReminderParser's fallback.
 * False positives are intentional: only definitely non-temporal text skips NER.
 */
internal fun mayContainReminderExpression(text: String): Boolean {
    if (text.any(Char::isDigit)) return true
    val normalized = text.lowercase(Locale.ROOT)
    return reminderExpressionFragments.any(normalized::contains)
}

private val reminderExpressionFragments = listOf(
    // Relative dates and weekdays accepted by the current Ukrainian parser.
    "сьогодні", "завтра", "понеділ", "вівтор", "серед", "четвер", "п'ятниц", "п’ятниц", "пятниц", "субот", "неділ",
    // Named times and duration units (also catches number words + unit).
    "ранку", "вранці", "вдень", "обід", "вечора", "ввечері", "ночі", "вночі",
    "хв", "год", "днів", "дні", "день", "дн", "тиж", "місяц", "місяць", "рок", "рік",
    // Conservatively retain common alternate spellings/languages for model input.
    "today", "tomorrow", "yesterday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
    "morning", "afternoon", "evening", "tonight", "noon", "midnight",
    "second", "minute", "hour", "day", "week", "month", "year",
)
