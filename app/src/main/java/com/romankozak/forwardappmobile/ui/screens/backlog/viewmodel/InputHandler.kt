package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.domain.ReminderParser
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType
import com.romankozak.forwardappmobile.ui.screens.backlog.types.InputMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar

// Клас більше не Singleton, а звичайний клас, що створюється у ViewModel
class InputHandler(
    private val goalRepository: GoalRepository,
    private val scope: CoroutineScope,
    private val listIdFlow: StateFlow<String>,
    private val resultListener: ResultListener,
    private val reminderParser: ReminderParser // <-- ЗАЛЕЖНІСТЬ
) {
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
        if (currentInputMode == InputMode.AddGoal) {
            // Використовуємо впроваджений гібридний парсер
            val result = reminderParser.parse(newValue.text)
            resultListener.updateInputState(
                inputValue = newValue,
                detectedReminderSuggestion = result.suggestionText,
                detectedReminderCalendar = result.calendar
            )
        } else if (currentInputMode == InputMode.SearchInList) {
            resultListener.updateInputState(inputValue = newValue, localSearchQuery = newValue.text)
        } else {
            resultListener.updateInputState(inputValue = newValue)
        }
    }

    fun onInputModeSelected(mode: InputMode, currentInputValue: TextFieldValue) {
        onClearDetectedReminder()
        val searchQuery = if (mode == InputMode.SearchInList) currentInputValue.text else ""
        resultListener.updateInputState(inputMode = mode, localSearchQuery = searchQuery)
    }

    fun submitInput(inputValue: TextFieldValue, inputMode: InputMode, detectedCalendar: Calendar?) {
        val originalText = inputValue.text.trim()
        if (originalText.isBlank()) return

        when (inputMode) {
            InputMode.AddGoal -> {
                val currentListId = listIdFlow.value
                if (currentListId.isBlank()) return

                // Повторно парсимо текст, щоб отримати suggestionText для очищення
                val parseResult = reminderParser.parse(originalText)
                var textToSave = originalText

                if (parseResult.calendar != null && parseResult.suggestionText != null) {
                    textToSave = originalText.replace(parseResult.suggestionText, "", ignoreCase = true).trim()
                    if (textToSave.isBlank()) {
                        textToSave = "Ціль з нагадуванням"
                    }
                }

                scope.launch(Dispatchers.IO) {
                    val reminderTime = detectedCalendar?.timeInMillis
                    val newItemIdentifier: String
                    if (reminderTime != null) {
                        newItemIdentifier = goalRepository.addGoalWithReminder(textToSave, currentListId, reminderTime)
                        resultListener.onGoalCreatedWithReminder(newItemIdentifier)
                    } else {
                        newItemIdentifier = goalRepository.addGoalToList(textToSave, currentListId)
                    }

                    withContext(Dispatchers.Main) {
                        resultListener.updateInputState(
                            inputValue = TextFieldValue(""),
                            newlyAddedItemId = newItemIdentifier,
                            clearDetectedReminder = true
                        )
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

    fun onClearDetectedReminder() {
        resultListener.updateInputState(clearDetectedReminder = true)
    }

    // ... решта методів без змін ...
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