package com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.model

data class NoteDocument(
    val id: String,
    val projectId: String,
    val name: String,
    val content: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastCursorPosition: Long,
)

data class NoteDocumentItem(
    val id: String,
    val documentId: String,
    val parentId: String?,
    val content: String,
    val isCompleted: Boolean,
    val order: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
