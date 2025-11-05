package com.romankozak.forwardappmobile.ui.screens.goalsettings

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.*
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.features.projects.data.ProjectRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.ui.screens.projectsettings.ProjectSettingsEvent
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.TagUtils
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
import java.net.URLEncoder
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GoalSettingsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val projectRepository: ProjectRepository,
    private val contextHandler: ContextHandler,
    private val reminderRepository: ReminderRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), EvaluationTabActions, RemindersTabActions {

    private val goalId: String? = savedStateHandle["goalId"]
    private val initialProjectId: String? = savedStateHandle["projectId"]

    private val _uiState = MutableStateFlow(GoalSettingsUiState())
    val uiState: StateFlow<GoalSettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProjectSettingsEvent>()
    val events = _events.receiveAsFlow()

    private var currentGoal: Goal? = null

    val allContextNames: StateFlow<List<String>> = contextHandler.contextNamesFlow

    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

    init {
        viewModelScope.launch {
            val goalId: String? = savedStateHandle["goalId"]

            contextHandler.initialize()
            loadAvailableTags()

            if (goalId != null) {
                loadExistingGoal(goalId)
                reminderRepository.getRemindersForEntityFlow(goalId).collect { reminders ->
                    _uiState.update { it.copy(reminderTime = reminders.firstOrNull()?.reminderTime) }
                }
            } else {
                createNewGoal()
            }
        }
    }

    private suspend fun loadAvailableTags() {
        try {
            val allGoals = goalRepository.getAllGoals()
            val allTagsFromGoals =
                allGoals.flatMap { goal ->
                    TagUtils.extractTags(goal.text).map { it.fullTag }
                }.distinct().sorted()

            _allTags.value = allTagsFromGoals
        } catch (e: Exception) {
            Log.e("GoalSettingsVM", "Error loading tags", e)
            _allTags.value = emptyList()
        }
    }

    fun onListChooserResult(projectId: String) {
        if (projectId.isBlank()) return
        onAddProjectAssociation(projectId)
    }

    private suspend fun loadExistingGoal(goalId: String) {
        val goal = goalRepository.getGoalById(goalId)
        if (goal != null) {
            currentGoal = goal
            _uiState.update {
                it.copy(
                    title = it.title.copy(goal.text),
                    description = it.description.copy(goal.description ?: ""),
                    relatedLinks = goal.relatedLinks ?: emptyList(),
                    isReady = true,
                    isNewGoal = false,
                    createdAt = goal.createdAt,
                    updatedAt = goal.updatedAt,
                    valueImportance = goal.valueImportance,
                    valueImpact = goal.valueImpact,
                    effort = goal.effort,
                    cost = goal.cost,
                    risk = goal.risk,
                    weightEffort = goal.weightEffort,
                    weightCost = goal.weightCost,
                    weightRisk = goal.weightRisk,
                    rawScore = goal.rawScore,
                    displayScore = goal.displayScore,
                    scoringStatus = goal.scoringStatus,
                    isScoringEnabled = goal.scoringStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS,
                )
            }
        } else {
            // TODO: _events.send(ProjectSettingsEvent.NavigateBack("Ціль не знайдено"))
        }
    }

    private fun createNewGoal() {
        _uiState.update {
            it.copy(
                isReady = true,
                isNewGoal = true,
                scoringStatus = ScoringStatusValues.NOT_ASSESSED,
                isScoringEnabled = true,
            )
        }
        updateScores()
    }

    fun onSave() {
        viewModelScope.launch {
            if (_uiState.value.title.text.isBlank()) {
                // TODO: _events.send(ProjectSettingsEvent.NavigateBack("Назва цілі не може бути пустою"))
                return@launch
            }

            saveGoal()

            loadAvailableTags()

            _events.send(ProjectSettingsEvent.NavigateBack("Збережено"))
        }
    }

    private suspend fun saveGoal() {
        val goalFromState = buildGoalFromState(_uiState.value)
        val goalToSave = GoalScoringManager.calculateScores(goalFromState)

        if (currentGoal != null) {
            goalRepository.updateGoal(goalToSave)
            contextHandler.syncContextsOnUpdate(oldGoal = currentGoal!!, newGoal = goalToSave)
        } else {
            initialProjectId ?: return
            goalRepository.addGoalToProject(goalToSave.text, initialProjectId)
        }
    }

    private fun buildGoalFromState(state: GoalSettingsUiState): Goal {
        val currentTime = System.currentTimeMillis()
        val descriptionToSave = state.description.text.ifEmpty { null }

        val baseGoal =
            currentGoal ?: Goal(
                id = UUID.randomUUID().toString(),
                text = "",
                completed = false,
                createdAt = currentTime,
                updatedAt = currentTime,
            )

        return baseGoal.copy(
            text = state.title.text,
            description = descriptionToSave,
            updatedAt = currentTime,
            relatedLinks = state.relatedLinks,
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk,
            scoringStatus = state.scoringStatus,
        )
    }

    fun onTextChange(newValue: androidx.compose.ui.text.input.TextFieldValue) = _uiState.update { it.copy(title = newValue) }

    fun onDescriptionChange(newValue: androidx.compose.ui.text.input.TextFieldValue) = _uiState.update { it.copy(description = newValue) }

    override fun onValueImportanceChange(value: Float) = onScoringParameterChange { it.copy(valueImportance = value) }

    override fun onValueImpactChange(value: Float) = onScoringParameterChange { it.copy(valueImpact = value) }

    override fun onEffortChange(value: Float) = onScoringParameterChange { it.copy(effort = value) }

    override fun onCostChange(value: Float) = onScoringParameterChange { it.copy(cost = value) }

    override fun onRiskChange(value: Float) = onScoringParameterChange { it.copy(risk = value) }

    override fun onWeightEffortChange(value: Float) = onScoringParameterChange { it.copy(weightEffort = value) }

    override fun onWeightCostChange(value: Float) = onScoringParameterChange { it.copy(weightCost = value) }

    override fun onWeightRiskChange(value: Float) = onScoringParameterChange { it.copy(weightRisk = value) }

    override fun onScoringStatusChange(newStatus: String) {
        _uiState.update { it.copy(scoringStatus = newStatus, isScoringEnabled = newStatus != ScoringStatusValues.IMPOSSIBLE_TO_ASSESS) }
        updateScores()
    }

    private fun onScoringParameterChange(update: (GoalSettingsUiState) -> GoalSettingsUiState) {
        _uiState.update(update)
        if (_uiState.value.scoringStatus == ScoringStatusValues.NOT_ASSESSED) {
            _uiState.update { it.copy(scoringStatus = ScoringStatusValues.ASSESSED) }
        }
        updateScores()
    }

    private fun updateScores() {
        val tempGoal = buildGoalFromState(_uiState.value)
        val updatedGoal = GoalScoringManager.calculateScores(tempGoal)
        _uiState.update { it.copy(rawScore = updatedGoal.rawScore, displayScore = updatedGoal.displayScore) }
    }

    fun onAddLinkRequest() {
        viewModelScope.launch {
            val disabledIds =
                _uiState.value.relatedLinks
                    .filter { it.type == LinkType.PROJECT }
                    .joinToString(",") { it.target }
            val title = URLEncoder.encode("Додати посилання на проект", "UTF-8")
            _events.send(ProjectSettingsEvent.Navigate("list_chooser_screen/$title?disabledIds=$disabledIds"))
        }
    }

    private fun onAddProjectAssociation(projectId: String) {
        viewModelScope.launch {
            val projectName = projectRepository.getProjectById(projectId)?.name
            val newLink =
                RelatedLink(
                    type = LinkType.PROJECT,
                    target = projectId,
                    displayName = projectName,
                )

            _uiState.update {
                if (it.relatedLinks.any { link -> link.target == projectId && link.type == LinkType.PROJECT }) {
                    it
                } else {
                    it.copy(relatedLinks = it.relatedLinks + newLink)
                }
            }
        }
    }

    fun onRemoveLinkAssociation(targetToRemove: String) {
        _uiState.update {
            it.copy(relatedLinks = it.relatedLinks.filterNot { link -> link.target == targetToRemove })
        }
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

    fun onAddWebLinkRequest() {
        viewModelScope.launch {
            _events.send(ProjectSettingsEvent.NavigateBack("Додавання веб-посилань буде реалізовано пізніше"))
        }
    }

    fun onAddObsidianLinkRequest() {
        viewModelScope.launch {
            _events.send(ProjectSettingsEvent.NavigateBack("Додавання Obsidian посилань буде реалізовано пізніше"))
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
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

        goalId?.let {
            viewModelScope.launch {
                reminderRepository.createReminder(it, "GOAL", newReminderTime)
            }
        }
    }

    override fun onClearReminder() {
        _uiState.update { it.copy(reminderTime = null) }
        goalId?.let {
            viewModelScope.launch {
                reminderRepository.clearRemindersForEntity(it)
            }
        }
    }
}