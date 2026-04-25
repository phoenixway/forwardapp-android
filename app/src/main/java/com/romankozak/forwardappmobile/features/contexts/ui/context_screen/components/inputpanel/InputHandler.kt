@file:Suppress(
    "LongParameterList",
    "TooManyFunctions",
    "TooGenericExceptionCaught",
    "InstanceOfCheckForException",
    "PackageNaming",
)

package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import android.util.Log
import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItemType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.GoalActionType
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

private const val SMART_DEBOUNCE_GRACE_MS = 50L
private const val DEFAULT_DEBOUNCE_MS = 800L
private const val REMINDER_PARSE_TIMEOUT_MS = 10_000L
private const val REMINDER_FLOW_TAG = "ReminderFlow"
private const val RECENTS_DEBUG_TAG = "Recents_Debug"
private const val UTF_8 = "UTF-8"

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
                if (lastInputTime <= currentTime + SMART_DEBOUNCE_GRACE_MS) {
                    try {
                        block()
                    } catch (e: Exception) {
                        if (e !is kotlinx.coroutines.CancellationException) {
                            Log.e("SmartDebouncer", "Execution error", e)
                        }
                    }
                }
            }
        return checkNotNull(job)
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

class InputHandler(
    private val contextRepository: ContextRepository,
    private val goalRepository: com.romankozak.forwardappmobile.data.repository.GoalRepository,
    private val scope: CoroutineScope,
    private val projectIdFlow: StateFlow<String>,
    private val resultListener: ResultListener,
    private val reminderParser: ReminderParser,
) {
    private val smartDebouncer = SmartDebouncer(DEFAULT_DEBOUNCE_MS)
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

        fun addIssue(text: String)

        fun addProjectComment(text: String)

        fun addMilestone(text: String)

        fun addDirectionItem(text: String)

        fun createObsidianNote(noteName: String, vault: String? = null)
    }

    fun onInputTextChanged(
        newValue: TextFieldValue,
        currentInputMode: InputMode,
    ) {
        resultListener.updateInputState(inputValue = newValue)
        if (currentInputMode == InputMode.AddGoal || currentInputMode == InputMode.AddIssue) {
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
            val result = reminderParser.parseWithTimeout(text, REMINDER_PARSE_TIMEOUT_MS)
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
            Log.e(REMINDER_FLOW_TAG, "Suggestion parse error: ${e.message}", e)
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
        if (originalText.isNotBlank()) {
            clearAndCancelParsing()

            when (inputMode) {
                InputMode.AddGoal -> submitGoalInput(originalText)
                InputMode.AddIssue -> {
                    resetInput(clearDetectedReminder = true)
                    resultListener.addIssue(originalText)
                }
                InputMode.AddQuickRecord -> {
                    resetInput()
                    resultListener.addQuickRecord(originalText)
                }
                InputMode.AddConnectionNote -> submitConnectionNoteInput(originalText)
                InputMode.SearchGlobal -> {
                    resultListener.requestNavigation(
                        "global_search_screen/${URLEncoder.encode(originalText, UTF_8)}",
                    )
                    resetInput()
                }
                InputMode.SearchInList -> {
                    resultListener.updateInputState(
                        inputValue = TextFieldValue(originalText),
                        localSearchQuery = originalText,
                    )
                }
                InputMode.AddProjectLog -> {
                    resetInput()
                    resultListener.addProjectComment(originalText)
                }
                InputMode.AddMilestone -> {
                    resetInput()
                    resultListener.addMilestone(originalText)
                }
                InputMode.AddDirection -> {
                    resetInput()
                    resultListener.addDirectionItem(originalText)
                }
            }
        }
    }

    fun onClearDetectedReminder() {
        clearAndCancelParsing()
    }

    private fun submitGoalInput(originalText: String) {
        val currentProjectId = projectIdFlow.value
        if (currentProjectId.isBlank()) {
            return
        }

        resetInput(clearDetectedReminder = true)

        scope.launch(Dispatchers.IO) {
            try {
                val definitiveResult =
                    reminderParser.parseWithTimeout(originalText, REMINDER_PARSE_TIMEOUT_MS)
                Log.d(
                    REMINDER_FLOW_TAG,
                    "Submit Parser Result: " +
                        "success=${definitiveResult.success}, " +
                        "calendar=${definitiveResult.calendar?.time}, " +
                        "suggestion='${definitiveResult.suggestionText}'",
                )

                val preparedGoal = prepareGoalInput(originalText, definitiveResult)
                val newItemIdentifier =
                    if (preparedGoal.reminderTime != null) {
                        goalRepository.addGoalWithReminder(
                            preparedGoal.textToSave,
                            currentProjectId,
                            preparedGoal.reminderTime,
                        ).id
                    } else {
                        goalRepository.addGoalToContext(
                            preparedGoal.textToSave,
                            currentProjectId,
                        )
                    }

                withContext(Dispatchers.Main) {
                    resultListener.updateInputState(newlyAddedItemId = newItemIdentifier)
                }
            } catch (e: Exception) {
                Log.e(REMINDER_FLOW_TAG, "Submit error: ${e.message}", e)
            }
        }
    }

    private fun submitConnectionNoteInput(originalText: String) {
        val currentProjectId = projectIdFlow.value
        if (currentProjectId.isBlank()) {
            return
        }

        resetInput()

        scope.launch(Dispatchers.IO) {
            val newItemId =
                contextRepository.addConnectionNoteToContext(
                    contextId = currentProjectId,
                    text = originalText,
                )
            if (newItemId.isNotBlank()) {
                resultListener.updateInputState(newlyAddedItemId = newItemId)
            }
        }
    }

    private data class PreparedGoalInput(
        val textToSave: String,
        val reminderTime: Long?,
    )

    private fun prepareGoalInput(
        originalText: String,
        definitiveResult: com.romankozak.forwardappmobile.domain.ner.ReminderParseResult,
    ): PreparedGoalInput {
        val detectedCalendar = definitiveResult.calendar.takeIf { definitiveResult.success }
        val detectedSuggestion = definitiveResult.suggestionText
        val hasReminderData = detectedCalendar != null && !detectedSuggestion.isNullOrBlank()
        val cleanedText =
            if (hasReminderData) {
                originalText
                    .replace(detectedSuggestion.orEmpty(), "", ignoreCase = true)
                    .trim()
            } else {
                originalText
            }
        return PreparedGoalInput(
            textToSave = cleanedText.takeIf { hasReminderData && it.isNotBlank() } ?: originalText,
            reminderTime = detectedCalendar?.timeInMillis.takeIf { hasReminderData },
        )
    }

    private fun resetInput(clearDetectedReminder: Boolean = false) {
        resultListener.updateInputState(
            inputValue = TextFieldValue(""),
            clearDetectedReminder = clearDetectedReminder,
        )
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
            val newItemId =
                contextRepository.addLinkItemToContextFromLink(projectIdFlow.value, link)
            resultListener.updateInputState(newlyAddedItemId = newItemId)
        }
        onDismissLinkDialogs()
    }

    fun onAddObsidianLinkConfirm(
        noteName: String?,
        vault: String? = null,
    ) {
        if (noteName.isNullOrBlank()) {
            onDismissLinkDialogs()
            return
        }
        scope.launch(Dispatchers.IO) {
            val link =
                RelatedLink(
                    type = LinkType.OBSIDIAN,
                    target = noteName,
                    displayName = noteName,
                    vault = vault?.trim()?.ifBlank { null },
                )
            val newItemId =
                contextRepository.addLinkItemToContextFromLink(projectIdFlow.value, link)
            resultListener.updateInputState(newlyAddedItemId = newItemId)
        }
        onDismissLinkDialogs()
    }

    fun onAddObsidianLinkAndCreateNewConfirm(
        noteName: String,
        vault: String? = null,
    ) {
        if (noteName.isBlank()) {
            onDismissLinkDialogs()
            return
        }
        val normalizedVault = vault?.trim()?.ifBlank { null }
        resultListener.createObsidianNote(noteName, normalizedVault)
        onAddObsidianLinkConfirm(noteName, normalizedVault)
    }

    fun onShowAddWebLinkDialog() =
        resultListener.updateDialogState(showAddWebLinkDialog = true)

    fun onShowAddObsidianLinkDialog() =
        resultListener.updateDialogState(showAddObsidianLinkDialog = true)

    fun onDismissLinkDialogs() =
        resultListener.updateDialogState(
            showAddWebLinkDialog = false,
            showAddObsidianLinkDialog = false,
        )

    fun onAddListLinkRequest() = resultListener.setPendingAction(GoalActionType.AddLinkToList)

    fun onAddListShortcutRequest() = resultListener.setPendingAction(GoalActionType.ADD_LIST_SHORTCUT)

    fun onShowRecentLists() {
        Log.d(RECENTS_DEBUG_TAG, "InputHandler: onShowRecentLists() called. Calling listener.")
        resultListener.showRecentListsSheet(true)
    }

    fun onDismissRecentLists() = resultListener.showRecentListsSheet(false)

    fun onRecentListSelected(item: RecentItem) {
        when (item.type) {
            RecentItemType.PROJECT -> {
                resultListener.requestNavigation("goal_detail_screen/${item.target}")
            }
            RecentItemType.NOTE -> {
                resultListener.requestNavigation("note_edit_screen?noteId=${item.target}")
            }
            RecentItemType.NOTE_DOCUMENT -> {
                resultListener.requestNavigation("note_document_screen/${item.target}")
            }
            RecentItemType.CHECKLIST -> {
                resultListener.requestNavigation("checklist_screen?checklistId=${item.target}")
            }
            RecentItemType.MUSIC_NOTE -> {
                resultListener.requestNavigation("music_note_screen/${item.target}")
            }
            RecentItemType.OBSIDIAN_LINK -> {
                resultListener.createObsidianNote(item.target)
            }
        }
        onDismissRecentLists()
    }
}
