package com.romankozak.forwardappmobile.shared.features.projects.logs.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectExecutionLog(
    val id: String,
    val projectId: String,
    val timestamp: Long,
    val type: String,
    val description: String,
    val details: String? = null,
)
