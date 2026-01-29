package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот чеклиста.
 */
data class ChecklistSnapshot(
    val id: String,
    val contextId: String,
    val name: String,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)