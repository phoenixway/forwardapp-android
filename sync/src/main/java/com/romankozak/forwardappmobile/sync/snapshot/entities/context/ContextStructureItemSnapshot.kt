package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот елемента структури (ролі) в конкретній конфігурації.
 */
data class ContextStructureItemSnapshot(
    val id: String,
    val contextStructureId: String,
    val entityType: String,
    val roleCode: String,
    val containerType: String?,
    val title: String,
    val mandatory: Boolean,
    val isEnabled: Boolean,
    val version: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)