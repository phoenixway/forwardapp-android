package com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ner.ReminderParser
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.ner.ReminderParseResult
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

class SmartDebouncer(private val delayMs: Long) {
    private var job: Job? = null
    private var lastInputTime = 0L

    fun debounce(coroutineScope: CoroutineScope, block: suspend () -> Unit): Job {
        val currentTime = System.currentTimeMillis()
        lastInputTime = currentTime

        job?.cancel()
        job = coroutineScope.launch {
            delay(delayMs)
            if (lastInputTime <= currentTime + 50) {
                try {
                    block()
                } catch (e: Exception) {
                    if (e !is kotlinx.coroutines.CancellationException) {
                        Log.e("SmartDebouncer", "Execution error", e)
                    }
                }
            }
        }
        return job!!
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
    private val smartDebouncer = SmartDebouncer(800L)
    private var nerJob: Job? = null

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
        resultListener.updateInputState(inputValue = newValue)
        if (currentInputMode == InputMode.AddGoal) {
            if (newValue.text.trim().isNotEmpty()) {
                nerJob = smartDebouncer.debounce(scope) {
                    parseReminderForSuggestion(newValue.text.trim())
                }
            } else {
                clearAndCancelParsing()
            }
        } else if (currentInputMode == InputMode.SearchInList) {
            resultListener.updateInputState(localSearchQuery = newValue.text)
        }
    }

    // This function is now only for the live UI suggestion.
    private suspend fun parseReminderForSuggestion(text: String) {
        try {
            val result = reminderParser.parseWithTimeout(text, 10000L)
            withContext(Dispatchers.Main) {
                if (result.success) {
                    resultListener.updateInputState(
                        detectedReminderSuggestion = result.suggestionText,
                        detectedReminderCalendar = result.calendar
                    )
                } else {
                    resultListener.updateInputState(clearDetectedReminder = true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[InputHandler] Suggestion parse error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                resultListener.updateInputState(clearDetectedReminder = true)
            }
        }
    }

    fun onInputModeSelected(mode: InputMode, currentInputValue: TextFieldValue) {
        clearAndCancelParsing()
        val searchQuery = if (mode == InputMode.SearchInList) currentInputValue.text else ""
        resultListener.updateInputState(inputMode = mode, localSearchQuery = searchQuery)
    }

    // ... (початок файлу InputHandler.kt без змін) ...

    fun submitInput(inputValue: TextFieldValue, inputMode: InputMode) {
        val originalText = inputValue.text.trim()
        if (originalText.isBlank()) return

        // Скасовуємо будь-який фоновий аналіз для підказок
        clearAndCancelParsing()

        when (inputMode) {
            InputMode.AddGoal -> {
                val currentListId = listIdFlow.value
                if (currentListId.isBlank()) return

                // --- НОВА ЛОГІКА ---
                // 1. Надаємо миттєвий зворотний зв'язок: очищуємо поле введення.
                // UI більше не чекає на завершення аналізу.
                resultListener.updateInputState(
                    inputValue = TextFieldValue(""),
                    clearDetectedReminder = true
                )

                // 2. Запускаємо "важку" роботу (аналіз та збереження) у фоновому потоці.
                scope.launch(Dispatchers.IO) {
                    try {
                        // Запускаємо аналіз тексту безпосередньо для результату.
                        val definitiveResult = reminderParser.parseWithTimeout(originalText, 10000L)
                        Log.d(TAG, "[InputHandler] Parser for submit finished. Success: ${definitiveResult.success}")

                        var textToSave = originalText
                        var reminderTime: Long? = null

                        if (definitiveResult.success) {
                            val detectedCalendar = definitiveResult.calendar
                            val detectedSuggestion = definitiveResult.suggestionText

                            if (detectedCalendar != null && !detectedSuggestion.isNullOrBlank()) {
                                reminderTime = detectedCalendar.timeInMillis
                                val cleanedText = originalText.replace(detectedSuggestion, "", ignoreCase = true).trim()
                                textToSave = if (cleanedText.isNotBlank()) cleanedText else originalText
                            }
                        }

                        // Зберігаємо ціль в базу даних.
                        val newItemIdentifier: String = if (reminderTime != null) {
                            goalRepository.addGoalWithReminder(textToSave, currentListId, reminderTime)
                        } else {
                            goalRepository.addGoalToList(textToSave, currentListId)
                        }

                        if (reminderTime != null) {
                            resultListener.onGoalCreatedWithReminder(newItemIdentifier)
                        }

                        // 3. Після завершення фонової роботи, оновлюємо UI,
                        // щоб список прокрутився до нової цілі.
                        withContext(Dispatchers.Main) {
                            resultListener.updateInputState(
                                newlyAddedItemId = newItemIdentifier
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[InputHandler] Submit error: ${e.message}", e)
                        // У випадку помилки можна показати сповіщення користувачу.
                        // Наразі поле вже очищено, тому додаткових дій не потрібно.
                    }
                }
            }
            InputMode.AddQuickRecord -> {
                // Для швидких записів поведінка може бути аналогічною
                resultListener.updateInputState(inputValue = TextFieldValue(""))
                resultListener.addQuickRecord(originalText)
            }
            InputMode.SearchGlobal -> {
                resultListener.requestNavigation("global_search_screen/${URLEncoder.encode(originalText, "UTF-8")}")
                resultListener.updateInputState(inputValue = TextFieldValue(""))
            }
            InputMode.SearchInList -> { /* Live search, no action */ }
        }
    }

// ... (решта файлу InputHandler.kt без змін) ...

    fun onClearDetectedReminder() {
        clearAndCancelParsing()
    }

    private fun clearAndCancelParsing() {
        nerJob?.cancel()
        smartDebouncer.cancel()
        resultListener.updateInputState(clearDetectedReminder = true)
    }

    fun cleanup() {
        clearAndCancelParsing()
    }

    // ... (rest of the file is unchanged)
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