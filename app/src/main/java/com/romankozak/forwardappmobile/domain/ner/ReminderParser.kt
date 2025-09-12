package com.romankozak.forwardappmobile.domain.ner

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ReminderParser
    @Inject
    constructor(
        private val nerManager: NerManager,
    ) {
        private val TAG = "ReminderParser"

        private val textToNumberMap =
            mapOf(
                "один" to 1,
                "одну" to 1,
                "одна" to 1,
                "два" to 2,
                "дві" to 2,
                "три" to 3,
                "чотири" to 4,
                "п'ять" to 5,
                "шість" to 6,
                "сім" to 7,
                "вісім" to 8,
                "дев'ять" to 9,
                "десять" to 10,
            )

        fun parseAsync(
            text: String,
            callback: (ReminderParseResult) -> Unit,
        ) {
            Log.d(TAG, "[ReminderParser] Parsing started: '$text'")

            val nerState = nerManager.nerState.value
            if (nerState !is NerState.Ready) {
                Log.w(TAG, "[ReminderParser] NER not ready, state: $nerState, trying fallback")
                val fallbackResult = fallbackParseDuration(text)
                if (fallbackResult != null) {
                    Log.d(TAG, "[ReminderParser] Fallback successful: ${fallbackResult.suggestionText}")
                    callback(fallbackResult)
                } else {
                    callback(
                        ReminderParseResult(
                            originalText = text,
                            dateTimeEntities = emptyList(),
                            otherEntities = emptyList(),
                            success = false,
                            errorMessage = "NER not ready and fallback failed",
                        ),
                    )
                }
                return
            }

            nerManager.predictAsync(text) { nerEntities ->
                val result =
                    if (nerEntities != null && nerEntities.isNotEmpty()) {
                        Log.d(TAG, "[ReminderParser] NER found ${nerEntities.size} entities")
                        processEntitiesWithReminder(text, nerEntities)
                    } else {
                        Log.w(TAG, "[ReminderParser] No entities from NER model, trying fallback parser")
                        fallbackParseDuration(text) ?: ReminderParseResult(
                            originalText = text,
                            dateTimeEntities = emptyList(),
                            otherEntities = emptyList(),
                            success = false,
                            errorMessage = "No entities detected by NER or fallback",
                        )
                    }
                Log.d(TAG, "[ReminderParser] Parse complete: success=${result.success}, suggestion=${result.suggestionText}")
                callback(result)
            }
        }

        suspend fun parseAsync(text: String): ReminderParseResult =
            suspendCoroutine { cont ->
                Log.d(TAG, "[ReminderParser] Suspend parseAsync started: '$text'")
                parseAsync(text) { result ->
                    cont.resume(result)
                }
            }

        suspend fun parseWithTimeout(
            text: String,
            timeoutMs: Long = 10000L,
        ): ReminderParseResult =
            try {
                withTimeout(timeoutMs) {
                    parseAsync(text)
                }
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Parsing timeout for: '$text', trying fallback")
                fallbackParseDuration(text) ?: ReminderParseResult(
                    originalText = text,
                    dateTimeEntities = emptyList(),
                    otherEntities = emptyList(),
                    success = false,
                    errorMessage = "Parsing timeout",
                )
            } catch (e: Exception) {
                Log.e(TAG, "Parsing error for: '$text'", e)
                fallbackParseDuration(text) ?: ReminderParseResult(
                    originalText = text,
                    dateTimeEntities = emptyList(),
                    otherEntities = emptyList(),
                    success = false,
                    errorMessage = "Parsing error: ${e.message}",
                )
            }

        private fun processEntitiesWithReminder(
            text: String,
            nerEntities: List<Entity>,
        ): ReminderParseResult {
            Log.d(TAG, "[ReminderParser] Processing ${nerEntities.size} entities: $nerEntities")
            return NerReminderParser.parse(text, nerEntities)
        }

        private fun fallbackParseDuration(text: String): ReminderParseResult? {
            val cleanText = text.lowercase(Locale.forLanguageTag("uk-UA")).trim()
            Log.d(TAG, "[ReminderParser] Fallback parsing: '$cleanText'")

            val numberWords = textToNumberMap.keys.joinToString("|")

            val durationPattern =
                Regex(
                    """(через|за)\s*(\d+|$numberWords)\s*(хвилину|хвилин|годину|годин|день|днів|тиждень|тижні|місяць|місяці|рік|років|хв|год|дн)\b""",
                )

            val patterns =
                listOf(
                    durationPattern,
                    Regex("""(?:о|в)\s*(\d{1,2})(?:[:.]\s*(\d{2}))?"""),
                    Regex("""(сьогодні|завтра|післязавтра)(?:\s*о?\s*(\d{1,2})(?:[:.]\s*(\d{2}))?)?"""),
                )

            for (pattern in patterns) {
                val match = pattern.find(cleanText)
                if (match != null) {
                    Log.d(TAG, "[ReminderParser] Found match: '${match.value}' at range ${match.range} in text '$cleanText'")

                    val calendar = Calendar.getInstance()
                    var success = false
                    val suggestionText = match.value

                    val isDuration = pattern == durationPattern
                    if (isDuration) {
                        val numberString = match.groups[2]?.value ?: continue
                        val number = textToNumberMap[numberString] ?: numberString.toIntOrNull() ?: continue
                        val unit = match.groups[3]?.value ?: continue

                        Log.d(
                            TAG,
                            "[ReminderParser] Fallback found: number=$number, unit='$unit', match='${match.value}' at ${match.range}",
                        )

                        success =
                            when {
                                unit.startsWith("хв") -> {
                                    calendar.add(Calendar.MINUTE, number)
                                    true
                                }
                                unit.startsWith("год") -> {
                                    calendar.add(Calendar.HOUR_OF_DAY, number)
                                    true
                                }
                                unit.startsWith("дн") || unit.startsWith("день") -> {
                                    calendar.add(Calendar.DAY_OF_YEAR, number)
                                    true
                                }
                                unit.startsWith("тижн") -> {
                                    calendar.add(Calendar.WEEK_OF_YEAR, number)
                                    true
                                }
                                unit.startsWith("місяць") -> {
                                    calendar.add(Calendar.MONTH, number)
                                    true
                                }
                                unit.startsWith("рок") -> {
                                    calendar.add(Calendar.YEAR, number)
                                    true
                                }
                                else -> {
                                    Log.w(TAG, "[ReminderParser] Unknown time unit: '$unit'")
                                    false
                                }
                            }
                    } else {
                        when {
                            match.groups[1]?.value?.toIntOrNull() != null -> {
                                val hour = match.groups[1]?.value?.toInt() ?: continue
                                val minute = match.groups[2]?.value?.toIntOrNull() ?: 0
                                if (hour in 0..23 && minute in 0..59) {
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    calendar.set(Calendar.SECOND, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)

                                    val now = Calendar.getInstance()
                                    if (calendar.timeInMillis < now.timeInMillis) {
                                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                                    }
                                    success = true
                                }
                            }
                            else -> {
                                val dateWord = match.groups[1]?.value
                                when (dateWord) {
                                    "сьогодні" -> {
                                        val hour = match.groups[2]?.value?.toIntOrNull()
                                        val minute = match.groups[3]?.value?.toIntOrNull() ?: 0
                                        if (hour != null) {
                                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                                            calendar.set(Calendar.MINUTE, minute)
                                        } else {
                                            calendar.set(Calendar.HOUR_OF_DAY, 9)
                                            calendar.set(Calendar.MINUTE, 0)
                                        }
                                        calendar.set(Calendar.SECOND, 0)
                                        calendar.set(Calendar.MILLISECOND, 0)
                                        success = true
                                    }
                                    "завтра" -> {
                                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                                        val hour = match.groups[2]?.value?.toIntOrNull() ?: 9
                                        val minute = match.groups[3]?.value?.toIntOrNull() ?: 0
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        calendar.set(Calendar.SECOND, 0)
                                        calendar.set(Calendar.MILLISECOND, 0)
                                        success = true
                                    }
                                    "післязавтра" -> {
                                        calendar.add(Calendar.DAY_OF_YEAR, 2)
                                        val hour = match.groups[2]?.value?.toIntOrNull() ?: 9
                                        val minute = match.groups[3]?.value?.toIntOrNull() ?: 0
                                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                                        calendar.set(Calendar.MINUTE, minute)
                                        calendar.set(Calendar.SECOND, 0)
                                        calendar.set(Calendar.MILLISECOND, 0)
                                        success = true
                                    }
                                }
                            }
                        }
                    }

                    if (success) {
                        Log.d(TAG, "[ReminderParser] Fallback successful with pattern: ${pattern.pattern}")

                        val matchStart = match.range.first
                        val matchEnd = match.range.last + 1

                        val beforeMatch = cleanText.substring(0, matchStart).trim()
                        val afterMatch = cleanText.substring(matchEnd).trim()
                        val goalText = (beforeMatch + " " + afterMatch).trim()

                        Log.d(TAG, "[ReminderParser] Goal text extracted: '$goalText' (before: '$beforeMatch', after: '$afterMatch')")

                        return ReminderParseResult(
                            originalText = text,
                            dateTimeEntities =
                                listOf(
                                    DateTimeEntity(
                                        text = match.value,
                                        label = if (isDuration) "DURATION" else "TIME",
                                        start = matchStart,
                                        end = matchEnd,
                                        confidence = 0.8f,
                                    ),
                                ),
                            otherEntities =
                                if (goalText.isNotEmpty()) {
                                    listOf(
                                        Entity(
                                            text = goalText,
                                            label = "GOAL",
                                            start = 0,
                                            end = goalText.length,
                                        ),
                                    )
                                } else {
                                    emptyList()
                                },
                            success = true,
                            calendar = calendar,
                            suggestionText = suggestionText,
                            errorMessage = null,
                        )
                    }
                }
            }

            Log.d(TAG, "[ReminderParser] No fallback patterns matched")
            return null
        }
    }

data class ReminderParseResult(
    val originalText: String,
    val dateTimeEntities: List<DateTimeEntity>,
    val otherEntities: List<Entity>,
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

@Composable
fun ReminderInput(reminderParser: ReminderParser) {
    var text by remember { mutableStateOf("") }
    var parseResult by remember { mutableStateOf<ReminderParseResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column {
        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Введіть нагадування") },
        )

        Button(
            onClick = {
                if (text.isNotBlank()) {
                    isLoading = true
                    reminderParser.parseAsync(text) { result ->
                        parseResult = result
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && text.isNotBlank(),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Парсити")
            }
        }

        parseResult?.let { result ->
            if (result.success) {
                Text("Знайдено ${result.dateTimeEntities.size} дат/часів")
                result.dateTimeEntities.forEach { entity ->
                    Text("${entity.label}: ${entity.text}")
                }
                result.calendar?.let { calendar ->
                    Text("Час нагадування: ${calendar.time}")
                }
                result.suggestionText?.let { suggestion ->
                    Text("Виявлено: $suggestion")
                }
            } else {
                Text("Помилка: ${result.errorMessage ?: "Нічого не знайдено"}")
            }
        }
    }
}

class ReminderRepository
    @Inject
    constructor(
        private val reminderParser: ReminderParser,
    ) {
        fun parseReminderWithCallback(
            text: String,
            callback: (ReminderParseResult) -> Unit,
        ) {
            reminderParser.parseAsync(text, callback)
        }

        suspend fun parseReminder(text: String): ReminderParseResult = reminderParser.parseAsync(text)

        suspend fun parseReminderSafe(text: String): ReminderParseResult = reminderParser.parseWithTimeout(text, 15000L)
    }

class ReminderViewModel
    @Inject
    constructor(
        val reminderParser: ReminderParser,
    ) : ViewModel() {
        private val _parseResult = MutableStateFlow<ReminderParseResult?>(null)
        val parseResult = _parseResult.asStateFlow()

        private val _isLoading = MutableStateFlow(false)
        val isLoading = _isLoading.asStateFlow()

        fun parseText(text: String) {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    val result = reminderParser.parseWithTimeout(text)
                    _parseResult.value = result
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
