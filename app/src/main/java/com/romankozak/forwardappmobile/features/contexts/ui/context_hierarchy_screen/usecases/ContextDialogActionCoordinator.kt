package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.MainSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconRepository
import dagger.hilt.android.scopes.ViewModelScoped
import timber.log.Timber
import javax.inject.Inject

@ViewModelScoped
class ContextDialogActionCoordinator
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val contextActionsUseCase: ContextActionsUseCase,
        private val dialogUseCase: DialogUseCase,
        private val mainBeaconRepository: MainBeaconRepository,
    ) {
        fun requestAddContext(
            currentSubState: MainSubState,
            currentBreadcrumbs: List<BreadcrumbItem>,
            orientationHierarchy: List<OrientationHierarchyItem>,
        ) {
            val focusedContextId =
                (currentSubState as? ProjectHierarchyScreenSubState.ProjectFocused)?.projectId
                    ?: currentBreadcrumbs.lastOrNull { it.target == BreadcrumbTarget.Context }?.id
            savedStateHandle[PENDING_BEACON_FOR_NEW_CONTEXT_ID_KEY] =
                if (focusedContextId != null) {
                    null
                } else {
                    resolveFocusedBeaconId(
                        currentSubState = currentSubState,
                        currentBreadcrumbs = currentBreadcrumbs,
                        orientationHierarchy = orientationHierarchy,
                    )
                }
            dialogUseCase.onAddProjectRequest(focusedContextId)
        }

        fun requestAddSubcontext(parentProjectId: String) {
            dialogUseCase.onAddProjectRequest(parentProjectId)
        }

        fun requestDelete(
            projectId: String,
            projectName: String,
        ) {
            dialogUseCase.onDeleteRequest(
                projectId = projectId,
                projectName = projectName,
            )
        }

        suspend fun requestMove(
            projectId: String,
            allProjects: List<Context>,
        ): ProjectUiEvent.Navigate? {
            val target = contextActionsUseCase.getMoveProjectRoute(projectId, allProjects) ?: return null
            savedStateHandle[PROJECT_BEING_MOVED_ID_KEY] = projectId
            dialogUseCase.dismissDialog()
            return ProjectUiEvent.Navigate(target)
        }

        suspend fun confirmDelete(
            projectId: String,
            childMap: Map<String, List<Context>>,
        ) {
            contextActionsUseCase.onDeleteProjectConfirmed(projectId, childMap)
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
            val newWorkspaceId =
                contextActionsUseCase.addNewProject(
                    name = name,
                    parentId = parentId,
                    roleCode = roleCode,
                )

            if (newWorkspaceId != null) {
                savedStateHandle.get<String>(PENDING_BEACON_FOR_NEW_CONTEXT_ID_KEY)
                    ?.let { beaconId ->
                        mainBeaconRepository.addRelatedContexts(
                            beaconId,
                            setOf(newWorkspaceId),
                        )
                    }
            }

            savedStateHandle[PENDING_BEACON_FOR_NEW_CONTEXT_ID_KEY] = null
            dialogUseCase.dismissDialog()
        }

        private fun resolveFocusedBeaconId(
            currentSubState: MainSubState,
            currentBreadcrumbs: List<BreadcrumbItem>,
            orientationHierarchy: List<OrientationHierarchyItem>,
        ): String? {
            val beaconIds =
                orientationHierarchy
                    .mapNotNull { item -> (item.node as? OrientationHierarchyNode.Beacon)?.id }
                    .toSet()
            return when (currentSubState) {
                is ProjectHierarchyScreenSubState.OrientationFocused ->
                    currentSubState.nodeId.takeIf { it in beaconIds }
                else ->
                    currentBreadcrumbs
                        .lastOrNull { it.target == BreadcrumbTarget.OrientationNode && it.id in beaconIds }
                        ?.id
            }
        }

        companion object {
            private const val PROJECT_BEING_MOVED_ID_KEY = "projectBeingMovedId"
            private const val PENDING_BEACON_FOR_NEW_CONTEXT_ID_KEY = "pendingBeaconForNewContextId"
        }
    }
