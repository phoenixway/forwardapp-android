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
import javax.inject.Inject

class InputHandler @Inject constructor(
    private val goalRepository: GoalRepository,
    private val scope: CoroutineScope,
    private val listIdFlow: StateFlow<String>,
    private val resultListener: ResultListener
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
            // Парсимо текст, щоб знайти потенційне нагадування
            val result = ReminderParser.parse(newValue.text)
            // Оновлюємо стан, але НЕ ЗМІНЮЄМО текст у полі вводу.
            // Користувач бачить те, що ввів, а під полем з'явиться чіп.
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

                // Перед збереженням очищуємо текст від згадки про час
                val parseResult = ReminderParser.parse(originalText)
                var textToSave = originalText
                if (parseResult.calendar != null && parseResult.suggestionText != null) {
                    // Видаляємо розпізнану частину тексту
                    textToSave = textToSave.replace(parseResult.suggestionText, "", ignoreCase = true).trim()

                    // Додатково видаляємо можливі залишки, як-от "о", "в"
                    val timeRegex = "(\\sо\\s|\\sв\\s)".toRegex(RegexOption.IGNORE_CASE)
                    textToSave = textToSave.replace(timeRegex, " ").trim()

                    // Якщо після очищення нічого не залишилось, використовуємо назву за замовчуванням
                    if (textToSave.isBlank()) {
                        textToSave = "Ціль з нагадуванням"
                    }
                }

                scope.launch(Dispatchers.IO) {
                    val reminderTime = detectedCalendar?.timeInMillis
                    val newGoalId: String
                    if (reminderTime != null) {
                        newGoalId = goalRepository.addGoalWithReminder(textToSave, currentListId, reminderTime)
                        resultListener.onGoalCreatedWithReminder(newGoalId)
                    } else {
                        // Викликаємо стару функцію, якщо нагадування не розпізнано.
                        val listItemId = goalRepository.addGoalToList(textToSave, currentListId)
                        newGoalId = listItemId // Тут може бути не той ID, але це обмеження старої функції
                    }

                    withContext(Dispatchers.Main) {
                        resultListener.updateInputState(
                            inputValue = TextFieldValue(""),
                            newlyAddedItemId = newGoalId,
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