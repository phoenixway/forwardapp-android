package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context as AndroidContext
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLogEntryTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.ContextTimeMetrics
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItemType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import com.romankozak.forwardappmobile.core.gate.CapabilityGate
import com.romankozak.forwardappmobile.core.navigation.ClearAndNavigateHomeUseCase
import com.romankozak.forwardappmobile.core.navigation.ClearCommand
import com.romankozak.forwardappmobile.core.navigation.ClearExecutionContext
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.ner.NerManager
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.domain.ner.ReminderParser
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.domain.wifirestapi.FileDataRequest
import com.romankozak.forwardappmobile.domain.wifirestapi.RetrofitClient
import com.romankozak.forwardappmobile.features.attachments.ui.context.AttachmentType
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.state.PlanningModeManager
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.SearchUseCase
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.withCompletedAtEnd
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.ContextManagementTab
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.utils.TagUtils
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.ContextMarkdownExporter
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.InboxHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.InboxHandlerResultListener
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.InboxMarkdownHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.ItemActionHandler
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.viewmodel.SelectionHandler
import com.romankozak.forwardappmobile.ui.common.editor.NoteTitleExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val TAG = "BacklogVM_DEBUG"

sealed class UiEvent {
  data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()

  data class Navigate(val target: NavTarget) : UiEvent()

  data class ResetSwipeState(val itemId: String) : UiEvent()

  data class ScrollTo(val index: Int) : UiEvent()

  data class NavigateBackAndReveal(val contextId: String) : UiEvent()

  data class HandleLinkClick(val link: RelatedLink) : UiEvent()

  data class OpenUri(val uri: String) : UiEvent()

  data object ScrollToLatestInboxRecord : UiEvent()

  data object NavigateBack : UiEvent()
}

enum class GoalActionType {
  CreateInstance,
  MoveInstance,
  CopyGoal,
  AddLinkToList,
  ADD_LIST_SHORTCUT,
}

sealed class GoalActionDialogState {
  object Hidden : GoalActionDialogState()

  data class AwaitingActionChoice(val itemContent: BacklogItemContent) : GoalActionDialogState()
}

data class UiState(
    val localSearchQuery: String = "",
    val goalToHighlight: String? = null,
    val inputMode: InputMode = InputMode.AddGoal,
    val newlyAddedItemId: String? = null,
    val selectedItemIds: Set<String> = emptySet(),
    val inputValue: TextFieldValue = TextFieldValue(""),
    val resetTriggers: Map<String, Int> = emptyMap(),
    val swipedItemId: String? = null,
    val swipeResetCounter: Int = 0,
    val showAddWebLinkDialog: Boolean = false,
    val showAddObsidianLinkDialog: Boolean = false,
    val itemToHighlight: String? = null,
    val inboxRecordToHighlight: String? = null,
    val needsStateRefresh: Boolean = false,
    val enableInbox: Boolean = true,
    val enableLog: Boolean = true,
    val enableArtifact: Boolean = true,
    val isProjectManagementEnabled: Boolean = false,
    val enableBacklog: Boolean = true,
    val enableDashboard: Boolean = true,
    val enableAttachments: Boolean = true,
    val enableAutoLinkSubprojects: Boolean = true,
    val experimentalCapabilityIds: List<CapabilityId> = emptyList(),
    val currentView: ContextViewMode = ContextViewMode.BACKLOG,
    val showRecentProjectsSheet: Boolean = false,
    val showImportFromMarkdownDialog: Boolean = false,
    val showImportBacklogFromMarkdownDialog: Boolean = false,
    val refreshTrigger: Int = 0,
    val detectedReminderSuggestion: String? = null,
    val detectedReminderCalendar: Calendar? = null,
    val nerState: NerState = NerState.NotInitialized,
    val recordForReminderDialog: ActivityRecord? = null,
    val contextTimeMetrics: ContextTimeMetrics? = null,
    val showShareDialog: Boolean = false,
    val showCreateNoteDocumentDialog: Boolean = false,
    val showRemindersDialog: Boolean = false,
    val itemForRemindersDialog: BacklogItemContent? = null,
    val remindersForDialog: List<Reminder> = emptyList(),
    val logEntryToEdit: ContextLog? = null,
    val artifactToEdit: ContextArtifact? = null,
    val selectedDashboardTab: ContextManagementTab = ContextManagementTab.Dashboard,
    val showNoteDocumentEditor: Boolean = false,
    val showDisplayPropertiesDialog: Boolean = false,
    val showCheckboxes: Boolean = false,
) {
  val isSelectionModeActive: Boolean
    get() = selectedItemIds.isNotEmpty()
}

interface BacklogMarkdownHandlerResultListener {
  fun copyToClipboard(
      text: String,
      label: String,
  )

  fun showSnackbar(
      message: String,
      action: String?,
  )

  fun forceRefresh()
}

class BacklogMarkdownHandler
@Inject
constructor(
    private val contextRepository: ContextRepository,
    private val goalRepository: com.romankozak.forwardappmobile.data.repository.GoalRepository,
    private val scope: CoroutineScope,
    private val listener: BacklogMarkdownHandlerResultListener,
) {
  fun exportToMarkdown(content: List<BacklogItemContent>) {
    if (content.isEmpty()) {
      listener.showSnackbar("Backlog is empty. Nothing to export.", null)
      return
    }
    val markdownBuilder = StringBuilder()
    content.forEach { item ->
      val line =
          when (item) {
            is BacklogItemContent.GoalItem -> {
              val checkbox = if (item.goal.completed) "- [x]" else "- [ ]"
              "$checkbox ${item.goal.text}"
            }

            is BacklogItemContent.SublistItem -> "- [C] ${item.project.name}"
            is BacklogItemContent.LinkItem -> {
              val displayName = item.link.linkData.displayName ?: item.link.linkData.target
              "- [L] [$displayName](${item.link.linkData.target})"
            }
            is BacklogItemContent.NoteItem -> "- [N] ${item.note.title}"
            is BacklogItemContent.NoteDocumentItem -> "- [D] ${item.document.name}"
            is BacklogItemContent.ChecklistItem -> "- [Ch] ${item.checklist.name}"
          }
      markdownBuilder.appendLine(line)
    }
    val markdownText = markdownBuilder.toString()
    listener.copyToClipboard(markdownText, "Backlog Export")
    listener.showSnackbar("Backlog copied to clipboard.", null)
  }

  fun importFromMarkdown(
      markdownText: String,
      contextId: String,
  ) {
    if (markdownText.isBlank()) {
      listener.showSnackbar("Nothing to import.", null)
      return
    }
    scope.launch(Dispatchers.IO) {
      val lines = markdownText.lines().filter { it.isNotBlank() }
      var importedCount = 0
      for (line in lines) {
        try {
          val trimmedLine = line.trim()
          when {
            trimmedLine.startsWith("- [ ]") -> {
              val goalText = trimmedLine.removePrefix("- [ ]").trim()
              if (goalText.isNotEmpty()) {
                goalRepository.addGoalToContext(goalText, contextId, completed = false)
                importedCount++
              }
            }

            trimmedLine.startsWith("- [x]") -> {
              val goalText = trimmedLine.removePrefix("- [x]").trim()
              if (goalText.isNotEmpty()) {
                goalRepository.addGoalToContext(goalText, contextId, completed = true)
                importedCount++
              }
            }
          }
        } catch (e: Exception) {
          Log.e("BacklogMarkdownHandler", "Failed to import line: $line", e)
        }
      }
      withContext(Dispatchers.Main) {
        listener.showSnackbar("Imported $importedCount items.", null)
        listener.forceRefresh()
      }
    }
  }
}
