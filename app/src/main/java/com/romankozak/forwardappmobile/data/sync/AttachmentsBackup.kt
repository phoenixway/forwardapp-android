package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.core.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.core.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.core.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.core.data.models.LinkItemEntity

data class AttachmentsBackup(
    val backupSchemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val documents: List<NoteDocumentEntity> = emptyList(),
    val documentItems: List<NoteDocumentItemEntity> = emptyList(),
    val checklists: List<ChecklistEntity> = emptyList(),
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    val linkItemEntities: List<LinkItemEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val contextAttachmentCrossRefs: List<ContextAttachmentCrossRef> = emptyList(),
)
