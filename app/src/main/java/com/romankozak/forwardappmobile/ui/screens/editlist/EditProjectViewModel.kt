package com.romankozak.forwardappmobile.ui.screens.editlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.data.repository.ProjectRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class EditProjectUiState(
    val project: Project? = null,
    val name: String = "",
    val tags: List<String> = emptyList(),
    val reminderTime: Long? = null,
    val scoringStatus: ScoringStatus = ScoringStatus.NOT_ASSESSED,
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
)

@HiltViewModel
class EditProjectViewModel
@Inject
constructor(
    private val projectRepository: ProjectRepository,
    private val alarmScheduler: AlarmScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val projectId: String = checkNotNull(savedStateHandle["listId"])

    private val _uiState = MutableStateFlow(EditProjectUiState())
    val uiState: StateFlow<EditProjectUiState> = _uiState.asStateFlow()

    private var originalProject: Project? = null

    init {
        viewModelScope.launch {
            val loadedProject = projectRepository.getProjectById(projectId)
            originalProject = loadedProject
            _uiState.update {
                if (loadedProject != null) {
                    it.copy(
                        project = loadedProject,
                        name = loadedProject.name,
                        tags = loadedProject.tags?.filter { it.isNotBlank() } ?: emptyList(),
                        reminderTime = loadedProject.reminderTime,
                        scoringStatus = loadedProject.scoringStatus,
                        isScoringEnabled = loadedProject.scoringStatus != ScoringStatus.IMPOSSIBLE_TO_ASSESS,
                        valueImportance = loadedProject.valueImportance,
                        valueImpact = loadedProject.valueImpact,
                        effort = loadedProject.effort,
                        cost = loadedProject.cost,
                        risk = loadedProject.risk,
                        weightEffort = loadedProject.weightEffort,
                        weightCost = loadedProject.weightCost,
                        weightRisk = loadedProject.weightRisk,
                        rawScore = loadedProject.rawScore
                    )
                } else {
                    it
                }
            }
        }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onTagsChange(newTags: List<String>) {
        _uiState.update { it.copy(tags = newTags.filter { it.isNotBlank() }) }
    }

    fun onSave(): Project? {
        if (_uiState.value.name.isBlank()) return null

        val state = _uiState.value
        val currentProject = originalProject ?: return null

        val tempProject = currentProject.copy(
            name = state.name,
            tags = state.tags.filter { it.isNotBlank() }.map { it.trim() },
            updatedAt = System.currentTimeMillis(),
            reminderTime = state.reminderTime,
            scoringStatus = state.scoringStatus,
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk
        )

        val updatedProject = GoalScoringManager.calculateScoresForProject(tempProject)

        viewModelScope.launch {
            val oldReminderTime = originalProject?.reminderTime
            val newReminderTime = updatedProject.reminderTime
            if (newReminderTime != oldReminderTime) {
                if (newReminderTime != null) {
                    alarmScheduler.scheduleForProject(updatedProject)
                } else {
                    originalProject?.let { alarmScheduler.cancelForProject(it) }
                }
            }

            projectRepository.updateProject(updatedProject)
        }
        return updatedProject
    }

    fun onSetReminder(year: Int, month: Int, day: Int, hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(year, month, day, hour, minute, 0)
        }
        _uiState.update { it.copy(reminderTime = calendar.timeInMillis) }
    }

    fun onClearReminder() {
        _uiState.update { it.copy(reminderTime = null) }
    }

    fun onScoringStatusChange(newStatus: ScoringStatus) {
        _uiState.update { it.copy(scoringStatus = newStatus, isScoringEnabled = newStatus != ScoringStatus.IMPOSSIBLE_TO_ASSESS) }
        updateScores()
    }

    fun onValueImportanceChange(value: Float) = onScoringParameterChange { it.copy(valueImportance = value) }
    fun onValueImpactChange(value: Float) = onScoringParameterChange { it.copy(valueImpact = value) }
    fun onEffortChange(value: Float) = onScoringParameterChange { it.copy(effort = value) }
    fun onCostChange(value: Float) = onScoringParameterChange { it.copy(cost = value) }
    fun onRiskChange(value: Float) = onScoringParameterChange { it.copy(risk = value) }
    fun onWeightEffortChange(value: Float) = onScoringParameterChange { it.copy(weightEffort = value) }
    fun onWeightCostChange(value: Float) = onScoringParameterChange { it.copy(weightCost = value) }
    fun onWeightRiskChange(value: Float) = onScoringParameterChange { it.copy(weightRisk = value) }

    private fun onScoringParameterChange(update: (EditProjectUiState) -> EditProjectUiState) {
        _uiState.update(update)
        if (_uiState.value.scoringStatus == ScoringStatus.NOT_ASSESSED) {
            _uiState.update { it.copy(scoringStatus = ScoringStatus.ASSESSED) }
        }
        updateScores()
    }

    private fun updateScores() {
        val state = _uiState.value
        val tempProject = (state.project ?: return).copy(
            scoringStatus = state.scoringStatus,
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk
        )
        val updatedProject = GoalScoringManager.calculateScoresForProject(tempProject)
        _uiState.update { it.copy(rawScore = updatedProject.rawScore) }
    }
}