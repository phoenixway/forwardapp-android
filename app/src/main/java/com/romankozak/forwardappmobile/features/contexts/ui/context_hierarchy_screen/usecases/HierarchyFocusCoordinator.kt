package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
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

        fun focusContext(
            context: Context,
            currentHierarchy: ContextHierarchyData,
            currentSubState: MainSubState,
            currentBreadcrumbs: List<BreadcrumbItem>,
            orientationHierarchy: List<OrientationHierarchyItem>,
        ) {
            revealContext(
                context = context,
                currentHierarchy = currentHierarchy,
                currentSubState = currentSubState,
                currentBreadcrumbs = currentBreadcrumbs,
                orientationHierarchy = orientationHierarchy,
                enterFocus = true,
            )
        }

        fun revealContext(
            context: Context,
            currentHierarchy: ContextHierarchyData,
            currentSubState: MainSubState,
            currentBreadcrumbs: List<BreadcrumbItem>,
            orientationHierarchy: List<OrientationHierarchyItem>,
            enterFocus: Boolean,
        ) {
            val orientationBreadcrumbs =
                buildOrientationBreadcrumbsToContext(
                    items = orientationHierarchy,
                    contextId = context.id,
                )
            if (orientationBreadcrumbs.isNotEmpty()) {
                searchUseCase.navigateToProjectWithBreadcrumbs(
                    projectId = context.id,
                    breadcrumbs = orientationBreadcrumbs,
                )
            } else {
                searchUseCase.navigateToProject(
                    projectId = context.id,
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
                searchUseCase.enterProjectFocus(context.id)
            }
        }

        fun handleBackNavigation(
            currentHierarchy: ContextHierarchyData,
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
