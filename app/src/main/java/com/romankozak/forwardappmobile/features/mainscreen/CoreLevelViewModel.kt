package com.romankozak.forwardappmobile.features.mainscreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconLevelType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.repository.MusicNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconCardUi
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconEditorState
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconGroupUi
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconWithRelations
import com.romankozak.forwardappmobile.features.mainscreen.core.deriveMainBeaconCompactCardSummary
import com.romankozak.forwardappmobile.features.mainscreen.core.displayLabel
import com.romankozak.forwardappmobile.features.mainscreen.core.toEditorState
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.ScopeAttachmentOption
import com.romankozak.forwardappmobile.features.mainscreen.scopelinks.toScopeAttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.NewDocumentDraft
import com.romankozak.forwardappmobile.sync.AttachmentsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val CORE_TAGS = setOf("core", "main-beacons")
private const val FLOW_STOP_TIMEOUT_MILLIS = 5000L

data class CoreLevelUiState(
    val allProjects: List<Context> = emptyList(),
    val projects: List<Context> = emptyList(),
    val beacons: List<MainBeaconCardUi> = emptyList(),
    val groups: List<MainBeaconGroupUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CoreLevelViewModel
    @Inject
    constructor(
        private val contextRepository: ContextRepository,
        private val settingsRepository: SettingsRepository,
        private val attachmentsRepository: AttachmentsRepository,
        private val noteDocumentRepository: NoteDocumentRepository,
        private val musicNoteRepository: MusicNoteRepository,
        private val checklistRepository: ChecklistRepository,
        private val mainBeaconRepository: MainBeaconRepository,
    ) : ViewModel() {
        private val allContexts =
            contextRepository.getAllContextsFlow()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MILLIS),
                    initialValue = emptyList(),
                )

        val mainBeaconDetails: StateFlow<List<MainBeaconWithRelations>> =
            mainBeaconRepository.observeMainBeaconDetails()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MILLIS),
                    initialValue = emptyList(),
                )

        private val mainBeaconGroups: StateFlow<List<MainBeaconGroup>> =
            mainBeaconRepository.observeGroups()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MILLIS),
                    initialValue = emptyList(),
                )

        val uiState: StateFlow<CoreLevelUiState> =
            combine(allContexts, mainBeaconDetails, mainBeaconGroups) { projects, beacons, groups ->
                val coreProjects =
                    projects.filter {
                        it.tags?.contains("main-beacons") == true || it.tags?.contains("core") == true
                    }
                CoreLevelUiState(
                    allProjects = projects,
                    projects = coreProjects,
                    groups =
                        groups.map { group ->
                            MainBeaconGroupUi(
                                id = group.id,
                                title = group.title,
                                description = group.description,
                            )
                        },
                    beacons =
                        beacons.map { details ->
                            val compactSummary = deriveMainBeaconCompactCardSummary(details.levelStatuses)
                            MainBeaconCardUi(
                                id = details.beacon.id,
                                title = details.beacon.title,
                                readinessStatus = details.beacon.readinessStatus,
                                highestCompletedLevel = compactSummary.highestCompletedLevel,
                                breakPointLevel = compactSummary.breakPointLevel,
                                blockReason = compactSummary.blockReason,
                                nextRequiredAction = compactSummary.nextRequiredAction,
                                relatedContextIds = details.relatedContexts.map { it.id },
                                relatedAttachmentIds = details.relatedAttachments.map { it.id },
                                groupIds = details.groupIds,
                                parentBeaconId = details.beacon.parentBeaconId,
                            )
                        },
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MILLIS),
                initialValue = CoreLevelUiState(isLoading = true),
            )

        private val _attachmentOptions = MutableStateFlow<List<ScopeAttachmentOption>>(emptyList())
        val attachmentOptions: StateFlow<List<ScopeAttachmentOption>> = _attachmentOptions.asStateFlow()

        private val _linkedAttachmentIds = MutableStateFlow<List<String>>(emptyList())
        val linkedAttachmentIds: StateFlow<List<String>> = _linkedAttachmentIds.asStateFlow()

        private val _connectionsOrder = MutableStateFlow<List<String>>(emptyList())
        val connectionsOrder: StateFlow<List<String>> = _connectionsOrder.asStateFlow()

        val collapsedGroupIds: StateFlow<Set<String>> =
            settingsRepository.coreBeaconCollapsedGroupIdsFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MILLIS), emptySet())

        private val _isScopeLinksSheetVisible = MutableStateFlow(false)
        val isScopeLinksSheetVisible: StateFlow<Boolean> = _isScopeLinksSheetVisible.asStateFlow()
        val obsidianVaultName: StateFlow<String> =
            settingsRepository.obsidianVaultNameFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(FLOW_STOP_TIMEOUT_MILLIS), "")

        init {
            attachmentsRepository.getAttachmentLibraryItems()
                .onEach { results ->
                    _attachmentOptions.value = results.mapNotNull { it.toScopeAttachmentOption() }
                }.launchIn(viewModelScope)

            settingsRepository.coreLinkedAttachmentIdsFlow
                .onEach { ids -> _linkedAttachmentIds.value = ids.toList() }
                .launchIn(viewModelScope)

            settingsRepository.coreConnectionsOrderFlow
                .onEach { order -> _connectionsOrder.value = order }
                .launchIn(viewModelScope)
        }

        fun buildEditorState(beaconId: String?): MainBeaconEditorState {
            val details = beaconId?.let { id ->
                mainBeaconDetails.value.firstOrNull { it.beacon.id == id }
            }
            return when {
                beaconId == null ->
                    MainBeaconEditorState(
                    levelStatuses =
                        MainBeaconRepository.DefaultLevels.map { levelType ->
                            MainBeaconLevelStatus(
                                mainBeaconId = "",
                                levelType = levelType,
                                syncStatus = MainBeaconRepository.defaultSyncStatus(levelType),
                            ).toEditorState()
                        },
                    )
                details == null -> MainBeaconEditorState()
                else ->
                    MainBeaconEditorState(
                        id = details.beacon.id,
                        title = details.beacon.title,
                        description = details.beacon.description.orEmpty(),
                        whyItMatters = details.beacon.whyItMatters.orEmpty(),
                        successShape = details.beacon.successShape.orEmpty(),
                        failureShape = details.beacon.failureShape.orEmpty(),
                        antiGoal = details.beacon.antiGoal.orEmpty(),
                        decisionImpact = details.beacon.decisionImpact.orEmpty(),
                        readinessStatus = details.beacon.readinessStatus,
                        blockerText = details.beacon.blockerText.orEmpty(),
                        nextActionText = details.beacon.nextActionText.orEmpty(),
                        relatedContextIds =
                            details.relatedContexts.mapTo(linkedSetOf()) { it.id },
                        relatedAttachmentIds =
                            details.relatedAttachments.mapTo(linkedSetOf()) { it.id },
                        groupIds = details.groupIds.toSet(),
                        parentBeaconId = details.beacon.parentBeaconId,
                        levelStatuses = details.levelStatuses.map { it.toEditorState() },
                        createdAt = details.beacon.createdAt,
                        updatedAt = details.beacon.updatedAt,
                        isNew = false,
                    )
            }
        }

        fun saveBeacon(editor: MainBeaconEditorState) {
            val title = editor.title.trim()
            if (title.isBlank()) return
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val existing =
                    editor.id?.let { id ->
                        mainBeaconDetails.value.firstOrNull { it.beacon.id == id }?.beacon
                    }
                val beaconId = existing?.id ?: editor.id ?: UUID.randomUUID().toString()
                val parentBeaconId = normalizedParentBeaconId(beaconId, editor.parentBeaconId)
                val beacon =
                    MainBeacon(
                        id = beaconId,
                        title = title,
                        description = editor.description.trim().ifBlank { null },
                        whyItMatters = editor.whyItMatters.trim().ifBlank { null },
                        successShape = editor.successShape.trim().ifBlank { null },
                        failureShape = editor.failureShape.trim().ifBlank { null },
                        antiGoal = editor.antiGoal.trim().ifBlank { null },
                        decisionImpact = editor.decisionImpact.trim().ifBlank { null },
                        readinessStatus = editor.readinessStatus,
                        blockerText = editor.blockerText.trim().ifBlank { null },
                        nextActionText = editor.nextActionText.trim().ifBlank { null },
                        parentBeaconId = parentBeaconId,
                        order = existing?.order ?: 0L,
                        updatedAt = now,
                        createdAt = existing?.createdAt ?: now,
                    )
                val levelStatuses =
                    editor.levelStatuses.map { level ->
                        MainBeaconLevelStatus(
                            mainBeaconId = beacon.id,
                            levelType = level.levelType,
                            generalStatus = level.generalStatus,
                            syncStatus = level.syncStatus,
                            blockerText = level.blockerText.trim().ifBlank { null },
                            nextActionText = level.nextActionText.trim().ifBlank { null },
                            updatedAt = now,
                        )
                    }

                if (existing == null) {
                    mainBeaconRepository.createBeacon(
                        beacon = beacon,
                        relatedContextIds = editor.relatedContextIds,
                        relatedAttachmentIds = editor.relatedAttachmentIds,
                        groupIds = editor.groupIds,
                        levelStatuses = levelStatuses,
                    )
                } else {
                    mainBeaconRepository.updateBeacon(
                        beacon = beacon,
                        relatedContextIds = editor.relatedContextIds,
                        relatedAttachmentIds = editor.relatedAttachmentIds,
                        groupIds = editor.groupIds,
                        levelStatuses = levelStatuses,
                    )
                }
            }
        }

        private fun normalizedParentBeaconId(
            beaconId: String,
            requestedParentId: String?,
        ): String? {
            val parentId = requestedParentId?.takeIf { it != beaconId }
            val byId = mainBeaconDetails.value.associateBy { it.beacon.id }
            var cursor = parentId
            val visited = mutableSetOf<String>()
            var createsCycle = parentId == null || parentId !in byId
            while (cursor != null && visited.add(cursor)) {
                if (cursor == beaconId) {
                    createsCycle = true
                }
                cursor = byId[cursor]?.beacon?.parentBeaconId
            }
            return parentId.takeUnless { createsCycle }
        }

        fun createBeaconGroup(
            title: String,
            description: String? = null,
        ) {
            viewModelScope.launch {
                mainBeaconRepository.createGroup(title, description)
            }
        }

        fun updateBeaconGroup(
            groupId: String,
            title: String,
            description: String? = null,
        ) {
            val current = mainBeaconGroups.value.firstOrNull { it.id == groupId } ?: return
            viewModelScope.launch {
                mainBeaconRepository.updateGroup(
                    current.copy(
                        title = title,
                        description = description,
                    ),
                )
            }
        }

        fun deleteBeaconGroup(groupId: String) {
            viewModelScope.launch {
                mainBeaconRepository.deleteGroup(groupId)
            }
        }

        fun deleteBeacon(beaconId: String) {
            viewModelScope.launch {
                mainBeaconRepository.deleteBeacon(beaconId)
            }
        }

        fun reorderBeacons(beaconIdsInOrder: List<String>) {
            viewModelScope.launch {
                mainBeaconRepository.reorderBeacons(beaconIdsInOrder)
            }
        }

        fun addCoreLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, addTag = "core")
            }
        }

        fun removeCoreLink(contextId: String) {
            viewModelScope.launch {
                updateTags(contextId, removeTags = CORE_TAGS)
            }
        }

        fun addAttachmentLink(attachmentId: String) {
            val next = (_linkedAttachmentIds.value + attachmentId).distinct()
            _linkedAttachmentIds.value = next
            viewModelScope.launch {
                settingsRepository.setCoreLinkedAttachmentIds(next.toSet())
            }
        }

        fun removeAttachmentLink(attachmentId: String) {
            val next = _linkedAttachmentIds.value.filterNot { it == attachmentId }
            _linkedAttachmentIds.value = next
            viewModelScope.launch {
                settingsRepository.setCoreLinkedAttachmentIds(next.toSet())
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
                        contextId = SystemContexts.MAIN_BEACONS.raw,
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
                        contextId = SystemContexts.MAIN_BEACONS.raw,
                        link =
                            RelatedLink(
                                type = LinkType.OBSIDIAN,
                                target = target,
                                displayName = display,
                                vault = vault,
                            ),
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

        suspend fun createCoreDocumentForPicker(request: NewDocumentDraft): String? {
            return when (request) {
                is NewDocumentDraft.Note -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "New note" },
                            contextId = SystemContexts.MAIN_BEACONS.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.NOTE_DOCUMENT, documentId)?.id
                }
                is NewDocumentDraft.JournalDocument -> {
                    val documentId =
                        noteDocumentRepository.createDocument(
                            name = request.name.ifBlank { "New journal" },
                            contextId = SystemContexts.MAIN_BEACONS.raw,
                            attachmentType = BacklogItemTypeValues.JOURNAL_DOCUMENT,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.JOURNAL_DOCUMENT, documentId)?.id
                }
                is NewDocumentDraft.MusicNote -> {
                    val musicNoteId =
                        musicNoteRepository.create(
                            name = request.name.ifBlank { "New music note" },
                            contextId = SystemContexts.MAIN_BEACONS.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.MUSIC_NOTE, musicNoteId)?.id
                }
                is NewDocumentDraft.Checklist -> {
                    val checklistId =
                        checklistRepository.createChecklist(
                            name = request.name.ifBlank { "New checklist" },
                            contextId = SystemContexts.MAIN_BEACONS.raw,
                        )
                    attachmentsRepository.findAttachmentByEntity(BacklogItemTypeValues.CHECKLIST, checklistId)?.id
                }
                is NewDocumentDraft.WebLink -> {
                    createLinkAttachmentOrNull(
                        type = LinkType.URL,
                        target = request.url.trim(),
                        displayName = request.name.trim().ifBlank { request.url.trim() },
                        contextId = SystemContexts.MAIN_BEACONS.raw,
                    )
                }
                is NewDocumentDraft.Obsidian -> {
                    createLinkAttachmentOrNull(
                        type = LinkType.OBSIDIAN,
                        target = request.noteName.trim(),
                        displayName = request.displayName.trim().ifBlank { request.noteName.trim() },
                        contextId = SystemContexts.MAIN_BEACONS.raw,
                        vault = request.vault,
                    )
                }
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
                settingsRepository.setCoreConnectionsOrder(order)
            }
        }

        fun setGroupExpanded(
            groupId: String,
            expanded: Boolean,
        ) {
            viewModelScope.launch {
                val nextCollapsedIds =
                    if (expanded) {
                        collapsedGroupIds.value - groupId
                    } else {
                        collapsedGroupIds.value + groupId
                    }
                settingsRepository.setCoreBeaconCollapsedGroupIds(nextCollapsedIds)
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
            if (target.isBlank()) return null
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
