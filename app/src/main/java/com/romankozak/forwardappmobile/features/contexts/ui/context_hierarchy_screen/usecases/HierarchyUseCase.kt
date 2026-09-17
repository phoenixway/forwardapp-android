package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.buildPresentationPathToProject
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.createHierarchyDescendantOverflowMap
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.findAncestorsRecursive
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.fuzzyMatch
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.normalizedParentId
import javax.inject.Inject

class HierarchyUseCase
    @Inject
    constructor() {
        private val presentationTreeBuilder = HierarchyPresentationTreeBuilder()

        fun createProjectHierarchy(
            filterState: FilterState,
        ): HierarchyPresentationData {
            HierarchyDebugLogger.d {
                "createProjectHierarchy: flatSize=${filterState.flatList.size}, mode=${filterState.mode}, searchActive=${filterState.searchActive}"
            }
            val result =
                try {
                    val (flatList, _, _, mode, settings) = filterState
                    val isPlanningModeActive = mode != PlanningMode.All

                    val hierarchy =
                        if (!isPlanningModeActive) {
                            createRegularHierarchy(flatList)
                        } else {
                            createPlanningHierarchy(
                                flatList,
                                mode,
                                settings,
                            )
                        }
                    HierarchyDebugLogger.d {
                        "createProjectHierarchy result -> topLevel=${hierarchy.topLevelProjects.size}, childParents=${hierarchy.childMap.size}"
                    }
                    hierarchy
                } catch (e: Exception) {
                    HierarchyDebugLogger.e("Exception in createProjectHierarchy", e)
                    HierarchyPresentationData()
                }
            return result
        }

        private fun createRegularHierarchy(
            flatList: List<HierarchyContextPresentationNode>,
        ): HierarchyPresentationData {
            if (flatList.isEmpty()) {
                HierarchyDebugLogger.d { "createRegularHierarchy: empty flat list" }
                return HierarchyPresentationData(allProjects = flatList)
            }

            val hierarchy = presentationTreeBuilder.build(flatList)
            val parentIds = flatList.mapNotNull { it.parentId.normalizedParentId() }.toSet()
            val existingIds = flatList.mapTo(hashSetOf()) { it.id }
            val orphanCount = parentIds.count { it !in existingIds }

            HierarchyDebugLogger.d {
                "createRegularHierarchy: flat=${flatList.size}, topLevel=${hierarchy.topLevelProjects.size}, childParents=${hierarchy.childMap.size}, orphans=$orphanCount"
            }

            return hierarchy
        }

        private fun createPlanningHierarchy(
            flatList: List<HierarchyContextPresentationNode>,
            mode: PlanningMode,
            settings: PlanningSettingsState,
        ): HierarchyPresentationData {
            val projectLookup = flatList.associateBy { it.id }

            val targetTag =
                when (mode) {
                    PlanningMode.Today -> settings.dailyTag
                    PlanningMode.Medium -> settings.mediumTag
                    PlanningMode.Long -> settings.longTag
                    else -> null
                }

            val matchingProjects =
                if (targetTag != null) {
                    flatList.filter { targetTag in it.tags }
                } else {
                    emptyList()
                }

            val childrenByParentId =
                flatList.groupBy { it.parentId.normalizedParentId() }
            val descendantIds = mutableSetOf<String>()

            fun collectDescendants(projectId: String) {
                val children = childrenByParentId[projectId].orEmpty()
                for (child in children) {
                    if (descendantIds.add(child.id)) {
                        collectDescendants(child.id)
                    }
                }
            }

            matchingProjects.forEach { collectDescendants(it.id) }

            val ancestorIds = mutableSetOf<String>()
            matchingProjects.forEach { project ->
                val visited = mutableSetOf(project.id)
                var parentId = project.parentId.normalizedParentId()

                while (parentId != null && visited.add(parentId)) {
                    val parent = projectLookup[parentId] ?: break
                    ancestorIds += parent.id
                    parentId = parent.parentId.normalizedParentId()
                }
            }

            val visibleIds =
                ancestorIds +
                    matchingProjects.map { it.id } +
                    descendantIds

            val visibleProjects = flatList.filter { it.id in visibleIds }
            val visibleHierarchy = presentationTreeBuilder.build(visibleProjects)

            return visibleHierarchy.copy(allProjects = flatList)
        }

        fun createLongDescendantsMap(hierarchy: HierarchyPresentationData): Map<String, Boolean> {
            return createHierarchyDescendantOverflowMap(hierarchy)
        }

        fun createSearchResults(
            filterState: FilterState,
            fullHierarchy: HierarchyPresentationData,
        ): List<SearchResult> {
            if (!filterState.searchActive || filterState.query.isBlank()) {
                return emptyList()
            }

            val matchingProjects =
                if (filterState.query.length > 3) {
                    filterState.flatList.filter { fuzzyMatch(filterState.query, it.name) }
                } else {
                    filterState.flatList.filter {
                        it.name.contains(filterState.query, ignoreCase = true)
                    }
                }

            return matchingProjects.map { project ->
                SearchResult(
                    projectId = project.id,
                    projectName = project.name,
                    parentPath =
                        buildPresentationPathToProject(
                            project.id,
                            fullHierarchy,
                        ).map { it.name },
                )
            }.sortedBy { it.projectName }
        }

    }
