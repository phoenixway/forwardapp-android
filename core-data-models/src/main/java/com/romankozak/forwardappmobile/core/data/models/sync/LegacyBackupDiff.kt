package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.core.data.models.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.BacklogItem
import com.romankozak.forwardappmobile.core.data.models.BacklogOrder
import com.romankozak.forwardappmobile.core.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.Context
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.ContextLog
import com.romankozak.forwardappmobile.core.data.models.Goal
import com.romankozak.forwardappmobile.core.data.models.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.LinkItemEntity
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.ScriptEntity

data class LegacyBackupDiff(
    val projects: DiffResult<Context> = DiffResult(),
    val goals: DiffResult<Goal> = DiffResult(),
    val backlogItems: DiffResult<BacklogItem> = DiffResult(),
    val backlogOrders: DiffResult<BacklogOrder> = DiffResult(),
    val legacyNotes: DiffResult<LegacyNoteEntity> = DiffResult(),
    val activityRecords: DiffResult<ActivityRecord> = DiffResult(),
    val documents: DiffResult<NoteDocumentEntity> = DiffResult(),
    val checklists: DiffResult<ChecklistEntity> = DiffResult(),
    val checklistItems: DiffResult<ChecklistItemEntity> = DiffResult(),
    val linkItems: DiffResult<LinkItemEntity> = DiffResult(),
    val inboxRecords: DiffResult<InboxRecord> = DiffResult(),
    val contextLogs: DiffResult<ContextLog> = DiffResult(),
    val scripts: DiffResult<ScriptEntity> = DiffResult(),
    val attachments: DiffResult<AttachmentEntity> = DiffResult(),
    val contextAttachmentCrossRefs: DiffResult<ContextAttachmentCrossRef> = DiffResult(),
)
