package com.romankozak.forwardappmobile.features.mainscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionPriority
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestRepository
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconWithRelations
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.ScopeAttachmentOption
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.toScopeAttachmentOption
import com.romankozak.forwardappmobile.features.missions.domain.repository.MissionRepository
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.UUID
import javax.inject.Inject

private const val STRATEGIC_ARC_TAG = "arc"
private const val FLOW_STOP_TIMEOUT_MILLIS = 5000L

data class StrategicArcUiState(
    val allProjects: List<Context> = emptyList(),
    val projects: List<Context> = emptyList(),
    val beacons: List<MainBeaconWithRelations> = emptyList(),
    val beaconGroups: List<MainBeaconGroup> = emptyList(),
    val arcQuests: List<ArcQuestEntity> = emptyList(),
    val currentArcKey: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

enum class StrategicArcTab {
    QUESTS,
    ARTIFACT,
}

@HiltViewModel
class StrategicArcViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val settingsRepository: SettingsRepository,
        private val attachmentsRepository: AttachmentsRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
        private val arcQuestRepository: ArcQuestRepository,
        private val mainBeaconRepository: MainBeaconRepository,
        private val missionRepository: MissionRepository,
    ) : ViewModel() {
        private val currentArcKey = MutableStateFlow(YearMonth.now().toString())
        private val _selectedTab = MutableStateFlow(StrategicArcTab.QUESTS)
        val selectedTab: StateFlow<StrategicArcTab> = _selectedTab.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<StrategicArcUiState> =
            combine(
                contextRepository.getAllContextsFlow(),
                mainBeaconRepository.observeMainBeaconDetails(),
                mainBeaconRepository.observeGroups(),
                currentArcKey.flatMapLatest { arcKey -> arcQuestRepository.observeArcQuests(arcKey) },
                currentArcKey,
            ) { projects, beacons, beaconGroups, arcQuests, arcKey ->
                    val arcProjects =
                        projects.filter {
                            it.tags?.contains("arc") == true
                        }
                    StrategicArcUiState(
                        allProjects = projects,
                        projects = arcProjects,
                        beacons = beacons,
                        beaconGroups = beaconGroups,
                        arcQuests = arcQuests,
                        currentArcKey = arcKey,
                    )
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MILLIS),
                    initialValue = StrategicArcUiState(isLoading = true),
                )

        private val _attachmentOptions = MutableStateFlow<List<ScopeAttachmentOption>>(emptyList())
        val attachmentOptions: StateFlow<List<ScopeAttachmentOption>> = _attachmentOptions.asStateFlow()

        private val _linkedAttachmentIds = MutableStateFlow<List<String>>(emptyList())
        val linkedAttachmentIds: StateFlow<List<String>> = _linkedAttachmentIds.asStateFlow()

        private val _scopeContextsExpanded = MutableStateFlow(true)
        val scopeContextsExpanded: StateFlow<Boolean> = _scopeContextsExpanded.asStateFlow()

        private val _scopeAttachmentsExpanded = MutableStateFlow(true)
        val scopeAttachmentsExpanded: StateFlow<Boolean> = _scopeAttachmentsExpanded.asStateFlow()
        private val _connectionsOrder = MutableStateFlow<List<String>>(emptyList())
        val connectionsOrder: StateFlow<List<String>> = _connectionsOrder.asStateFlow()

        private val _isScopeLinksSheetVisible = MutableStateFlow(false)
        val isScopeLinksSheetVisible: StateFlow<Boolean> = _isScopeLinksSheetVisible.asStateFlow()
        private var arcQuestReorderJob: Job? = null
        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MILLIS), "")

        init {
            attachmentsRepository.getAttachmentLibraryItems()
                .onEach { results ->
                    _attachmentOptions.value = results.mapNotNull { it.toScopeAttachmentOption() }
                }
                .launchIn(viewModelScope)

            settingsRepository.strategicArcLinkedAttachmentIdsFlow
                .onEach { ids -> _linkedAttachmentIds.value = ids.toList() }
                .launchIn(viewModelScope)

            settingsRepository.strategicArcScopeContextsExpandedFlow
                .onEach { expanded -> _scopeContextsExpanded.value = expanded }
                .launchIn(viewModelScope)

            settingsRepository.strategicArcScopeAttachmentsExpandedFlow
                .onEach { expanded -> _scopeAttachmentsExpanded.value = expanded }
                .launchIn(viewModelScope)

            settingsRepository.strategicArcConnectionsOrderFlow
                .onEach { order -> _connectionsOrder.value = order }
                .launchIn(viewModelScope)
        }

        fun addArcLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, addTag = STRATEGIC_ARC_TAG)
            }
        }

        fun removeArcLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, removeTags = setOf(STRATEGIC_ARC_TAG))
            }
        }

        fun addAttachmentLink(attachmentId: String) {
            val next = (_linkedAttachmentIds.value + attachmentId).distinct()
            _linkedAttachmentIds.value = next
            viewModelScope.launch {
                settingsRepository.setStrategicArcLinkedAttachmentIds(next.toSet())
            }
        }

        fun removeAttachmentLink(attachmentId: String) {
            val next = _linkedAttachmentIds.value.filterNot { it == attachmentId }
            _linkedAttachmentIds.value = next
            viewModelScope.launch {
                settingsRepository.setStrategicArcLinkedAttachmentIds(next.toSet())
            }
        }

        fun addUrlLink(
            url: String,
            name: String,
        ) {
            val target = url.trim()
            if (target.isBlank()) return
            val display = name.trim().ifBlank { target }
            viewModelScope.launch {
                val attachmentId =
                    attachmentsRepository.createLinkAttachment(
                        contextId = SystemContexts.STRATEGIC.raw,
                        link = RelatedLink(type = LinkType.URL, target = target, displayName = display),
                    )
                addAttachmentLink(attachmentId)
            }
        }

        fun addObsidianLink(
            noteName: String,
            displayName: String,
            vault: String? = null,
        ) {
            val target = noteName.trim()
            if (target.isBlank()) return
            val display = displayName.trim().ifBlank { target }
            viewModelScope.launch {
                val attachmentId =
                    attachmentsRepository.createLinkAttachment(
                        contextId = SystemContexts.STRATEGIC.raw,
                        link = RelatedLink(type = LinkType.OBSIDIAN, target = target, displayName = display, vault = vault),
                    )
                addAttachmentLink(attachmentId)
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

        suspend fun createArcDocumentForPicker(request: NewDocumentDraft): String? {
            return when (request) {
                is NewDocumentDraft.Note -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "New note" },
                            contextId = SystemContexts.STRATEGIC.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, documentId)?.id
                }
                is NewDocumentDraft.JournalDocument -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "New journal" },
                            contextId = SystemContexts.STRATEGIC.raw,
                            attachmentType = BacklogItemTypeValues.JOURNAL_DOCUMENT,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.JOURNAL_DOCUMENT, documentId)?.id
                }
                is NewDocumentDraft.MusicNote -> {
                    val musicNoteId =
                        musicNoteRepository.create(
                            name = request.name.ifBlank { "New music note" },
                            contextId = SystemContexts.STRATEGIC.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.MUSIC_NOTE, musicNoteId)?.id
                }
                is NewDocumentDraft.Checklist -> {
                    val checklistId =
                        checklistRepository.createChecklist(
                            name = request.name.ifBlank { "New checklist" },
                            contextId = SystemContexts.STRATEGIC.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.CHECKLIST, checklistId)?.id
                }
                is NewDocumentDraft.WebLink -> {
                    val target = request.url.trim()
                    createLinkAttachmentOrNull(
                        type = LinkType.URL,
                        target = target,
                        displayName = request.name.trim().ifBlank { target },
                        contextId = SystemContexts.STRATEGIC.raw,
                    )
                }
                is NewDocumentDraft.Obsidian -> {
                    val target = request.noteName.trim()
                    createLinkAttachmentOrNull(
                        type = LinkType.OBSIDIAN,
                        target = target,
                        displayName = request.displayName.trim().ifBlank { target },
                        contextId = SystemContexts.STRATEGIC.raw,
                        vault = request.vault,
                    )
                }
            }
        }

        fun setScopeContextsExpanded(expanded: Boolean) {
            _scopeContextsExpanded.value = expanded
            viewModelScope.launch {
                settingsRepository.setStrategicArcScopeContextsExpanded(expanded)
            }
        }

        fun setScopeAttachmentsExpanded(expanded: Boolean) {
            _scopeAttachmentsExpanded.value = expanded
            viewModelScope.launch {
                settingsRepository.setStrategicArcScopeAttachmentsExpanded(expanded)
            }
        }

        fun toggleScopeLinksSheet() {
            _isScopeLinksSheetVisible.value = !_isScopeLinksSheetVisible.value
        }

        fun dismissScopeLinksSheet() {
            _isScopeLinksSheetVisible.value = false
        }

        fun updateConnectionsOrder(order: List<String>) {
            _connectionsOrder.value = order
            viewModelScope.launch {
                settingsRepository.setStrategicArcConnectionsOrder(order)
            }
        }

        fun selectTab(tab: StrategicArcTab) {
            _selectedTab.value = tab
        }

        fun addArcQuest(title: String) {
            val trimmed = title.trim()
            if (trimmed.isBlank()) return
            viewModelScope.launch {
                arcQuestRepository.addQuest(
                    ArcQuestEntity(
                        arcKey = currentArcKey.value,
                        title = trimmed,
                    ),
                )
            }
        }

        fun addArcQuestFromContext(contextId: String) {
            viewModelScope.launch {
                val context = contextRepository.getContextById(contextId) ?: return@launch
                arcQuestRepository.addQuest(
                    ArcQuestEntity(
                        arcKey = currentArcKey.value,
                        title = context.name,
                        linkedContextId = context.id,
                        sourceType = ArcQuestSourceType.CONTEXT.name,
                        sourceId = context.id,
                    ),
                )
            }
        }

        fun addArcQuestFromBeacon(beaconId: String) {
            viewModelScope.launch {
                val beacon =
                    mainBeaconRepository
                        .getBeaconById(beaconId)
                        ?: return@launch
                arcQuestRepository.addQuest(
                    ArcQuestEntity(
                        arcKey = currentArcKey.value,
                        title = beacon.title,
                        description = beacon.description,
                        sourceType = ArcQuestSourceType.BEACON.name,
                        sourceId = beacon.id,
                    ),
                )
            }
        }

        fun addArcQuestFromBeaconGroup(groupId: String) {
            viewModelScope.launch {
                val group =
                    mainBeaconRepository
                        .getGroupById(groupId)
                        ?: return@launch
                arcQuestRepository.addQuest(
                    ArcQuestEntity(
                        arcKey = currentArcKey.value,
                        title = group.title,
                        description = group.description,
                        sourceType = ArcQuestSourceType.BEACON_GROUP.name,
                        sourceId = group.id,
                    ),
                )
            }
        }

        fun updateArcQuest(
            quest: ArcQuestEntity,
            title: String,
            description: String?,
        ) {
            val trimmed = title.trim()
            if (trimmed.isBlank()) return
            viewModelScope.launch {
                arcQuestRepository.updateQuest(
                    quest.copy(
                        title = trimmed,
                        description = description?.trim()?.ifBlank { null },
                    ),
                )
            }
        }

        fun deleteArcQuest(quest: ArcQuestEntity) {
            viewModelScope.launch {
                arcQuestRepository.deleteQuest(quest)
            }
        }

        fun reorderArcQuests(quests: List<ArcQuestEntity>) {
            val reorderedQuests = quests.toList()
            arcQuestReorderJob?.cancel()
            arcQuestReorderJob =
                viewModelScope.launch {
                    arcQuestRepository.reorder(reorderedQuests)
                }
        }

        fun createMissionFromArcQuest(quest: ArcQuestEntity) {
            val title = quest.title.trim()
            if (title.isBlank()) return
            viewModelScope.launch {
                val missionId =
                    missionRepository.insertMissionWithAutoOrder(
                        TacticalMission(
                            title = title,
                            description = quest.description,
                            deadline = NO_DEADLINE,
                            status = MissionStatus.ACTIVE,
                            priority = MissionPriority.MEDIUM,
                            projectId = quest.linkedContextId,
                            linkedProjectIds = quest.linkedContextId?.let(::listOf).orEmpty(),
                            linkedAttachmentIds = emptyList(),
                            weekKey = currentIsoWeekKey(),
                            sourceType = MissionSourceType.ARC_QUEST,
                            sourceArcQuestId = quest.id,
                        ),
                    )
                arcQuestRepository.updateQuest(quest.copy(linkedMissionId = missionId))
            }
        }

        fun openOrCreateArcArtifact(onReady: (String) -> Unit) {
            val arcKey = currentArcKey.value
            val name = "Strategic Arc $arcKey"
            viewModelScope.launch {
                val documentId =
                    noteDocumentRepository.findDocumentByName(name)?.id
                        ?: noteDocumentRepository.createDocument(
                            name = name,
                            contextId = SystemContexts.STRATEGIC.raw,
                            content = "# Strategic Arc $arcKey\n\n",
                            roleCode = "strategic_arc_artifact",
                            isSystem = false,
                        )
                onReady(documentId)
            }
        }

        private suspend fun updateTags(
            contextId: String,
            addTag: String? = null,
            removeTags: Set<String> = emptySet(),
        ) {
            val context = contextRepository.getContextById(contextId) ?: return
            val current = context.tags.orEmpty()
            val next =
                current
                    .filterNot { it in removeTags }
                    .toMutableList()
            if (addTag != null && addTag !in next) {
                next.add(addTag)
            }
            if (next != current) {
                contextRepository.updateContext(context.copy(tags = next))
            }
        }

        private suspend fun createLinkAttachmentOrNull(
            type: LinkType,
            target: String,
            displayName: String,
            contextId: String,
            vault: String? = null,
        ): String? {
            if (target.isBlank()) {
                return null
            }

            return attachmentsRepository.createLinkAttachment(
                contextId = contextId,
                link =
                    RelatedLink(
                        type = type,
                        target = target,
                        displayName = displayName,
                        vault = vault?.trim()?.ifBlank { null },
                    ),
            )
        }
    }

private fun currentIsoWeekKey(): String {
    val now = LocalDate.now()
    val weekFields = WeekFields.ISO
    val weekBasedYear = now.get(weekFields.weekBasedYear())
    val week = now.get(weekFields.weekOfWeekBasedYear())
    return "%04d-W%02d".format(weekBasedYear, week)
}
