// file: ui/screens/mainscreen/state/DialogStateManager.kt - CORRECTED

package com.romankozak.forwardappmobile.ui.screens.mainscreen.state

import android.net.Uri
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DialogState
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Manages the state of all dialogs on the main screen.
 */
@ViewModelScoped
class DialogStateManager @Inject constructor() {

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
        // FIXED: Used the correct state name 'About'.
        _dialogState.value = DialogState.About
    }

    fun onImportFromFileRequested(uri: Uri) {
        // FIXED: Used the correct state name 'ConfirmImport'.
        _dialogState.value = DialogState.ConfirmImport(uri)
    }

    fun dismissDialog() {
        _dialogState.value = DialogState.Hidden
    }
}