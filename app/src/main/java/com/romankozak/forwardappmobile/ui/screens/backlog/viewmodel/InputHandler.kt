package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType
import com.romankozak.forwardappmobile.ui.screens.backlog.types.InputMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject

class InputHandler @Inject constructor(
    private val goalRepository: GoalRepository,
    // Ми більше не використовуємо SettingsRepository та OllamaService тут
    // private val settingsRepository: SettingsRepository,
    // private val ollamaService: OllamaService,
    private val scope: CoroutineScope,
    private val listIdFlow: StateFlow<String>,
    private val resultListener: ResultListener
) {
    interface ResultListener {
        fun updateInputState(
            inputValue: TextFieldValue? = null,
            inputMode: InputMode? = null,
            localSearchQuery: String? = null,
            newlyAddedItemId: String? = null
        )

        fun updateDialogState(
            showAddWebLinkDialog: Boolean? = null,
            showAddObsidianLinkDialog: Boolean? = null
        )

        fun showRecentListsSheet(show: Boolean)
        fun setPendingAction(actionType: GoalActionType, itemIds: Set<String> = emptySet(), goalIds: Set<String> = emptySet())
        fun requestNavigation(route: String)
        fun forceRefresh()
        // Ця нова функція буде викликатись для додавання запису в інбокс
        fun addQuickRecord(text: String)
    }

    fun onInputTextChanged(newValue: TextFieldValue, currentInputMode: InputMode) {
        resultListener.updateInputState(inputValue = newValue)
        if (currentInputMode == InputMode.SearchInList) {
            resultListener.updateInputState(localSearchQuery = newValue.text)
        }
    }

    fun onInputModeSelected(mode: InputMode, currentInputValue: TextFieldValue) {
        val searchQuery = if (mode == InputMode.SearchInList) currentInputValue.text else ""
        resultListener.updateInputState(inputMode = mode, localSearchQuery = searchQuery)
    }

    fun submitInput(inputValue: TextFieldValue, inputMode: InputMode) {
        val textToSubmit = inputValue.text.trim()
        if (textToSubmit.isBlank()) return

        // Використовуємо один when для обробки всіх можливих режимів
        when (inputMode) {
            InputMode.AddGoal -> {
                val currentListId = listIdFlow.value
                if (currentListId.isBlank()) return

                scope.launch(Dispatchers.IO) {
                    val newItemId = goalRepository.addGoalToList(textToSubmit, currentListId)
                    // Оновлюємо UI на головному потоці
                    withContext(Dispatchers.Main) {
                        resultListener.updateInputState(
                            inputValue = TextFieldValue(""),
                            newlyAddedItemId = newItemId
                        )
                        resultListener.forceRefresh()
                    }
                }
            }

            InputMode.AddQuickRecord -> {
                // Просто викликаємо метод слухача.
                // ViewModel сама подбає про запуск coroutine та очищення поля вводу.
                resultListener.addQuickRecord(textToSubmit)
            }

            InputMode.SearchGlobal -> {
                resultListener.requestNavigation("global_search_screen/$textToSubmit")
                // Очищуємо поле вводу після пошуку для кращого UX
                resultListener.updateInputState(inputValue = TextFieldValue(""))
            }

            InputMode.SearchInList -> {
                // Для локального пошуку кнопка "Надіслати" неактивна,
                // але цей випадок робить 'when' вичерпним. Дій не потрібно.
            }
        }
    }

    fun onAddWebLinkConfirm(url: String, name: String?) {
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

    fun onAddObsidianLinkConfirm(noteName: String) {
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

    fun onShowRecentLists() {
        Log.d("RECENT_DEBUG", "onShowRecentLists activated")
        resultListener.showRecentListsSheet(true)
    }
    fun onDismissRecentLists() = resultListener.showRecentListsSheet(false)
    fun onRecentListSelected(listId: String) {
        resultListener.requestNavigation("goal_detail_screen/$listId")
        onDismissRecentLists()
    }
}