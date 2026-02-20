package com.romankozak.forwardappmobile.features.strategicmanagement

import com.romankozak.forwardappmobile.core.data.models.entities.Context

data class StrategicManagementUiState(
    val allProjects: List<Context> = emptyList(),
    val dashboardProjects: List<Context> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
