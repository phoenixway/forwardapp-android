
package com.romankozak.forwardappmobile.ui.screens.strategicmanagement

import com.romankozak.forwardappmobile.shared.data.database.models.Project

data class StrategicManagementUiState(
    val dashboardProjects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
