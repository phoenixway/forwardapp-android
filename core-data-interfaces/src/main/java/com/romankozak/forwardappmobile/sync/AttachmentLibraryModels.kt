package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink


enum class AttachmentLibraryType {
    NOTE_DOCUMENT,
    CHECKLIST,
    LINK,
    CONTEXT,
}

enum class AttachmentLibraryFilter {
    All,
    Notes,
    Checklists,
    Links,
    Contexts,
    ;

    fun matches(type: AttachmentLibraryType): Boolean =
        when (this) {
            All -> true
            Notes -> type == AttachmentLibraryType.NOTE_DOCUMENT
            Checklists -> type == AttachmentLibraryType.CHECKLIST
            Links -> type == AttachmentLibraryType.LINK
            Contexts -> type == AttachmentLibraryType.CONTEXT
        }
}

data class AttachmentContextRef(
    val id: String,
    val name: String,
)

data class AttachmentLibraryItem(
    val id: String,
    val entityId: String,
    val title: String,
    val subtitle: String?,
    val type: AttachmentLibraryType,
    val contexts: List<AttachmentContextRef>,
    val ownerContext: AttachmentContextRef?,
    val updatedAt: Long,
    val linkData: RelatedLink? = null,
)

data class AttachmentLibraryQueryResult(
    val id: String,
    val entityId: String,
    val attachmentType: String,
    val ownerContextId: String?,
    val attachmentUpdatedAt: Long,
    val noteName: String?,
    val noteUpdatedAt: Long?,
    val checklistName: String?,
    val linkDisplayName: String?,
    val linkTarget: String?,
    val linkCreatedAt: Long?,
    val scriptName: String?,
    val contextName: String?,
    val contextUpdatedAt: Long?,
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
    data class NavigateToContextChooser(val title: String) : AttachmentsLibraryEvent

    data class ShowToast(val message: String) : AttachmentsLibraryEvent
}
