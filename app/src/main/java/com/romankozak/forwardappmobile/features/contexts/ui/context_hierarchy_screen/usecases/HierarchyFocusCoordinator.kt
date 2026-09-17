package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbTarget
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.MainSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class HierarchyFocusCoordinator
    @Inject
    constructor(
        private val searchUseCase: SearchUseCase,
    ) {
        fun focusOrientationNode(
            nodeId: String,
            orientationHierarchy: List<OrientationHierarchyItem>,
        ) {
            searchUseCase.currentBreadcrumbs.value =
                buildOrientationBreadcrumbs(
                    items = orientationHierarchy,
                    nodeId = nodeId,
                )
            searchUseCase.pushSubState(ProjectHierarchyScreenSubState.OrientationFocused(nodeId))
        }

        fun navigateToBreadcrumb(breadcrumb: BreadcrumbItem) {
            searchUseCase.navigateToBreadcrumb(breadcrumb)
        }

        fun clearNavigation() {
            searchUseCase.clearNavigation()
        }

        /**
         * Reveals a read-side hierarchy node by stable id. This intentionally
         * has no Room Context requirement: a valid shell-free
         * reserved System Workspace is represented by the orientation presentation
         * graph and remains read-only.
         */
        fun revealProject(
            projectId: String,
            currentHierarchy: HierarchyPresentationData,
            currentSubState: MainSubState,
            currentBreadcrumbs: List<BreadcrumbItem>,
            orientationHierarchy: List<OrientationHierarchyItem>,
            enterFocus: Boolean,
            replaceFocusPath: Boolean = false,
        ) {
            // Stable-id reveal is a read-side operation, but the id must still
            // belong to the admitted presentation universe. Do not let an
            // arbitrary id manufacture focused/navigation state.
            if (currentHierarchy.allProjects.none { it.id == projectId }) return

            val orientationBreadcrumbs =
                buildOrientationBreadcrumbsToContext(
                    items = orientationHierarchy,
                    contextId = projectId,
                )
            if (orientationBreadcrumbs.isNotEmpty()) {
                searchUseCase.navigateToProjectWithBreadcrumbs(
                    projectId = projectId,
                    breadcrumbs = orientationBreadcrumbs,
                )
            } else {
                searchUseCase.navigateToProject(
                    projectId = projectId,
                    currentHierarchy = currentHierarchy,
                    breadcrumbPrefix =
                        currentOrientationBreadcrumbPrefix(
                            currentSubState = currentSubState,
                            currentBreadcrumbs = currentBreadcrumbs,
                            orientationHierarchy = orientationHierarchy,
                        ),
                )
            }
            if (enterFocus) {
                if (replaceFocusPath && orientationBreadcrumbs.isNotEmpty()) {
                    searchUseCase.enterProjectFocusPath(
                        projectId = projectId,
                        breadcrumbs = orientationBreadcrumbs,
                    )
                } else {
                    searchUseCase.enterProjectFocus(projectId)
                }
            }
        }

        fun handleBackNavigation(
            currentHierarchy: HierarchyPresentationData,
            goBack: () -> Unit,
        ) {
            searchUseCase.handleBackNavigation(
                currentHierarchy = currentHierarchy,
                goBack = goBack,
            )
        }

        private fun currentOrientationBreadcrumbPrefix(
            currentSubState: MainSubState,
            currentBreadcrumbs: List<BreadcrumbItem>,
            orientationHierarchy: List<OrientationHierarchyItem>,
        ): List<BreadcrumbItem> {
            val existingOrientationPrefix =
                currentBreadcrumbs.takeWhile { it.target == BreadcrumbTarget.OrientationNode }
            if (existingOrientationPrefix.isNotEmpty()) {
                return existingOrientationPrefix
            }

            val orientationState = currentSubState as? ProjectHierarchyScreenSubState.OrientationFocused
                ?: return emptyList()
            val rootNode =
                orientationHierarchy
                    .firstOrNull { item -> item.node.id == orientationState.nodeId }
                    ?.node
                    ?: return emptyList()
            return listOf(
                BreadcrumbItem(
                    id = rootNode.id,
                    name = rootNode.title,
                    level = 0,
                    target = BreadcrumbTarget.OrientationNode,
                ),
            )
        }
    }
