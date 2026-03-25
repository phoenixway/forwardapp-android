package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import java.util.Calendar

class UiStateActions(
    private val stateManager: ContextStateManager,
) {
    fun updateInputState(inputState: InputStateUpdate) {
        stateManager.updateState { currentState ->
            currentState.copy(
                inputValue = inputState.inputValue ?: currentState.inputValue,
                inputMode = inputState.inputMode ?: currentState.inputMode,
                localSearchQuery = inputState.localSearchQuery ?: currentState.localSearchQuery,
                newlyAddedItemId = inputState.newlyAddedItemId,
                detectedReminderSuggestion =
                    when {
                        inputState.clearDetectedReminder -> null
                        inputState.detectedReminderSuggestion != null -> inputState.detectedReminderSuggestion
                        else -> currentState.detectedReminderSuggestion
                    },
                detectedReminderCalendar =
                    when {
                        inputState.clearDetectedReminder -> null
                        inputState.detectedReminderCalendar != null -> inputState.detectedReminderCalendar
                        else -> currentState.detectedReminderCalendar
                    },
            )
        }
    }

    fun updateDialogState(
        showAddWebLinkDialog: Boolean?,
        showAddObsidianLinkDialog: Boolean?,
    ) {
        stateManager.updateState {
            it.copy(
                showAddWebLinkDialog = showAddWebLinkDialog ?: it.showAddWebLinkDialog,
                showAddObsidianLinkDialog = showAddObsidianLinkDialog ?: it.showAddObsidianLinkDialog,
            )
        }
    }

    fun closeSearch() {
        stateManager.updateState { it.copy(localSearchQuery = "", inputValue = TextFieldValue("")) }
    }

    fun showRecentProjectsSheet() {
        setRecentProjectsSheetVisibility(isVisible = true)
    }

    fun dismissRecentProjectsSheet() {
        setRecentProjectsSheetVisibility(isVisible = false)
    }

    fun showShareDialog() {
        setShareDialogVisibility(isVisible = true)
    }

    fun dismissShareDialog() {
        setShareDialogVisibility(isVisible = false)
    }

    fun showAddWebLinkDialog() {
        setWebLinkDialogVisibility(isVisible = true)
    }

    fun dismissAddWebLinkDialog() {
        setWebLinkDialogVisibility(isVisible = false)
    }

    fun showAddObsidianLinkDialog() {
        setObsidianLinkDialogVisibility(isVisible = true)
    }

    fun dismissAddObsidianLinkDialog() {
        setObsidianLinkDialogVisibility(isVisible = false)
    }

    fun highlightItem(itemId: String?) {
        stateManager.updateState { it.copy(itemToHighlight = itemId) }
    }

    fun highlightGoal(goalId: String?) {
        stateManager.updateState { it.copy(goalToHighlight = goalId) }
    }

    fun highlightInboxRecord(recordId: String?) {
        stateManager.updateState { it.copy(inboxRecordToHighlight = recordId) }
    }

    fun clearHighlightState() {
        stateManager.updateState { it.copy(goalToHighlight = null, itemToHighlight = null) }
    }

    fun clearInboxHighlightState() {
        stateManager.updateState { it.copy(inboxRecordToHighlight = null) }
    }

    fun markNewItemConsumed() {
        stateManager.updateState { it.copy(newlyAddedItemId = null) }
    }

    fun toggleCheckboxes() {
        stateManager.updateState { it.copy(showCheckboxes = !it.showCheckboxes) }
    }

    private fun setRecentProjectsSheetVisibility(isVisible: Boolean) {
        stateManager.updateState { it.copy(showRecentProjectsSheet = isVisible) }
    }

    private fun setShareDialogVisibility(isVisible: Boolean) {
        stateManager.updateState { it.copy(showShareDialog = isVisible) }
    }

    private fun setWebLinkDialogVisibility(isVisible: Boolean) {
        stateManager.updateState { it.copy(showAddWebLinkDialog = isVisible) }
    }

    private fun setObsidianLinkDialogVisibility(isVisible: Boolean) {
        stateManager.updateState { it.copy(showAddObsidianLinkDialog = isVisible) }
    }
}

data class InputStateUpdate(
    val inputValue: TextFieldValue? = null,
    val inputMode:
        com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputMode? = null,
    val localSearchQuery: String? = null,
    val newlyAddedItemId: String? = null,
    val detectedReminderSuggestion: String? = null,
    val detectedReminderCalendar: Calendar? = null,
    val clearDetectedReminder: Boolean = false,
)
