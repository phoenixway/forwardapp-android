package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.features.attachments.data.models.AttachmentEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ChecklistItemEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.ContextAttachmentCrossRef
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentEntity
import com.romankozak.forwardappmobile.features.attachments.data.models.NoteDocumentItemEntity
import com.romankozak.forwardappmobile.features.contexts.data.models.LinkItemEntity

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
