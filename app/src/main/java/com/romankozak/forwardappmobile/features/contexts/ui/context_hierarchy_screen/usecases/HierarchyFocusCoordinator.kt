package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2BreadcrumbTarget
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyRead
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
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
            placementId: String? = null,
        ) {
            searchUseCase.currentBreadcrumbs.value =
                buildOrientationBreadcrumbs(
                    items = orientationHierarchy,
                    nodeId = nodeId,
                    placementId = placementId,
                )
            searchUseCase.pushSubState(
                ProjectHierarchyScreenSubState.OrientationFocused(
                    nodeId = nodeId,
                    placementId = placementId,
                ),
            )
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
            placementId: String? = null,
            currentHierarchy: HierarchyPresentationData,
            currentSubState: MainSubState,
            currentBreadcrumbs: List<BreadcrumbItem>,
            orientationHierarchy: List<OrientationHierarchyItem>,
            canonicalRead: CanonicalV2ProductionHierarchyRead? = null,
            enterFocus: Boolean,
            replaceFocusPath: Boolean = false,
        ) {
            // Stable-id reveal is a read-side operation, but the id must still
            // belong to the admitted presentation universe. Do not let an
            // arbitrary id manufacture focused/navigation state.
            if (currentHierarchy.allProjects.none { it.id == projectId }) return

            val orientationBreadcrumbs =
                when {
                    placementId == null -> emptyList()
                    canonicalRead == null -> emptyList()
                    else ->
                        canonicalRead
                            .breadcrumbsToOccurrence(PlacementId(placementId))
                            .map { breadcrumb ->
                                BreadcrumbItem(
                                    id = breadcrumb.id,
                                    name = breadcrumb.title,
                                    level = breadcrumb.level,
                                    target =
                                        when (breadcrumb.target) {
                                            CanonicalV2BreadcrumbTarget.CONTEXT ->
                                                BreadcrumbTarget.Context
                                            CanonicalV2BreadcrumbTarget.ORIENTATION_NODE ->
                                                BreadcrumbTarget.OrientationNode
                                        },
                                    placementId = breadcrumb.placementId?.value,
                                )
                            }
                }

            // Exact occurrence identity is authoritative when supplied.
            // Never reinterpret a missing/malformed PlacementId as target-only
            // navigation through the legacy presentation tree.
            if (placementId != null && orientationBreadcrumbs.isEmpty()) return

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
                        placementId = placementId,
                    )
                } else {
                    searchUseCase.enterProjectFocus(
                        projectId = projectId,
                        placementId = placementId,
                    )
                }
            }
        }

        fun handleBackNavigation(
            currentHierarchy: HierarchyPresentationData,
            orientationHierarchy: List<OrientationHierarchyItem>,
            goBack: () -> Unit,
        ) {
            searchUseCase.handleBackNavigation(
                currentHierarchy = currentHierarchy,
                orientationHierarchy = orientationHierarchy,
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
                findOrientationHierarchyItem(
                    items = orientationHierarchy,
                    nodeId = orientationState.nodeId,
                    placementId = orientationState.placementId,
                )?.node
                    ?: return emptyList()
            return listOf(
                BreadcrumbItem(
                    id = rootNode.id,
                    name = rootNode.title,
                    level = 0,
                    target = BreadcrumbTarget.OrientationNode,
                    placementId = rootNode.placementId?.value,
                ),
            )
        }
    }
