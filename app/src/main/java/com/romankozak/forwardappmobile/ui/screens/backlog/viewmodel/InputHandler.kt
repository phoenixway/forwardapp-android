package com.romankozak.forwardappmobile.ui.screens.backlog.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.Note
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.OllamaService
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType
import com.romankozak.forwardappmobile.ui.screens.backlog.types.InputMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.UUID
import javax.inject.Inject

class InputHandler @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val ollamaService: OllamaService,
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

        val currentListId = listIdFlow.value
        if (currentListId.isBlank()) return

        scope.launch(Dispatchers.IO) {
            val newItemId: String? = when (inputMode) {
                InputMode.AddGoal -> goalRepository.addGoalToList(textToSubmit, currentListId)
                InputMode.AddNote -> handleNoteSubmission(textToSubmit, currentListId)
                InputMode.SearchInList -> null
                InputMode.SearchGlobal -> {
                    resultListener.requestNavigation("global_search_screen/$textToSubmit")
                    null
                }
            }

            withContext(Dispatchers.Main) {
                resultListener.updateInputState(
                    inputValue = TextFieldValue(""),
                    newlyAddedItemId = newItemId
                )
                if (inputMode == InputMode.AddGoal || (inputMode == InputMode.AddNote && textToSubmit.length <= 60)) {
                    resultListener.forceRefresh()
                }
            }
        }
    }

    private suspend fun handleNoteSubmission(text: String, listId: String): String {
        val words = text.split(Regex("\\s+"))
        val isTooLongForTitle = text.length > 60 || words.size > 5

        return if (isTooLongForTitle) {
            val initialNote = Note(
                id = UUID.randomUUID().toString(),
                title = "Генерація заголовку...",
                content = text,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val generatedItemId = goalRepository.addNoteToList(initialNote, listId)

            withContext(Dispatchers.Main) { resultListener.forceRefresh() }

            scope.launch { // Launch a concurrent job for title generation
                val baseUrl = settingsRepository.ollamaUrlFlow.first()
                val fastModel = settingsRepository.ollamaFastModelFlow.first()
                val finalTitle = if (baseUrl.isNotBlank() && fastModel.isNotBlank()) {
                    ollamaService.generateTitle(baseUrl, fastModel, text)
                        .getOrElse { text.split(Regex("\\s+")).take(5).joinToString(" ") + "..." }
                } else {
                    text.split(Regex("\\s+")).take(5).joinToString(" ") + "..."
                }
                val updatedNote = initialNote.copy(title = finalTitle, updatedAt = System.currentTimeMillis())
                goalRepository.updateNote(updatedNote)
            }
            generatedItemId
        } else {
            val newNote = Note(
                id = UUID.randomUUID().toString(),
                title = text,
                content = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            goalRepository.addNoteToList(newNote, listId)
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

    fun onShowRecentLists() = resultListener.showRecentListsSheet(true)
    fun onDismissRecentLists() = resultListener.showRecentListsSheet(false)
    fun onRecentListSelected(listId: String) {
        resultListener.requestNavigation("goal_detail_screen/$listId")
        onDismissRecentLists()
    }
}