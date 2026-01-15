package com.romankozak.forwardappmobile.features.attachments.ui.library

import com.romankozak.forwardappmobile.data.database.models.RelatedLink

enum class AttachmentLibraryType {
    NOTE_DOCUMENT,
    CHECKLIST,
    LINK,
}

enum class AttachmentLibraryFilter {
    All,
    Notes,
    Checklists,
    Links;

    fun matches(type: AttachmentLibraryType): Boolean =
        when (this) {
            All -> true
            Notes -> type == AttachmentLibraryType.NOTE_DOCUMENT
            Checklists -> type == AttachmentLibraryType.CHECKLIST
            Links -> type == AttachmentLibraryType.LINK
        }
}

data class AttachmentProjectRef(
    val id: String,
    val name: String,
)

data class AttachmentLibraryItem(
    val id: String,
    val entityId: String,
    val title: String,
    val subtitle: String?,
    val type: AttachmentLibraryType,
    val projects: List<AttachmentProjectRef>,
    val ownerProject: AttachmentProjectRef?,
    val updatedAt: Long,
    val linkData: RelatedLink? = null,
)

data class AttachmentLibraryQueryResult(
    val id: String,
    val entityId: String,
    val attachmentType: String,
    val ownerProjectId: String?,
    val attachmentUpdatedAt: Long,
    val noteName: String?,
    val noteUpdatedAt: Long?,
    val checklistName: String?,
    val linkDisplayName: String?,
    val linkTarget: String?,
    val linkCreatedAt: Long?,
)

data class AttachmentsLibraryUiState(
    val query: String = "",
    val filter: AttachmentLibraryFilter = AttachmentLibraryFilter.All,
    val items: List<AttachmentLibraryItem> = emptyList(),
    val totalCount: Int = 0,
    val matchedCount: Int = 0,
    val isFeatureEnabled: Boolean = false,
)

sealed interface AttachmentsLibraryEvent {
    data class NavigateToProjectChooser(val title: String) : AttachmentsLibraryEvent
    data class ShowToast(val message: String) : AttachmentsLibraryEvent
}
