package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.data.recurrence.CanonicalTaskRecurrenceAuthoringAdapter
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

data class EditTaskUiState(
    val task: DayTask? = null,
    val title: String = "",
    val description: String = "",
    val priority: TaskPriority = TaskPriority.NONE,
    val duration: Long? = null,
    val scheduledTime: Long? = null,
    val dueTime: Long? = null,
    val executionStrictness: TaskExecutionStrictness = TaskExecutionStrictness.NORMAL,
    val points: Int = 0,
    val dayAnchorTime: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule? = null,
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: Set<DayOfWeek> = emptySet(),
    val contextLinks: List<TaskContextLinkUi> = emptyList(),
)

sealed class EditTaskUiEvent {
    object NavigateUp : EditTaskUiEvent()
}

data class TaskContextLinkUi(
    val id: String,
    val name: String,
)

@HiltViewModel
class EditTaskViewModel
    @Inject
    constructor(
        private val dayManagementRepository: DayManagementRepository,
        private val canonicalTaskRecurrenceAuthoringAdapter: CanonicalTaskRecurrenceAuthoringAdapter,
        private val contextRepository: ContextRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(EditTaskUiState())
        val uiState = _uiState.asStateFlow()
        private var loadedTaskId: String? = null

        private val _uiEvent = Channel<EditTaskUiEvent>()
        val uiEvent = _uiEvent.receiveAsFlow()

        init {
            loadTask(savedStateHandle.get<String>("taskId"))
        }

        fun loadTask(taskId: String?) {
            if (taskId.isNullOrBlank()) {
                loadedTaskId = null
                _uiState.value = EditTaskUiState()
                return
            }
            if (loadedTaskId == taskId && _uiState.value.task?.id == taskId) return

            loadedTaskId = taskId
            viewModelScope.launch {
                val task = dayManagementRepository.getTaskById(taskId)
                val canonicalSeries =
                    task?.let { canonicalTaskRecurrenceAuthoringAdapter.getSeriesForOccurrence(it) }
                val recurrenceRule = canonicalSeries?.rule?.toAndroidTaskRecurrenceRule()
                val contextLinks = task?.linkedProjectIds.normalizedContextIds().resolveContextLinks()
                _uiState.value =
                    EditTaskUiState(
                        task = task,
                        title = task?.title ?: "",
                        description = task?.description ?: "",
                        priority = task?.priority ?: TaskPriority.NONE,
                        duration = task?.estimatedDurationMinutes,
                        scheduledTime = task?.scheduledTime,
                        dueTime = task?.dueTime,
                        executionStrictness = task?.executionStrictness ?: TaskExecutionStrictness.NORMAL,
                        points = task?.points ?: 0,
                        dayAnchorTime =
                            task
                                ?.dayPlanId
                                ?.let { dayManagementRepository.getPlanById(it)?.date }
                                ?: System.currentTimeMillis(),
                        isRecurring = canonicalSeries != null,
                        recurrenceRule = recurrenceRule,
                        recurrenceFrequency = recurrenceRule?.frequency ?: RecurrenceFrequency.DAILY,
                        recurrenceInterval = recurrenceRule?.interval ?: 1,
                        recurrenceDaysOfWeek = recurrenceRule?.daysOfWeek?.toSet() ?: emptySet(),
                        contextLinks = contextLinks,
                    )
            }
        }

        fun reset() {
            loadedTaskId = null
            _uiState.value = EditTaskUiState()
        }

        fun onTitleChange(title: String) {
            _uiState.value = _uiState.value.copy(title = title)
        }

        fun onDescriptionChange(description: String) {
            _uiState.value = _uiState.value.copy(description = description)
        }

        fun onPriorityChange(priority: TaskPriority) {
            _uiState.value = _uiState.value.copy(priority = priority)
        }

        fun onDurationChange(duration: Long?) {
            _uiState.value = _uiState.value.copy(duration = duration)
        }

        fun onScheduledTimeChange(scheduledTime: Long?) {
            _uiState.value = _uiState.value.copy(scheduledTime = scheduledTime)
        }

        fun onDueTimeChange(dueTime: Long?) {
            _uiState.value = _uiState.value.copy(dueTime = dueTime)
        }

        fun onExecutionStrictnessChange(strictness: TaskExecutionStrictness) {
            _uiState.value = _uiState.value.copy(executionStrictness = strictness)
        }

        fun onPointsChange(points: Int) {
            _uiState.value = _uiState.value.copy(points = points)
        }

        fun onRecurringChange(isRecurring: Boolean) {
            _uiState.value = _uiState.value.copy(isRecurring = isRecurring)
        }

        fun onRecurrenceFrequencyChange(frequency: RecurrenceFrequency) {
            _uiState.value = _uiState.value.copy(recurrenceFrequency = frequency)
        }

        fun onRecurrenceIntervalChange(interval: Int) {
            _uiState.value = _uiState.value.copy(recurrenceInterval = interval.coerceAtLeast(1))
        }

        fun onRecurrenceDayOfWeekToggle(day: DayOfWeek) {
            val days = _uiState.value.recurrenceDaysOfWeek.toMutableSet()
            if (days.contains(day)) {
                days.remove(day)
            } else {
                days.add(day)
            }
            _uiState.value = _uiState.value.copy(recurrenceDaysOfWeek = days)
        }

        fun onContextChooserResult(contextId: String?) {
            val normalizedId = contextId?.takeUnless { it == "root" }?.takeIf { it.isNotBlank() } ?: return
            viewModelScope.launch {
                val currentLinks = _uiState.value.contextLinks
                if (currentLinks.any { it.id == normalizedId }) return@launch
                val context = contextRepository.getContextById(normalizedId) ?: return@launch
                _uiState.value =
                    _uiState.value.copy(
                        contextLinks = currentLinks + TaskContextLinkUi(id = context.id, name = context.name),
                    )
            }
        }

        fun removeContextLink(contextId: String) {
            _uiState.value =
                _uiState.value.copy(
                    contextLinks = _uiState.value.contextLinks.filterNot { it.id == contextId },
                )
        }

        fun saveTask() {
            viewModelScope.launch {
                val state = _uiState.value
                val originalTask = state.task ?: return@launch

                if (state.isRecurring) {
                    saveRecurringTask(state, originalTask)
                } else {
                    saveNonRecurringTask(state, originalTask)
                }
                _uiEvent.send(EditTaskUiEvent.NavigateUp)
            }
        }
        private suspend fun saveRecurringTask(
            state: EditTaskUiState,
            originalTask: DayTask,
        ) {
            val canonicalRule = buildRecurrenceRule(state).toCanonicalTaskRecurrenceRule()

            if (originalTask.recurrenceSeriesId != null) {
                val storedSeries =
                    requireNotNull(
                        canonicalTaskRecurrenceAuthoringAdapter.getSeriesForOccurrence(originalTask),
                    ) {
                        "Canonical recurring task series not found for ${originalTask.id}"
                    }

                val updatedOccurrence =
                    if (storedSeries.rule == canonicalRule) {
                        canonicalTaskRecurrenceAuthoringAdapter.updateSeriesTemplate(
                            task = originalTask,
                            title = state.title,
                            description = state.description,
                            goalId = originalTask.goalId,
                            projectId = originalTask.projectId,
                            taskType = originalTask.taskType,
                            linkedProjectIds = state.contextLinks.map { it.id },
                            linkedAttachmentIds = originalTask.linkedAttachmentIds.orEmpty(),
                            priority = state.priority,
                            estimatedDurationMinutes = state.duration,
                            points = state.points,
                            executionStrictness = state.executionStrictness,
                        )
                    } else {
                        canonicalTaskRecurrenceAuthoringAdapter.splitSeriesFromOccurrence(
                            task = originalTask,
                            title = state.title,
                            description = state.description,
                            goalId = originalTask.goalId,
                            projectId = originalTask.projectId,
                            taskType = originalTask.taskType,
                            linkedProjectIds = state.contextLinks.map { it.id },
                            linkedAttachmentIds = originalTask.linkedAttachmentIds.orEmpty(),
                            priority = state.priority,
                            estimatedDurationMinutes = state.duration,
                            points = state.points,
                            executionStrictness = state.executionStrictness,
                            rule = canonicalRule,
                        )
                    }

                updateTaskInstance(state, updatedOccurrence)
                return
            }

            val convertedOccurrence =
                canonicalTaskRecurrenceAuthoringAdapter.convertOneOffToSeries(
                    task = originalTask,
                    title = state.title,
                    description = state.description,
                    goalId = originalTask.goalId,
                    projectId = originalTask.projectId,
                    taskType = originalTask.taskType,
                    linkedProjectIds = state.contextLinks.map { it.id },
                    linkedAttachmentIds = originalTask.linkedAttachmentIds.orEmpty(),
                    priority = state.priority,
                    estimatedDurationMinutes = state.duration,
                    points = state.points,
                    executionStrictness = state.executionStrictness,
                    rule = canonicalRule,
                )
            updateTaskInstance(state, convertedOccurrence)
        }
        private suspend fun saveNonRecurringTask(
            state: EditTaskUiState,
            originalTask: DayTask,
        ) {
            if (originalTask.recurrenceSeriesId != null) {
                val detachedTask =
                    canonicalTaskRecurrenceAuthoringAdapter.detachCurrentOccurrence(originalTask)
                updateTaskInstance(state, detachedTask)
                return
            }

            updateTaskInstance(state, originalTask)
        }

        private suspend fun updateTaskInstance(
            state: EditTaskUiState,
            originalTask: DayTask,
        ) {
            dayManagementRepository.updateTask(
                DayManagementRepository.UpdateTaskParams(
                    taskId = originalTask.id,
                    title = state.title,
                    description = state.description,
                    priority = state.priority,
                    duration = state.duration,
                    scheduledTime = state.scheduledTime,
                    dueTime = state.dueTime,
                    executionStrictness = state.executionStrictness,
                    points = state.points,
                    projectId = originalTask.projectId,
                    linkedProjectIds = state.contextLinks.map { it.id },
                    updateContextLinks = true,
                ),
            )
        }

        private fun List<String>?.normalizedContextIds(): List<String> =
            orEmpty()
                .map(String::trim)
                .filter { it.isNotBlank() && it != "root" }
                .distinct()

        private suspend fun List<String>.resolveContextLinks(): List<TaskContextLinkUi> =
            mapNotNull { id ->
                contextRepository.getContextById(id)?.let { context ->
                    TaskContextLinkUi(id = context.id, name = context.name)
                }
            }

        private fun buildRecurrenceRule(state: EditTaskUiState): RecurrenceRule =
            RecurrenceRule(
                frequency = state.recurrenceFrequency,
                interval = state.recurrenceInterval.coerceAtLeast(1),
                daysOfWeek =
                    if (state.recurrenceFrequency == RecurrenceFrequency.WEEKLY) {
                        state.recurrenceDaysOfWeek
                            .takeIf { it.isNotEmpty() }
                            ?.sortedBy { it.value }
                    } else {
                        null
                    },
            )
    }
