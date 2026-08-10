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
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.GENERAL_MISSION_STREAM_ID
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
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
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionStreamRepository
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
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationType
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.UUID
import javax.inject.Inject
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.features.missions.domain.repository.TacticalIterationRepository

enum class TacticsWorkspaceMode {
    STREAMS,
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
	        private val tacticalIterationRepository: TacticalIterationRepository,
	        private val missionStreamRepository: MissionStreamRepository,
	        private val tacticsWorkspaceStateRepository: TacticsWorkspaceStateRepository,
	        private val listItemDao: ListItemDao,
	        private val goalDao: GoalDao,
	    ) : ViewModel() {
        companion object {
            const val MISSION_REMINDER_ENTITY_TYPE = "TACTICAL_MISSION"
            private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        }

        private val _missions = MutableStateFlow<List<TacticalMission>>(emptyList())
        val missions: StateFlow<List<TacticalMission>> = _missions.asStateFlow()
        private val _selectedMode = MutableStateFlow(TacticsWorkspaceMode.STREAMS)
        val selectedMode: StateFlow<TacticsWorkspaceMode> = _selectedMode.asStateFlow()
        private val _selectedActivitySlotContextId = MutableStateFlow<String?>(null)
        val selectedActivitySlotContextId: StateFlow<String?> = _selectedActivitySlotContextId.asStateFlow()
        private val _selectedMissionStreamId = MutableStateFlow(GENERAL_MISSION_STREAM_ID)
        val selectedMissionStreamId: StateFlow<String> = _selectedMissionStreamId.asStateFlow()
        private val _selectedPlanningContextId = MutableStateFlow<String?>(null)
        val selectedPlanningContextId: StateFlow<String?> = _selectedPlanningContextId.asStateFlow()
        private val _recentMissionStreamIds = MutableStateFlow(listOf(GENERAL_MISSION_STREAM_ID))
        private val _iterationDurationDays = MutableStateFlow<Int?>(null)
        val iterationDurationDays: StateFlow<Int?> = _iterationDurationDays.asStateFlow()
        private val _iterationDurationHours = MutableStateFlow<Int?>(null)
        val iterationDurationHours: StateFlow<Int?> = _iterationDurationHours.asStateFlow()
        private val _activeIteration = MutableStateFlow<TacticalIteration?>(null)
        val activeIteration: StateFlow<TacticalIteration?> = _activeIteration.asStateFlow()
        private val _isMissionStreamsSheetVisible = MutableStateFlow(false)
	        val isMissionStreamsSheetVisible: StateFlow<Boolean> = _isMissionStreamsSheetVisible.asStateFlow()
        val currentWeekKey: String = currentIsoWeekKey()
        val tacticalIterations: StateFlow<List<TacticalIteration>> =
            tacticalIterationRepository
                .observeIterations()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )
        private val allContexts =
            contextRepository
                .getAllContextsFlow()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList(),
                )
        val missionStreams: StateFlow<List<MissionStream>> =
            missionStreamRepository
                .observeActiveStreams()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue =
                        listOf(
                            MissionStream(
                                id = GENERAL_MISSION_STREAM_ID,
                                title = "General",
                                isDefault = true,
                                streamOrder = Long.MIN_VALUE,
                            ),
                        ),
	                )
	        val recentMissionStreams: StateFlow<List<MissionStream>> =
	            combine(missionStreams, _recentMissionStreamIds) { streams, recentIds ->
	                streams.sortedByRecent(recentIds)
	            }.stateIn(
	                scope = viewModelScope,
	                started = SharingStarted.Eagerly,
	                initialValue = missionStreams.value,
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
            combine(
                missions,
                selectedMode,
                selectedMissionStreamId,
                missionStreams,
                activeIteration,
            ) { allMissions, mode, selectedStreamId, streams, iteration ->
                val weekMissions =
                    allMissions
                        .filter { it.isInCurrentIteration(iteration?.id, currentWeekKey) }
                        .filter { it.sourceBacklogItemId == null }
                val streamOrderById = streams.mapIndexed { index, stream -> stream.id to index }.toMap()
                val effectiveMode =
                    if (mode == TacticsWorkspaceMode.PLAN && iteration?.status == TacticalIterationStatus.ACTIVE) {
                        TacticsWorkspaceMode.STREAMS
                    } else {
                        mode
                    }
                when (effectiveMode) {
                    TacticsWorkspaceMode.STREAMS ->
                        weekMissions
                            .filter { it.normalizedMissionStreamId() == selectedStreamId }
                            .sortedWith(compareBy<TacticalMission> { it.orderInWeek }.thenBy { it.createdAt })
                    TacticsWorkspaceMode.ALL ->
                        weekMissions.sortedWith(
                            compareBy<TacticalMission> {
                                streamOrderById[it.normalizedMissionStreamId()] ?: Int.MAX_VALUE
                            }
                                .thenBy { it.orderInWeek }
                                .thenBy { it.createdAt },
                        )
                    TacticsWorkspaceMode.PLAN ->
                        weekMissions.sortedWith(compareBy<TacticalMission> { it.orderInWeek }.thenBy { it.createdAt })
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )
        val missionStreamCounts: StateFlow<Map<String, Int>> =
            missions
                .combine(activeIteration) { allMissions, iteration ->
                    allMissions
                        .filter { it.isInCurrentIteration(iteration?.id, currentWeekKey) }
                        .filter { it.sourceBacklogItemId == null }
                        .groupingBy { it.normalizedMissionStreamId() }
                        .eachCount()
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyMap(),
                )
        @OptIn(ExperimentalCoroutinesApi::class)
        val planningBacklogItems: StateFlow<List<TacticalPlanBacklogItem>> =
            combine(
                selectedPlanningContextId.flatMapLatest { contextId ->
                    if (contextId == null) {
                        flowOf(contextId to emptyList<BacklogItem>())
                    } else {
                        listItemDao.getItemsForContextStream(contextId).map { items -> contextId to items }
                    }
                },
                goalDao.getAllVisibleGoalsFlow(),
                allContexts,
                missions,
                activeIteration,
            ) { backlogState, goals, contexts, allMissions, iteration ->
                val (contextId, backlogItems) = backlogState
                val goalById = goals.associateBy { it.id }
                val contextById = contexts.associateBy { it.id }
                val selectedBacklogIds =
                    allMissions
                        .asSequence()
                        .filter { it.isInCurrentIteration(iteration?.id, currentWeekKey) }
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
            viewModelScope.launch {
                missionStreamRepository.ensureDefaultStream()
            }
            viewModelScope.launch {
                val currentIteration = tacticalIterationRepository.getCurrentIteration()
                _activeIteration.value = currentIteration
                when {
                    currentIteration?.status == TacticalIterationStatus.DRAFT -> selectMode(TacticsWorkspaceMode.PLAN)
                    currentIteration?.status == TacticalIterationStatus.ACTIVE &&
                        _selectedMode.value == TacticsWorkspaceMode.PLAN ->
                        selectMode(TacticsWorkspaceMode.STREAMS)
                }
            }
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

	            tacticsWorkspaceStateRepository.state
	                .onEach { state ->
	                    _selectedMode.value = state.selectedMode
                    _selectedMissionStreamId.value = state.selectedMissionStreamId
                    _selectedPlanningContextId.value = state.selectedPlanningContextId
                    _recentMissionStreamIds.value = state.recentMissionStreamIds
                    _iterationDurationDays.value = state.iterationDurationDays
                    _iterationDurationHours.value = state.iterationDurationHours
                }
	                .launchIn(viewModelScope)

	            backlogClipboardUseCase.clipboardPayload
	                .onEach { _canPasteAsMissions.value = backlogClipboardUseCase.canPasteIntoTacticalMissions() }
                .launchIn(viewModelScope)

	            missionStreams
	                .onEach { streams ->
	                    if (streams.none { it.id == _selectedMissionStreamId.value }) {
	                        selectMissionStream(GENERAL_MISSION_STREAM_ID)
	                    }
	                }
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
                    .filter {
                        it.isCurrentWeekMission(currentWeekKey) &&
                            it.activitySlotContextId == activitySlotContextId
                    }
                    .mapNotNull { it.orderInSlot }
                    .minOrNull() ?: 0L
            return minOrder - 1L
        }

        private fun activeMissionStreamId(): String = _selectedMissionStreamId.value

        private fun activeIterationId(): String = _activeIteration.value?.id ?: currentWeekKey

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
                    iterationId = activeIterationId(),
                    missionStreamId = activeMissionStreamId(),
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

        fun moveMissionToCurrentIteration(mission: TacticalMission) {
            val activeIterationId = activeIterationId()
            if (mission.isInCurrentIteration(activeIterationId, currentWeekKey)) return
            viewModelScope.launch {
                val streamId = mission.normalizedMissionStreamId()
                val duplicateExists =
                    _missions.value.any { candidate ->
                        candidate.isInCurrentIteration(activeIterationId, currentWeekKey) &&
                            candidate.title.equals(mission.title, ignoreCase = true) &&
                            candidate.normalizedMissionStreamId() == streamId &&
                            candidate.sourceBacklogItemId == mission.sourceBacklogItemId
                    }
                if (duplicateExists) {
                    _uiMessages.tryEmit("Місія вже є в поточній ітерації")
                    return@launch
                }

                val carriedMission =
                    mission.copy(
                        id = 0,
                        weekKey = currentWeekKey,
                        iterationId = activeIterationId,
                        carriedFromMissionId = mission.id,
                        status = MissionStatus.ACTIVE,
                        startTime = System.currentTimeMillis(),
                        deadline = currentIterationDeadline(),
                        orderInSlot = nextSlotOrder(mission.activitySlotContextId),
                        sourceType = MissionSourceType.PREVIOUS_WEEK,
                    )
                val id = missionRepository.insertMissionWithAutoOrder(carriedMission)
                missionRepository.setAttachments(id, carriedMission.linkedAttachmentIds ?: emptyList())
                _pendingScrollToMissionId.value = id
                _uiMessages.tryEmit("Місію перенесено в поточну ітерацію")
            }
        }

        private fun currentIterationDeadline(): Long {
            val iteration = _activeIteration.value
            val plannedEndAt = iteration?.plannedEndAt
            val days = _iterationDurationDays.value?.takeIf { it > 0 }
            return when {
                plannedEndAt != null -> plannedEndAt
                iteration?.type == TacticalIterationType.OPEN_ENDED -> NO_DEADLINE
                days != null -> System.currentTimeMillis() + days * MILLIS_PER_DAY
                else -> NO_DEADLINE
            }
        }

        fun addQuickMission(title: String) {
            addQuickMission(title = title, activitySlotContextId = null)
        }

        fun addQuickMissionForCurrentStream(title: String) {
            addQuickMission(title = title, activitySlotContextId = null)
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
                    iterationId = activeIterationId(),
                    missionStreamId = activeMissionStreamId(),
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
                    iterationId = activeIterationId(),
                    missionStreamId = activeMissionStreamId(),
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
	            viewModelScope.launch {
	                tacticsWorkspaceStateRepository.setSelectedMode(mode)
	            }
	        }

        fun selectActivitySlot(contextId: String?) {
            _selectedActivitySlotContextId.value = contextId
        }

	        fun selectMissionStream(streamId: String) {
	            _selectedMissionStreamId.value = streamId
	            _recentMissionStreamIds.value = _recentMissionStreamIds.value.withRecentFirst(streamId)
	            viewModelScope.launch {
	                tacticsWorkspaceStateRepository.setSelectedMissionStream(streamId)
	            }
	        }

        fun openMissionStreamsSheet() {
            _isMissionStreamsSheetVisible.value = true
        }

        fun dismissMissionStreamsSheet() {
            _isMissionStreamsSheetVisible.value = false
        }

	        fun addMissionStream(title: String) {
	            viewModelScope.launch {
	                missionStreamRepository.addStream(title)?.let { streamId ->
	                    selectMissionStream(streamId)
	                }
	            }
	        }

        fun updateMissionStream(
            stream: MissionStream,
            title: String,
            description: String?,
            budgetPercent: Int? = stream.budgetPercent,
        ) {
            viewModelScope.launch {
                missionStreamRepository.updateStream(stream, title, description, budgetPercent)
            }
        }

        fun setIterationDuration(
            days: Int?,
            hours: Int?,
        ) {
            viewModelScope.launch {
                val normalizedDays = days?.takeIf { it > 0 }
                val normalizedHours =
                    hours
                        ?.takeIf { it > 0 }
                        ?.takeIf { candidate -> normalizedDays?.let { candidate <= it * 24 } == true }
                tacticsWorkspaceStateRepository.setIterationDuration(
                    days = normalizedDays,
                    hours = normalizedHours,
                )
            }
        }

        fun archiveMissionStream(stream: MissionStream) {
            if (stream.isDefault) return
            viewModelScope.launch {
                _missions.value
                    .filter { it.normalizedMissionStreamId() == stream.id }
                    .forEach { mission ->
                        updateTacticalMissionUseCase(mission.copy(missionStreamId = GENERAL_MISSION_STREAM_ID))
                    }
	                missionStreamRepository.archiveStream(stream.id)
	                if (_selectedMissionStreamId.value == stream.id) {
	                    selectMissionStream(GENERAL_MISSION_STREAM_ID)
	                }
	            }
	        }

        fun reorderMissionStreams(streams: List<MissionStream>) {
            viewModelScope.launch {
                missionStreamRepository.reorder(streams)
            }
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
	            viewModelScope.launch {
	                tacticsWorkspaceStateRepository.setSelectedPlanningContext(contextId)
	            }
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

        fun assignMissionToStream(
            mission: TacticalMission,
            streamId: String,
        ) {
            updateMission(mission.copy(missionStreamId = streamId))
        }

        fun completeMission(mission: TacticalMission) {
            updateMission(mission.copy(status = MissionStatus.COMPLETED))
        }

        fun pauseMission(mission: TacticalMission) {
            updateMission(mission.copy(status = MissionStatus.PAUSED))
        }

        fun activateMission(mission: TacticalMission) {
            updateMission(mission.copy(status = MissionStatus.ACTIVE))
        }

        fun startTimeboxedIteration() {
            viewModelScope.launch {
                _activeIteration.value = tacticalIterationRepository.startCurrentOrCreateTimeboxed(currentWeekKey)
                selectMode(TacticsWorkspaceMode.STREAMS)
                _uiMessages.tryEmit("Тактичний цикл у виконанні")
            }
        }

        fun startOpenEndedIteration() {
            viewModelScope.launch {
                _activeIteration.value = tacticalIterationRepository.closeActiveAndStartOpenEnded()
                selectMode(TacticsWorkspaceMode.PLAN)
                _uiMessages.tryEmit("Створено відкритий тактичний цикл")
            }
        }

        fun planTimeboxedIteration() {
            viewModelScope.launch {
                _activeIteration.value = tacticalIterationRepository.ensureDraftTimeboxed(currentWeekKey)
                selectMode(TacticsWorkspaceMode.PLAN)
                _uiMessages.tryEmit("Тактичний цикл у розробці")
            }
        }

        fun startNewTimeboxedIteration() {
            viewModelScope.launch {
                _activeIteration.value = tacticalIterationRepository.closeActiveAndStartTimeboxed(currentWeekKey)
                selectMode(TacticsWorkspaceMode.PLAN)
                _uiMessages.tryEmit("Створено новий тактичний цикл")
            }
        }

        fun finishCurrentIteration() {
            viewModelScope.launch {
                tacticalIterationRepository.finishCurrentIteration()
                _activeIteration.value = tacticalIterationRepository.getCurrentIteration()
                selectMode(TacticsWorkspaceMode.STREAMS)
                _uiMessages.tryEmit("Тактичний цикл завершено")
            }
        }

        fun finishCurrentAndPlanNextIteration() {
            viewModelScope.launch {
                _activeIteration.value = tacticalIterationRepository.finishCurrentAndCreateDraft(currentWeekKey)
                selectMode(TacticsWorkspaceMode.PLAN)
                _uiMessages.tryEmit("Цикл завершено, новий цикл у розробці")
            }
        }

        fun reorderVisibleMissions(reordered: List<TacticalMission>) {
            viewModelScope.launch {
                reordered.forEachIndexed { index, mission ->
                    updateTacticalMissionUseCase(mission.copy(orderInWeek = index.toLong(), order = index.toLong()))
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
                    iterationId = activeIterationId(),
                    missionStreamId = activeMissionStreamId(),
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
                val report =
                    backlogClipboardUseCase.pasteIntoTacticalMissions(
                        targetWeekKey = currentWeekKey,
                        targetMissionStreamId = activeMissionStreamId(),
                    )
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

fun TacticalMission.isCurrentWeekMission(currentWeekKey: String): Boolean =
    isInCurrentIteration(activeIterationId = currentWeekKey, currentWeekKey = currentWeekKey)

fun TacticalMission.isInCurrentIteration(
    activeIterationId: String?,
    currentWeekKey: String,
): Boolean =
    when {
        activeIterationId != null && iterationId == activeIterationId -> true
        iterationId != null -> false
        activeIterationId != null && activeIterationId != currentWeekKey -> false
        else -> weekKey.isBlank() || weekKey == currentWeekKey
    }

fun TacticalMission.normalizedMissionStreamId(): String = missionStreamId ?: GENERAL_MISSION_STREAM_ID

private fun List<MissionStream>.sortedByRecent(recentIds: List<String>): List<MissionStream> {
    val recentRankById = recentIds.withIndex().associate { it.value to it.index }
    return sortedWith(
        compareBy<MissionStream> { recentRankById[it.id] ?: Int.MAX_VALUE }
            .thenBy { indexOf(it).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE },
    )
}

private fun List<String>.withRecentFirst(streamId: String): List<String> =
    (listOf(streamId) + filterNot { it == streamId })
        .take(30)

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
