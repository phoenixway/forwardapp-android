package com.romankozak.forwardappmobile.shared.features.notes.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val projectId: String,
    val title: String,
    val content: String?,
    val createdAt: Long,
    val updatedAt: Long?
)
