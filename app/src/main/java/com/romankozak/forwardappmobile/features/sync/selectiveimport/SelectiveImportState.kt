package com.romankozak.forwardappmobile.features.sync.selectiveimport

import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ScriptEntity
import com.romankozak.forwardappmobile.core.data.models.sync.BackupDiff
import com.romankozak.forwardappmobile.core.data.models.sync.DiffResult
import com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus
import com.romankozak.forwardappmobile.core.data.models.sync.UpdatedItem
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.BacklogItemSnapshot

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

    fun mapListItemDiff(diff: DiffResult<BacklogItemSnapshot>): List<SelectableDiffItem<BacklogItem>> {
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
        checklists = mapDiff(this.checklists, { it.toEntity() }),
        checklistItems = mapDiff(this.checklistItems, { it.toEntity() }),
        linkItems = mapDiff(this.linkItems, { it.toEntity() }),
        inboxRecords = mapDiff(this.inboxRecords, { it.toEntity() }),
        contextLogs = mapDiff(this.contextLogs, { it.toEntity() }),
        scripts = mapDiff(this.scripts, { it.toEntity() }),
        attachments = mapDiff(this.attachments, { it.toEntity() }),
        backlogOrders = mapDiff(this.backlogOrders, { it.toEntity() }),
        allContextAttachmentCrossRefs =
            this.contextAttachmentCrossRefs.added.map {
                it.toEntity()
            } + this.contextAttachmentCrossRefs.updated.map { it.incoming.toEntity() },
    )
}
