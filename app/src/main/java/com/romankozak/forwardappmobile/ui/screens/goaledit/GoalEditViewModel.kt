// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/goaledit/GoalEditViewModel.kt

package com.romankozak.forwardappmobile.ui.screens.goaledit

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.GoalInstance
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.ui.utils.HierarchyFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed class GoalEditEvent {
    data class NavigateBack(val message: String? = null) : GoalEditEvent()
}

data class GoalEditUiState(
    val goalDescription: TextFieldValue = TextFieldValue(""),
    val associatedLists: List<GoalList> = emptyList(),
    val isReady: Boolean = false,
    val isNewGoal: Boolean = true,
    val showListChooser: Boolean = false,
    val isScoringEnabled: Boolean = true,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val valueImportance: Float = 0f,
    val valueImpact: Float = 0f,
    val effort: Float = 0f,
    val cost: Float = 0f,
    val risk: Float = 0f,
    val weightEffort: Float = 1f,
    val weightCost: Float = 1f,
    val weightRisk: Float = 1f,
    val scoringStatus: ScoringStatus = ScoringStatus.NOT_ASSESSED,
    val rawScore: Float = 0f,
    val displayScore: Int = 0,
    val goalText: TextFieldValue = TextFieldValue(""),
    val isDescriptionEditorOpen: Boolean = false,
)

@HiltViewModel
class GoalEditViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val settingsRepository: SettingsRepository,
    private val contextHandler: ContextHandler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val goalId: String? = savedStateHandle["goalId"]
    private val initialListId: String? = savedStateHandle["listId"]

    private val _uiState = MutableStateFlow(GoalEditUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<GoalEditEvent>()
    val events = _events.receiveAsFlow()

    private var currentGoal: Goal? = null

    var markdown: String = "## Початковий текст"
        private set

    private var saveJob: Job? = null


    val listHierarchy: StateFlow<ListHierarchyData> = goalRepository.getAllGoalListsFlow()
        .map { allLists ->
            val topLevel = allLists.filter { it.parentId == null }.sortedBy { it.order }
            val childMap = allLists.filter { it.parentId != null }.groupBy { it.parentId!! }
            ListHierarchyData(allLists, topLevel, childMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    private val _listChooserUserExpandedIds = MutableStateFlow<Set<String>>(emptySet())
    val listChooserUserExpandedIds = _listChooserUserExpandedIds.asStateFlow()

    private val _listChooserFilterText = MutableStateFlow("")
    val listChooserFilterText = _listChooserFilterText.asStateFlow()

    val fullHierarchyForDialog: StateFlow<ListHierarchyData> = listHierarchy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    val filteredListHierarchy: StateFlow<ListHierarchyData> =
        combine(listChooserFilterText, fullHierarchyForDialog) { filter, originalHierarchy ->
            HierarchyFilter.filter(originalHierarchy, filter)
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListHierarchyData())

    val listChooserFinalExpandedIds: StateFlow<Set<String>> = combine(
        listChooserFilterText,
        filteredListHierarchy,
        listChooserUserExpandedIds
    ) { filter, filteredHierarchy, userExpanded ->
        if (filter.isBlank()) {
            userExpanded
        } else {
            val forcedExpansion = filteredHierarchy.childMap.keys
            userExpanded + forcedExpansion
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())


    private val contextTagMap = mutableMapOf<String, String>()

    init {
        viewModelScope.launch {
            contextHandler.initialize()
            if (goalId != null) {
                loadExistingGoal(goalId)
            } else {
                createNewGoal()
            }
        }
    }

    fun onListChooserFilterChanged(text: String) {
        _listChooserFilterText.value = text
    }

    fun onListChooserToggleExpanded(listId: String) {
        val currentIds = _listChooserUserExpandedIds.value.toMutableSet()
        if (listId in currentIds) {
            currentIds.remove(listId)
        } else {
            currentIds.add(listId)
        }
        _listChooserUserExpandedIds.value = currentIds
    }

    fun onShowListChooser(initialSelectedId: String? = null) {
        _uiState.update { it.copy(showListChooser = true) }

        if (initialSelectedId != null) {
            viewModelScope.launch(Dispatchers.Default) {
                val allListsById = listHierarchy.value.allLists.associateBy { it.id }
                val ancestorIds = mutableSetOf<String>()
                var currentId: String? = initialSelectedId
                while (currentId != null) {
                    val parentId = allListsById[currentId]?.parentId
                    parentId?.let { ancestorIds.add(it) }
                    currentId = parentId
                }
                if (ancestorIds.isNotEmpty()) {
                    val currentExpanded = _listChooserUserExpandedIds.value.toMutableSet()
                    currentExpanded.addAll(ancestorIds)
                    _listChooserUserExpandedIds.value = currentExpanded
                }
            }
        }
    }

    fun onDismissListChooser() {
        _uiState.update { it.copy(showListChooser = false) }
        onListChooserFilterChanged("")
        _listChooserUserExpandedIds.value = emptySet()
    }

    fun addNewList(id: String, parentId: String?, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            goalRepository.createGoalListWithId(id, name, parentId)

            if (parentId != null) {
                val allLists = fullHierarchyForDialog.value.allLists
                val listLookup = allLists.associateBy { it.id }
                val ancestorIds = findAncestorIds(parentId, listLookup)
                _listChooserUserExpandedIds.update { currentIds ->
                    currentIds + ancestorIds
                }
            }
        }
    }

    private fun findAncestorIds(startId: String?, listLookup: Map<String, GoalList>): Set<String> {
        val ancestors = mutableSetOf<String>()
        var currentId = startId
        while (currentId != null && listLookup.containsKey(currentId)) {
            ancestors.add(currentId)
            currentId = listLookup[currentId]?.parentId
        }
        return ancestors
    }

    val allContextNames: StateFlow<List<String>> = contextHandler.contextNamesFlow
    private suspend fun loadExistingGoal(goalId: String) {
        val goal = goalRepository.getGoalById(goalId)
        if (goal != null) {
            currentGoal = goal
            val associatedIds = goal.associatedListIds ?: emptyList()
            val lists = if (associatedIds.isNotEmpty()) goalRepository.getListsByIds(associatedIds) else emptyList()

            _uiState.update {
                it.copy(
                    goalText = TextFieldValue(goal.text),
                    goalDescription = TextFieldValue(goal.description ?: ""),
                    associatedLists = lists,
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
                    isScoringEnabled = goal.scoringStatus != ScoringStatus.IMPOSSIBLE_TO_ASSESS
                )
            }
        } else {
            _events.send(GoalEditEvent.NavigateBack("Ціль не знайдено"))
        }
    }

    private fun createNewGoal() {
        _uiState.update {
            it.copy(isReady = true, isNewGoal = true, scoringStatus = ScoringStatus.NOT_ASSESSED, isScoringEnabled = true)
        }
        updateScores()
    }

    fun onTextChange(newValue: TextFieldValue) {
        _uiState.update { it.copy(goalText = newValue) }
    }

    fun onDescriptionChange(newValue: TextFieldValue) {
        _uiState.update { it.copy(goalDescription = newValue) }
    }


    fun onValueImportanceChange(value: Float) = onScoringParameterChange { it.copy(valueImportance = value) }
    fun onValueImpactChange(value: Float) = onScoringParameterChange { it.copy(valueImpact = value) }
    fun onEffortChange(value: Float) = onScoringParameterChange { it.copy(effort = value) }
    fun onCostChange(value: Float) = onScoringParameterChange { it.copy(cost = value) }
    fun onRiskChange(value: Float) = onScoringParameterChange { it.copy(risk = value) }
    fun onWeightEffortChange(value: Float) = onScoringParameterChange { it.copy(weightEffort = value) }
    fun onWeightCostChange(value: Float) = onScoringParameterChange { it.copy(weightCost = value) }
    fun onWeightRiskChange(value: Float) = onScoringParameterChange { it.copy(weightRisk = value) }

    fun onScoringStatusChange(newStatus: ScoringStatus) {
        _uiState.update { currentState ->
            var nextState = currentState.copy(
                scoringStatus = newStatus,
                isScoringEnabled = newStatus != ScoringStatus.IMPOSSIBLE_TO_ASSESS
            )
            if (newStatus == ScoringStatus.NOT_ASSESSED || newStatus == ScoringStatus.IMPOSSIBLE_TO_ASSESS) {
                nextState = nextState.copy(
                    valueImportance = 0f,
                    valueImpact = 0f,
                    effort = 0f,
                    cost = 0f,
                    risk = 0f,
                    weightEffort = 1f,
                    weightCost = 1f,
                    weightRisk = 1f
                )
            }
            nextState
        }
        updateScores()
    }

    private fun onScoringParameterChange(update: (GoalEditUiState) -> GoalEditUiState) {
        _uiState.update { currentState ->
            val nextState = update(currentState)
            if (currentState.scoringStatus == ScoringStatus.NOT_ASSESSED) {
                nextState.copy(scoringStatus = ScoringStatus.ASSESSED)
            } else {
                nextState
            }
        }
        updateScores()
    }

    private fun updateScores() {
        val tempGoal = buildGoalFromState(_uiState.value)
        val updatedGoal = GoalScoringManager.calculateScores(tempGoal)
        _uiState.update {
            it.copy(
                rawScore = updatedGoal.rawScore,
                displayScore = updatedGoal.displayScore
            )
        }
    }

    fun onAddListAssociation(listId: String) {
        viewModelScope.launch {
            if (_uiState.value.associatedLists.any { it.id == listId }) return@launch
            val listToAdd = listHierarchy.value.allLists.find { it.id == listId }
            if (listToAdd != null) {
                _uiState.update {
                    it.copy(associatedLists = it.associatedLists + listToAdd)
                }
            }
        }
        onDismissListChooser()
    }

    fun onRemoveListAssociation(listId: String) {
        _uiState.update {
            it.copy(associatedLists = it.associatedLists.filterNot { list -> list.id == listId })
        }
    }

    private fun buildGoalFromState(state: GoalEditUiState): Goal {
        val currentTime = System.currentTimeMillis()
        val descriptionToSave = state.goalDescription.text.ifEmpty { null }

        return currentGoal?.copy(
            text = state.goalText.text,
            description = descriptionToSave,
            updatedAt = currentTime,
            associatedListIds = state.associatedLists.map { it.id },
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk,
            scoringStatus = state.scoringStatus
        ) ?: Goal(
            id = UUID.randomUUID().toString(),
            text = state.goalText.text,
            description = descriptionToSave,
            completed = false,
            createdAt = currentTime,
            updatedAt = currentTime,
            tags = null,
            associatedListIds = state.associatedLists.map { it.id },
            valueImportance = state.valueImportance,
            valueImpact = state.valueImpact,
            effort = state.effort,
            cost = state.cost,
            risk = state.risk,
            weightEffort = state.weightEffort,
            weightCost = state.weightCost,
            weightRisk = state.weightRisk,
            scoringStatus = state.scoringStatus
        )
    }

    fun onSave() {
        android.util.Log.d("ContextDebug", "SAVE ACTION TRIGGERED. Is new goal = ${uiState.value.isNewGoal}")
        viewModelScope.launch {
            if (_uiState.value.goalText.text.isBlank()) {
                _events.send(GoalEditEvent.NavigateBack("Назва цілі не може бути пустою"))
                return@launch
            }

            val goalFromState = buildGoalFromState(_uiState.value)
            val goalToSave = GoalScoringManager.calculateScores(goalFromState)

            if (currentGoal != null) {
                goalRepository.updateGoal(goalToSave)
                contextHandler.syncContextsOnUpdate(oldGoal = currentGoal!!, newGoal = goalToSave)
            } else {
                val listIdForNewGoal = initialListId ?: return@launch

                val finalGoal = if (goalToSave.associatedListIds.isNullOrEmpty()){
                    goalToSave.copy(associatedListIds = listOf(listIdForNewGoal))
                } else goalToSave

                goalRepository.insertGoal(finalGoal)

                // ✨ ВИПРАВЛЕНО: Використовуємо негативний timestamp, щоб розмістити нову ціль нагорі списку
                val order = -System.currentTimeMillis()
                val newInstance = GoalInstance(
                    instanceId = UUID.randomUUID().toString(),
                    goalId = finalGoal.id,
                    listId = listIdForNewGoal,
                    order = order
                )
                goalRepository.insertInstance(newInstance)
                contextHandler.handleContextsOnCreate(finalGoal)
            }

            _events.send(GoalEditEvent.NavigateBack("Збережено"))
        }
    }

    private suspend fun loadContextSettings() {
        android.util.Log.d("ContextDebug", "Starting to load context settings...")
        val contextKeysList = listOf(
            SettingsRepository.ContextKeys.BUY,
            SettingsRepository.ContextKeys.PM,
            SettingsRepository.ContextKeys.PAPER,
            SettingsRepository.ContextKeys.MENTAL,
            SettingsRepository.ContextKeys.PROVIDENCE,
            SettingsRepository.ContextKeys.MANUAL,
            SettingsRepository.ContextKeys.RESEARCH,
            SettingsRepository.ContextKeys.DEVICE
        )

        android.util.Log.d("ContextDebug", "Processing an explicit list of ${contextKeysList.size} keys.")

        val deferreds = contextKeysList.map { key ->
            viewModelScope.async(Dispatchers.IO) {
                try {
                    val contextName = key.name.removePrefix("context_tag_")
                    val tag = settingsRepository.getContextTagFlow(key).first()

                    android.util.Log.d("ContextDebug", "Loading -> Key: '${key.name}', Context: '$contextName', Tag: '$tag'")
                    contextTagMap[contextName] = tag

                } catch (e: Exception) {
                    android.util.Log.e("ContextDebug", "Error loading a context setting for key ${key.name}", e)
                }
            }
        }
        deferreds.awaitAll()

        android.util.Log.d("ContextDebug", "Finished loading settings. Final contextTagMap: $contextTagMap")
    }

    private suspend fun handleContextBasedInstanceCreation(goal: Goal) {
        val regex = "@\\{?(\\w+)\\}?".toRegex()

        val matches = regex.findAll(goal.text)
        val contexts = matches.map { it.groupValues[1] }.toSet()

        android.util.Log.d("ContextDebug", "Goal: '${goal.text}'. Found contexts: $contexts")
        if (contexts.isEmpty()) return

        contexts.forEach { contextName ->
            val tag = contextTagMap[contextName]
            android.util.Log.d("ContextDebug", "Processing context: '$contextName'. Found tag in map: '$tag'")
            if (tag != null) {
                val targetListIds = goalRepository.findListIdsByTag(tag)
                targetListIds.forEach { listId ->
                    val exists = goalRepository.doesInstanceExist(goal.id, listId)
                    android.util.Log.d("ContextDebug", "Checking list '$listId'. Instance exists: $exists")
                    if (!exists) {
                        android.util.Log.d("ContextDebug", "CREATING INSTANCE for goal ${goal.id} in list $listId")
                        // ✨ ВИПРАВЛЕНО: Використовуємо негативний timestamp, щоб розмістити новий екземпляр нагорі списку
                        val order = -System.currentTimeMillis()
                        val newInstance = GoalInstance(
                            instanceId = UUID.randomUUID().toString(),
                            goalId = goal.id,
                            listId = listId,
                            order = order
                        )
                        goalRepository.insertInstance(newInstance)
                    }
                }
            }
        }
    }

    fun openDescriptionEditor() {
        _uiState.update { it.copy(isDescriptionEditorOpen = true) }
    }

    fun closeDescriptionEditor() {
        _uiState.update { it.copy(isDescriptionEditorOpen = false) }
    }

    fun onDescriptionChangeAndCloseEditor(newDescription: String) {
        _uiState.update {
            it.copy(
                goalDescription = it.goalDescription.copy(text = newDescription),
                isDescriptionEditorOpen = false
            )
        }
    }

    fun updateMarkdown(newText: String, goalId: Long) {
        markdown = newText

        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(1000L) // debounce 1 сек
            goalRepository.updateMarkdown(goalId, newText)
        }
    }
}