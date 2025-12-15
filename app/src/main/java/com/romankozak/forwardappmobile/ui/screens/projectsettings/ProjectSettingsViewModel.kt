package com.romankozak.forwardappmobile.ui.screens.projectsettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.ui.screens.common.tabs.RemindersTabActions
import com.romankozak.forwardappmobile.ui.screens.common.tabs.EvaluationTabActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectSettingsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val reminderRepository: ReminderRepository,
    private val savedStateHandle: SavedStateHandle,
    private val structurePresetDao: com.romankozak.forwardappmobile.data.dao.StructurePresetDao,
    private val projectStructureRepository: com.romankozak.forwardappmobile.data.repository.ProjectStructureRepository,
    private val structurePresetService: com.romankozak.forwardappmobile.domain.structure.StructurePresetService,
) : ViewModel(), EvaluationTabActions, RemindersTabActions {

    private val projectId: String? = savedStateHandle["projectId"]

    private val _uiState = MutableStateFlow(ProjectSettingsUiState())
    val uiState: StateFlow<ProjectSettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProjectSettingsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (projectId != null) {
                loadExistingProject(projectId)
                reminderRepository.getRemindersForEntityFlow(projectId).collect { reminders ->
                    _uiState.update { it.copy(reminderTime = reminders.firstOrNull()?.reminderTime) }
                }
            } else {
                // TODO: Handle project creation
            }
        }

        viewModelScope.launch {
            structurePresetDao.getAll().collect { presets ->
                _uiState.update { it.copy(availablePresets = presets) }
            }
        }
    }

    private suspend fun loadExistingProject(projectId: String) {
        val project = projectRepository.getProjectById(projectId)
        if (project != null) {
            _uiState.update {
                it.copy(
                    title = it.title.copy(project.name),
                    description = it.description.copy(project.description ?: ""),
                    tags = project.tags ?: emptyList(),
                    isReady = true,
                    isNewProject = false,
                    showCheckboxes = project.showCheckboxes,
                    valueImportance = project.valueImportance,
                    valueImpact = project.valueImpact,
                    effort = project.effort,
                    cost = project.cost,
                    risk = project.risk,
                    weightEffort = project.weightEffort,
                    weightCost = project.weightCost,
                    weightRisk = project.weightRisk,
                    rawScore = project.rawScore,
                    displayScore = project.displayScore,
                    scoringStatus = project.scoringStatus,
                    isScoringEnabled = project.scoringStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
                    isProjectManagementEnabled = project.isProjectManagementEnabled ?: false,
                )
            }
            val structure = projectStructureRepository.getStructureByProject(projectId)
            val presetLabel = structure?.basePresetCode?.let { code -> structurePresetDao.getByCode(code)?.label }
            val structureFeatures = mapOf(
                "Inbox" to (structure?.enableInbox ?: _uiState.value.features["Inbox"] ?: true),
                "Log" to (structure?.enableLog ?: _uiState.value.features["Log"] ?: true),
                "Artifact" to (structure?.enableArtifact ?: _uiState.value.features["Artifact"] ?: true),
                "Advanced" to (structure?.enableAdvanced ?: _uiState.value.features["Advanced"] ?: false),
                "Dashboard" to (structure?.enableDashboard ?: _uiState.value.features["Dashboard"] ?: true),
                "Backlog" to (structure?.enableBacklog ?: _uiState.value.features["Backlog"] ?: true),
                "Attachments" to (structure?.enableAttachments ?: _uiState.value.features["Attachments"] ?: true),
                "Auto link subprojects" to (structure?.enableAutoLinkSubprojects ?: _uiState.value.features["Auto link subprojects"] ?: true),
            )
            _uiState.update {
                it.copy(
                    currentPresetLabel = presetLabel,
                    features = structureFeatures,
                    autoLinkSubprojects = structureFeatures["Auto link subprojects"] ?: true,
                    isProjectManagementEnabled = structureFeatures["Advanced"] == true
                )
            }
        } else {
            _events.send(ProjectSettingsEvent.NavigateBack("Проект не знайдено"))
        }
    }

    fun onSave() {
        viewModelScope.launch {
            if (_uiState.value.title.text.isBlank()) {
                _events.send(ProjectSettingsEvent.NavigateBack("Назва проекту не може бути пустою"))
                return@launch
            }
            saveProject()
            _events.send(ProjectSettingsEvent.NavigateBack("Збережено"))
        }
    }

    private suspend fun saveProject() {
        val projectId: String = savedStateHandle["projectId"] ?: return
        val project = projectRepository.getProjectById(projectId) ?: return

        val updatedProject = project.copy(
            name = _uiState.value.title.text,
            description = _uiState.value.description.text.ifEmpty { null },
            tags = _uiState.value.tags,
            showCheckboxes = _uiState.value.showCheckboxes,
            isProjectManagementEnabled = _uiState.value.isProjectManagementEnabled,
            valueImportance = _uiState.value.valueImportance,
            valueImpact = _uiState.value.valueImpact,
            effort = _uiState.value.effort,
            cost = _uiState.value.cost,
            risk = _uiState.value.risk,
            weightEffort = _uiState.value.weightEffort,
            weightCost = _uiState.value.weightCost,
            weightRisk = _uiState.value.weightRisk,
            rawScore = _uiState.value.rawScore,
            displayScore = _uiState.value.displayScore,
            scoringStatus = _uiState.value.scoringStatus
        )
        projectRepository.updateProject(updatedProject)
        persistFeatureFlags()
    }

    fun onTextChange(newValue: androidx.compose.ui.text.input.TextFieldValue) = _uiState.update { it.copy(title = newValue) }

    fun onDescriptionChange(newValue: androidx.compose.ui.text.input.TextFieldValue) = _uiState.update { it.copy(description = newValue) }

    override fun onValueImportanceChange(value: Float) = _uiState.update { it.copy(valueImportance = value) }

    override fun onValueImpactChange(value: Float) = _uiState.update { it.copy(valueImpact = value) }

    override fun onEffortChange(value: Float) = _uiState.update { it.copy(effort = value) }

    override fun onCostChange(value: Float) = _uiState.update { it.copy(cost = value) }

    override fun onRiskChange(value: Float) = _uiState.update { it.copy(risk = value) }

    override fun onWeightEffortChange(value: Float) = _uiState.update { it.copy(weightEffort = value) }

    override fun onWeightCostChange(value: Float) = _uiState.update { it.copy(weightCost = value) }

    override fun onWeightRiskChange(value: Float) = _uiState.update { it.copy(weightRisk = value) }

    override fun onScoringStatusChange(newStatus: String) {
        _uiState.update { it.copy(scoringStatus = newStatus, isScoringEnabled = newStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS) }
    }

    fun openDescriptionEditor() = _uiState.update { it.copy(isDescriptionEditorOpen = true) }

    fun closeDescriptionEditor() = _uiState.update { it.copy(isDescriptionEditorOpen = false) }

    fun onDescriptionChangeAndCloseEditor(newDescription: String) {
        _uiState.update {
            it.copy(
                description = it.description.copy(text = newDescription),
                isDescriptionEditorOpen = false,
            )
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun onShowCheckboxesChange(show: Boolean) {
        _uiState.update { it.copy(showCheckboxes = show) }
    }

    fun onAddTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags + tag) }
    }

    fun onRemoveTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
    }

    fun onProjectManagementChange(enabled: Boolean) {
        _uiState.update { it.copy(isProjectManagementEnabled = enabled) }
    }

    fun onAutoLinkSubprojectsChange(enabled: Boolean) {
        _uiState.update {
            it.copy(
                autoLinkSubprojects = enabled,
                features = it.features + ("Auto link subprojects" to enabled)
            )
        }
    }

    fun onApplyPreset(code: String) {
        val pid = projectId ?: return
        viewModelScope.launch {
            structurePresetService.applyPresetToProject(pid, code)
            val label = structurePresetDao.getByCode(code)?.label
            val preset = structurePresetDao.getByCode(code)
            _uiState.update { state ->
                state.copy(
                    currentPresetLabel = label,
                    features = state.features + mapOf(
                        "Inbox" to (preset?.enableInbox ?: true),
                        "Log" to (preset?.enableLog ?: true),
                        "Artifact" to (preset?.enableArtifact ?: true),
                        "Advanced" to (preset?.enableAdvanced ?: false),
                        "Dashboard" to (preset?.enableDashboard ?: true),
                        "Backlog" to (preset?.enableBacklog ?: true),
                        "Attachments" to (preset?.enableAttachments ?: true),
                        "Auto link subprojects" to (preset?.enableAutoLinkSubprojects ?: true),
                    ),
                    autoLinkSubprojects = preset?.enableAutoLinkSubprojects ?: state.autoLinkSubprojects,
                    isProjectManagementEnabled = preset?.enableAdvanced ?: state.isProjectManagementEnabled
                )
            }
            persistFeatureFlags()
        }
    }

    fun onToggleFeature(key: String, enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                features = state.features + (key to enabled),
                isProjectManagementEnabled = if (key == "Advanced") enabled else state.isProjectManagementEnabled,
                autoLinkSubprojects = if (key == "Auto link subprojects") enabled else state.autoLinkSubprojects,
            )
        }
    }

    private suspend fun persistFeatureFlags() {
        val pid = projectId ?: return
        val structure = projectStructureRepository.ensureStructure(pid)
        val flags = _uiState.value.features + mapOf(
            "Inbox" to (_uiState.value.features["Inbox"] ?: true),
            "Log" to (_uiState.value.features["Log"] ?: true),
            "Artifact" to (_uiState.value.features["Artifact"] ?: true),
            "Advanced" to (_uiState.value.features["Advanced"] ?: false),
            "Auto link subprojects" to (_uiState.value.features["Auto link subprojects"] ?: true),
        )
        val updated = structure.copy(
            enableInbox = flags["Inbox"],
            enableLog = flags["Log"],
            enableArtifact = flags["Artifact"],
            enableAdvanced = flags["Advanced"],
            enableDashboard = flags["Dashboard"],
            enableBacklog = flags["Backlog"],
            enableAttachments = flags["Attachments"],
            enableAutoLinkSubprojects = flags["Auto link subprojects"],
        )
        projectStructureRepository.updateStructure(updated)
        _uiState.update { it.copy(isProjectManagementEnabled = flags["Advanced"] == true) }
    }

    override fun onSetReminder(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ) {
        val calendar =
            java.util.Calendar.getInstance().apply {
                set(year, month, day, hour, minute, 0)
            }
        val newReminderTime = calendar.timeInMillis
        _uiState.update { it.copy(reminderTime = newReminderTime) }

        projectId?.let {
            viewModelScope.launch {
                reminderRepository.createReminder(it, "PROJECT", newReminderTime)
            }
        }
    }

    override fun onClearReminder() {
        _uiState.update { it.copy(reminderTime = null) }
        projectId?.let {
            viewModelScope.launch {
                reminderRepository.clearRemindersForEntity(it)
            }
        }
    }

    fun onOpenStructure() {
        projectId?.let {
            viewModelScope.launch {
                _events.send(ProjectSettingsEvent.Navigate(com.romankozak.forwardappmobile.ui.navigation.NavTarget.ProjectStructure(it)))
            }
        }
    }
}
