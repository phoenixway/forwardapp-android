
package com.romankozak.forwardappmobile.ui.screens.strategicmanagement

import com.romankozak.forwardappmobile.data.database.models.Project

data class StrategicManagementUiState(
    val dashboardProjects: List<Project> = emptyList(),
    val elementsProjects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
