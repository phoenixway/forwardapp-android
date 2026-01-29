package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот скрипта автоматизації.
 */
data class ScriptSnapshot(
    val id: String,
    val contextId: String?,
    val name: String,
    val description: String?,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)