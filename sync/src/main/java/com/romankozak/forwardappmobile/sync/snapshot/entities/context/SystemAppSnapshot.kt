package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот системного застосунку (наприклад, State мого життя).
 */
data class SystemAppSnapshot(
    val id: String,
    val systemKey: String,
    val appType: String,
    val contextId: String,
    val noteDocumentId: String?,
    val updatedAt: Long,
    val createdAt: Long
)