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
import com.romankozak.forwardappmobile.data.database.models.ScriptEntity
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.data.sync.DatabaseContent
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef

data class SelectiveImportState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val backupContent: SelectableDatabaseContent? = null
)

data class SelectableDatabaseContent(
    val projects: List<SelectableItem<Project>> = emptyList(),
    val goals: List<SelectableItem<Goal>> = emptyList(),
    val legacyNotes: List<SelectableItem<LegacyNoteEntity>> = emptyList(),
    val activityRecords: List<SelectableItem<ActivityRecord>> = emptyList(),
    val listItems: List<SelectableItem<ListItem>> = emptyList(),
    val documents: List<SelectableItem<NoteDocumentEntity>> = emptyList(),
    val documentItems: List<NoteDocumentItemEntity> = emptyList(), // Dependent, not directly selectable
    val checklists: List<SelectableItem<ChecklistEntity>> = emptyList(),
    val checklistItems: List<ChecklistItemEntity> = emptyList(), // Dependent, not directly selectable
    val linkItems: List<SelectableItem<LinkItemEntity>> = emptyList(),
    val inboxRecords: List<SelectableItem<com.romankozak.forwardappmobile.data.database.models.InboxRecord>> = emptyList(),
    val projectExecutionLogs: List<SelectableItem<com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog>> = emptyList(),
    val scripts: List<SelectableItem<com.romankozak.forwardappmobile.data.database.models.ScriptEntity>> = emptyList(),
    val attachments: List<SelectableItem<AttachmentEntity>> = emptyList(),
    val allProjectAttachmentCrossRefs: List<ProjectAttachmentCrossRef> = emptyList() // Dependent, not directly selectable
)

data class SelectableItem<T>(
     val item: T,
     val isSelected: Boolean = false
 )

fun DatabaseContent.toSelectable(): SelectableDatabaseContent {
    return SelectableDatabaseContent(
        projects = this.projects.map { SelectableItem(it) },
        goals = this.goals.map { SelectableItem(it) },
        legacyNotes = this.legacyNotes.map { SelectableItem(it) },
        activityRecords = this.activityRecords.map { SelectableItem(it) },
        listItems = this.listItems.map { SelectableItem(it) },
        documents = this.documents.map { SelectableItem(it) },
        documentItems = this.documentItems,
        checklists = this.checklists.map { SelectableItem(it) },
        checklistItems = this.checklistItems,
        linkItems = this.linkItemEntities.map { SelectableItem(it) },
        inboxRecords = this.inboxRecords.map { SelectableItem(it) },
        projectExecutionLogs = this.projectExecutionLogs.map { SelectableItem(it) },
        scripts = this.scripts.map { SelectableItem(it) },
        attachments = this.attachments.map { SelectableItem(it) },
        allProjectAttachmentCrossRefs = this.projectAttachmentCrossRefs
    )
}
