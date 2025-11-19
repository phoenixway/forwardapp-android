package com.romankozak.forwardappmobile.domain.ner

import java.util.Calendar

data class ReminderParseResult(
    val originalText: String,
    val dateTimeEntities: List<DateTimeEntity>,
    val otherEntities: List<Any>, // Simplified
    val success: Boolean,
    val calendar: Calendar? = null,
    val suggestionText: String? = null,
    val errorMessage: String? = null,
)

data class DateTimeEntity(
    val text: String,
    val label: String,
    val start: Int,
    val end: Int,
    val confidence: Float,
)
