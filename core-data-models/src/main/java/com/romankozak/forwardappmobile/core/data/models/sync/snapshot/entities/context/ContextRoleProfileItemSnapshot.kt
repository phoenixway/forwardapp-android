package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

/**
 * Снапшот елемента (ролі) всередині глобального пресету.
 */
data class ContextRoleProfileItemSnapshot(
    val id: String,
    val presetId: String,
    val entityType: String,
    val roleCode: String,
    val containerType: String?,
    val title: String,
    val mandatory: Boolean,
    val version: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)