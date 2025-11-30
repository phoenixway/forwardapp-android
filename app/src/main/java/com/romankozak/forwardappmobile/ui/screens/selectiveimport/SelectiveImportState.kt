package com.romankozak.forwardappmobile.ui.screens.selectiveimport

import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.sync.DatabaseContent
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef

typealias Thought = LegacyNoteEntity
typealias Stats = ActivityRecord

data class SelectiveImportState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val backupContent: SelectableDatabaseContent? = null
)

data class SelectableDatabaseContent(
    val projects: List<SelectableItem<Project>> = emptyList(),
    val goals: List<SelectableItem<Goal>> = emptyList(),
    val thoughts: List<SelectableItem<Thought>> = emptyList(),
    val stats: List<SelectableItem<Stats>> = emptyList(),
    val allListItems: List<ListItem> = emptyList(),
    val allDocuments: List<NoteDocumentEntity> = emptyList(),
    val allDocumentItems: List<NoteDocumentItemEntity> = emptyList(),
    val allChecklists: List<ChecklistEntity> = emptyList(),
    val allChecklistItems: List<ChecklistItemEntity> = emptyList(),
    val allLinkItems: List<LinkItemEntity> = emptyList(),
    val allAttachments: List<AttachmentEntity> = emptyList(),
    val allProjectAttachmentCrossRefs: List<ProjectAttachmentCrossRef> = emptyList()
)

data class SelectableItem<T>(
     val item: T,
     val isSelected: Boolean = false
 )

fun DatabaseContent.toSelectable(): SelectableDatabaseContent {
    return SelectableDatabaseContent(
        projects = this.projects.map { SelectableItem(it) },
        goals = this.goals.map { SelectableItem(it) },
        thoughts = this.legacyNotes.map { SelectableItem(it) },
        stats = this.activityRecords.map { SelectableItem(it) },
        allListItems = this.listItems,
        allDocuments = this.documents,
        allDocumentItems = this.documentItems,
        allChecklists = this.checklists,
        allChecklistItems = this.checklistItems,
        allLinkItems = this.linkItemEntities,
        allAttachments = this.attachments,
        allProjectAttachmentCrossRefs = this.projectAttachmentCrossRefs
    )
}
