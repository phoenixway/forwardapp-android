package com.romankozak.forwardappmobile.core.data.models.sync.snapshot.entities.context

/**
 * Снапшот артефакту контексту (текстовий опис/підсумок).
 */
data class ContextArtifactSnapshot(
    val id: String,
    val contextId: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)