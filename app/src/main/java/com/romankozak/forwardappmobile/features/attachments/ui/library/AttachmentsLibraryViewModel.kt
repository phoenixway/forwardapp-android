package com.romankozak.forwardappmobile.features.attachments.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemTypeValues
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.sync.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttachmentsLibraryViewModel @Inject constructor(
    private val attachmentRepository: AttachmentsRepository,
    private val contextDao: ContextDao,
) : ViewModel() {

    private val _events = MutableSharedFlow<AttachmentsLibraryEvent>()
    val events = _events.asSharedFlow()

    private val queryState = MutableStateFlow("")
    private val filterState = MutableStateFlow(AttachmentLibraryFilter.All)
    private var pendingShareItem: AttachmentLibraryItem? = null

    // Явно вказуємо типи параметрів у лямбді, щоб уникнути помилки MatchGroup
    val uiState: StateFlow<AttachmentsLibraryUiState> = combine(
        attachmentRepository.getAttachmentLibraryItems(),
        attachmentRepository.getAllAttachmentLinks(),
        contextDao.getAllContextsFlow(),
        queryState,
        filterState
    ) { queryResults: List<AttachmentLibraryQueryResult>,
        links: List<ContextAttachmentCrossRef>,
        contexts: List<Context>,
        query: String,
        filter: AttachmentLibraryFilter ->

        val contextRefs = contexts.associateBy({ it.id }) {
            AttachmentContextRef(it.id, it.name)
        }
        val linksByAttachment = links.groupBy { it.attachmentId }

        val items = queryResults.mapNotNull { result ->
            val type = when (result.attachmentType) {
                BacklogItemTypeValues.NOTE_DOCUMENT -> AttachmentLibraryType.NOTE_DOCUMENT
                BacklogItemTypeValues.CHECKLIST -> AttachmentLibraryType.CHECKLIST
                BacklogItemTypeValues.LINK_ITEM -> AttachmentLibraryType.LINK
                BacklogItemTypeValues.CONTEXT -> AttachmentLibraryType.CONTEXT
                else -> return@mapNotNull null
            }

            val associatedContexts = linksByAttachment[result.id]
                ?.mapNotNull { link -> contextRefs[link.contextId] }
                ?.distinctBy { it.id }
                ?: emptyList()

            val ownerContext = result.ownerContextId?.let { contextRefs[it] }

            val allContexts = (associatedContexts + listOfNotNull(ownerContext)).distinctBy { it.id }

            mapToLibraryItem(result, type, allContexts, ownerContext)
        }

        val filteredItems = items.filter { item ->
            filter.matches(item.type) && (
                    query.isBlank() ||
                            item.title.contains(query, ignoreCase = true) ||
                            item.subtitle?.contains(query, ignoreCase = true) == true ||
                            item.contexts.any { it.name.contains(query, ignoreCase = true) }
                    )
        }.sortedByDescending { it.updatedAt }

        AttachmentsLibraryUiState(
            query = query,
            filter = filter,
            items = filteredItems,
            totalCount = items.size,
            matchedCount = filteredItems.size,
            isFeatureEnabled = FeatureToggles.isEnabled(FeatureFlag.AttachmentsLibrary),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AttachmentsLibraryUiState(
            isFeatureEnabled = FeatureToggles.isEnabled(FeatureFlag.AttachmentsLibrary)
        )
    )

    private fun mapToLibraryItem(
        result: AttachmentLibraryQueryResult,
        type: AttachmentLibraryType,
        contexts: List<AttachmentContextRef>,
        owner: AttachmentContextRef?
    ): AttachmentLibraryItem? {
        return when (type) {
            AttachmentLibraryType.NOTE_DOCUMENT -> {
                val name = result.noteName ?: return null
                AttachmentLibraryItem(
                    id = result.id,
                    entityId = result.entityId,
                    title = name,
                    subtitle = null, // Додано обов'язковий параметр
                    type = type,
                    contexts = contexts,
                    ownerContext = owner,
                    updatedAt = result.noteUpdatedAt ?: result.attachmentUpdatedAt
                )
            }
            AttachmentLibraryType.CHECKLIST -> {
                val name = result.checklistName ?: return null
                AttachmentLibraryItem(
                    id = result.id,
                    entityId = result.entityId,
                    title = name,
                    subtitle = null, // Додано обов'язковий параметр
                    type = type,
                    contexts = contexts,
                    ownerContext = owner,
                    updatedAt = result.attachmentUpdatedAt
                )
            }
            AttachmentLibraryType.LINK -> {
                val json = result.linkDisplayName ?: return null
                val linkData = try {
                    com.google.gson.Gson().fromJson(json, RelatedLink::class.java)
                } catch (e: Exception) {
                    null
                } ?: return null

                AttachmentLibraryItem(
                    id = result.id,
                    entityId = result.entityId,
                    title = linkData.displayName ?: linkData.target,
                    subtitle = linkData.target,
                    type = type,
                    contexts = contexts,
                    ownerContext = owner,
                    updatedAt = result.linkCreatedAt ?: result.attachmentUpdatedAt,
                    linkData = linkData
                )
            }
            AttachmentLibraryType.CONTEXT -> {
                val name = result.contextName ?: return null
                AttachmentLibraryItem(
                    id = result.id,
                    entityId = result.entityId,
                    title = name,
                    subtitle = null, // Додано обов'язковий параметр
                    type = type,
                    contexts = contexts,
                    ownerContext = owner,
                    updatedAt = result.contextUpdatedAt ?: result.attachmentUpdatedAt
                )
            }
        }
    }

    fun onQueryChange(value: String) { queryState.value = value }
    fun onFilterChange(filter: AttachmentLibraryFilter) { filterState.value = filter }

    fun onShareToContextClick(item: AttachmentLibraryItem) {
        pendingShareItem = item
        viewModelScope.launch {
            _events.emit(AttachmentsLibraryEvent.NavigateToContextChooser("Виберіть контекст для \"${item.title}\""))
        }
    }

    fun onContextChosen(contextId: String?) {
        val attachment = pendingShareItem ?: return
        if (contextId.isNullOrBlank() || contextId == "root") {
            pendingShareItem = null
            return
        }

        viewModelScope.launch {
            attachmentRepository.linkAttachmentToContext(attachment.id, contextId)
            _events.emit(AttachmentsLibraryEvent.ShowToast("Додано до контексту"))
            pendingShareItem = null
        }
    }
}