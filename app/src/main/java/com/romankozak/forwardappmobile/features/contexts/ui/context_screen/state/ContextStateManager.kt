
package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state

import com.romankozak.forwardappmobile.core.data.models.entities.*
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization.ContextManagementTab
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.InputMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Управляє станом UI для Context Screen
 */
class ContextStateManager(
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(ContextUiState())
    val uiState: StateFlow<ContextUiState> = _uiState.asStateFlow()

    private val _isProcessingHome = MutableStateFlow(false)

    fun isProcessingHome() = _isProcessingHome.value

    fun setProcessingHome(processing: Boolean) {
        _isProcessingHome.value = processing
    }

    fun updateContext(data: ContextData.Loaded) {
        _uiState.update { current ->
            current.copy(
                context = data.context,
                items = data.items,
                configuration = data.config,
                logs = data.logs,
                checklists = data.checklists,
                noteDocuments = data.noteDocuments,
                directionItems = data.directionItems,
                linkedContextNames = data.linkedContextNames,
                reminders = data.reminders,
                recentItems = data.recentItems,
                notes = data.notes,
            )
        }
    }

    fun clear() {
        _uiState.update { ContextUiState() }
    }

    fun switchViewMode(mode: ContextViewMode) {
        _uiState.update { it.copy(currentViewMode = mode) }
    }

    fun switchTab(tab: ContextManagementTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun toggleSearchMode() {
        _uiState.update {
            it.copy(
                isSearchMode = !it.isSearchMode,
                searchQuery = if (!it.isSearchMode) it.searchQuery else "",
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun showDisplayPropertiesDialog() {
        _uiState.update { it.copy(showDisplayPropertiesDialog = true) }
    }

    fun dismissDisplayPropertiesDialog() {
        _uiState.update { it.copy(showDisplayPropertiesDialog = false) }
    }

    fun showNoteDocumentEditor() {
        _uiState.update { it.copy(showNoteDocumentEditor = true) }
    }

    fun dismissNoteDocumentEditor() {
        _uiState.update { it.copy(showNoteDocumentEditor = false) }
    }

    fun setLogEntryToEdit(log: ContextLog?) {
        _uiState.update { it.copy(logEntryToEdit = log) }
    }

    fun setArtifactToEdit(artifact: ContextArtifact?) {
        _uiState.update { it.copy(artifactToEdit = artifact) }
    }

    fun updateState(transform: (ContextUiState) -> ContextUiState) {
        _uiState.update(transform)
    }

    fun setInputMode(mode: InputMode) {
        _uiState.update { it.copy(inputMode = mode) }
    }

    fun setInputValue(value: androidx.compose.ui.text.input.TextFieldValue) {
        _uiState.update { it.copy(inputValue = value) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItemIds = emptySet()) }
    }

    fun toggleItemSelection(itemId: String) {
        _uiState.update { current ->
            val newSelection =
                if (itemId in current.selectedItemIds) {
                    current.selectedItemIds - itemId
                } else {
                    current.selectedItemIds + itemId
                }
            current.copy(selectedItemIds = newSelection)
        }
    }
}

/**
 * Sealed class для представлення стану завантаження даних контексту
 */
sealed class ContextData {
    data object Empty : ContextData()

    data class Loaded(
        val context: Context?,
        val items: List<BacklogItemContent>,
        val config: ContextConfiguration,
        val logs: List<ContextLog>,
        val checklists: List<ChecklistEntity>,
        val noteDocuments: List<NoteDocumentEntity>,
        val directionItems: List<DirectionItemEntity>,
        val linkedContextNames: Map<String, String>,
        val reminders: List<Reminder>,
        val recentItems: List<RecentItem>,
        val notes: List<LegacyNoteEntity>,
    ) : ContextData()
}

/**
 * Data class для UI стану
 */
data class ContextUiState(
    // Data from repositories
    val context: Context? = null,
    val items: List<BacklogItemContent> = emptyList(),
    val configuration: ContextConfiguration? = null,
    val logs: List<ContextLog> = emptyList(),
    val checklists: List<ChecklistEntity> = emptyList(),
    val noteDocuments: List<NoteDocumentEntity> = emptyList(),
    val directionItems: List<DirectionItemEntity> = emptyList(),
    val linkedContextNames: Map<String, String> = emptyMap(),
    val reminders: List<Reminder> = emptyList(),
    val recentItems: List<RecentItem> = emptyList(),
    val notes: List<LegacyNoteEntity> = emptyList(),
    // UI State - View Mode & Navigation
    val currentViewMode: ContextViewMode = ContextViewMode.BACKLOG,
    val currentTab: ContextManagementTab = ContextManagementTab.Dashboard,
    val isSearchMode: Boolean = false,
    val searchQuery: String = "",
    val localSearchQuery: String = "",
    // UI State - Dialogs
    val showDisplayPropertiesDialog: Boolean = false,
    val showNoteDocumentEditor: Boolean = false,
    val showCreateNoteDocumentDialog: Boolean = false,
    val showAddWebLinkDialog: Boolean = false,
    val showAddObsidianLinkDialog: Boolean = false,
    val showRecentProjectsSheet: Boolean = false,
    val showImportFromMarkdownDialog: Boolean = false,
    val showImportBacklogFromMarkdownDialog: Boolean = false,
    val showShareDialog: Boolean = false,
    val showRemindersDialog: Boolean = false,
    // UI State - Edit Modes
    val logEntryToEdit: ContextLog? = null,
    val artifactToEdit: ContextArtifact? = null,
    val itemForRemindersDialog: BacklogItemContent? = null,
    val remindersForDialog: List<Reminder> = emptyList(),
    val recordForReminderDialog: ActivityRecord? = null,
    // UI State - Input & Selection
    val inputMode: InputMode = InputMode.AddGoal,
    val inputValue: androidx.compose.ui.text.input.TextFieldValue = androidx.compose.ui.text.input.TextFieldValue(""),
    val selectedItemIds: Set<String> = emptySet(),
    val swipedItemId: String? = null,
    val swipeResetCounter: Int = 0,
    val resetTriggers: Map<String, Int> = emptyMap(),
    // UI State - Highlighting & Focus
    val goalToHighlight: String? = null,
    val itemToHighlight: String? = null,
    val inboxRecordToHighlight: String? = null,
    val newlyAddedItemId: String? = null,
    // UI State - NER & Reminders
    val detectedReminderSuggestion: String? = null,
    val detectedReminderCalendar: java.util.Calendar? = null,
    val nerState: com.romankozak.forwardappmobile.domain.ner.NerState = com.romankozak.forwardappmobile.domain.ner.NerState.NotInitialized,
    // UI State - Metrics & Time
    val contextTimeMetrics: ContextTimeMetrics? = null,
    // UI State - Capabilities & Feature Flags
    val showCheckboxes: Boolean = false,
    val enableInbox: Boolean = true,
    val enableLog: Boolean = true,
    val enableArtifact: Boolean = true,
    val isProjectManagementEnabled: Boolean = false,
    val enableBacklog: Boolean = true,
    val enableDashboard: Boolean = true,
    val enableAttachments: Boolean = true,
    val enableAutoLinkSubprojects: Boolean = true,
    val experimentalCapabilityIds: List<com.romankozak.forwardappmobile.core.capability.CapabilityId> = emptyList(),
    // UI State - Refresh & State Management
    val needsStateRefresh: Boolean = false,
    val refreshTrigger: Int = 0,
) {
    val isSelectionModeActive: Boolean
        get() = selectedItemIds.isNotEmpty()
}

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
