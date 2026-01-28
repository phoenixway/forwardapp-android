package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот порядку сортування в беклозі.
 */
data class BacklogOrderSnapshot(
    val id: String,
    val listId: String,
    val itemId: String,
    val order: Long,
    val orderVersion: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)