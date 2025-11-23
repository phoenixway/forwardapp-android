package com.romankozak.forwardappmobile.features.attachments.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romankozak.forwardappmobile.config.FeatureFlag
import com.romankozak.forwardappmobile.config.FeatureToggles
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.database.models.ListItemTypeValues
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AttachmentsLibraryViewModel @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val projectDao: ProjectDao,
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
                            projectDao.getAllProjects(),
                            queryState,
                            filterState,
                        ) { array ->
                            @Suppress("UNCHECKED_CAST")
                            val queryResults = array[0] as List<AttachmentLibraryQueryResult>
                            @Suppress("UNCHECKED_CAST")
                            val links = array[1] as List<ProjectAttachmentCrossRef>
                            @Suppress("UNCHECKED_CAST")
                            val projects = array[2] as List<Project>
                            val query = array[3] as String
                            val filter = array[4] as AttachmentLibraryFilter
                
                            val projectRefs = projects.associateBy({ it.id }) { AttachmentProjectRef(it.id, it.name) }
                            val linksByAttachment = links.groupBy { it.attachmentId }
                
                            val items =
                                queryResults.mapNotNull { result ->
                                    val type =
                                        when (result.attachmentType) {
                                            ListItemTypeValues.NOTE_DOCUMENT -> AttachmentLibraryType.NOTE_DOCUMENT
                                            ListItemTypeValues.CHECKLIST -> AttachmentLibraryType.CHECKLIST
                                            ListItemTypeValues.LINK_ITEM -> AttachmentLibraryType.LINK
                                            else -> return@mapNotNull null
                                        }
                
                                    val associatedProjects =
                                        linksByAttachment[result.id]
                                            ?.mapNotNull { link -> projectRefs[link.projectId] }
                                            ?.distinctBy { it.id }
                                            ?: emptyList()
                
                                    val ownerProject = result.ownerProjectId?.let { projectRefs[it] }
                
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
                                                projects = associatedProjects,
                                                ownerProject = ownerProject,
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
                                                projects = associatedProjects,
                                                ownerProject = ownerProject,
                                                updatedAt = result.attachmentUpdatedAt,
                                            )
                                        }
                                        AttachmentLibraryType.LINK -> {
                                            if (result.linkDisplayName == null) {
                                                return@mapNotNull null
                                            }
                                            val linkData = try {
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
                                                projects = associatedProjects,
                                                ownerProject = ownerProject,
                                                updatedAt = result.linkCreatedAt ?: result.attachmentUpdatedAt,
                                                linkData = linkData,
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

    fun onShareToProjectClick(item: AttachmentLibraryItem) {
        pendingShareItem = item
        viewModelScope.launch {
            _events.emit(
                AttachmentsLibraryEvent.NavigateToProjectChooser(
                    title = "Виберіть проєкт для \"${item.title}\"",
                ),
            )
        }
    }

    fun onProjectChosen(projectId: String?) {
        val attachment = pendingShareItem ?: return
        if (projectId.isNullOrBlank() || projectId == "root") {
            pendingShareItem = null
            return
        }

        viewModelScope.launch {
            attachmentRepository.linkAttachmentToProject(
                attachmentId = attachment.id,
                projectId = projectId,
            )
            _events.emit(AttachmentsLibraryEvent.ShowToast("Додано до проєкту"))
            pendingShareItem = null
        }
    }
}
