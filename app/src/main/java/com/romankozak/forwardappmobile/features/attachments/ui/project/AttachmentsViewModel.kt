package com.romankozak.forwardappmobile.features.attachments.ui.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.shared.data.database.models.LinkType
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.shared.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.shared.features.reminders.data.model.Reminder
import com.romankozak.forwardappmobile.shared.features.reminders.data.repository.uuid4
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Calendar
import javax.inject.Inject

enum class AddAttachmentDialogType {
    NONE,
    WEB_LINK,
    OBSIDIAN_LINK
}

enum class PendingAttachmentType {
    NONE,
    PROJECT_LINK,
    PROJECT_SHORTCUT
}

data class AttachmentsUiState(
    val project: Project? = null,
    val name: String = "",
    val tags: List<String> = emptyList(),
    val reminderTime: Long? = null,
    val scoringStatus: String = ScoringStatusValues.NOT_ASSESSED,
    val isScoringEnabled: Boolean = true,
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,
    val rawScore: Float = 0f,
    val showAddAttachmentDialog: AddAttachmentDialogType = AddAttachmentDialogType.NONE,
    val pendingAttachmentType: PendingAttachmentType = PendingAttachmentType.NONE,
)

@HiltViewModel
class AttachmentsViewModel @Inject constructor(
    private val projectRepository: ProjectRepositoryCore,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val recentItemsRepository: RecentItemsRepository,
    private val listItemRepository: ListItemRepository,
    private val goalScoringManager: GoalScoringManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val projectId: StateFlow<String> = savedStateHandle.getStateFlow("projectId", "")

    private val _uiState = MutableStateFlow(AttachmentsUiState())
    val uiState: StateFlow<AttachmentsUiState> = _uiState.asStateFlow()

    private val _uiEventFlow = Channel<UiEvent>()
    val uiEventFlow = _uiEventFlow.receiveAsFlow()

    private var originalProject: Project? = null

    init {
        viewModelScope.launch {
            savedStateHandle.getLiveData<String>("list_chooser_result").asFlow().collect { result ->
                if (result != null) {
                    android.util.Log.d("AttachmentsViewModel", "Result received: $result")
                    when (uiState.value.pendingAttachmentType) {
                        PendingAttachmentType.PROJECT_LINK -> onAddProjectLink(result)
                        PendingAttachmentType.PROJECT_SHORTCUT -> onAddProjectShortcut(result)
                        PendingAttachmentType.NONE -> {
                            android.util.Log.w("AttachmentsViewModel", "Received a list chooser result but no pending attachment type.")
                        }
                    }
                    savedStateHandle.remove<String>("list_chooser_result")
                    android.util.Log.d("AttachmentsViewModel", "Result removed from SavedStateHandle.")
                }
            }
        }

        viewModelScope.launch {
            val loadedProject = projectRepository.getProjectById(projectId.value)
            originalProject = loadedProject
            _uiState.update {
                if (loadedProject != null) {
                    it.copy(
                        project = loadedProject,
                        name = loadedProject.name,
                        tags = loadedProject.tags?.filter { it.isNotBlank() } ?: emptyList(),
                        scoringStatus = loadedProject.scoringStatus,
                        isScoringEnabled = loadedProject.scoringStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
                        valueImportance = loadedProject.valueImportance,
                        valueImpact = loadedProject.valueImpact,
                        effort = loadedProject.effort,
                        cost = loadedProject.cost,
                        risk = loadedProject.risk,
                        weightEffort = loadedProject.weightEffort,
                        weightCost = loadedProject.weightCost,
                        weightRisk = loadedProject.weightRisk,
                        rawScore = loadedProject.rawScore,
                    )
                } else {
                    it
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val attachments: StateFlow<List<ListItemContent>> =
        projectId.flatMapLatest { projectId ->
            if (projectId.isNotEmpty()) {
                projectRepository.getProjectContentStream(projectId).map { content ->
                    content.filter { item ->
                        item is ListItemContent.LinkItem ||
                            item is ListItemContent.NoteDocumentItem ||
                            item is ListItemContent.ChecklistItem
                    }
                }
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteAttachment(attachment: ListItemContent) {
        viewModelScope.launch {
            val currentProjectId = projectId.value
            if (currentProjectId.isNotEmpty()) {
                projectRepository.deleteListItems(currentProjectId, listOf(attachment.listItem.id))
            }
        }
    }

    fun onAddAttachment(type: AttachmentType) {
        when (type) {
            AttachmentType.NOTES -> {
                viewModelScope.launch {
                    _uiEventFlow.send(UiEvent.Navigate("note_document_edit_screen?projectId=${projectId.value}"))
                }
            }
            AttachmentType.CHECKLIST -> {
                viewModelScope.launch {
                    _uiEventFlow.send(UiEvent.Navigate("checklist_screen?projectId=${projectId.value}"))
                }
            }
            AttachmentType.WEB_LINK -> {
                _uiState.update { it.copy(showAddAttachmentDialog = AddAttachmentDialogType.WEB_LINK) }
            }
            AttachmentType.OBSIDIAN_LINK -> {
                _uiState.update { it.copy(showAddAttachmentDialog = AddAttachmentDialogType.OBSIDIAN_LINK) }
            }
            AttachmentType.PROJECT_LINK -> {
                _uiState.update { it.copy(pendingAttachmentType = PendingAttachmentType.PROJECT_LINK) }
                viewModelScope.launch {
                    _uiEventFlow.send(UiEvent.NavigateToListChooser("Add link to another project", projectId.value))
                }
            }
            AttachmentType.PROJECT_SHORTCUT -> {
                _uiState.update { it.copy(pendingAttachmentType = PendingAttachmentType.PROJECT_SHORTCUT) }
                viewModelScope.launch {
                    _uiEventFlow.send(UiEvent.NavigateToListChooser("Add shortcut to another project", projectId.value))
                }
            }
        }
    }

    fun onLinkClick(link: RelatedLink) {
        viewModelScope.launch {
            when (link.type) {
                LinkType.PROJECT -> {
                    _uiEventFlow.send(UiEvent.Navigate("goal_detail_screen/${link.target}"))
                }
                LinkType.URL -> {
                    _uiEventFlow.send(UiEvent.OpenUri(link.target))
                }
                LinkType.OBSIDIAN -> {
                    recentItemsRepository.logObsidianLinkAccess(link.target, link.displayName)
                    val vaultName = settingsRepository.obsidianVaultNameFlow.first()
                    val encodedNoteName = URLEncoder.encode(link.target, "UTF-8")
                    val uri = "obsidian://new?vault=$vaultName&name=$encodedNoteName"
                    _uiEventFlow.send(UiEvent.OpenUri(uri))
                }
                null -> {
                    // Do nothing
                }
            }
        }
    }

    fun onDismissAddAttachmentDialog() {
        _uiState.update { it.copy(showAddAttachmentDialog = AddAttachmentDialogType.NONE) }
    }

    fun onAddWebLink(link: RelatedLink) {
        viewModelScope.launch {
            projectRepository.addLinkItemToProjectFromLink(projectId.value, link)
        }
    }

    fun onAddProjectLink(targetProjectId: String) {
        viewModelScope.launch {
            projectRepository.addProjectLinkToProject(targetProjectId, projectId.value)
        }
    }

    fun onAddProjectShortcut(targetProjectId: String) {
        viewModelScope.launch {
            projectRepository.addProjectLinkToProject(targetProjectId, projectId.value)
        }
    }

    fun onPendingAttachmentTypeResolved() {
        _uiState.update { it.copy(pendingAttachmentType = PendingAttachmentType.NONE) }
    }

    fun onReminderTimeSelected(calendar: Calendar) {
        _uiState.update { it.copy(reminderTime = calendar.timeInMillis) }
    }

    fun onReminderCleared() {
        _uiState.update { it.copy(reminderTime = null) }
    }

    fun onSetReminder() {
        viewModelScope.launch {
            uiState.value.project?.let { project ->
                uiState.value.reminderTime?.let { reminderTime ->
                        val reminder =
                            Reminder(
                                id = uuid4(),
                                entityId = project.id,
                                entityType = "PROJECT",
                                reminderTime = reminderTime,
                                status = "SCHEDULED",
                                creationTime = System.currentTimeMillis(),
                                snoozeUntil = null,
                            )
                    alarmScheduler.schedule(reminder)
                }
            }
        }
    }

    fun onScoringEnabledChange(isEnabled: Boolean) {
        _uiState.update { it.copy(isScoringEnabled = isEnabled) }
    }

    fun onScoringStatusChange(status: String) {
        _uiState.update { it.copy(scoringStatus = status) }
    }

    fun onValueImportanceChange(value: Float) {
        _uiState.update { it.copy(valueImportance = value) }
    }

    fun onValueImpactChange(value: Float) {
        _uiState.update { it.copy(valueImpact = value) }
    }

    fun onEffortChange(value: Float) {
        _uiState.update { it.copy(effort = value) }
    }

    fun onCostChange(value: Float) {
        _uiState.update { it.copy(cost = value) }
    }

    fun onRiskChange(value: Float) {
        _uiState.update { it.copy(risk = value) }
    }

    fun onWeightEffortChange(value: Float) {
        _uiState.update { it.copy(weightEffort = value) }
    }

    fun onWeightCostChange(value: Float) {
        _uiState.update { it.copy(weightCost = value) }
    }

    fun onWeightRiskChange(value: Float) {
        _uiState.update { it.copy(weightRisk = value) }
    }

    fun onRawScoreChange(value: Float) {
        _uiState.update { it.copy(rawScore = value) }
    }

    fun onSaveScoring() {
        viewModelScope.launch {
            uiState.value.project?.let { project ->
                val updatedProject =
                    project.copy(
                        scoringStatus = uiState.value.scoringStatus,
                        valueImportance = uiState.value.valueImportance,
                        valueImpact = uiState.value.valueImpact,
                        effort = uiState.value.effort,
                        cost = uiState.value.cost,
                        risk = uiState.value.risk,
                        weightEffort = uiState.value.weightEffort,
                        weightCost = uiState.value.weightCost,
                        weightRisk = uiState.value.weightRisk,
                        rawScore = uiState.value.rawScore,
                    )
                val scoredProject = goalScoringManager.calculateScoresForProject(updatedProject)
                projectRepository.updateProject(scoredProject)
            }
        }
    }

    sealed interface UiEvent {
        data class Navigate(val route: String) : UiEvent
        data class OpenUri(val uri: String) : UiEvent
        data class NavigateToListChooser(val title: String, val currentProjectId: String) : UiEvent
    }
}
