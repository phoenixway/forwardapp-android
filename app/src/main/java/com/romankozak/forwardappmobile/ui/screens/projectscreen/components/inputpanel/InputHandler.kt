package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.inputpanel

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.shared.data.database.models.LinkType
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.ui.screens.projectscreen.GoalActionType
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

class SmartDebouncer(
    private val delayMs: Long,
) {
    private var job: Job? = null
    private var lastInputTime = 0L

    fun debounce(
        coroutineScope: CoroutineScope,
        block: suspend () -> Unit,
    ): Job {
        val currentTime = System.currentTimeMillis()
        lastInputTime = currentTime

        job?.cancel()
        job =
            coroutineScope.launch {
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
    private val projectRepository: ProjectRepositoryCore,
    private val goalRepository: com.romankozak.forwardappmobile.data.repository.GoalRepository,
    private val listItemRepository: com.romankozak.forwardappmobile.data.repository.ListItemRepository,
    private val scope: CoroutineScope,
    private val projectIdFlow: StateFlow<String>,
    private val resultListener: ResultListener,
    private val reminderParser: ReminderParser,
    private val alarmScheduler: AlarmScheduler,
) {
    private val TAG = "ReminderFlow"
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
            clearDetectedReminder: Boolean = false,
        )

        fun updateDialogState(
            showAddWebLinkDialog: Boolean? = null,
            showAddObsidianLinkDialog: Boolean? = null,
        )

        fun showRecentListsSheet(show: Boolean)

        fun setPendingAction(
            actionType: GoalActionType,
            itemIds: Set<String> = emptySet(),
            goalIds: Set<String> = emptySet(),
        )

        fun requestNavigation(route: String)

        fun forceRefresh()

        fun addQuickRecord(text: String)

        fun addProjectComment(text: String)

        fun addMilestone(text: String)

        fun createObsidianNote(noteName: String)
    }

    fun onInputTextChanged(
        newValue: TextFieldValue,
        currentInputMode: InputMode,
    ) {
        resultListener.updateInputState(inputValue = newValue)
        if (currentInputMode == InputMode.AddGoal) {
            if (newValue.text.trim().isNotEmpty()) {
                nerJob =
                    smartDebouncer.debounce(scope) {
                        parseReminderForSuggestion(newValue.text.trim())
                    }
            } else {
                clearAndCancelParsing()
            }
        } else if (currentInputMode == InputMode.SearchInList) {
            resultListener.updateInputState(localSearchQuery = newValue.text)
        }
    }

    private suspend fun parseReminderForSuggestion(text: String) {
        try {
            val result = reminderParser.parseWithTimeout(text, 10000L)
            withContext(Dispatchers.Main) {
                if (result.success) {
                    resultListener.updateInputState(
                        detectedReminderSuggestion = result.suggestionText,
                        detectedReminderCalendar = result.calendar,
                    )
                } else {
                    resultListener.updateInputState(clearDetectedReminder = true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Suggestion parse error: ${e.message}", e)
            withContext(Dispatchers.Main) {
                resultListener.updateInputState(clearDetectedReminder = true)
            }
        }
    }

    fun onInputModeSelected(
        mode: InputMode,
        currentInputValue: TextFieldValue,
    ) {
        clearAndCancelParsing()
        val searchQuery = if (mode == InputMode.SearchInList) currentInputValue.text else ""
        resultListener.updateInputState(inputMode = mode, localSearchQuery = searchQuery)
    }

    fun submitInput(
        inputValue: TextFieldValue,
        inputMode: InputMode,
    ) {
        val originalText = inputValue.text.trim()
        if (originalText.isBlank()) return

        clearAndCancelParsing()

        when (inputMode) {
            InputMode.AddGoal -> {
                val currentProjectId = projectIdFlow.value
                if (currentProjectId.isBlank()) return

                resultListener.updateInputState(
                    inputValue = TextFieldValue(""),
                    clearDetectedReminder = true,
                )

                scope.launch(Dispatchers.IO) {
                    try {
                        val definitiveResult = reminderParser.parseWithTimeout(originalText, 10000L)
                        Log.d(
                            TAG,
                            "Submit Parser Result: success=${definitiveResult.success}, calendar=${definitiveResult.calendar?.time}, suggestion='${definitiveResult.suggestionText}'",
                        )

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

                        val newItemIdentifier: String
                        if (reminderTime != null) {
                            val newGoal = goalRepository.addGoalWithReminder(textToSave, currentProjectId, reminderTime)
                            newItemIdentifier = newGoal.id
                        } else {
                            newItemIdentifier = goalRepository.addGoalToProject(textToSave, currentProjectId)
                        }

                        withContext(Dispatchers.Main) {
                            resultListener.updateInputState(
                                newlyAddedItemId = newItemIdentifier,
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Submit error: ${e.message}", e)
                    }
                }
            }
            InputMode.AddQuickRecord -> {
                resultListener.updateInputState(inputValue = TextFieldValue(""))
                resultListener.addQuickRecord(originalText)
            }
            InputMode.SearchGlobal -> {
                resultListener.requestNavigation("global_search_screen/${URLEncoder.encode(originalText, "UTF-8")}")
                resultListener.updateInputState(inputValue = TextFieldValue(""))
            }
            InputMode.SearchInList -> { }
            InputMode.AddProjectLog -> {
                resultListener.updateInputState(inputValue = TextFieldValue(""))
                resultListener.addProjectComment(originalText)
            }
            InputMode.AddMilestone -> {
                resultListener.updateInputState(inputValue = TextFieldValue(""))
                resultListener.addMilestone(originalText)
            }
        }
    }

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

    fun onAddWebLinkConfirm(
        url: String?,
        name: String?,
    ) {
        if (url.isNullOrBlank()) {
            onDismissLinkDialogs()
            return
        }
        scope.launch(Dispatchers.IO) {
            val displayName =
                if (name.isNullOrBlank()) {
                    try {
                        URL(url).host
                    } catch (_: Exception) {
                        url
                    }
                } else {
                    name
                }
            val link = RelatedLink(type = LinkType.URL, target = url, displayName = displayName)
            val newItemId = projectRepository.addLinkItemToProjectFromLink(projectIdFlow.value, link)
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
            val newItemId = projectRepository.addLinkItemToProjectFromLink(projectIdFlow.value, link)
            resultListener.updateInputState(newlyAddedItemId = newItemId)
        }
        onDismissLinkDialogs()
    }

    fun onAddObsidianLinkAndCreateNewConfirm(noteName: String) {
        if (noteName.isBlank()) {
            onDismissLinkDialogs()
            return
        }
        resultListener.createObsidianNote(noteName)
        onAddObsidianLinkConfirm(noteName)
    }

    fun onShowAddWebLinkDialog() = resultListener.updateDialogState(showAddWebLinkDialog = true)

    fun onShowAddObsidianLinkDialog() = resultListener.updateDialogState(showAddObsidianLinkDialog = true)

    fun onDismissLinkDialogs() = resultListener.updateDialogState(showAddWebLinkDialog = false, showAddObsidianLinkDialog = false)

    fun onAddListLinkRequest() = resultListener.setPendingAction(GoalActionType.AddLinkToList)

    fun onAddListShortcutRequest() = resultListener.setPendingAction(GoalActionType.ADD_LIST_SHORTCUT)

    fun onShowRecentLists() {
        Log.d("Recents_Debug", "InputHandler: onShowRecentLists() called. Calling listener.")
        resultListener.showRecentListsSheet(true)
    }

    fun onDismissRecentLists() = resultListener.showRecentListsSheet(false)

    fun onRecentListSelected(item: com.romankozak.forwardappmobile.data.database.models.RecentItem) {
        when (item.type) {
            com.romankozak.forwardappmobile.data.database.models.RecentItemType.PROJECT -> {
                resultListener.requestNavigation("goal_detail_screen/${item.target}")
            }
            com.romankozak.forwardappmobile.data.database.models.RecentItemType.NOTE -> {
                resultListener.requestNavigation("note_edit_screen?noteId=${item.target}")
            }
            com.romankozak.forwardappmobile.data.database.models.RecentItemType.NOTE_DOCUMENT -> {
                resultListener.requestNavigation("note_document_screen/${item.target}")
            }
            com.romankozak.forwardappmobile.data.database.models.RecentItemType.CHECKLIST -> {
                resultListener.requestNavigation("checklist_screen?checklistId=${item.target}")
            }
            com.romankozak.forwardappmobile.data.database.models.RecentItemType.OBSIDIAN_LINK -> {
                resultListener.createObsidianNote(item.target)
            }
        }
        onDismissRecentLists()
    }
}
