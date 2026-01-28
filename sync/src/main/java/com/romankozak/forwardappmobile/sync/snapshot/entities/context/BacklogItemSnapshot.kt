package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот зв'язку між контекстом та сутністю (ціллю/підсписком).
 */
data class BacklogItemSnapshot(
    val id: String,
    val contextId: String,
    val entityId: String,
    val itemType: String,
    val order: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)