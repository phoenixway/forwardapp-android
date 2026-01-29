package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот документа (структурованої нотатки).
 */
data class NoteDocumentSnapshot(
    val id: String,
    val contextId: String,
    val name: String,
    val content: String?,
    val lastCursorPosition: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)