package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.NewTaskParameters
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogClipboardUseCase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.handlers.TodayTabScopeLinksHandler
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class ParentInfo(
    val id: String, // This is the ID of the goal or project
    val title: String,
    val type: ParentType,
    val projectId: String? = null, // Add this for goals
)

enum class ParentType {
    GOAL,
    PROJECT,
}

data class DayTaskWithReminder(
    val dayTask: DayTask,
    val reminder: Reminder?,
    val parentInfo: ParentInfo? = null,
)

data class LinkOption(
    val id: String,
    val name: String,
    val linkType: LinkType? = null,
    val attachmentType: String? = null,
    val entityId: String? = null,
    val target: String? = null,
    val vault: String? = null,
)

data class DayPlanUiState(
    val dayPlan: DayPlan? = null,
    val tasks: List<DayTaskWithReminder> = emptyList(),
    val availableProjects: List<LinkOption> = emptyList(),
    val availableAttachments: List<LinkOption> = emptyList(),
    val todayScopeLinkedProjectIds: List<String> = emptyList(),
    val todayScopeLinkedAttachmentIds: List<String> = emptyList(),
    val linkedProjectTitles: Map<String, String> = emptyMap(),
    val linkedAttachmentTitles: Map<String, String> = emptyMap(),
    val scopeContextsExpanded: Boolean = true,
    val scopeAttachmentsExpanded: Boolean = true,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val lastUpdated: Long? = null,
    val isReordering: Boolean = false,
    val isToday: Boolean = true,
    val bestCompletedPoints: Int = 0,
)

private data class ProjectOptionsSnapshot(
    val options: List<LinkOption> = emptyList(),
    val titlesById: Map<String, String> = emptyMap(),
)

private data class AttachmentOptionsSnapshot(
    val options: List<LinkOption> = emptyList(),
    val titlesById: Map<String, String> = emptyMap(),
)

private data class TodayScopeLinksSnapshot(
    val linkedProjectIds: Set<String> = emptySet(),
    val linkedAttachmentIds: Set<String> = emptySet(),
    val scopeContextsExpanded: Boolean = true,
    val scopeAttachmentsExpanded: Boolean = true,
)

sealed class DayPlanUiEvent {
    data class NavigateToEditTask(val taskId: String) : DayPlanUiEvent()
}

enum class EditingMode { SINGLE, ALL_INSTANCES }

@HiltViewModel
class DayPlanViewModel
    @Inject
    constructor(
        private val dayManagementRepository: DayManagementRepository,
        private val reminderRepository: ReminderRepository,
        private val contextDao: ContextDao,
        private val contextRepository: ContextRepository,
        private val attachmentsRepository: AttachmentsRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
        private val settingsRepository: SettingsRepository,
        private val missionRepository: MissionRepository,
        private val backlogClipboardUseCase: BacklogClipboardUseCase,
) : ViewModel() {
    private companion object {
        const val UI_STATE_STOP_TIMEOUT_MILLIS = 5000L
        const val COMPACT_ID_LENGTH = 8
        const val MAX_TASK_TITLE_LENGTH = 100
        const val MAX_DURATION_MINUTES = 1440L
        const val MIN_TOP_ORDER_OFFSET = 1L
        const val MILLIS_PER_SECOND = 1000L
        const val SECONDS_PER_MINUTE = 60L
        const val MINUTES_PER_HOUR = 60L
        const val PRIORITY_RANK_CRITICAL = 0
        const val PRIORITY_RANK_HIGH = 1
        const val PRIORITY_RANK_MEDIUM = 2
        const val PRIORITY_RANK_LOW = 3
        const val PRIORITY_RANK_NONE = 4
    }

        init {
            Log.d("DayPlanViewModel", "DayPlanViewModel initialized.")
        }

        private val _planId = MutableStateFlow<String?>(null)
        private val _isScopeLinksSheetVisible = MutableStateFlow(false)
        val isScopeLinksSheetVisible: StateFlow<Boolean> = _isScopeLinksSheetVisible.asStateFlow()
        private val _connectionsOrder = MutableStateFlow<List<String>>(emptyList())
        val connectionsOrder: StateFlow<List<String>> = _connectionsOrder.asStateFlow()
        private val _pendingScrollToTaskId = MutableStateFlow<String?>(null)
        val pendingScrollToTaskId: StateFlow<String?> = _pendingScrollToTaskId.asStateFlow()
        val contextMarkerToEmojiMap: StateFlow<Map<String, String>> = contextRepository.contextMarkerToEmojiMap
        val contextMarkerNames: StateFlow<List<String>> = contextRepository.contextMarkerNamesFlow
        private val allContextsFlow =
            contextDao
                .getAllContextsFlow()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )
        private val attachmentLibraryFlow =
            attachmentsRepository
                .getAttachmentLibraryItems()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )
        val allTags: StateFlow<List<String>> =
            contextRepository
                .getAllContextsFlow()
                .map { contexts ->
                    contexts
                        .flatMap { it.tags.orEmpty() }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList(),
                )
        private val scopeLinksHandler =
            TodayTabScopeLinksHandler(
                settingsRepository = settingsRepository,
                isScopeLinksSheetVisible = _isScopeLinksSheetVisible,
                scope = viewModelScope,
            )

        init {
            settingsRepository.dayConnectionsOrderFlow
                .onEach { order -> _connectionsOrder.value = order }
                .launchIn(viewModelScope)
        }

        private val availableProjectsSnapshot =
            allContextsFlow
                .map { contexts ->
                    val titlesById =
                        contexts.associate { context ->
                            context.id to context.name.ifBlank { compactId(context.id) }
                        }
                    val options =
                        contexts
                            .asSequence()
                            .map { LinkOption(it.id, titlesById.getValue(it.id)) }
                            .sortedBy { it.name.lowercase() }
                            .toList()
                    ProjectOptionsSnapshot(
                        options = options,
                        titlesById = titlesById,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = ProjectOptionsSnapshot(),
                )

        private val availableAttachmentsSnapshot =
            attachmentLibraryFlow
                .map { attachments ->
                    val titlesById =
                        attachments
                            .asSequence()
                            .mapNotNull { result ->
                                val relatedLink = parseRelatedLink(result.linkDisplayName)
                                resolveAttachmentTitle(result, relatedLink)?.let { result.id to it }
                            }.toMap()
                    val options =
                        attachments
                            .asSequence()
                            .mapNotNull { result ->
                                val relatedLink = parseRelatedLink(result.linkDisplayName)
                                titlesById[result.id]?.let { title ->
                                    LinkOption(
                                        id = result.id,
                                        name = title,
                                        linkType = relatedLink?.type,
                                        attachmentType = result.attachmentType,
                                        entityId = result.entityId,
                                        target = relatedLink?.target,
                                        vault = relatedLink?.vault,
                                    )
                                }
                            }.sortedBy { it.name.lowercase() }
                            .toList()
                    AttachmentOptionsSnapshot(
                        options = options,
                        titlesById = titlesById,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = AttachmentOptionsSnapshot(),
                )

        @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<DayPlanUiState> =
            _planId
                .filterNotNull()
                .flatMapLatest { planId ->
                    Log.d(
                        "DayPlanViewModel",
                        "flatMapLatest: Processing planId: $planId",
                    )
                    val dayPlanFlow =
                        dayManagementRepository.getPlanByIdStream(planId)
                            .onEach { dayPlan ->
                                Log.d(
                                    "DayPlanViewModel",
                                    "dayPlanFlow: Received dayPlan: ${dayPlan?.id} for planId: $planId",
                                )
                                dayPlan?.let { dayManagementRepository.generateRecurringTasksForDate(it.date) }
                            }

                    val tasksFlow =
                        dayManagementRepository.getTasksForDay(planId)
                            .flatMapLatest { tasks ->
                                Log.d(
                                    "DayPlanViewModel",
                                    "tasksFlow: Received ${tasks.size} tasks for planId: $planId",
                                )
                                if (tasks.isEmpty()) {
                                    flowOf(emptyList())
                                } else {
                                    combine(
                                        tasks.map { task ->
                                            val reminderFlow = reminderRepository.getRemindersForEntityFlow(task.id)
                                            val parentInfoFlow: Flow<ParentInfo?> =
                                                if (task.goalId != null) {
                                                    flow {
                                                        val goal =
                                                            dayManagementRepository.getGoal(task.goalId!!)
                                                        val projectId =
                                                            goal?.let {
                                                                dayManagementRepository.findProjectIdForGoal(
                                                                    it.id,
                                                                )
                                                            }
                                                        emit(
                                                            goal?.let {
                                                                ParentInfo(
                                                                    it.id,
                                                                    it.text,
                                                                    ParentType.GOAL,
                                                                    projectId,
                                                                )
                                                            },
                                                        )
                                                    }
                                                } else if (task.projectId != null) {
                                                    flow {
                                                        emit(
                                                            dayManagementRepository.getProject(
                                                                task.projectId!!,
                                                            ),
                                                        )
                                                    }.map { project ->
                                                        project?.let {
                                                            ParentInfo(
                                                                it.id,
                                                                it.name,
                                                                ParentType.PROJECT,
                                                                it.id,
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    flowOf(null)
                                                }
                                            combine(
                                                reminderFlow,
                                                parentInfoFlow,
                                            ) { reminders, parentInfo ->
                                                DayTaskWithReminder(
                                                    task,
                                                    reminders.firstOrNull(),
                                                    parentInfo,
                                                )
                                            }
                                        },
                                    ) { it.toList() }
                                }
                            }

                    val scopeContextsExpandedFlow = settingsRepository.dayScopeContextsExpandedFlow
                    val scopeAttachmentsExpandedFlow = settingsRepository.dayScopeAttachmentsExpandedFlow
                    val todayLinkedProjectIdsFlow = settingsRepository.todayLinkedProjectIdsFlow
                    val todayLinkedAttachmentIdsFlow = settingsRepository.todayLinkedAttachmentIdsFlow
                    val scopeExpansionFlow =
                        combine(
                            scopeContextsExpandedFlow,
                            scopeAttachmentsExpandedFlow,
                        ) { scopeContextsExpanded, scopeAttachmentsExpanded ->
                            scopeContextsExpanded to scopeAttachmentsExpanded
                        }
                    val todayScopeLinksFlow =
                        combine(
                            todayLinkedProjectIdsFlow,
                            todayLinkedAttachmentIdsFlow,
                            scopeExpansionFlow,
                        ) { todayLinkedProjectIds, todayLinkedAttachmentIds, scopeExpansion ->
                            val (scopeContextsExpanded, scopeAttachmentsExpanded) = scopeExpansion
                            TodayScopeLinksSnapshot(
                                linkedProjectIds = todayLinkedProjectIds,
                                linkedAttachmentIds = todayLinkedAttachmentIds,
                                scopeContextsExpanded = scopeContextsExpanded,
                                scopeAttachmentsExpanded = scopeAttachmentsExpanded,
                            )
                        }

                    combine(
                        dayPlanFlow,
                        tasksFlow,
                        availableProjectsSnapshot,
                        availableAttachmentsSnapshot,
                        todayScopeLinksFlow,
                    ) {
                            dayPlan: DayPlan?,
                            tasks: List<DayTaskWithReminder>,
                            projectSnapshot: ProjectOptionsSnapshot,
                            attachmentSnapshot: AttachmentOptionsSnapshot,
                            todayScopeLinks: TodayScopeLinksSnapshot,
                        ->
                        Log.d(
                            "DayPlanViewModel",
                            (
                                "UI State combine: dayPlanId=${dayPlan?.id}, " +
                                    "tasksCount=${tasks.size} (before creating DayPlanUiState)"
                            ),
                        )
                        val linkedProjectIds = todayScopeLinks.linkedProjectIds
                        val linkedAttachmentIds = todayScopeLinks.linkedAttachmentIds
                        val linkedProjectTitles =
                            linkedProjectIds.associateWith { projectId ->
                                projectSnapshot.titlesById[projectId] ?: compactId(projectId)
                            }
                        val linkedAttachmentTitles =
                            linkedAttachmentIds.associateWith { attachmentId ->
                                attachmentSnapshot.titlesById[attachmentId] ?: compactId(attachmentId)
                            }
                        val highestCompletedPoints = dayManagementRepository.getHighestCompletedPointsAcrossPlans()
                        DayPlanUiState(
                            dayPlan = dayPlan,
                            tasks = sortTasksWithOrder(tasks),
                            availableProjects = projectSnapshot.options,
                            availableAttachments = attachmentSnapshot.options,
                            todayScopeLinkedProjectIds = linkedProjectIds.toList(),
                            todayScopeLinkedAttachmentIds = linkedAttachmentIds.toList(),
                            linkedProjectTitles = linkedProjectTitles,
                            linkedAttachmentTitles = linkedAttachmentTitles,
                            scopeContextsExpanded = todayScopeLinks.scopeContextsExpanded,
                            scopeAttachmentsExpanded = todayScopeLinks.scopeAttachmentsExpanded,
                            isLoading = false,
                            isRefreshing = false,
                            isToday = dayPlan?.let { isTimestampToday(it.date) } ?: true,
                            bestCompletedPoints = highestCompletedPoints,
                            lastUpdated = System.currentTimeMillis(),
                        )
                    }
                }
                .catch { e ->
                    Log.e("DayPlanViewModel", "Error in uiState flow", e)
                    emit(DayPlanUiState(isLoading = false, error = "Помилка завантаження даних: ${e.localizedMessage}"))
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = DayPlanUiState(isLoading = true),
                )

        private fun resolveAttachmentTitle(
            result: AttachmentLibraryQueryResult,
            relatedLink: RelatedLink?,
        ): String? =
            result.noteName?.takeIf { it.isNotBlank() }
                ?: result.musicNoteName?.takeIf { it.isNotBlank() }
                ?: result.checklistName?.takeIf { it.isNotBlank() }
                ?: relatedLink?.displayName?.takeIf { it.isNotBlank() }
                ?: relatedLink?.target?.takeIf { it.isNotBlank() }
                ?: result.contextName?.takeIf { it.isNotBlank() }
                ?: compactId(result.id)

        private fun parseRelatedLink(linkDisplayNameJson: String?): RelatedLink? {
            if (linkDisplayNameJson.isNullOrBlank()) return null
            return runCatching { Gson().fromJson(linkDisplayNameJson, RelatedLink::class.java) }.getOrNull()
        }

        private fun compactId(id: String): String = id.take(COMPACT_ID_LENGTH)

        private val _isAddTaskDialogOpen = MutableStateFlow(false)
        val isAddTaskDialogOpen: StateFlow<Boolean> = _isAddTaskDialogOpen.asStateFlow()

        private val _selectedTask = MutableStateFlow<DayTaskWithReminder?>(null)
        val selectedTask: StateFlow<DayTaskWithReminder?> = _selectedTask.asStateFlow()

        private val _showDeleteConfirmationDialog = MutableStateFlow<DayTaskWithReminder?>(null)
        val showDeleteConfirmationDialog: StateFlow<DayTaskWithReminder?> = _showDeleteConfirmationDialog.asStateFlow()

        private val _showEditConfirmationDialog = MutableStateFlow<DayTaskWithReminder?>(null)
        val showEditConfirmationDialog: StateFlow<DayTaskWithReminder?> = _showEditConfirmationDialog.asStateFlow()

        private var editingMode: EditingMode = EditingMode.SINGLE

        private val _uiEvent = Channel<DayPlanUiEvent>()
        val uiEvent = _uiEvent.receiveAsFlow()
        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

        fun loadDataForPlan(dayPlanId: String) {
            Log.d("DayPlanViewModel", "Loading data for plan: $dayPlanId")
            _planId.value = dayPlanId
        }

        fun addPlanProjectLink(projectId: String) {
            scopeLinksHandler.addPlanProjectLink(projectId)
        }

        fun removePlanProjectLink(projectId: String) {
            scopeLinksHandler.removePlanProjectLink(projectId)
        }

        fun addPlanAttachmentLink(attachmentId: String) {
            scopeLinksHandler.addPlanAttachmentLink(attachmentId)
        }

        fun addPlanExternalLink(
            url: String,
            name: String,
        ) {
            val target = url.trim()
            if (target.isBlank()) return
            val display = name.trim().ifBlank { target }
            viewModelScope.launch(Dispatchers.IO) {
                val attachmentId =
                    attachmentsRepository.createLinkAttachment(
                        contextId = SystemContexts.TODAY.raw,
                        link = RelatedLink(type = LinkType.URL, target = target, displayName = display),
                    )
                scopeLinksHandler.addPlanAttachmentLink(attachmentId)
            }
        }

        fun addPlanObsidianLink(
            noteName: String,
            displayName: String,
            vault: String,
        ) {
            val target = noteName.trim()
            if (target.isBlank()) return
            val display = displayName.trim().ifBlank { target }
            val normalizedVault = vault.trim().ifBlank { null }
            viewModelScope.launch(Dispatchers.IO) {
                val attachmentId =
                    attachmentsRepository.createLinkAttachment(
                        contextId = SystemContexts.TODAY.raw,
                        link =
                            RelatedLink(
                                type = LinkType.OBSIDIAN,
                                target = target,
                                displayName = display,
                                vault = normalizedVault,
                            ),
                    )
                scopeLinksHandler.addPlanAttachmentLink(attachmentId)
            }
        }

        suspend fun createRootContextForPicker(name: String): String? {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return null
            val id = UUID.randomUUID().toString()
            contextRepository.createContextWithId(
                id = id,
                name = trimmed,
                parentId = null,
            )
            return id
        }

        suspend fun createPlanDocumentForPicker(request: NewDocumentDraft): String? =
            when (request) {
                is NewDocumentDraft.Note -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "New note" },
                            contextId = SystemContexts.TODAY.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, documentId)?.id
                }
                is NewDocumentDraft.MusicNote -> {
                    val musicNoteId =
                        musicNoteRepository.create(
                            name = request.name.ifBlank { "New music note" },
                            contextId = SystemContexts.TODAY.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.MUSIC_NOTE, musicNoteId)?.id
                }
                is NewDocumentDraft.Checklist -> {
                    val checklistId =
                        checklistRepository.createChecklist(
                            name = request.name.ifBlank { "New checklist" },
                            contextId = SystemContexts.TODAY.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.CHECKLIST, checklistId)?.id
                }
                is NewDocumentDraft.WebLink -> {
                    val target = request.url.trim()
                    target
                        .takeIf { it.isNotBlank() }
                        ?.let { nonBlankTarget ->
                            attachmentsRepository.createLinkAttachment(
                                contextId = SystemContexts.TODAY.raw,
                                link =
                                    RelatedLink(
                                        type = LinkType.URL,
                                        target = nonBlankTarget,
                                        displayName =
                                            request.name.trim().ifBlank { nonBlankTarget },
                                    ),
                            )
                        }
                }
                is NewDocumentDraft.Obsidian -> {
                    val target = request.noteName.trim()
                    target
                        .takeIf { it.isNotBlank() }
                        ?.let { nonBlankTarget ->
                            attachmentsRepository.createLinkAttachment(
                                contextId = SystemContexts.TODAY.raw,
                                link =
                                    RelatedLink(
                                        type = LinkType.OBSIDIAN,
                                        target = nonBlankTarget,
                                        displayName =
                                            request.displayName.trim().ifBlank { nonBlankTarget },
                                        vault = request.vault,
                                    ),
                            )
                        }
                }
            }

        fun removePlanAttachmentLink(attachmentId: String) {
            scopeLinksHandler.removePlanAttachmentLink(attachmentId)
        }

        fun setScopeContextsExpanded(expanded: Boolean) {
            scopeLinksHandler.setScopeContextsExpanded(expanded)
        }

        fun setScopeAttachmentsExpanded(expanded: Boolean) {
            scopeLinksHandler.setScopeAttachmentsExpanded(expanded)
        }

        fun toggleScopeLinksSheet() {
            scopeLinksHandler.toggleScopeLinksSheet()
        }

        fun dismissScopeLinksSheet() {
            scopeLinksHandler.dismissScopeLinksSheet()
        }

        fun updateConnectionsOrder(order: List<String>) {
            _connectionsOrder.value = order
            viewModelScope.launch {
                settingsRepository.setDayConnectionsOrder(order)
            }
        }

        fun openAddTaskDialog() {
            _isAddTaskDialogOpen.value = true
        }

        fun dismissAddTaskDialog() {
            _isAddTaskDialogOpen.value = false
        }

        fun onTaskLongPressed(taskWithReminder: DayTaskWithReminder) {
            _selectedTask.value = taskWithReminder
        }

        fun selectTask(taskWithReminder: DayTaskWithReminder) {
            _selectedTask.value = taskWithReminder
        }

        fun clearSelectedTask() {
            _selectedTask.value = null
        }

        fun dismissError() {
            // This is now handled by the main flow, but can be used for one-time events if needed
        }

        private fun isTimestampToday(timestamp: Long): Boolean {
            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_YEAR)
            val year = calendar.get(Calendar.YEAR)
            calendar.timeInMillis = timestamp
            val otherDay = calendar.get(Calendar.DAY_OF_YEAR)
            val otherYear = calendar.get(Calendar.YEAR)
            return today == otherDay && year == otherYear
        }

        fun updateTasksOrder(
            dayPlanId: String,
            reorderedTasks: List<DayTaskWithReminder>,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val tasksForRepo =
                        reorderedTasks.mapIndexed { index, taskWithReminder ->
                            taskWithReminder.dayTask.copy(order = index.toLong())
                        }
                    dayManagementRepository.updateTasksOrder(dayPlanId, tasksForRepo)
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error updating task order", e)
                }
            }
        }

        fun addTask(
            dayPlanId: String,
            title: String,
            description: String,
            duration: Long?,
            priority: TaskPriority,
            recurrenceRule: RecurrenceRule?,
            points: Int,
            projectId: String? = null,
            linkedProjectIds: List<String>? = null,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isEmpty() || trimmedTitle.length > MAX_TASK_TITLE_LENGTH) {
                        // Validation failure is intentionally ignored here.
                        return@launch
                    }

                    // New tasks should appear at the top in Today list.
                    val currentTasks = uiState.value.tasks
                    val minOrder = currentTasks.minOfOrNull { it.dayTask.order } ?: 0L
                    val topOrder = minOrder - MIN_TOP_ORDER_OFFSET

                    if (recurrenceRule != null) {
                        dayManagementRepository.addRecurringTask(
                            DayManagementRepository.AddRecurringTaskParams(
                                title = trimmedTitle,
                                description = description.trim().takeIf { it.isNotEmpty() },
                                duration = duration,
                                priority = priority,
                                recurrenceRule = recurrenceRule,
                                dayPlanId = dayPlanId,
                                points = points,
                                order = topOrder,
                            ),
                        )
                    } else {
                        val createdTask =
                            dayManagementRepository.addTaskToDayPlan(
                            NewTaskParameters(
                                dayPlanId = dayPlanId,
                                title = trimmedTitle,
                                description = description.trim().takeIf { it.isNotEmpty() },
                                projectId = projectId,
                                estimatedDurationMinutes =
                                    duration?.takeIf { it > 0 && it <= MAX_DURATION_MINUTES },
                                priority = priority,
                                order = topOrder,
                                points = points,
                                linkedProjectIds = linkedProjectIds,
                            ),
                        )
                        _pendingScrollToTaskId.value = createdTask.id
                    }
                    dismissAddTaskDialog()
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error adding task", e)
                }
            }
        }

        fun consumePendingScrollToTask() {
            _pendingScrollToTaskId.value = null
        }

        fun addTaskFromContext(contextId: String) {
            val dayPlanId = uiState.value.dayPlan?.id ?: return
            val sourceContext =
                allContextsFlow.value.firstOrNull { it.id == contextId }
                    ?: return
            val title = sourceContext.name.trim().ifBlank { compactId(sourceContext.id) }

            addTask(
                dayPlanId = dayPlanId,
                title = title,
                description = "",
                duration = null,
                priority = TaskPriority.MEDIUM,
                recurrenceRule = null,
                points = 0,
                projectId = sourceContext.id,
                linkedProjectIds = listOf(sourceContext.id),
            )
        }

        fun addGoalAsRecurringTask(
            goalId: String,
            dayPlanId: String,
            recurrenceRule: RecurrenceRule,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val goal = dayManagementRepository.getGoal(goalId) ?: return@launch
                    val projectId = dayManagementRepository.findProjectIdForGoal(goalId)
                    dayManagementRepository.addRecurringTask(
                        DayManagementRepository.AddRecurringTaskParams(
                            title = goal.text,
                            description = goal.description,
                            duration = null,
                            priority = TaskPriority.MEDIUM,
                            recurrenceRule = recurrenceRule,
                            dayPlanId = dayPlanId,
                            goalId = goalId,
                            projectId = projectId,
                        ),
                    )
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error adding goal as recurring task", e)
                }
            }
        }

        fun onEditTaskClicked(taskWithReminder: DayTaskWithReminder) {
            _selectedTask.value = taskWithReminder
            if (taskWithReminder.dayTask.recurringTaskId != null) {
                _showEditConfirmationDialog.value = taskWithReminder
            } else {
                editingMode = EditingMode.SINGLE
                openEditTaskDialog()
            }
        }

        fun dismissEditConfirmationDialog() {
            _showEditConfirmationDialog.value = null
        }

        fun editSingleInstanceOfRecurringTask(taskWithReminder: DayTaskWithReminder) {
            viewModelScope.launch(Dispatchers.IO) {
                dayManagementRepository.detachFromRecurrence(taskWithReminder.dayTask.id)
                dismissEditConfirmationDialog()
                editingMode = EditingMode.SINGLE
                openEditTaskDialog()
            }
        }

        fun editAllFutureInstancesOfRecurringTask() {
            dismissEditConfirmationDialog()
            editingMode = EditingMode.ALL_INSTANCES
            openEditTaskDialog()
        }

        fun onDeleteTaskClicked(taskWithReminder: DayTaskWithReminder) {
            if (taskWithReminder.dayTask.recurringTaskId != null) {
                _showDeleteConfirmationDialog.value = taskWithReminder
            } else {
                deleteTask(taskWithReminder.dayTask.id)
            }
        }

        fun dismissDeleteConfirmationDialog() {
            _showDeleteConfirmationDialog.value = null
        }

        fun deleteSingleInstanceOfRecurringTask(taskWithReminder: DayTaskWithReminder) {
            viewModelScope.launch(Dispatchers.IO) {
                dayManagementRepository.deleteTask(taskWithReminder.dayTask.id)
                dismissDeleteConfirmationDialog()
            }
        }

        fun deleteAllFutureInstancesOfRecurringTask(taskWithReminder: DayTaskWithReminder) {
            viewModelScope.launch(Dispatchers.IO) {
                taskWithReminder.dayTask.recurringTaskId?.let {
                    dayManagementRepository.deleteAllFutureInstancesOfRecurringTask(
                        it,
                        taskWithReminder.dayTask.dayPlanId,
                    )
                }
                dismissDeleteConfirmationDialog()
            }
        }

        fun deleteTask(taskId: String) {
            viewModelScope.launch {
                try {
                    dayManagementRepository.deleteTask(taskId)
                    clearSelectedTask()
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error deleting task", e)
                }
            }
        }

        fun toggleTaskCompletion(taskId: String) {
            viewModelScope.launch {
                val task = uiState.value.tasks.find { it.dayTask.id == taskId }?.dayTask ?: return@launch
                try {
                    // Використовуємо ?.let для безпечного розпакування та smart cast
                    task.recurringTaskId?.let { recurringId ->
                        val recurringTask = dayManagementRepository.getRecurringTask(recurringId)

                        if (recurringTask?.recurrenceRule?.frequency == RecurrenceFrequency.HOURLY) {
                            val intervalMillis =
                                recurringTask.recurrenceRule.interval *
                                    MINUTES_PER_HOUR *
                                    SECONDS_PER_MINUTE *
                                    MILLIS_PER_SECOND
                            val nextOccurrence = System.currentTimeMillis() + intervalMillis
                            dayManagementRepository.updateTaskNextOccurrence(taskId, nextOccurrence)
                            return@launch
                        }
                    }

                    // Якщо recurringTaskId == null або частота не HOURLY, просто тоглимо статус
                    dayManagementRepository.toggleTaskCompletion(taskId)
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error toggling task completion", e)
                }
            }
        }

        fun refreshPlan() {
            _planId.value?.let { planId ->
                // Re-emitting the same value will trigger a refresh in flatMapLatest
                _planId.value = null
                _planId.value = planId
            }
        }

        private fun sortTasksWithOrder(tasks: List<DayTaskWithReminder>): List<DayTaskWithReminder> {
            return tasks.sortedWith(
                compareBy<DayTaskWithReminder> { it.dayTask.completed }
                    .thenBy { it.dayTask.order }
                    .thenBy { it.dayTask.title.lowercase() },
            )
        }

        private fun sortTasks(tasks: List<DayTaskWithReminder>): List<DayTaskWithReminder> {
            return tasks.sortedWith(
                compareBy<DayTaskWithReminder> { it.dayTask.completed }
                    .thenBy { task ->
                        when (task.dayTask.priority) {
                            TaskPriority.CRITICAL -> PRIORITY_RANK_CRITICAL
                            TaskPriority.HIGH -> PRIORITY_RANK_HIGH
                            TaskPriority.MEDIUM -> PRIORITY_RANK_MEDIUM
                            TaskPriority.LOW -> PRIORITY_RANK_LOW
                            TaskPriority.NONE -> PRIORITY_RANK_NONE
                        }
                    }
                    .thenBy { it.dayTask.dueTime ?: Long.MAX_VALUE }
                    .thenBy { it.dayTask.title.lowercase() },
            )
        }

        fun hasOverdueTasks(): Boolean {
            val currentTime = System.currentTimeMillis()
            return uiState.value.tasks.any { taskWithReminder ->
                val task = taskWithReminder.dayTask
                val dueTime = task.dueTime // 1. Фіксуємо значення у локальній змінній

                // 2. Використовуємо дужки для чіткості та локальну змінну для smart cast
                !task.completed && (dueTime != null) && (dueTime < currentTime)
            }
        }

        fun getCompletionStats(): Triple<Int, Int, Float> {
            val tasks = uiState.value.tasks
            val completed = tasks.count { it.dayTask.completed }
            val total = tasks.size
            val percentage = if (total > 0) completed.toFloat() / total else 0f
            return Triple(completed, total, percentage)
        }

        fun sortTasksByPriority(dayPlanId: String) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val currentTasks = uiState.value.tasks
                    val sortedTasks = sortTasks(currentTasks)
                    val tasksWithNewOrder =
                        sortedTasks.mapIndexed { index, taskWithReminder ->
                            taskWithReminder.dayTask.copy(order = index.toLong())
                        }
                    dayManagementRepository.updateTasksOrder(dayPlanId, tasksWithNewOrder)
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error sorting tasks by priority", e)
                }
            }
        }

        fun navigateToPreviousDay() {
            viewModelScope.launch(Dispatchers.IO) {
                uiState.value.dayPlan?.date?.let { currentDate ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = currentDate
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val previousDate = calendar.timeInMillis
                    Log.d(
                        "DayPlanViewModel",
                        "Navigating to previous day. CurrentDate: $currentDate, PreviousDate: $previousDate",
                    )
                    try {
                        val prevDayPlan = dayManagementRepository.createOrUpdateDayPlan(previousDate)
                        Log.d(
                            "DayPlanViewModel",
                            "Previous day plan created/updated. ID: ${prevDayPlan.id}",
                        )
                        loadDataForPlan(prevDayPlan.id)
                    } catch (e: Exception) {
                        Log.e("DayPlanViewModel", "Error navigating to previous day", e)
                    }
                } ?: run {
                    Log.w(
                        "DayPlanViewModel",
                        "navigateToPreviousDay: dayPlan is null, cannot navigate.",
                    )
                }
            }
        }

        fun navigateToNextDay() {
            viewModelScope.launch(Dispatchers.IO) {
                if (uiState.value.isToday) {
                    Log.d(
                        "DayPlanViewModel",
                        "navigateToNextDay: Currently on today's plan, cannot navigate forward.",
                    )
                    return@launch
                }
                uiState.value.dayPlan?.date?.let { currentDate ->
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = currentDate
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val nextDate = calendar.timeInMillis
                    Log.d(
                        "DayPlanViewModel",
                        "Navigating to next day. CurrentDate: $currentDate, NextDate: $nextDate",
                    )
                    try {
                        val nextDayPlan = dayManagementRepository.createOrUpdateDayPlan(nextDate)
                        Log.d(
                            "DayPlanViewModel",
                            "Next day plan created/updated. ID: ${nextDayPlan.id}",
                        )
                        loadDataForPlan(nextDayPlan.id)
                    } catch (e: Exception) {
                        Log.e("DayPlanViewModel", "Error navigating to next day", e)
                    }
                } ?: run {
                    Log.w(
                        "DayPlanViewModel",
                        "navigateToNextDay: dayPlan is null, cannot navigate.",
                    )
                }
            }
        }

        fun copyTaskToTodaysPlan(taskToCopyWithReminder: DayTaskWithReminder) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    dayManagementRepository.copyTaskToTodaysPlan(taskToCopyWithReminder.dayTask)
                    clearSelectedTask()
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error copying task", e)
                }
            }
        }

        fun copyTaskToEntityClipboard(taskWithReminder: DayTaskWithReminder) {
            backlogClipboardUseCase.copyDayTasks(
                sourceContextId = taskWithReminder.dayTask.projectId.orEmpty(),
                taskIds = listOf(taskWithReminder.dayTask.id),
            )
        }

        fun cutTaskToEntityClipboard(taskWithReminder: DayTaskWithReminder) {
            backlogClipboardUseCase.cutDayTasks(
                sourceContextId = taskWithReminder.dayTask.projectId.orEmpty(),
                taskIds = listOf(taskWithReminder.dayTask.id),
            )
        }

        fun moveTaskToTomorrow(taskToMoveWithReminder: DayTaskWithReminder) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    dayManagementRepository.moveTaskToTomorrow(taskToMoveWithReminder.dayTask)
                    clearSelectedTask()
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error moving task to tomorrow", e)
                }
            }
        }

        fun addTaskToTacticalMissions(taskWithReminder: DayTaskWithReminder) {
            viewModelScope.launch(Dispatchers.IO) {
                val task = taskWithReminder.dayTask
                val trimmedTitle = task.title.trim()
                if (trimmedTitle.isBlank()) return@launch
                try {
                    missionRepository.insertMissionWithAutoOrder(
                        TacticalMission(
                            title = trimmedTitle,
                            description = task.description,
                            deadline = System.currentTimeMillis(),
                            status = MissionStatus.ACTIVE,
                            projectId = task.projectId,
                            linkedProjectIds = task.linkedProjectIds.orEmpty(),
                            linkedAttachmentIds = emptyList(),
                        ),
                    )
                    clearSelectedTask()
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error adding task to tactical missions", e)
                }
            }
        }

        private val _isEditTaskDialogOpen = MutableStateFlow(false)
        val isEditTaskDialogOpen: StateFlow<Boolean> = _isEditTaskDialogOpen.asStateFlow()

        fun openEditTaskDialog() {
            _isEditTaskDialogOpen.value = true
        }

        fun dismissEditTaskDialog() {
            _isEditTaskDialogOpen.value = false
        }

        fun updateTask(
            taskId: String,
            title: String,
            description: String,
            duration: Long?,
            priority: TaskPriority,
            points: Int,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                val taskWithReminder = selectedTask.value ?: return@launch
                val task = taskWithReminder.dayTask
                try {
                    if (editingMode == EditingMode.ALL_INSTANCES && task.recurringTaskId != null) {
                        dayManagementRepository.splitRecurringTask(
                            DayManagementRepository.SplitRecurringTaskParams(
                                originalTask = task,
                                newTitle = title,
                                newDescription = description,
                                newPriority = priority,
                                newDuration = duration,
                                points = task.points,
                            ),
                        )
                    } else {
                        dayManagementRepository.updateTask(
                            DayManagementRepository.UpdateTaskParams(
                                taskId = taskId,
                                title = title,
                                description = description,
                                priority = priority,
                                duration = duration,
                                points = points,
                            ),
                        )
                    }
                    dismissEditTaskDialog()
                    clearSelectedTask()
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error updating task", e)
                }
            }
        }

        fun setTaskReminder(
            taskId: String,
            reminderTime: Long,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    reminderRepository.createReminder(taskId, "TASK", reminderTime)
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error setting reminder", e)
                }
            }
        }

        fun clearTaskReminder(taskId: String) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    reminderRepository.clearRemindersForEntity(taskId)
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error clearing reminder", e)
                }
            }
        }

        fun moveTaskToTop(taskWithReminder: DayTaskWithReminder) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val currentTasks = uiState.value.tasks.toMutableList()
                    currentTasks.remove(taskWithReminder)
                    currentTasks.add(0, taskWithReminder.copy(dayTask = taskWithReminder.dayTask.copy(order = 0)))

                    val tasksForRepo =
                        currentTasks.mapIndexed { index, tWithR ->
                            tWithR.dayTask.copy(order = index.toLong())
                        }

                    dayManagementRepository.updateTasksOrder(taskWithReminder.dayTask.dayPlanId, tasksForRepo)
                    clearSelectedTask()
                } catch (e: Exception) {
                    Log.e("DayPlanViewModel", "Error moving task to top", e)
                }
            }
        }
    }
