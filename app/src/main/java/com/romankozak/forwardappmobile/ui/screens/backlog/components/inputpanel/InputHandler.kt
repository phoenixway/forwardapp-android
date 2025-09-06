package com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ner.ReminderParser
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType
import com.romankozak.forwardappmobile.ui.screens.backlog.types.InputMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar

// Improved debouncer with better cancellation handling
class SmartDebouncer(private val delayMs: Long) {
    private var job: Job? = null
    private var lastInputTime = 0L

    fun debounce(coroutineScope: CoroutineScope, block: suspend () -> Unit) {
        val currentTime = System.currentTimeMillis()
        lastInputTime = currentTime

        job?.cancel()
        job = coroutineScope.launch {
            delay(delayMs)
            // Only proceed if this is still the latest input
            if (lastInputTime <= currentTime + 50) { // 50ms tolerance
                try {
                    block()
                } catch (e: Exception) {
                    if (e !is kotlinx.coroutines.CancellationException) {
                        Log.e("SmartDebouncer", "Execution error", e)
                    }
                }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

class InputHandler(
    private val goalRepository: GoalRepository,
    private val scope: CoroutineScope,
    private val listIdFlow: StateFlow<String>,
    private val resultListener: ResultListener,
    private val reminderParser: ReminderParser
) {
    private val TAG = "NER_DEBUG"
    private val smartDebouncer = SmartDebouncer(800L) // Increased delay to allow NER to complete

    interface ResultListener {
        fun updateInputState(
            inputValue: TextFieldValue? = null,
            inputMode: InputMode? = null,
            localSearchQuery: String? = null,
            newlyAddedItemId: String? = null,
            detectedReminderSuggestion: String? = null,
            detectedReminderCalendar: Calendar? = null,
            clearDetectedReminder: Boolean = false
        )
        fun updateDialogState(showAddWebLinkDialog: Boolean? = null, showAddObsidianLinkDialog: Boolean? = null)
        fun showRecentListsSheet(show: Boolean)
        fun setPendingAction(actionType: GoalActionType, itemIds: Set<String> = emptySet(), goalIds: Set<String> = emptySet())
        fun requestNavigation(route: String)
        fun forceRefresh()
        fun addQuickRecord(text: String)
        fun onGoalCreatedWithReminder(goalId: String)
    }

    fun onInputTextChanged(newValue: TextFieldValue, currentInputMode: InputMode) {
        Log.d(TAG, "[InputHandler] onInputTextChanged. Mode: $currentInputMode, Text: '${newValue.text}'")

        when (currentInputMode) {
            InputMode.AddGoal -> {
                resultListener.updateInputState(inputValue = newValue)
                if (newValue.text.trim().isNotEmpty()) {
                    smartDebouncer.debounce(scope) {
                        Log.d(TAG, "[InputHandler] Starting parsing for: '${newValue.text}'")
                        parseReminderWithTimeout(newValue.text.trim())
                    }
                } else {
                    smartDebouncer.cancel()
                    resultListener.updateInputState(clearDetectedReminder = true)
                }
            }
            InputMode.SearchInList -> {
                Log.d(TAG, "[InputHandler] Mode is SearchInList. Updating search query.")
                resultListener.updateInputState(
                    inputValue = newValue,
                    localSearchQuery = newValue.text
                )
            }
            else -> {
                Log.d(TAG, "[InputHandler] Mode is OTHER. Just updating input value.")
                resultListener.updateInputState(inputValue = newValue)
            }
        }
    }

    private suspend fun parseReminderWithTimeout(text: String) {
        try {
            Log.d(TAG, "[InputHandler] Parsing reminder: '$text'")
            val result = reminderParser.parseWithTimeout(text, 10000L) // 10 second timeout

            withContext(Dispatchers.Main) {
                if (result.success) {
                    Log.d(TAG, "[InputHandler] Parse SUCCESS: suggestion=${result.suggestionText}")
                    resultListener.updateInputState(
                        detectedReminderSuggestion = result.suggestionText,
                        detectedReminderCalendar = result.calendar
                    )
                } else {
                    Log.d(TAG, "[InputHandler] Parse FAILED: ${result.errorMessage}")
                    // Try simple fallback parsing
                    val fallbackResult = tryFallbackParsing(text)
                    if (fallbackResult != null) {
                        Log.d(TAG, "[InputHandler] Fallback parsing SUCCESS")
                        resultListener.updateInputState(
                            detectedReminderSuggestion = fallbackResult.suggestionText,
                            detectedReminderCalendar = fallbackResult.calendar
                        )
                    } else {
                        resultListener.updateInputState(clearDetectedReminder = true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[InputHandler] Parse error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                // Try fallback parsing even on exception
                val fallbackResult = tryFallbackParsing(text)
                if (fallbackResult != null) {
                    Log.d(TAG, "[InputHandler] Fallback parsing after error SUCCESS")
                    resultListener.updateInputState(
                        detectedReminderSuggestion = fallbackResult.suggestionText,
                        detectedReminderCalendar = fallbackResult.calendar
                    )
                } else {
                    resultListener.updateInputState(clearDetectedReminder = true)
                }
            }
        }
    }

    private fun tryFallbackParsing(text: String): FallbackResult? {
        val cleanText = text.lowercase().trim()

        // Pattern for "через X хв/год/днів"
        val durationPattern = Regex("""через\s*(\d+)\s*(хв|хвилин|год|годин|дні|день|тижн|місяць)""")
        val match = durationPattern.find(cleanText) ?: return null

        val number = match.groups[1]?.value?.toIntOrNull() ?: return null
        val unit = match.groups[2]?.value ?: return null
        val calendar = Calendar.getInstance()

        val success = when (unit) {
            "хв", "хвилин" -> {
                calendar.add(Calendar.MINUTE, number)
                true
            }
            "год", "годин" -> {
                calendar.add(Calendar.HOUR_OF_DAY, number)
                true
            }
            "дні", "день" -> {
                calendar.add(Calendar.DAY_OF_YEAR, number)
                true
            }
            "тижн" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, number)
                true
            }
            "місяць" -> {
                calendar.add(Calendar.MONTH, number)
                true
            }
            else -> false
        }

        return if (success) {
            FallbackResult(match.value, calendar)
        } else null
    }

    private data class FallbackResult(
        val suggestionText: String,
        val calendar: Calendar
    )

    fun onInputModeSelected(mode: InputMode, currentInputValue: TextFieldValue) {
        // Cancel any ongoing parsing when mode changes
        smartDebouncer.cancel()
        onClearDetectedReminder()

        val searchQuery = if (mode == InputMode.SearchInList) currentInputValue.text else ""
        resultListener.updateInputState(inputMode = mode, localSearchQuery = searchQuery)
    }

    fun submitInput(inputValue: TextFieldValue, inputMode: InputMode, detectedCalendar: Calendar?) {
        val originalText = inputValue.text.trim()
        if (originalText.isBlank()) return

        // Cancel parsing on submit
        smartDebouncer.cancel()

        when (inputMode) {
            InputMode.AddGoal -> {
                val currentListId = listIdFlow.value
                if (currentListId.isBlank()) return

                scope.launch(Dispatchers.IO) {
                    try {
                        var textToSave = originalText
                        var reminderTime: Long? = null

                        if (detectedCalendar != null) {
                            reminderTime = detectedCalendar.timeInMillis

                            try {
                                val parseResult = reminderParser.parseWithTimeout(originalText, 3000L)
                                parseResult.suggestionText?.let { suggestion ->
                                    val cleanedText = originalText.replace(suggestion, "", ignoreCase = true).trim()
                                    if (cleanedText.isNotBlank()) {
                                        textToSave = cleanedText
                                    }
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Could not clean text, using original", e)
                                val fallbackCleaned = cleanTextWithFallback(originalText)
                                if (fallbackCleaned.isNotBlank()) {
                                    textToSave = fallbackCleaned
                                }
                            }
                        }

                        // Створюємо ціль (з ремайндером або без)
                        val newItemIdentifier: String = if (reminderTime != null) {
                            goalRepository.addGoalWithReminder(textToSave, currentListId, reminderTime)
                        } else {
                            goalRepository.addGoalToList(textToSave, currentListId)
                        }

                        // Якщо є ремайндер, налаштовуємо алярм
                        if (reminderTime != null) {
                            goalRepository.getGoalById(newItemIdentifier)?.let { newGoal ->
                                // Використовуємо алярм напряму, не через resultListener
                                try {
                                    // Припускаємо, що у вас є доступ до alarmScheduler
                                    // Якщо ні, то можна додати це в ResultListener
                                    resultListener.onGoalCreatedWithReminder(newItemIdentifier)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to schedule alarm", e)
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            resultListener.updateInputState(
                                inputValue = TextFieldValue(""),
                                newlyAddedItemId = newItemIdentifier, // Це має працювати для автоскролу
                                clearDetectedReminder = true
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[InputHandler] Submit error: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            resultListener.updateInputState(
                                inputValue = TextFieldValue(""),
                                clearDetectedReminder = true
                            )
                        }
                    }
                }
            }


            InputMode.AddQuickRecord -> resultListener.addQuickRecord(originalText)
            InputMode.SearchGlobal -> {
                resultListener.requestNavigation("global_search_screen/${URLEncoder.encode(originalText, "UTF-8")}")
                resultListener.updateInputState(inputValue = TextFieldValue(""))
            }
            InputMode.SearchInList -> { /* Live search, no action */ }
        }
    }

    private fun cleanTextWithFallback(originalText: String): String {
        val durationPattern = Regex("""через\s*\d+\s*(хв|хвилин|год|годин|дні|день|тижн|місяць)""")
        return originalText.replace(durationPattern, "").trim()
    }

    fun onClearDetectedReminder() {
        smartDebouncer.cancel()
        resultListener.updateInputState(clearDetectedReminder = true)
    }

    fun cleanup() {
        smartDebouncer.cancel()
    }

    // ... rest of methods without changes ...
    fun onAddWebLinkConfirm(url: String?, name: String?) {
        if (url.isNullOrBlank()) {
            onDismissLinkDialogs()
            return
        }
        scope.launch(Dispatchers.IO) {
            val displayName = if (name.isNullOrBlank()) {
                try { URL(url).host } catch (_: Exception) { url }
            } else { name }
            val link = RelatedLink(type = LinkType.URL, target = url, displayName = displayName)
            val newItemId = goalRepository.addLinkItemToList(listIdFlow.value, link)
            resultListener.updateInputState(newlyAddedItemId = newItemId)
        }
        onDismissLinkDialogs()
    }

    fun onAddObsidianLinkConfirm(noteName: String?) {
        if (noteName.isNullOrBlank()) {
            onDismissLinkDialogs()
            return
        }
        scope.launch(Dispatchers.IO) {
            val link = RelatedLink(type = LinkType.OBSIDIAN, target = noteName, displayName = noteName)
            val newItemId = goalRepository.addLinkItemToList(listIdFlow.value, link)
            resultListener.updateInputState(newlyAddedItemId = newItemId)
        }
        onDismissLinkDialogs()
    }

    fun onShowAddWebLinkDialog() = resultListener.updateDialogState(showAddWebLinkDialog = true)
    fun onShowAddObsidianLinkDialog() = resultListener.updateDialogState(showAddObsidianLinkDialog = true)
    fun onDismissLinkDialogs() = resultListener.updateDialogState(showAddWebLinkDialog = false, showAddObsidianLinkDialog = false)
    fun onAddListLinkRequest() = resultListener.setPendingAction(GoalActionType.AddLinkToList)
    fun onAddListShortcutRequest() = resultListener.setPendingAction(GoalActionType.ADD_LIST_SHORTCUT)
    fun onShowRecentLists() = resultListener.showRecentListsSheet(true)
    fun onDismissRecentLists() = resultListener.showRecentListsSheet(false)
    fun onRecentListSelected(listId: String) {
        resultListener.requestNavigation("goal_detail_screen/$listId")
        onDismissRecentLists()
    }
}