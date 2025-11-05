package com.romankozak.forwardappmobile.ui.screens.attachments.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.config.FeatureToggles
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.models.AttachmentEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectAttachmentCrossRef
import com.romankozak.forwardappmobile.data.repository.AttachmentRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class AttachmentsLibraryViewModel @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val noteDocumentRepository: NoteDocumentRepository,
    private val checklistRepository: ChecklistRepository,
    private val projectDao: ProjectDao,
) : ViewModel() {

    private val queryState = MutableStateFlow("")
    private val filterState = MutableStateFlow(AttachmentLibraryFilter.All)

    val uiState =
        combine(
            attachmentRepository.getAllAttachments(),
            attachmentRepository.getAllAttachmentLinks(),
            attachmentRepository.getAllLinkItems(),
            noteDocumentRepository.getAllDocumentsAsFlow(),
            checklistRepository.getAllChecklistsAsFlow(),
            projectDao.getAllProjects(),
            queryState,
            filterState,
        ) { array ->
            @Suppress("UNCHECKED_CAST")
            val attachments = array[0] as List<AttachmentEntity>
            @Suppress("UNCHECKED_CAST")
            val links = array[1] as List<ProjectAttachmentCrossRef>
            @Suppress("UNCHECKED_CAST")
            val linkItems = array[2] as List<LinkItemEntity>
            @Suppress("UNCHECKED_CAST")
            val noteDocuments = array[3] as List<NoteDocumentEntity>
            @Suppress("UNCHECKED_CAST")
            val checklists = array[4] as List<ChecklistEntity>
            @Suppress("UNCHECKED_CAST")
            val projects = array[5] as List<Project>
            val query = array[6] as String
            val filter = array[7] as AttachmentLibraryFilter
            val projectRefs = projects.associateBy({ it.id }) { AttachmentProjectRef(it.id, it.name) }
            val noteDocumentsMap = noteDocuments.associateBy { it.id }
            val checklistsMap = checklists.associateBy { it.id }
            val linkItemsMap = linkItems.associateBy { it.id }
            val linksByAttachment = links.groupBy { it.attachmentId }

            val items =
                attachments.mapNotNull { attachment ->
                    val type =
                        when (attachment.attachmentType) {
                            ListItemTypeValues.NOTE_DOCUMENT -> AttachmentLibraryType.NOTE_DOCUMENT
                            ListItemTypeValues.CHECKLIST -> AttachmentLibraryType.CHECKLIST
                            ListItemTypeValues.LINK_ITEM -> AttachmentLibraryType.LINK
                            else -> return@mapNotNull null
                        }

                    val associatedProjects =
                        linksByAttachment[attachment.id]
                            ?.mapNotNull { link -> projectRefs[link.projectId] }
                            ?.distinctBy { it.id }
                            ?: emptyList()

                    val ownerProject = attachment.ownerProjectId?.let { projectRefs[it] }

                    when (type) {
                        AttachmentLibraryType.NOTE_DOCUMENT -> {
                            val document = noteDocumentsMap[attachment.entityId] ?: return@mapNotNull null
                            AttachmentLibraryItem(
                                id = attachment.id,
                                entityId = document.id,
                                title = document.name,
                                subtitle = null,
                                type = type,
                                projects = associatedProjects,
                                ownerProject = ownerProject,
                                updatedAt = document.updatedAt,
                            )
                        }
                        AttachmentLibraryType.CHECKLIST -> {
                            val checklist = checklistsMap[attachment.entityId] ?: return@mapNotNull null
                            AttachmentLibraryItem(
                                id = attachment.id,
                                entityId = checklist.id,
                                title = checklist.name,
                                subtitle = null,
                                type = type,
                                projects = associatedProjects,
                                ownerProject = ownerProject,
                                updatedAt = attachment.updatedAt,
                            )
                        }
                        AttachmentLibraryType.LINK -> {
                            val linkItem = linkItemsMap[attachment.entityId] ?: return@mapNotNull null
                            val displayName = linkItem.linkData.displayName ?: linkItem.linkData.target
                            AttachmentLibraryItem(
                                id = attachment.id,
                                entityId = linkItem.id,
                                title = displayName,
                                subtitle = linkItem.linkData.target,
                                type = type,
                                projects = associatedProjects,
                                ownerProject = ownerProject,
                                updatedAt = attachment.updatedAt,
                                linkData = linkItem.linkData,
                            )
                        }
                    }
                }

            val filteredItems =
                items.filter { item ->
                    filter.matches(item.type) &&
                        (query.isBlank() ||
                            item.title.contains(query, ignoreCase = true) ||
                            (item.subtitle?.contains(query, ignoreCase = true) == true) ||
                            item.projects.any { it.name.contains(query, ignoreCase = true) })
                }.sortedByDescending { it.updatedAt }

            AttachmentsLibraryUiState(
                query = query,
                filter = filter,
                items = filteredItems,
                totalCount = items.size,
                matchedCount = filteredItems.size,
                isFeatureEnabled = FeatureToggles.attachmentsLibraryEnabled,
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AttachmentsLibraryUiState(isFeatureEnabled = FeatureToggles.attachmentsLibraryEnabled),
        )

    fun onQueryChange(value: String) {
        queryState.value = value
    }

    fun onFilterChange(filter: AttachmentLibraryFilter) {
        filterState.value = filter
    }
}
