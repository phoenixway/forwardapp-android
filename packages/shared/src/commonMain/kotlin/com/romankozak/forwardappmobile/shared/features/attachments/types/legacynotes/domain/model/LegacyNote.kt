package com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.model

data class LegacyNote(
    val id: String,
    val projectId: String,
    val title: String,
    val content: String?,
    val createdAt: Long,
    val updatedAt: Long?,
)
