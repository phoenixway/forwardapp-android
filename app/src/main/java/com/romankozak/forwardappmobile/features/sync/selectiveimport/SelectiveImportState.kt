package com.romankozak.forwardappmobile.features.sync.selectiveimport

import com.romankozak.forwardappmobile.core.data.models.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.core.data.models.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.ContextLog
import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.core.data.models.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.sync.BackupDiff
import com.romankozak.forwardappmobile.core.data.models.sync.DiffResult
import com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus
import com.romankozak.forwardappmobile.core.data.models.sync.UpdatedItem
import com.romankozak.forwardappmobile.core.data.models.sync.snapshot.toEntity

data class SelectiveImportState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val backupContent: SelectableDatabaseContent? = null,
)

data class SelectableDatabaseContent(
    val projects: List<SelectableDiffItem<Context>> = emptyList(),
    val goals: List<SelectableDiffItem<Goal>> = emptyList(),
    val legacyNotes: List<SelectableDiffItem<LegacyNoteEntity>> = emptyList(),
    val activityRecords: List<SelectableDiffItem<ActivityRecord>> = emptyList(),
    val backlogItems: List<SelectableDiffItem<BacklogItem>> = emptyList(),
    val backlogOrders: List<SelectableDiffItem<BacklogOrder>> = emptyList(),
    val documents: List<SelectableDiffItem<NoteDocumentEntity>> = emptyList(),
    val documentItems: List<SelectableDiffItem<NoteDocumentItemEntity>> = emptyList(), // Dependent, not directly selectable
    val checklists: List<SelectableDiffItem<ChecklistEntity>> = emptyList(),
    val checklistItems: List<SelectableDiffItem<ChecklistItemEntity>> = emptyList(), // Dependent, not directly selectable
    val linkItems: List<SelectableDiffItem<LinkItemEntity>> = emptyList(),
    val inboxRecords: List<SelectableDiffItem<InboxRecord>> = emptyList(),
    val contextLogs: List<SelectableDiffItem<ContextLog>> = emptyList(),
    val scripts: List<SelectableDiffItem<ScriptEntity>> = emptyList(),
    val attachments: List<SelectableDiffItem<AttachmentEntity>> = emptyList(),
    val allContextAttachmentCrossRefs: List<ContextAttachmentCrossRef> = emptyList(), // Dependent, not directly selectable
)

data class SelectableDiffItem<T>(
    val item: T,
    val status: DiffStatus,
    val isSelected: Boolean = false,
    val isSelectable: Boolean = true,
    val changeInfo: String? = null,
)

fun BackupDiff.toSelectable(): SelectableDatabaseContent {
    fun <T, R> mapDiff(
        diff: DiffResult<T>,
        toEntity: (T) -> R,
        updatedInfo: (UpdatedItem<T>) -> String? = { null },
    ): List<SelectableDiffItem<R>> {
        val newItems =
            diff.added.map {
                SelectableDiffItem(item = toEntity(it), status = DiffStatus.NEW, isSelected = true, isSelectable = true)
            }
        val updatedItems =
            diff.updated.map {
                SelectableDiffItem(
                    item = toEntity(it.incoming),
                    status = DiffStatus.UPDATED,
                    isSelected = true,
                    isSelectable = true,
                    changeInfo = updatedInfo(it),
                )
            }
        val deletedItems =
            diff.deleted.map {
                SelectableDiffItem(item = toEntity(it), status = DiffStatus.DELETED, isSelected = false, isSelectable = false)
            }
        return newItems + updatedItems + deletedItems
    }

    fun mapListItemDiff(diff: DiffResult<com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context.BacklogItemSnapshot>): List<SelectableDiffItem<BacklogItem>> {
        return mapDiff(diff, { it.toEntity() }) { updated ->
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
        projects = mapDiff(this.projects, { it.toEntity() }),
        goals = mapDiff(this.goals, { it.toEntity() }),
        legacyNotes = mapDiff(this.legacyNotes, { it.toEntity() }),
        activityRecords = mapDiff(this.activityRecords, { it.toEntity() }),
        backlogItems = mapListItemDiff(this.backlogItems),
        documents = mapDiff(this.documents, { it.toEntity() }),
        documentItems = mapDiff(this.documentItems, { it.toEntity() }),
        checklists = mapDiff(this.checklists, { it.toEntity() }),
        checklistItems = mapDiff(this.checklistItems, { it.toEntity() }),
        linkItems = mapDiff(this.linkItems, { it.toEntity() }),
        inboxRecords = mapDiff(this.inboxRecords, { it.toEntity() }),
        contextLogs = mapDiff(this.contextLogs, { it.toEntity() }),
        scripts = mapDiff(this.scripts, { it.toEntity() }),
        attachments = mapDiff(this.attachments, { it.toEntity() }),
        backlogOrders = mapDiff(this.backlogOrders, { it.toEntity() }),
        allContextAttachmentCrossRefs = this.contextAttachmentCrossRefs.added.map { it.toEntity() } + this.contextAttachmentCrossRefs.updated.map { it.incoming.toEntity() },
    )
}
