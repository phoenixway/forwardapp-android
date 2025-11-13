package com.romankozak.forwardappmobile.ui.screens.mainscreen.models

import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project

data class MainScreenUiState(
    val isLoading: Boolean = true,
    val projects: List<Project> = emptyList(),
    val activeDialog: ProjectEditorState = ProjectEditorState.Hidden,
    val pendingDeletion: Project? = null,
    val isActionInProgress: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasProjects: Boolean get() = projects.isNotEmpty()
}

sealed interface ProjectEditorState {
    data object Hidden : ProjectEditorState
    data class Create(val parentId: String? = null) : ProjectEditorState
    data class Edit(val project: Project) : ProjectEditorState
}
