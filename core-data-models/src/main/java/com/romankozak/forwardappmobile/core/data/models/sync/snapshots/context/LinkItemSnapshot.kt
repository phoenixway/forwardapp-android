package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

/**
 * Снапшот зовнішнього посилання.
 */
data class LinkItemSnapshot(
    val id: String,
    val url: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)