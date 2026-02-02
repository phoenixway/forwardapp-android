package com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context

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