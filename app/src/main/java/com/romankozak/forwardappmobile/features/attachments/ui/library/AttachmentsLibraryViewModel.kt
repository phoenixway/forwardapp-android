package com.romankozak.forwardappmobile.features.attachments.ui.library

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.core.data.models.BacklogItemTypeValues
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.attachments.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.RelatedLink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttachmentsLibraryViewModel
    @Inject
    constructor(
        private val attachmentRepository: AttachmentRepository,
        private val contextDao: ContextDao,
    ) : ViewModel() {
        private val _events = MutableSharedFlow<AttachmentsLibraryEvent>()
        val events = _events.asSharedFlow()

        private val queryState = MutableStateFlow("")
        private val filterState = MutableStateFlow(AttachmentLibraryFilter.All)
        private var pendingShareItem: AttachmentLibraryItem? = null

        val uiState =
            combine(
                attachmentRepository.getAttachmentLibraryItems(),
                attachmentRepository.getAllAttachmentLinks(),
                contextDao.getAllContexts(),
                queryState,
                filterState,
            ) { array ->
                @Suppress("UNCHECKED_CAST")
                val queryResults = array[0] as List<AttachmentLibraryQueryResult>

                @Suppress("UNCHECKED_CAST")
                val links = array[1] as List<ContextAttachmentCrossRef>

                @Suppress("UNCHECKED_CAST")
                val contexts = array[2] as List<Context>
                val query = array[3] as String
                val filter = array[4] as AttachmentLibraryFilter

                Log.d("ATTACHMENTS_LIBRARY", "Query results: ${queryResults.size} items")
                Log.d("ATTACHMENTS_LIBRARY", "Links: ${links.size} cross-refs")
                Log.d("ATTACHMENTS_LIBRARY", "Contexts: ${contexts.size} contexts")

                val contextRefs = contexts.associateBy({ it.id }) { AttachmentContextRef(it.id, it.name) }
                val linksByAttachment = links.groupBy { it.attachmentId }

                val items =
                    queryResults.mapNotNull { result ->
                        val type =
                            when (result.attachmentType) {
                                BacklogItemTypeValues.NOTE_DOCUMENT -> AttachmentLibraryType.NOTE_DOCUMENT
                                BacklogItemTypeValues.CHECKLIST -> AttachmentLibraryType.CHECKLIST
                                BacklogItemTypeValues.LINK_ITEM -> AttachmentLibraryType.LINK
                                BacklogItemTypeValues.CONTEXT -> AttachmentLibraryType.CONTEXT
                                else -> return@mapNotNull null
                            }

                        val associatedContexts =
                            linksByAttachment[result.id]
                                ?.mapNotNull { link -> contextRefs[link.contextId] }
                                ?.distinctBy { it.id }
                                ?: emptyList()

                        val ownerContext = result.ownerContextId?.let { contextRefs[it] }

                        when (type) {
                            AttachmentLibraryType.NOTE_DOCUMENT -> {
                                if (result.noteName == null) {
                                    return@mapNotNull null
                                }
                                AttachmentLibraryItem(
                                    id = result.id,
                                    entityId = result.entityId,
                                    title = result.noteName,
                                    subtitle = null,
                                    type = type,
                                    contexts = associatedContexts,
                                    ownerContext = ownerContext,
                                    updatedAt = result.noteUpdatedAt ?: result.attachmentUpdatedAt,
                                )
                            }
                            AttachmentLibraryType.CHECKLIST -> {
                                if (result.checklistName == null) {
                                    return@mapNotNull null
                                }
                                AttachmentLibraryItem(
                                    id = result.id,
                                    entityId = result.entityId,
                                    title = result.checklistName,
                                    subtitle = null,
                                    type = type,
                                    contexts = associatedContexts,
                                    ownerContext = ownerContext,
                                    updatedAt = result.attachmentUpdatedAt,
                                )
                            }
                            AttachmentLibraryType.LINK -> {
                                if (result.linkDisplayName == null) {
                                    return@mapNotNull null
                                }
                                val linkData =
                                    try {
                                        com.google.gson.Gson().fromJson(result.linkDisplayName, RelatedLink::class.java)
                                    } catch (e: Exception) {
                                        null
                                    }

                                if (linkData == null) return@mapNotNull null

                                AttachmentLibraryItem(
                                    id = result.id,
                                    entityId = result.entityId,
                                    title = linkData.displayName ?: linkData.target,
                                    subtitle = linkData.target,
                                    type = type,
                                    contexts = associatedContexts,
                                    ownerContext = ownerContext,
                                    updatedAt = result.linkCreatedAt ?: result.attachmentUpdatedAt,
                                    linkData = linkData,
                                )
                            }
                            AttachmentLibraryType.CONTEXT -> {
                                if (result.contextName == null) {
                                    return@mapNotNull null
                                }
                                AttachmentLibraryItem(
                                    id = result.id,
                                    entityId = result.entityId,
                                    title = result.contextName,
                                    subtitle = null,
                                    type = type,
                                    contexts = associatedContexts,
                                    ownerContext = ownerContext,
                                    updatedAt = result.contextUpdatedAt ?: result.attachmentUpdatedAt,
                                )
                            }
                        }
                    }

                val filteredItems =
                    items.filter { item ->
                        filter.matches(item.type) &&
                            (
                                query.isBlank() ||
                                    item.title.contains(query, ignoreCase = true) ||
                                    (item.subtitle?.contains(query, ignoreCase = true) == true) ||
                                    item.contexts.any { it.name.contains(query, ignoreCase = true) }
                            )
                    }.sortedByDescending { it.updatedAt }

                Log.d("ATTACHMENTS_LIBRARY", "Total items after mapping: ${items.size}")
                Log.d("ATTACHMENTS_LIBRARY", "Filtered items: ${filteredItems.size}")
                if (filteredItems.isEmpty()) {
                    Log.w("ATTACHMENTS_LIBRARY", "No items found! Check if query results are empty or all filtered out")
                }

                AttachmentsLibraryUiState(
                    query = query,
                    filter = filter,
                    items = filteredItems,
                    totalCount = items.size,
                    matchedCount = filteredItems.size,
                    isFeatureEnabled = FeatureToggles.isEnabled(FeatureFlag.AttachmentsLibrary),
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                AttachmentsLibraryUiState(isFeatureEnabled = FeatureToggles.isEnabled(FeatureFlag.AttachmentsLibrary)),
            )

        fun onQueryChange(value: String) {
            queryState.value = value
        }

        fun onFilterChange(filter: AttachmentLibraryFilter) {
            filterState.value = filter
        }

        fun onShareToContextClick(item: AttachmentLibraryItem) {
            pendingShareItem = item
            viewModelScope.launch {
                _events.emit(
                    AttachmentsLibraryEvent.NavigateToContextChooser(
                        title = "Виберіть контекст для \"${item.title}\"",
                    ),
                )
            }
        }

        fun onContextChosen(contextId: String?) {
            val attachment = pendingShareItem ?: return
            if (contextId.isNullOrBlank() || contextId == "root") {
                pendingShareItem = null
                return
            }

            viewModelScope.launch {
                attachmentRepository.linkAttachmentToContext(
                    attachmentId = attachment.id,
                    contextId = contextId,
                )
                _events.emit(AttachmentsLibraryEvent.ShowToast("Додано до контексту"))
                pendingShareItem = null
            }
        }
    }
