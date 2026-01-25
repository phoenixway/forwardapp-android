package com.romankozak.forwardappmobile.features.strategicmanagement

import com.romankozak.forwardappmobile.features.contexts.data.models.Context

data class StrategicManagementUiState(
    val dashboardProjects: List<Context> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
