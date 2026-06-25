package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogClipboardUseCase
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestRepository
import com.romankozak.forwardappmobile.features.missions.domain.repository.TacticalActivitySlotRepository
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
import com.romankozak.forwardappmobile.features.missions.domain.usecase.AddTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.DeleteTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.GetTacticalMissionsUseCase
import com.romankozak.forwardappmobile.features.missions.domain.usecase.UpdateTacticalMissionUseCase
import com.romankozak.forwardappmobile.features.missions.presentation.handlers.TacticalScopeLinksHandler
import com.romankozak.forwardappmobile.sync.AttachmentLibraryQueryResult
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.NewTaskParameters
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.UUID
import javax.inject.Inject
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE

enum class TacticsWorkspaceMode {
    SLOTS,
    ALL,
    PLAN,
}

data class TacticalPlanBacklogItem(
    val item: BacklogItem,
    val title: String,
    val description: String?,
    val alreadyInWeek: Boolean,
)

@HiltViewModel
class TacticalMissionViewModel
    @Inject
    constructor(
        private val getTacticalMissionsUseCase: GetTacticalMissionsUseCase,
        private val addTacticalMissionUseCase: AddTacticalMissionUseCase,
        private val updateTacticalMissionUseCase: UpdateTacticalMissionUseCase,
        private val deleteTacticalMissionUseCase: DeleteTacticalMissionUseCase,
        private val missionRepository: MissionRepository,
        private val contextRepository: ContextRepository,
        private val dayManagementRepository: DayManagementRepository,
        private val attachmentsRepository: AttachmentsRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
        private val settingsRepository: SettingsRepository,
        private val backlogClipboardUseCase: BacklogClipboardUseCase,
        private val reminderRepository: ReminderRepository,
        private val arcQuestRepository: ArcQuestRepository,
        private val tacticalActivitySlotRepository: TacticalActivitySlotRepository,
        private val listItemDao: ListItemDao,
        private val goalDao: GoalDao,
    ) : ViewModel() {
        companion object {
            const val MISSION_REMINDER_ENTITY_TYPE = "TACTICAL_MISSION"
        }

        private val _missions = MutableStateFlow<List<TacticalMission>>(emptyList())
        val missions: StateFlow<List<TacticalMission>> = _missions.asStateFlow()
        private val _selectedMode = MutableStateFlow(TacticsWorkspaceMode.SLOTS)
        val selectedMode: StateFlow<TacticsWorkspaceMode> = _selectedMode.asStateFlow()
        private val _selectedActivitySlotContextId = MutableStateFlow<String?>(null)
        val selectedActivitySlotContextId: StateFlow<String?> = _selectedActivitySlotContextId.asStateFlow()
        private val _selectedPlanningContextId = MutableStateFlow<String?>(null)
        val selectedPlanningContextId: StateFlow<String?> = _selectedPlanningContextId.asStateFlow()
        val currentWeekKey: String = currentIsoWeekKey()
        private val allContexts =
            contextRepository
                .getAllContextsFlow()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )
        val activitySlotContexts: StateFlow<List<Context>> =
            combine(allContexts, tacticalActivitySlotRepository.observeSlots()) { contexts, slots ->
                val contextById = contexts.associateBy { it.id }
                slots.mapNotNull { slot -> contextById[slot.contextId] }
            }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )
        val visibleMissions: StateFlow<List<TacticalMission>> =
            combine(missions, selectedMode, selectedActivitySlotContextId) { allMissions, mode, selectedSlotId ->
                val weekMissions =
                    allMissions
                        .filter { it.isCurrentWeekMission(currentWeekKey) }
                        .filter { it.sourceBacklogItemId == null }
                when (mode) {
                    TacticsWorkspaceMode.SLOTS ->
                        weekMissions
                            .filter { it.activitySlotContextId == selectedSlotId }
                            .sortedWith(compareBy<TacticalMission> { it.orderInSlot ?: it.orderInWeek }.thenBy { it.createdAt })
                    TacticsWorkspaceMode.ALL ->
                        weekMissions.sortedWith(
                            compareBy<TacticalMission> { it.activitySlotContextId ?: "" }
                                .thenBy { it.orderInSlot ?: it.orderInWeek }
                                .thenBy { it.createdAt },
                        )
                    TacticsWorkspaceMode.PLAN -> emptyList()
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )
        @OptIn(ExperimentalCoroutinesApi::class)
        val planningBacklogItems: StateFlow<List<TacticalPlanBacklogItem>> =
            combine(
                selectedPlanningContextId.flatMapLatest { contextId ->
                    if (contextId == null) {
                        flowOf(emptyList())
                    } else {
                        listItemDao.getItemsForContextStream(contextId)
                    }
                },
                goalDao.getAllVisibleGoalsFlow(),
                allContexts,
                missions,
                selectedPlanningContextId,
            ) { backlogItems, goals, contexts, allMissions, contextId ->
                val goalById = goals.associateBy { it.id }
                val contextById = contexts.associateBy { it.id }
                val selectedBacklogIds =
                    allMissions
                        .asSequence()
                        .filter { it.isCurrentWeekMission(currentWeekKey) }
                        .mapNotNull { it.sourceBacklogItemId }
                        .toSet()
                backlogItems.map { item ->
                    item.toPlanBacklogItem(
                        goalsById = goalById,
                        contextsById = contextById,
                        alreadyInWeek = item.id in selectedBacklogIds,
                        fallbackContextId = contextId,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )
        private val allAttachmentLibraryItems =
            attachmentsRepository
                .getAttachmentLibraryItems()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )
        val projectOptions: StateFlow<List<ProjectOption>> =
            allContexts
                .map { projects ->
                    projects.map {
                        ProjectOption(
                            id = it.id,
                            name = it.name,
                            parentId = it.parentId,
                        )
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )
        val attachmentOptions: StateFlow<List<AttachmentOption>> =
            allAttachmentLibraryItems
                .map { results -> results.mapNotNull { it.toAttachmentOption() } }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )

        private val _isAddMissionDialogOpen = MutableStateFlow(false)
        val isAddMissionDialogOpen: StateFlow<Boolean> = _isAddMissionDialogOpen.asStateFlow()
        private val _pendingScrollToMissionId = MutableStateFlow<Long?>(null)
        val pendingScrollToMissionId: StateFlow<Long?> = _pendingScrollToMissionId.asStateFlow()
        private val _canPasteAsMissions = MutableStateFlow(false)
        val canPasteAsMissions: StateFlow<Boolean> = _canPasteAsMissions.asStateFlow()
        private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 4)
        val uiMessages = _uiMessages.asSharedFlow()

        private val _boardLinkedProjectIds = MutableStateFlow<List<String>>(emptyList())
        val boardLinkedProjectIds: StateFlow<List<String>> = _boardLinkedProjectIds.asStateFlow()

        private val _boardLinkedAttachmentIds = MutableStateFlow<List<String>>(emptyList())
        val boardLinkedAttachmentIds: StateFlow<List<String>> = _boardLinkedAttachmentIds.asStateFlow()

        private val _scopeContextsExpanded = MutableStateFlow(true)
        val scopeContextsExpanded: StateFlow<Boolean> = _scopeContextsExpanded.asStateFlow()

        private val _scopeAttachmentsExpanded = MutableStateFlow(true)
        val scopeAttachmentsExpanded: StateFlow<Boolean> = _scopeAttachmentsExpanded.asStateFlow()
        private val _connectionsOrder = MutableStateFlow<List<String>>(emptyList())
        val connectionsOrder: StateFlow<List<String>> = _connectionsOrder.asStateFlow()
        val contextMarkerNames: StateFlow<List<String>> = contextRepository.contextMarkerNamesFlow
        val allTags: StateFlow<List<String>> =
            allContexts
                .map { contexts ->
                    contexts
                        .flatMap { it.tags.orEmpty() }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )
        val missionReminderTimes: StateFlow<Map<Long, Long>> =
            reminderRepository.getAllReminders()
                .combine(missions) { reminders, currentMissions ->
                    val existingMissionIds = currentMissions.map { it.id.toString() }.toSet()
                    reminders
                        .asSequence()
                        .filter { it.entityType == MISSION_REMINDER_ENTITY_TYPE }
                        .filter { it.entityId in existingMissionIds }
                        .groupBy { it.entityId }
                        .mapNotNull { (entityId, entityReminders) ->
                            val reminderTime = entityReminders.minOfOrNull { it.reminderTime } ?: return@mapNotNull null
                            entityId.toLongOrNull()?.let { missionId ->
                                missionId to reminderTime
                            }
                        }.toMap()
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyMap(),
                )

        private val _isScopeLinksSheetVisible = MutableStateFlow(false)
        val isScopeLinksSheetVisible: StateFlow<Boolean> = _isScopeLinksSheetVisible.asStateFlow()
        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")
        private val scopeLinksHandler =
            TacticalScopeLinksHandler(
                settingsRepository = settingsRepository,
                boardLinkedProjectIds = _boardLinkedProjectIds,
                boardLinkedAttachmentIds = _boardLinkedAttachmentIds,
                isScopeLinksSheetVisible = _isScopeLinksSheetVisible,
                scope = viewModelScope,
            )

        init {
            loadMissions()

            settingsRepository.tacticalLinkedProjectIdsFlow
                .onEach { ids -> _boardLinkedProjectIds.value = ids.toList() }
                .launchIn(viewModelScope)

            settingsRepository.tacticalLinkedAttachmentIdsFlow
                .onEach { ids -> _boardLinkedAttachmentIds.value = ids.toList() }
                .launchIn(viewModelScope)

            settingsRepository.tacticalScopeContextsExpandedFlow
                .onEach { expanded -> _scopeContextsExpanded.value = expanded }
                .launchIn(viewModelScope)

            settingsRepository.tacticalScopeAttachmentsExpandedFlow
                .onEach { expanded -> _scopeAttachmentsExpanded.value = expanded }
                .launchIn(viewModelScope)

            settingsRepository.tacticalConnectionsOrderFlow
                .onEach { order -> _connectionsOrder.value = order }
                .launchIn(viewModelScope)

            backlogClipboardUseCase.clipboardPayload
                .onEach { _canPasteAsMissions.value = backlogClipboardUseCase.canPasteIntoTacticalMissions() }
                .launchIn(viewModelScope)
        }

        private fun loadMissions(projectId: String? = null) {
            getTacticalMissionsUseCase(projectId)
                .onEach { missions ->
                    _missions.value = missions
                }
                .launchIn(viewModelScope)
        }

        private fun nextSlotOrder(activitySlotContextId: String?): Long? {
            if (activitySlotContextId == null) return null
            val minOrder =
                _missions.value
                    .filter { it.isCurrentWeekMission(currentWeekKey) && it.activitySlotContextId == activitySlotContextId }
                    .mapNotNull { it.orderInSlot }
                    .minOrNull() ?: 0L
            return minOrder - 1L
        }

        fun addMission(
            title: String,
            description: String,
            deadline: Long,
            status: MissionStatus,
            projectLinks: List<String>,
            attachmentLinks: List<String>,
        ) {
            val newMission =
                TacticalMission(
                    title = title,
                    description = description,
                    deadline = deadline,
                    status = status,
                    projectId = null,
                    linkedProjectIds = projectLinks,
                    linkedAttachmentIds = attachmentLinks,
                    weekKey = currentWeekKey,
                    sourceType = MissionSourceType.MANUAL,
                )
            addMission(newMission)
        }

        fun addMission(mission: TacticalMission) {
            viewModelScope.launch {
                val id = missionRepository.insertMissionWithAutoOrder(mission)
                _pendingScrollToMissionId.value = id
                // Прив'язуємо вкладення до створеної місії
                missionRepository.setAttachments(id, mission.linkedAttachmentIds ?: emptyList())
            }
        }

        fun addQuickMission(title: String) {
            addQuickMission(title = title, activitySlotContextId = null)
        }

        fun addQuickMissionForCurrentSlot(title: String) {
            val activitySlotContextId =
                if (_selectedMode.value == TacticsWorkspaceMode.SLOTS) {
                    _selectedActivitySlotContextId.value
                } else {
                    null
                }
            addQuickMission(title = title, activitySlotContextId = activitySlotContextId)
        }

        fun addQuickMission(
            title: String,
            activitySlotContextId: String?,
        ) {
            val trimmedTitle = title.trim()
            if (trimmedTitle.isBlank()) return
            addMission(
                TacticalMission(
                    title = trimmedTitle,
                    description = null,
                    deadline = NO_DEADLINE,
                    status = MissionStatus.ACTIVE,
                    projectId = null,
                    linkedProjectIds = emptyList(),
                    linkedAttachmentIds = emptyList(),
                    weekKey = currentWeekKey,
                    activitySlotContextId = activitySlotContextId,
                    orderInSlot = nextSlotOrder(activitySlotContextId),
                    sourceType = MissionSourceType.MANUAL,
                ),
            )
        }

        fun addWeeklyMissionFromContext(contextId: String) {
            val context = allContexts.value.firstOrNull { it.id == contextId } ?: return
            val activitySlotContextId = context.id.takeIf { isKnownActivitySlot(it) }
            val now = System.currentTimeMillis()
            val oneWeekMs = 7L * 24L * 60L * 60L * 1000L
            addMission(
                TacticalMission(
                    title = context.name,
                    description = null,
                    startTime = now,
                    deadline = now + oneWeekMs,
                    status = MissionStatus.ACTIVE,
                    priority = MissionPriority.MEDIUM,
                    projectId = context.id,
                    linkedProjectIds = listOf(context.id),
                    linkedAttachmentIds = emptyList(),
                    weekKey = currentWeekKey,
                    activitySlotContextId = activitySlotContextId,
                    orderInSlot = nextSlotOrder(activitySlotContextId),
                    sourceType = MissionSourceType.MANUAL,
                    sourceContextId = context.id,
                ),
            )
        }

        fun consumePendingScrollToMission() {
            _pendingScrollToMissionId.value = null
        }

        fun addMissionToTodayPlan(mission: TacticalMission) {
            val trimmedTitle = mission.title.trim()
            if (trimmedTitle.isBlank()) return
            viewModelScope.launch {
                val todayPlan = dayManagementRepository.createOrUpdateDayPlan(System.currentTimeMillis())
                dayManagementRepository.addTaskToDayPlan(
                    NewTaskParameters(
                        dayPlanId = todayPlan.id,
                        title = trimmedTitle,
                        description = mission.description,
                        projectId = mission.projectId,
                        linkedProjectIds = mission.linkedProjectIds.orEmpty(),
                        priority = TaskPriority.MEDIUM,
                        taskType = BacklogItemTypeValues.GOAL,
                        points = 0,
                    ),
                )
            }
        }

        fun addMissionToCurrentArc(mission: TacticalMission) {
            val trimmedTitle = mission.title.trim()
            if (trimmedTitle.isBlank()) return
            viewModelScope.launch {
                arcQuestRepository.addQuest(
                    ArcQuestEntity(
                        arcKey = YearMonth.now().toString(),
                        title = trimmedTitle,
                        description = mission.description,
                        linkedContextId = mission.projectId,
                        linkedMissionId = mission.id,
                        sourceType = ArcQuestSourceType.MISSION.name,
                        sourceId = mission.id.toString(),
                    ),
                )
            }
        }

        fun selectMode(mode: TacticsWorkspaceMode) {
            _selectedMode.value = mode
        }

        fun selectActivitySlot(contextId: String?) {
            _selectedActivitySlotContextId.value = contextId
        }

        fun addActivitySlot(contextId: String) {
            viewModelScope.launch {
                tacticalActivitySlotRepository.addSlot(contextId)
                _selectedActivitySlotContextId.value = contextId
            }
        }

        fun removeActivitySlot(contextId: String) {
            viewModelScope.launch {
                tacticalActivitySlotRepository.removeSlot(contextId)
                if (_selectedActivitySlotContextId.value == contextId) {
                    _selectedActivitySlotContextId.value = null
                }
                if (_selectedPlanningContextId.value == contextId) {
                    _selectedPlanningContextId.value = null
                }
            }
        }

        fun selectPlanningContext(contextId: String?) {
            _selectedPlanningContextId.value = contextId
        }

        fun assignMissionToActivitySlot(
            mission: TacticalMission,
            activitySlotContextId: String?,
        ) {
            updateMission(
                mission.copy(
                    activitySlotContextId = activitySlotContextId,
                    orderInSlot = nextSlotOrder(activitySlotContextId),
                ),
            )
        }

        fun reorderVisibleMissions(reordered: List<TacticalMission>) {
            viewModelScope.launch {
                reordered.forEachIndexed { index, mission ->
                    updateTacticalMissionUseCase(
                        if (_selectedMode.value == TacticsWorkspaceMode.SLOTS) {
                            mission.copy(orderInSlot = index.toLong())
                        } else {
                            mission.copy(orderInWeek = index.toLong(), order = index.toLong())
                        },
                    )
                }
            }
        }

        fun createMissionFromBacklogItem(planItem: TacticalPlanBacklogItem) {
            if (planItem.alreadyInWeek) return
            val sourceContextId = _selectedPlanningContextId.value ?: planItem.item.contextId
            val activitySlotContextId = sourceContextId.takeIf { isKnownActivitySlot(it) }
            addMission(
                TacticalMission(
                    title = planItem.title,
                    description = planItem.description,
                    deadline = NO_DEADLINE,
                    status = MissionStatus.ACTIVE,
                    projectId = sourceContextId.takeUnless { activitySlotContextId != null },
                    linkedProjectIds = listOf(sourceContextId),
                    linkedAttachmentIds = emptyList(),
                    weekKey = currentWeekKey,
                    activitySlotContextId = activitySlotContextId,
                    orderInSlot = nextSlotOrder(activitySlotContextId),
                    sourceType =
                        if (activitySlotContextId != null) {
                            MissionSourceType.SLOT_BACKLOG_ITEM
                        } else {
                            MissionSourceType.CONTEXT_BACKLOG_ITEM
                        },
                    sourceContextId = sourceContextId,
                    sourceBacklogItemId = planItem.item.id,
                ),
            )
        }

        fun updateMission(
            id: Long,
            title: String,
            description: String?,
            deadline: Long,
            status: MissionStatus,
            projectLinks: List<String>,
            attachmentLinks: List<String>,
        ) {
            viewModelScope.launch {
                val existingMission = _missions.value.find { it.id == id }
                if (existingMission != null) {
                    val updatedMission =
                        existingMission.copy(
                            title = title,
                            description = description,
                            deadline = deadline,
                            status = status,
                            linkedProjectIds = projectLinks,
                            linkedAttachmentIds = attachmentLinks,
                        )
                    updateTacticalMissionUseCase(updatedMission)
                    missionRepository.setAttachments(id, attachmentLinks)
                }
            }
        }

        fun updateMission(mission: TacticalMission) {
            viewModelScope.launch {
                updateTacticalMissionUseCase(mission)
                missionRepository.setAttachments(mission.id, mission.linkedAttachmentIds ?: emptyList())
            }
        }

        fun deleteMission(missionId: Long) {
            viewModelScope.launch {
                reminderRepository.clearRemindersForEntity(missionId.toString())
                deleteTacticalMissionUseCase(missionId)
            }
        }

        fun setMissionReminder(
            missionId: Long,
            reminderTime: Long,
        ) {
            val entityId = missionId.toString()
            viewModelScope.launch {
                reminderRepository.clearRemindersForEntity(entityId)
                reminderRepository.createReminder(
                    entityId = entityId,
                    entityType = MISSION_REMINDER_ENTITY_TYPE,
                    reminderTime = reminderTime,
                )
            }
        }

        fun clearMissionReminder(missionId: Long) {
            viewModelScope.launch {
                reminderRepository.clearRemindersForEntity(missionId.toString())
            }
        }

        private fun isKnownActivitySlot(contextId: String): Boolean =
            activitySlotContexts.value.any { it.id == contextId }

        fun copyMissionToEntityClipboard(mission: TacticalMission) {
            backlogClipboardUseCase.copyTacticalMissions(
                sourceContextId = mission.projectId.orEmpty(),
                missionIds = listOf(mission.id),
            )
        }

        fun cutMissionToEntityClipboard(mission: TacticalMission) {
            backlogClipboardUseCase.cutTacticalMissions(
                sourceContextId = mission.projectId.orEmpty(),
                missionIds = listOf(mission.id),
            )
        }

        fun copyMissionsToEntityClipboard(missionIds: Set<Long>) {
            val ids = missionIds.toList()
            if (ids.isEmpty()) return
            backlogClipboardUseCase.copyTacticalMissions(
                sourceContextId = "",
                missionIds = ids,
            )
            _uiMessages.tryEmit("Скопійовано місії. Можна вставити у Тактики або беклог.")
        }

        fun cutMissionsToEntityClipboard(missionIds: Set<Long>) {
            val ids = missionIds.toList()
            if (ids.isEmpty()) return
            backlogClipboardUseCase.cutTacticalMissions(
                sourceContextId = "",
                missionIds = ids,
            )
            _uiMessages.tryEmit("Вирізано місії. Можна вставити у Тактики або беклог.")
        }

        fun pasteClipboardAsMissions() {
            if (!backlogClipboardUseCase.canPasteIntoTacticalMissions()) {
                _uiMessages.tryEmit("Буфер не містить елементів для вставки як місій")
                return
            }
            viewModelScope.launch {
                val report = backlogClipboardUseCase.pasteIntoTacticalMissions()
                _uiMessages.emit(report.toUserMessage())
            }
        }

        fun toggleMissionCompleted(mission: TacticalMission) {
            val updatedStatus =
                if (mission.status == MissionStatus.COMPLETED) {
                    MissionStatus.ACTIVE
                } else {
                    MissionStatus.COMPLETED
                }
            val updatedMission = mission.copy(status = updatedStatus)
            viewModelScope.launch {
                updateTacticalMissionUseCase(updatedMission)
            }
        }

        fun openAddMissionDialog() {
            _isAddMissionDialogOpen.value = true
        }

        fun dismissAddMissionDialog() {
            _isAddMissionDialogOpen.value = false
        }

        fun addBoardProjectLink(projectId: String) {
            scopeLinksHandler.addBoardProjectLink(projectId)
        }

        fun removeBoardProjectLink(projectId: String) {
            scopeLinksHandler.removeBoardProjectLink(projectId)
        }

        fun addBoardAttachmentLink(attachmentId: String) {
            scopeLinksHandler.addBoardAttachmentLink(attachmentId)
        }

        fun addBoardUrlLink(
            url: String,
            name: String,
        ) {
            val target = url.trim()
            if (target.isBlank()) return
            val display = name.trim().ifBlank { target }
            viewModelScope.launch {
                val attachmentId =
                    attachmentsRepository.createLinkAttachment(
                        contextId = SystemContexts.MISSION.raw,
                        link = RelatedLink(type = LinkType.URL, target = target, displayName = display),
                    )
                scopeLinksHandler.addBoardAttachmentLink(attachmentId)
            }
        }

        fun addBoardObsidianLink(
            noteName: String,
            displayName: String,
            vault: String,
        ) {
            val target = noteName.trim()
            if (target.isBlank()) return
            val display = displayName.trim().ifBlank { target }
            val normalizedVault = vault.trim().ifBlank { null }
            viewModelScope.launch {
                val attachmentId =
                    attachmentsRepository.createLinkAttachment(
                        contextId = SystemContexts.MISSION.raw,
                        link =
                            RelatedLink(
                                type = LinkType.OBSIDIAN,
                                target = target,
                                displayName = display,
                                vault = normalizedVault,
                            ),
                    )
                scopeLinksHandler.addBoardAttachmentLink(attachmentId)
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

        suspend fun createBoardDocumentForPicker(request: NewDocumentDraft): String? {
            return when (request) {
                is NewDocumentDraft.Note -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "New note" },
                            contextId = SystemContexts.MISSION.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, documentId)?.id
                }
                is NewDocumentDraft.JournalDocument -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "New journal" },
                            contextId = SystemContexts.MISSION.raw,
                            attachmentType = BacklogItemTypeValues.JOURNAL_DOCUMENT,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.JOURNAL_DOCUMENT, documentId)?.id
                }
                is NewDocumentDraft.MusicNote -> {
                    val musicNoteId =
                        musicNoteRepository.create(
                            name = request.name.ifBlank { "New music note" },
                            contextId = SystemContexts.MISSION.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.MUSIC_NOTE, musicNoteId)?.id
                }
                is NewDocumentDraft.Checklist -> {
                    val checklistId =
                        checklistRepository.createChecklist(
                            name = request.name.ifBlank { "New checklist" },
                            contextId = SystemContexts.MISSION.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.CHECKLIST, checklistId)?.id
                }
                is NewDocumentDraft.WebLink -> {
                    val target = request.url.trim()
                    if (target.isBlank()) return null
                    attachmentsRepository.createLinkAttachment(
                        contextId = SystemContexts.MISSION.raw,
                        link = RelatedLink(type = LinkType.URL, target = target, displayName = request.name.trim().ifBlank { target }),
                    )
                }
                is NewDocumentDraft.Obsidian -> {
                    val target = request.noteName.trim()
                    if (target.isBlank()) return null
                    attachmentsRepository.createLinkAttachment(
                        contextId = SystemContexts.MISSION.raw,
                        link =
                            RelatedLink(
                                type = LinkType.OBSIDIAN,
                                target = target,
                                displayName = request.displayName.trim().ifBlank { target },
                                vault = request.vault,
                            ),
                    )
                }
            }
        }

        fun removeBoardAttachmentLink(attachmentId: String) {
            scopeLinksHandler.removeBoardAttachmentLink(attachmentId)
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

        fun reorderMissions(missions: List<TacticalMission>) {
            viewModelScope.launch {
                missionRepository.reorderMissions(missions)
            }
        }

        fun updateConnectionsOrder(order: List<String>) {
            _connectionsOrder.value = order
            viewModelScope.launch {
                settingsRepository.setTacticalConnectionsOrder(order)
            }
        }
    }

data class ProjectOption(
    val id: String,
    val name: String,
    val parentId: String? = null,
)

data class AttachmentOption(
    val id: String,
    val name: String,
    val linkType: LinkType? = null,
    val attachmentType: String? = null,
    val entityId: String? = null,
    val target: String? = null,
    val vault: String? = null,
)

// Оновлене розширення для роботи з результатом запиту бібліотеки
private fun AttachmentLibraryQueryResult.toAttachmentOption(): AttachmentOption {
    val relatedLink =
        linkDisplayName?.let { json ->
            runCatching { Gson().fromJson(json, RelatedLink::class.java) }.getOrNull()
        }
    val linkLabel =
        relatedLink?.displayName?.takeIf { it.isNotBlank() }
            ?: relatedLink?.target?.takeIf { it.isNotBlank() }
    val label =
        noteName?.takeIf { it.isNotBlank() }
            ?: musicNoteName?.takeIf { it.isNotBlank() }
            ?: checklistName?.takeIf { it.isNotBlank() }
            ?: scriptName?.takeIf { it.isNotBlank() }
            ?: linkLabel
            ?: contextName
            ?: "Attachment ${id.takeLast(4)}" // Fallback

    return AttachmentOption(
        id = id,
        name = label,
        linkType = relatedLink?.type,
        attachmentType = attachmentType,
        entityId = entityId,
        target = relatedLink?.target,
        vault = relatedLink?.vault,
    )
}

private fun currentIsoWeekKey(): String {
    val now = LocalDate.now()
    val weekFields = WeekFields.ISO
    val weekBasedYear = now.get(weekFields.weekBasedYear())
    val week = now.get(weekFields.weekOfWeekBasedYear())
    return "%04d-W%02d".format(weekBasedYear, week)
}

fun TacticalMission.isCurrentWeekMission(currentWeekKey: String): Boolean = weekKey.isBlank() || weekKey == currentWeekKey

private fun BacklogItem.toPlanBacklogItem(
    goalsById: Map<String, Goal>,
    contextsById: Map<String, Context>,
    alreadyInWeek: Boolean,
    fallbackContextId: String?,
): TacticalPlanBacklogItem {
    val goal = goalsById[entityId]
    val context = contextsById[entityId]
    val fallbackContext = fallbackContextId?.let(contextsById::get)
    val title =
        when (itemType) {
            BacklogItemTypeValues.GOAL -> goal?.text
            BacklogItemTypeValues.CONTEXT -> context?.name
            else -> null
        }?.takeIf { it.isNotBlank() } ?: entityId
    val description =
        when (itemType) {
            BacklogItemTypeValues.GOAL -> goal?.description
            BacklogItemTypeValues.CONTEXT -> context?.description
            else -> fallbackContext?.name
        }
    return TacticalPlanBacklogItem(
        item = this,
        title = title,
        description = description,
        alreadyInWeek = alreadyInWeek,
    )
}
