package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот класичної текстової нотатки.
 */
data class LegacyNoteSnapshot(
    val id: String,
    val contextId: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)