package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот запису в лозі виконання проекту.
 */
data class ContextLogSnapshot(
    val id: String,
    val contextId: String,
    val timestamp: Long,
    val type: String,
    val description: String,
    val details: String?,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)