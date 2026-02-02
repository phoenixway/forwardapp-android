package com.romankozak.forwardappmobile.core.data.models.entities

data class AttachmentsBackup(
    val backupSchemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val documents: List<NoteDocumentEntity> = emptyList(),
    val checklists: List<ChecklistEntity> = emptyList(),
    val checklistItems: List<ChecklistItemEntity> = emptyList(),
    val linkItemEntities: List<LinkItemEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList(),
    val contextAttachmentCrossRefs: List<ContextAttachmentCrossRef> = emptyList(),
)