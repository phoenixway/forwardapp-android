package com.romankozak.forwardappmobile.core.data.models.sync

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
