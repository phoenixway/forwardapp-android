package com.romankozak.forwardappmobile.features.strategicmanagement

import com.romankozak.forwardappmobile.features.contexts.data.models.Project

data class StrategicManagementUiState(
    val dashboardProjects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
