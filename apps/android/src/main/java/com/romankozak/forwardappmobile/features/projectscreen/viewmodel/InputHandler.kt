package com.romankozak.forwardappmobile.features.projectscreen.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import me.tatarka.inject.annotations.Inject
import java.util.Calendar

// TODO: [GM-31] This file needs to be refactored with the new KMP architecture.
interface InputHandlerResultListener {
  fun updateInputState(
    inputValue: TextFieldValue? = null,
    inputMode: Any? = null,
    localSearchQuery: String? = null,
    newlyAddedItemId: String? = null,
    detectedReminderSuggestion: String? = null,
    detectedReminderCalendar: Calendar? = null,
    clearDetectedReminder: Boolean = false,
  )

  fun updateInputState(inputValue: TextFieldValue)

  fun updateDialogState(
    showAddWebLinkDialog: Boolean? = null,
    showAddObsidianLinkDialog: Boolean? = null,
  )

  fun showRecentListsSheet(show: Boolean)
  fun createObsidianNote(noteName: String)
}

@Inject
class InputHandler()