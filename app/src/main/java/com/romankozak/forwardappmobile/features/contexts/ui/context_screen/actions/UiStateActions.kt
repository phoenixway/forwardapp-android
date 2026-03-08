package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextStateManager
import java.util.Calendar

class UiStateActions(
    private val stateManager: ContextStateManager,
) {
    fun updateInputState(
        inputValue: TextFieldValue?,
        inputMode: com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputMode?,
        localSearchQuery: String?,
        newlyAddedItemId: String?,
        detectedReminderSuggestion: String?,
        detectedReminderCalendar: Calendar?,
        clearDetectedReminder: Boolean,
    ) {
        stateManager.updateState { currentState ->
            currentState.copy(
                inputValue = inputValue ?: currentState.inputValue,
                inputMode = inputMode ?: currentState.inputMode,
                localSearchQuery = localSearchQuery ?: currentState.localSearchQuery,
                newlyAddedItemId = newlyAddedItemId,
                detectedReminderSuggestion =
                    when {
                        clearDetectedReminder -> null
                        detectedReminderSuggestion != null -> detectedReminderSuggestion
                        else -> currentState.detectedReminderSuggestion
                    },
                detectedReminderCalendar =
                    when {
                        clearDetectedReminder -> null
                        detectedReminderCalendar != null -> detectedReminderCalendar
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
        stateManager.updateState { it.copy(showRecentProjectsSheet = true) }
    }

    fun dismissRecentProjectsSheet() {
        stateManager.updateState { it.copy(showRecentProjectsSheet = false) }
    }

    fun showShareDialog() {
        stateManager.updateState { it.copy(showShareDialog = true) }
    }

    fun dismissShareDialog() {
        stateManager.updateState { it.copy(showShareDialog = false) }
    }

    fun showAddWebLinkDialog() {
        stateManager.updateState { it.copy(showAddWebLinkDialog = true) }
    }

    fun dismissAddWebLinkDialog() {
        stateManager.updateState { it.copy(showAddWebLinkDialog = false) }
    }

    fun showAddObsidianLinkDialog() {
        stateManager.updateState { it.copy(showAddObsidianLinkDialog = true) }
    }

    fun dismissAddObsidianLinkDialog() {
        stateManager.updateState { it.copy(showAddObsidianLinkDialog = false) }
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

    fun setSwipedItem(itemId: String?) {
        stateManager.updateState { it.copy(swipedItemId = itemId) }
    }

    fun resetSwipeState() {
        stateManager.updateState {
            it.copy(
                swipedItemId = null,
                swipeResetCounter = it.swipeResetCounter + 1,
            )
        }
    }

    fun markNewItemConsumed() {
        stateManager.updateState { it.copy(newlyAddedItemId = null) }
    }

    fun toggleCheckboxes() {
        stateManager.updateState { it.copy(showCheckboxes = !it.showCheckboxes) }
    }

    fun bumpSwipeResetTrigger(itemId: String) {
        stateManager.updateState { currentState ->
            val newTriggers = currentState.resetTriggers.toMutableMap()
            newTriggers[itemId] = (newTriggers[itemId] ?: 0) + 1
            currentState.copy(resetTriggers = newTriggers)
        }
    }
}
