package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот пункту чеклиста.
 */
data class ChecklistItemSnapshot(
    val id: String,
    val checklistId: String,
    val content: String,
    val isChecked: Boolean,
    val itemOrder: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)