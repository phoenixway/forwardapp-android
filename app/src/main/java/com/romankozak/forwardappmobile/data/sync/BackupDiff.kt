package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.features.activitytracker.data.models.ActivityRecord
import com.romankozak.forwardappmobile.features.attachments.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ProjectAttachmentCrossRef
import com.romankozak.forwardappmobile.features.attachments.data.models.ScriptEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogItem
import com.romankozak.forwardappmobile.features.contexts.data.models.BacklogOrder
import com.romankozak.forwardappmobile.features.contexts.data.models.Context
import com.romankozak.forwardappmobile.features.contexts.data.models.ContextLog
import com.romankozak.forwardappmobile.features.contexts.data.models.Goal
import com.romankozak.forwardappmobile.features.contexts.data.models.InboxRecord
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkItemEntity

enum class DiffStatus { NEW, UPDATED, DELETED }

data class UpdatedItem<T>(val local: T, val incoming: T)

data class DiffResult<T>(
    val added: List<T> = emptyList(),
    val updated: List<UpdatedItem<T>> = emptyList(),
    val deleted: List<T> = emptyList(),
)

data class BackupDiff(
    val projects: DiffResult<Context> = DiffResult(),
    val goals: DiffResult<Goal> = DiffResult(),
    val backlogItems: DiffResult<BacklogItem> = DiffResult(),
    val backlogOrders: DiffResult<BacklogOrder> = DiffResult(),
    val legacyNotes: DiffResult<LegacyNoteEntity> = DiffResult(),
    val activityRecords: DiffResult<ActivityRecord> = DiffResult(),
    val documents: DiffResult<NoteDocumentEntity> = DiffResult(),
    val documentItems: DiffResult<NoteDocumentItemEntity> = DiffResult(),
    val checklists: DiffResult<ChecklistEntity> = DiffResult(),
    val checklistItems: DiffResult<ChecklistItemEntity> = DiffResult(),
    val linkItems: DiffResult<LinkItemEntity> = DiffResult(),
    val inboxRecords: DiffResult<InboxRecord> = DiffResult(),
    val contextLogs: DiffResult<ContextLog> = DiffResult(),
    val scripts: DiffResult<ScriptEntity> = DiffResult(),
    val attachments: DiffResult<AttachmentEntity> = DiffResult(),
    val projectAttachmentCrossRefs: DiffResult<ProjectAttachmentCrossRef> = DiffResult(),
)
