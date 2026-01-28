package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот артефакту контексту (текстовий опис/підсумок).
 */
data class ContextArtifactSnapshot(
    val id: String,
    val contextId: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean
)