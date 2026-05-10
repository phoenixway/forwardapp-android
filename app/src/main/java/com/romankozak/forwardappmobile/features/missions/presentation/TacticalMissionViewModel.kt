package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.contexts.domain.clipboard.BacklogClipboardUseCase
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.NewTaskParameters
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import java.util.UUID
import javax.inject.Inject
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE

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
    ) : ViewModel() {
        companion object {
            const val MISSION_REMINDER_ENTITY_TYPE = "TACTICAL_MISSION"
        }

        private val _missions = MutableStateFlow<List<TacticalMission>>(emptyList())
        val missions: StateFlow<List<TacticalMission>> = _missions.asStateFlow()
        private val allContexts =
            contextRepository
                .getAllContextsFlow()
                .stateIn(
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
                ),
            )
        }

        fun addWeeklyMissionFromContext(contextId: String) {
            val context = allContexts.value.firstOrNull { it.id == contextId } ?: return
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
