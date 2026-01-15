package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.database.models.ActivityRecord
import com.romankozak.forwardappmobile.data.database.models.BacklogOrder
import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.database.models.Goal
import com.romankozak.forwardappmobile.data.database.models.InboxRecord
import com.romankozak.forwardappmobile.data.database.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.ListItem
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.data.database.models.ScriptEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef

enum class DiffStatus { NEW, UPDATED, DELETED }

data class UpdatedItem<T>(val local: T, val incoming: T)

data class DiffResult<T>(
    val added: List<T> = emptyList(),
    val updated: List<UpdatedItem<T>> = emptyList(),
    val deleted: List<T> = emptyList(),
)

data class BackupDiff(
    val projects: DiffResult<Project> = DiffResult(),
    val goals: DiffResult<Goal> = DiffResult(),
    val listItems: DiffResult<ListItem> = DiffResult(),
    val backlogOrders: DiffResult<BacklogOrder> = DiffResult(),
    val legacyNotes: DiffResult<LegacyNoteEntity> = DiffResult(),
    val activityRecords: DiffResult<ActivityRecord> = DiffResult(),
    val documents: DiffResult<NoteDocumentEntity> = DiffResult(),
    val documentItems: DiffResult<NoteDocumentItemEntity> = DiffResult(),
    val checklists: DiffResult<ChecklistEntity> = DiffResult(),
    val checklistItems: DiffResult<ChecklistItemEntity> = DiffResult(),
    val linkItems: DiffResult<LinkItemEntity> = DiffResult(),
    val inboxRecords: DiffResult<InboxRecord> = DiffResult(),
    val projectExecutionLogs: DiffResult<ProjectExecutionLog> = DiffResult(),
    val scripts: DiffResult<ScriptEntity> = DiffResult(),
    val attachments: DiffResult<AttachmentEntity> = DiffResult(),
    val projectAttachmentCrossRefs: DiffResult<ProjectAttachmentCrossRef> = DiffResult(),
)
