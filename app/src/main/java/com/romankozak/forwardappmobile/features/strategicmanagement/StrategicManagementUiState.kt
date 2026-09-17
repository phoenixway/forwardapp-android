package com.romankozak.forwardappmobile.features.strategicmanagement

import com.romankozak.forwardappmobile.data.workspace.ContextPresentation

data class StrategicManagementUiState(
    val allProjects: List<ContextPresentation> = emptyList(),
    val dashboardProjects: List<ContextPresentation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
