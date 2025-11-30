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
import com.romankozak.forwardappmobile.data.sync.BackupDiff
import com.romankozak.forwardappmobile.data.sync.DiffStatus
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef

data class SelectiveImportState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val backupContent: SelectableDatabaseContent? = null
)

data class SelectableDatabaseContent(
    val projects: List<SelectableDiffItem<Project>> = emptyList(),
    val goals: List<SelectableDiffItem<Goal>> = emptyList(),
    val legacyNotes: List<SelectableDiffItem<LegacyNoteEntity>> = emptyList(),
    val activityRecords: List<SelectableDiffItem<ActivityRecord>> = emptyList(),
    val listItems: List<SelectableDiffItem<ListItem>> = emptyList(),
    val documents: List<SelectableDiffItem<NoteDocumentEntity>> = emptyList(),
    val documentItems: List<SelectableDiffItem<NoteDocumentItemEntity>> = emptyList(), // Dependent, not directly selectable
    val checklists: List<SelectableDiffItem<ChecklistEntity>> = emptyList(),
    val checklistItems: List<SelectableDiffItem<ChecklistItemEntity>> = emptyList(), // Dependent, not directly selectable
    val linkItems: List<SelectableDiffItem<LinkItemEntity>> = emptyList(),
    val inboxRecords: List<SelectableDiffItem<com.romankozak.forwardappmobile.data.database.models.InboxRecord>> = emptyList(),
    val projectExecutionLogs: List<SelectableDiffItem<com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog>> = emptyList(),
    val scripts: List<SelectableDiffItem<com.romankozak.forwardappmobile.data.database.models.ScriptEntity>> = emptyList(),
    val attachments: List<SelectableDiffItem<AttachmentEntity>> = emptyList(),
    val allProjectAttachmentCrossRefs: List<ProjectAttachmentCrossRef> = emptyList() // Dependent, not directly selectable
)

data class SelectableDiffItem<T>(
    val item: T,
    val status: DiffStatus,
    val isSelected: Boolean = false,
    val isSelectable: Boolean = true,
    val changeInfo: String? = null,
)

fun BackupDiff.toSelectable(): SelectableDatabaseContent {
    fun <T> mapDiff(
        diff: com.romankozak.forwardappmobile.data.sync.DiffResult<T>,
        updatedInfo: (com.romankozak.forwardappmobile.data.sync.UpdatedItem<T>) -> String? = { null }
    ): List<SelectableDiffItem<T>> {
        val newItems = diff.added.map {
            SelectableDiffItem(item = it, status = DiffStatus.NEW, isSelected = true, isSelectable = true)
        }
        val updatedItems = diff.updated.map {
            SelectableDiffItem(
                item = it.incoming,
                status = DiffStatus.UPDATED,
                isSelected = true,
                isSelectable = true,
                changeInfo = updatedInfo(it)
            )
        }
        val deletedItems = diff.deleted.map {
            SelectableDiffItem(item = it, status = DiffStatus.DELETED, isSelected = false, isSelectable = false)
        }
        return newItems + updatedItems + deletedItems
    }

    fun mapListItemDiff(diff: com.romankozak.forwardappmobile.data.sync.DiffResult<ListItem>): List<SelectableDiffItem<ListItem>> {
        return mapDiff(diff) { updated ->
            val oldOrder = updated.local.order
            val newOrder = updated.incoming.order
            val orderChanged = oldOrder != newOrder
            val onlyOrderChanged = updated.local.copy(order = newOrder) == updated.incoming
            when {
                orderChanged && onlyOrderChanged -> "Порядок: $oldOrder → $newOrder"
                orderChanged -> "Порядок: $oldOrder → $newOrder, інші зміни"
                else -> null
            }
        }
    }

    return SelectableDatabaseContent(
        projects = mapDiff(this.projects),
        goals = mapDiff(this.goals),
        legacyNotes = mapDiff(this.legacyNotes),
        activityRecords = mapDiff(this.activityRecords),
        listItems = mapListItemDiff(this.listItems),
        documents = mapDiff(this.documents),
        documentItems = mapDiff(this.documentItems),
        checklists = mapDiff(this.checklists),
        checklistItems = mapDiff(this.checklistItems),
        linkItems = mapDiff(this.linkItems),
        inboxRecords = mapDiff(this.inboxRecords),
        projectExecutionLogs = mapDiff(this.projectExecutionLogs),
        scripts = mapDiff(this.scripts),
        attachments = mapDiff(this.attachments),
        allProjectAttachmentCrossRefs = this.projectAttachmentCrossRefs.added + this.projectAttachmentCrossRefs.updated.map { it.incoming }
    )
}
