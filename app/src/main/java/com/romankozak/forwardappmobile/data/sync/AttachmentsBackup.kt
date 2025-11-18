package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.data.database.models.ChecklistEntity
import com.romankozak.forwardappmobile.data.database.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.data.database.models.LinkItemEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.data.database.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.model.ProjectAttachmentCrossRef

data class AttachmentsBackup(
    val backupSchemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val documents: List<NoteDocumentEntity> = emptyList(),
    val documentItems: List<NoteDocumentItemEntity> = emptyList(),
    val checklists: List<ChecklistEntity> = emptyList(),
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    val linkItemEntities: List<LinkItemEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val projectAttachmentCrossRefs: List<ProjectAttachmentCrossRef> = emptyList(),
)
