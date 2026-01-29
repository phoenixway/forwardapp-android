package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот окремого елемента всередині документа.
 */
data class NoteDocumentItemSnapshot(
    val id: String,
    val listId: String, // ID документа
    val parentId: String?,
    val content: String,
    val isCompleted: Boolean,
    val itemOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)