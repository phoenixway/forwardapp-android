package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.state

import android.net.Uri
import com.romankozak.forwardappmobile.core.data.models.entities.Context
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

        fun onAddSubprojectRequest(parentProject: Context) {
            _dialogState.value = DialogState.AddProject(parentProject.id)
        }

        fun onMenuRequested(project: Context) {
            _dialogState.value = DialogState.ProjectMenu(project)
        }

        fun onDeleteRequest(project: Context) {
            _dialogState.value = DialogState.ConfirmDelete(project)
        }

        fun onShowAboutDialog() {
            _dialogState.value = DialogState.About
        }

        fun onImportFromFileRequested(uri: Uri) {
            _dialogState.value = DialogState.ImportChoiceDialog(uri)
        }

        fun onExportToFileRequested() {
            _dialogState.value = DialogState.ExportChoiceDialog
        }

        fun dismissDialog() {
            _dialogState.value = DialogState.Hidden
        }
    }
