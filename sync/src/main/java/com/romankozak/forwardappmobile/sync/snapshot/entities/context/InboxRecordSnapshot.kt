package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот запису у вхідних (Inbox).
 */
data class InboxRecordSnapshot(
    val id: String,
    val contextId: String,
    val text: String,
    val order: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)