package com.romankozak.forwardappmobile.features.projectscreen.models

import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project

data class ProjectScreenUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val errorMessage: String? = null,
)
