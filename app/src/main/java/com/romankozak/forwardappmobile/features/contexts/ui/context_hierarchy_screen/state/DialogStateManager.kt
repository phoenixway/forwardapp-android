package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.state

import android.net.Uri
import com.romankozak.forwardappmobile.features.contexts.data.models.Project
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.DialogState
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


@ViewModelScoped
class DialogStateManager
    @Inject
    constructor() {
        private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
        val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

        fun onAddNewProjectRequest() {
            _dialogState.value = DialogState.AddProject(null)
        }

        fun onAddSubprojectRequest(parentProject: Project) {
            _dialogState.value = DialogState.AddProject(parentProject.id)
        }

        fun onMenuRequested(project: Project) {
            _dialogState.value = DialogState.ProjectMenu(project)
        }

        fun onDeleteRequest(project: Project) {
            _dialogState.value = DialogState.ConfirmDelete(project)
        }

        fun onShowAboutDialog() {
            
            _dialogState.value = DialogState.About
        }

        fun onImportFromFileRequested(uri: Uri) {
            
            _dialogState.value = DialogState.ConfirmImport(uri)
        }

        fun dismissDialog() {
            _dialogState.value = DialogState.Hidden
        }
    }
