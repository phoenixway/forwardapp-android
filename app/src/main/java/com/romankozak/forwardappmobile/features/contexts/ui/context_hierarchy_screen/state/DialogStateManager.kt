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

        fun onAddSubprojectRequest(parentProjectId: String) {
            _dialogState.value = DialogState.AddProject(parentProjectId)
        }

        fun onMenuRequested(
            projectId: String,
            projectName: String,
            canPasteContextLinks: Boolean = false,
        ) {
            _dialogState.value =
                DialogState.ProjectMenu(
                    projectId = projectId,
                    projectName = projectName,
                    canPasteContextLinks = canPasteContextLinks,
                )
        }

        fun onDeleteRequest(
            projectId: String,
            projectName: String,
        ) {
            _dialogState.value =
                DialogState.ConfirmDelete(
                    projectId = projectId,
                    projectName = projectName,
                )
        }

        fun showContextMigration(state: DialogState.ContextMigration) {
            _dialogState.value = state
        }

        fun currentContextMigration(): DialogState.ContextMigration? =
            _dialogState.value as? DialogState.ContextMigration

        fun updateContextMigration(
            update: (DialogState.ContextMigration) -> DialogState.ContextMigration,
        ) {
            val current = _dialogState.value as? DialogState.ContextMigration ?: return
            _dialogState.value = update(current)
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
