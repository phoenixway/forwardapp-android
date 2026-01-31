package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

/**
 * Снапшот системного застосунку, прив'язаного до ключа.
 */
data class SystemAppSnapshot(
    val id: String,
    val systemKey: String,
    val appType: String,
    val contextId: String,
    val noteDocumentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)