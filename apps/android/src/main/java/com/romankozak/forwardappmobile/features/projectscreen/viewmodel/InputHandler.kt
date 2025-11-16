package com.romankozak.forwardappmobile.features.projectscreen.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.repository.GoalRepository
import com.romankozak.forwardappmobile.shared.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.shared.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.shared.domain.models.LinkType
import com.romankozak.forwardappmobile.shared.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.shared.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.projectscreen.components.inputpanel.InputMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import java.util.Calendar

interface InputHandlerResultListener {
  fun updateInputState(
    inputValue: TextFieldValue? = null,
    inputMode: InputMode? = null,
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
class InputHandler(
  private val projectRepository: ProjectRepository,
  private val goalRepository: GoalRepository,
  private val listItemRepository: ListItemRepository,
  private val scope: CoroutineScope,
  private val projectIdFlow: StateFlow<String>,
  private val listener: InputHandlerResultListener,
  private val reminderParser: ReminderParser,
  private val alarmScheduler: AlarmScheduler,
) {
  fun onInputTextChanged(newValue: TextFieldValue, currentMode: InputMode) {
    listener.updateInputState(inputValue = newValue)
    if (currentMode == InputMode.AddGoal) {
      scope.launch {
        val reminder = reminderParser.parse(newValue.text)
        if (reminder != null) {
          listener.updateInputState(
            detectedReminderSuggestion = reminder.first,
            detectedReminderCalendar = reminder.second,
          )
        } else {
          listener.updateInputState(clearDetectedReminder = true)
        }
      }
    }
  }

  fun onAddGoal(goalText: String) {
    if (goalText.isBlank()) return
    scope.launch(Dispatchers.IO) {
      val newItemId = goalRepository.addGoalToProject(goalText, projectIdFlow.value)
      withContext(Dispatchers.Main) {
        listener.updateInputState(
          inputValue = TextFieldValue(""),
          newlyAddedItemId = newItemId,
          clearDetectedReminder = true,
        )
      }
    }
  }

  fun onAddWebLink(url: String, title: String?) {
    if (url.isBlank()) return
    scope.launch(Dispatchers.IO) {
      val link =
        RelatedLink(
          type = LinkType.WEB,
          target = url,
          displayName = title,
        )
      val newItemId = projectRepository.addLinkItemToProjectFromLink(projectIdFlow.value, link)
      withContext(Dispatchers.Main) {
        listener.updateInputState(newlyAddedItemId = newItemId)
        listener.updateDialogState(showAddWebLinkDialog = false)
      }
    }
  }

  fun onAddObsidianLink(noteName: String, title: String?) {
    if (noteName.isBlank()) return
    scope.launch(Dispatchers.IO) {
      val link =
        RelatedLink(
          type = LinkType.OBSIDIAN,
          target = noteName,
          displayName = title,
        )
      val newItemId = projectRepository.addLinkItemToProjectFromLink(projectIdFlow.value, link)
      withContext(Dispatchers.Main) {
        listener.updateInputState(newlyAddedItemId = newItemId)
        listener.updateDialogState(showAddObsidianLinkDialog = false)
        listener.createObsidianNote(noteName)
      }
    }
  }

  fun onShowAddWebLinkDialog() {
    listener.updateDialogState(showAddWebLinkDialog = true)
  }

  fun onShowAddObsidianLinkDialog() {
    listener.updateDialogState(showAddObsidianLinkDialog = true)
  }

  fun onDismissLinkDialogs() {
    listener.updateDialogState(showAddWebLinkDialog = false, showAddObsidianLinkDialog = false)
  }

  fun onAddListLinkRequest() {
    listener.showRecentListsSheet(true)
  }

  fun onAddListShortcutRequest() {
    listener.showRecentListsSheet(true)
  }

  fun onDismissRecentLists() {
    listener.showRecentListsSheet(false)
  }

  fun onClearDetectedReminder() {
    listener.updateInputState(clearDetectedReminder = true)
  }

  fun cleanup() {
    // No-op for now
  }
}
