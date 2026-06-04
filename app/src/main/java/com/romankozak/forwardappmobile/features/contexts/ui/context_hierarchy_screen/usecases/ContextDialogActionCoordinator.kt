package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.MainSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import dagger.hilt.android.scopes.ViewModelScoped
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@ViewModelScoped
class ContextDialogActionCoordinator
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val contextActionsUseCase: ContextActionsUseCase,
        private val dialogUseCase: DialogUseCase,
    ) {
        fun requestAddContext(
            currentSubState: MainSubState,
            allProjects: List<Context>,
        ) {
            val focusedState = currentSubState as? ProjectHierarchyScreenSubState.ProjectFocused
            val parentProject = focusedState?.let { state -> allProjects.find { it.id == state.projectId } }
            dialogUseCase.onAddProjectRequest(parentProject)
        }

        fun requestAddSubcontext(parentProject: Context) {
            dialogUseCase.onAddProjectRequest(parentProject)
        }

        fun requestDelete(project: Context) {
            dialogUseCase.onDeleteRequest(project)
        }

        suspend fun requestMove(
            project: Context,
            allProjects: List<Context>,
        ): ProjectUiEvent.Navigate {
            val target = contextActionsUseCase.getMoveProjectRoute(project, allProjects)
            savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = project.id
            dialogUseCase.dismissDialog()
            return ProjectUiEvent.Navigate(target)
        }

        suspend fun confirmDelete(
            project: Context,
            childMap: Map<String, List<Context>>,
        ) {
            contextActionsUseCase.onDeleteProjectConfirmed(project, childMap)
            dialogUseCase.dismissDialog()
        }

        suspend fun confirmMove(
            newParentId: String?,
            allProjects: List<Context>,
        ) {
            contextActionsUseCase.onListChooserResult(
                newParentId = newParentId,
                projectBeingMovedId = savedStateHandle[PROJECT_BEING_MOVED_ID_KEY],
                allProjects = allProjects,
            )
            savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = null
        }

        suspend fun confirmFullImport(uri: Uri): ProjectUiEvent.ShowToast {
            val result = contextActionsUseCase.onFullImportConfirmed(uri)
            dialogUseCase.dismissDialog()
            Timber.tag("IMPORT_DEBUG").e("Import error: ${result.exceptionOrNull()?.message}")
            return if (result.isSuccess) {
                ProjectUiEvent.ShowToast(result.getOrNull() ?: "Import successful")
            } else {
                ProjectUiEvent.ShowToast("Import error: ${result.exceptionOrNull()?.message}")
            }
        }

        suspend fun confirmFullImportV2(uri: Uri): ProjectUiEvent.ShowToast {
            val result = contextActionsUseCase.onFullImportConfirmedV2(uri)
            dialogUseCase.dismissDialog()
            return if (result.isSuccess) {
                ProjectUiEvent.ShowToast(result.getOrNull() ?: "Import V2 successful")
            } else {
                ProjectUiEvent.ShowToast("Import V2 error: ${result.exceptionOrNull()?.message}")
            }
        }

        suspend fun confirmAddContext(
            name: String,
            parentId: String?,
            roleCode: String?,
        ) {
            contextActionsUseCase.addNewProject(
                id = UUID.randomUUID().toString(),
                name = name,
                parentId = parentId,
                roleCode = roleCode,
            )
            dialogUseCase.dismissDialog()
        }

        companion object {
            private const val PROJECT_BEING_MOVED_ID_KEY = "projectBeingMovedId"
        }
    }
