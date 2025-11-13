package com.romankozak.forwardappmobile.shared.data.database.models

data class InboxRecord(
    val id: String,
    val projectId: String,
    val text: String,
    val createdAt: Long,
    val order: Long
)
