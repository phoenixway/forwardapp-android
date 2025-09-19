// file: ui/screens/mainscreen/state/DialogStateManager.kt

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
 * Керує станом усіх діалогових вікон на головному екрані.
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
        _dialogState.value = DialogState.ContextMenu(project)
    }

    fun onDeleteRequest(project: Project) {
        _dialogState.value = DialogState.ConfirmDelete(project)
    }

    fun onShowAboutDialog() {
        _dialogState.value = DialogState.AboutApp
    }

    fun onImportFromFileRequested(uri: Uri) {
        _dialogState.value = DialogState.ConfirmFullImport(uri)
    }

    fun dismissDialog() {
        _dialogState.value = DialogState.Hidden
    }
}