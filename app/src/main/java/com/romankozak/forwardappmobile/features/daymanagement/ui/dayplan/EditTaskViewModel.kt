package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.logicalProjectId
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.data.recurrence.CanonicalTaskRecurrenceAuthoringAdapter
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.core.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import javax.inject.Inject

data class EditTaskUiState(
    val task: DayTask? = null,
    val title: String = "",
    val description: String = "",
    val notes: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
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
    val isLoadingDetails: Boolean = false,
    val loadError: String? = null,
)

sealed class EditTaskUiEvent {
    data class NavigateUp(val taskId: String) : EditTaskUiEvent()
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
        private val systemWorkspacePresentationContextProjector: SystemWorkspacePresentationContextProjector,
        private val savedStateHandle: SavedStateHandle,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(EditTaskUiState())
        val uiState = _uiState.asStateFlow()
        private var loadedTaskId: String? = null
        private var loadedState: EditTaskUiState? = null
        private var completedSaveTaskId: String? = null
        private var loadJob: Job? = null
        val hasUnsavedNotes: Boolean get() = _uiState.value.task != null && completedSaveTaskId != _uiState.value.task?.id &&
            (_uiState.value.notes != loadedState?.notes || _uiState.value.description != loadedState?.description)

        private val _uiEvent = Channel<EditTaskUiEvent>(Channel.BUFFERED)
        val uiEvent = _uiEvent.receiveAsFlow()

        init {
            loadTask(savedStateHandle.get<String>("taskId"))
        }

        fun loadTask(taskId: String?) {
            if (taskId.isNullOrBlank()) {
                loadJob?.cancel()
                loadedTaskId = null
                _uiState.value = EditTaskUiState()
                return
            }
            if (loadedTaskId == taskId && _uiState.value.task?.id == taskId) return

            loadedTaskId = taskId
            completedSaveTaskId = null
            loadedState = null
            _uiState.value = EditTaskUiState()
            startLoad(taskId) {
                val task = requireNotNull(dayManagementRepository.getTaskById(taskId)?.takeUnless { it.isDeleted }) {
                    "Завдання більше недоступне"
                }
                if (loadedTaskId != taskId) return@startLoad
                val loaded =
                    EditTaskUiState(
                        task = task, title = task.title, description = task.description.orEmpty(), notes = task.notes.orEmpty(),
                        priority = task.priority, duration = task.estimatedDurationMinutes,
                        scheduledTime = task.scheduledTime, dueTime = task.dueTime,
                        executionStrictness = task.executionStrictness, points = task.points,
                        isRecurring = task.recurrenceSeriesId != null, isLoadingDetails = true,
                    )
                loadedState = loaded
                _uiState.value = loaded.copy(notes = savedStateHandle.get<String>(notesDraftKey(taskId)) ?: loaded.notes,
                    description = savedStateHandle.get<String>(descriptionDraftKey(taskId)) ?: loaded.description)
                loadDetails(task)
            }
        }

        private fun startLoad(taskId: String, action: suspend () -> Unit) {
            loadJob?.cancel()
            loadJob = viewModelScope.launch {
                try { action() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) {
                    if (loadedTaskId == taskId) _uiState.value = _uiState.value.copy(
                        loadError = failure.message ?: "Не вдалося завантажити властивості")
                }
            }
        }

        private suspend fun loadDetails(task: DayTask) {
            val (rule, anchor, links) = withContext(ioDispatcher) {
                coroutineScope {
                    val recurrence = async { canonicalTaskRecurrenceAuthoringAdapter.getSeriesForOccurrence(task)?.rule?.toAndroidTaskRecurrenceRule() }
                    val day = async { dayManagementRepository.getPlanById(task.dayPlanId)?.date ?: System.currentTimeMillis() }
                    val contexts = async { task.linkedProjectIds.normalizedContextIds().resolveContextLinks() }
                    Triple(recurrence.await(), day.await(), contexts.await())
                }
            }
            if (loadedTaskId != task.id) return
            fun EditTaskUiState.withDetails() = copy(dayAnchorTime = anchor, contextLinks = links,
                isRecurring = rule != null, recurrenceRule = rule,
                recurrenceFrequency = rule?.frequency ?: RecurrenceFrequency.DAILY,
                recurrenceInterval = rule?.interval ?: 1, recurrenceDaysOfWeek = rule?.daysOfWeek?.toSet().orEmpty(),
                isLoadingDetails = false, loadError = null)
            loadedState = loadedState?.withDetails()
            _uiState.value = _uiState.value.withDetails() // Preserve title/text edits made during the reads.
        }

        fun retryTaskLoad() {
            val task = _uiState.value.task
            if (task == null) { loadTask(loadedTaskId); return }
            _uiState.value = _uiState.value.copy(isLoadingDetails = true, loadError = null)
            startLoad(task.id) { loadDetails(task) }
        }

        fun reset() {
            loadJob?.cancel()
            completedSaveTaskId = null
            loadedTaskId?.let { savedStateHandle.remove<String>(notesDraftKey(it)) }
            loadedTaskId?.let { savedStateHandle.remove<String>(descriptionDraftKey(it)) }
            loadedState = null
            loadedTaskId = null
            _uiState.value = EditTaskUiState()
        }

        fun onTitleChange(title: String) {
            _uiState.value = _uiState.value.copy(title = title)
        }

        fun onDescriptionChange(description: String) {
            val id = _uiState.value.task?.id ?: return
            if (_uiState.value.isSaving) return
            savedStateHandle[descriptionDraftKey(id)] = description
            _uiState.value = _uiState.value.copy(description = description)
        }

        fun onNotesChange(notes: String) {
            val id = _uiState.value.task?.id ?: return
            if (_uiState.value.isSaving) return
            savedStateHandle[notesDraftKey(id)] = notes
            _uiState.value = _uiState.value.copy(notes = notes, saveError = null)
        }

        private fun notesDraftKey(id: String) = "dayTaskNotesDraft:$id"
        private fun descriptionDraftKey(id: String) = "dayTaskDescriptionDraft:$id"

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
                val presentation =
                    systemWorkspacePresentationContextProjector.resolvePresentation(
                        contextId = normalizedId,
                    ) ?: return@launch
                _uiState.value =
                    _uiState.value.copy(
                        contextLinks =
                            currentLinks +
                                TaskContextLinkUi(
                                    id = presentation.id,
                                    name = presentation.name,
                                ),
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
            val state = _uiState.value
            val originalTask = state.task ?: return
            if (state.isSaving || state.isLoadingDetails || state.loadError != null || completedSaveTaskId == originalTask.id) return
            _uiState.value = state.copy(isSaving = true, saveError = null)
            viewModelScope.launch {
                try {
                    if (state.notes != loadedState?.notes && state.copy(notes = loadedState?.notes.orEmpty(), saveError = null) == loadedState) {
                        dayManagementRepository.updateTaskNotes(originalTask.id, state.notes)
                    } else if (state.isRecurring) {
                        // Realization links save immediately on another tab; do not pass a stale
                        // task/version into recurrence conversion or splitting.
                        val currentTask = requireNotNull(dayManagementRepository.getTaskById(originalTask.id)?.takeUnless { it.isDeleted }) {
                            "Завдання більше недоступне"
                        }
                        saveRecurringTask(state, currentTask)
                    } else {
                        val currentTask = requireNotNull(dayManagementRepository.getTaskById(originalTask.id)?.takeUnless { it.isDeleted }) {
                            "Завдання більше недоступне"
                        }
                        saveNonRecurringTask(state, currentTask)
                    }
                    savedStateHandle.remove<String>(notesDraftKey(originalTask.id))
                    savedStateHandle.remove<String>(descriptionDraftKey(originalTask.id))
                    completedSaveTaskId = originalTask.id
                    _uiEvent.send(EditTaskUiEvent.NavigateUp(originalTask.id))
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    if (loadedTaskId == originalTask.id) _uiState.value = _uiState.value.copy(saveError = failure.message ?: "Не вдалося зберегти завдання")
                } finally {
                    if (loadedTaskId == originalTask.id) _uiState.value = _uiState.value.copy(isSaving = false)
                }
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
                            projectId = originalTask.logicalProjectId,
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
                            projectId = originalTask.logicalProjectId,
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
                    projectId = originalTask.logicalProjectId,
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
                    projectId = originalTask.logicalProjectId,
                    linkedProjectIds = state.contextLinks.map { it.id },
                    updateContextLinks = true,
                    notes = state.notes,
                    updateNotes = state.notes != loadedState?.notes,
                ),
            )
        }

        private fun List<String>?.normalizedContextIds(): List<String> =
            orEmpty()
                .map(String::trim)
                .filter { it.isNotBlank() && it != "root" }
                .distinct()

        private suspend fun List<String>.resolveContextLinks(): List<TaskContextLinkUi> {
            val resolved = mutableListOf<TaskContextLinkUi>()
            for (id in this) {
                val presentation =
                    systemWorkspacePresentationContextProjector.resolvePresentation(
                        contextId = id,
                    ) ?: continue
                resolved +=
                    TaskContextLinkUi(
                        id = presentation.id,
                        name = presentation.name,
                    )
            }
            return resolved
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
