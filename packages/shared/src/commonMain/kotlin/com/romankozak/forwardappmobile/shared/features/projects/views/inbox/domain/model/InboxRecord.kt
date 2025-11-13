package com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.model

data class InboxRecord(
    val id: String,
    val projectId: String,
    val text: String,
    val createdAt: Long,
    val itemOrder: Long,
)
