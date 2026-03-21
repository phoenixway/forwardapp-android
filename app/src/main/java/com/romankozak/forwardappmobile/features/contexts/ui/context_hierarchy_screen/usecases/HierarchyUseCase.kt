package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.SearchResult
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.buildPathToProject
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.createHierarchyDescendantOverflowMap
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.findAncestorsRecursive
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils.fuzzyMatch
import javax.inject.Inject

class HierarchyUseCase
    @Inject
    constructor() {
        private fun String?.normalizedParentId(): String? =
            this
                ?.trim()
                ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

        fun createProjectHierarchy(
            filterState: FilterState,
            expandedDaily: Set<String>?,
            expandedMedium: Set<String>?,
            expandedLong: Set<String>?,
        ): ContextHierarchyData {
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
                                expandedDaily,
                                expandedMedium,
                                expandedLong,
                            )
                        }
                    HierarchyDebugLogger.d {
                        "createProjectHierarchy result -> topLevel=${hierarchy.topLevelProjects.size}, childParents=${hierarchy.childMap.size}"
                    }
                    hierarchy
                } catch (e: Exception) {
                    HierarchyDebugLogger.e("Exception in createProjectHierarchy", e)
                    ContextHierarchyData()
                }
            return result
        }

        private fun createRegularHierarchy(flatList: List<Context>): ContextHierarchyData {
            if (flatList.isEmpty()) {
                HierarchyDebugLogger.d { "createRegularHierarchy: empty flat list" }
                return ContextHierarchyData(
                    allProjects = flatList,
                    topLevelProjects = emptyList(),
                    childMap = emptyMap(),
                )
            }

            val projectsById = flatList.associateBy { it.id }
            val topLevel = mutableListOf<Context>()
            val childMap = mutableMapOf<String, MutableList<Context>>()
            var orphanCount = 0

            flatList.forEach { project ->
                if (hasValidParent(project, projectsById)) {
                    val parentId = project.parentId.normalizedParentId()!!
                    childMap.getOrPut(parentId) { mutableListOf() }.add(project)
                } else {
                    val parentId = project.parentId.normalizedParentId()
                    if (parentId != null && !projectsById.containsKey(parentId)) {
                        orphanCount++
                    }
                    topLevel.add(project)
                }
            }

            HierarchyDebugLogger.d {
                "createRegularHierarchy: flat=${flatList.size}, topLevel=${topLevel.size}, childParents=${childMap.size}, orphans=$orphanCount"
            }

            return ContextHierarchyData(
                allProjects = flatList,
                topLevelProjects = topLevel.sortedBy { it.order },
                childMap = childMap.mapValues { (_, projects) -> projects.sortedBy { it.order } },
            )
        }

        private fun createPlanningHierarchy(
            flatList: List<Context>,
            mode: PlanningMode,
            settings: PlanningSettingsState,
            expandedDaily: Set<String>?,
            expandedMedium: Set<String>?,
            expandedLong: Set<String>?,
        ): ContextHierarchyData {
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
                    flatList.filter { it.tags?.contains(targetTag) == true }
                } else {
                    emptyList()
                }

            val childrenByParentId: Map<String?, List<Context>> =
                flatList.groupBy { it.parentId.normalizedParentId() }
            val descendantIds = mutableSetOf<String>()

            fun collectDescendants(projectId: String) {
                val children = childrenByParentId[projectId] ?: emptyList()
                for (child in children) {
                    if (descendantIds.add(child.id)) {
                        collectDescendants(child.id)
                    }
                }
            }

            matchingProjects.forEach { collectDescendants(it.id) }

            val ancestorIds = mutableSetOf<String>()
            val visitedAncestors = mutableSetOf<String>()
            matchingProjects.forEach { project ->
                findAncestorsRecursive(project.id, projectLookup, ancestorIds, visitedAncestors)
            }

            val visibleIds = ancestorIds + matchingProjects.map { it.id } + descendantIds

            val currentExpandedState =
                when (mode) {
                    PlanningMode.Today -> expandedDaily
                    PlanningMode.Medium -> expandedMedium
                    PlanningMode.Long -> expandedLong
                    else -> null
                }

            val shouldInitialize = currentExpandedState == null && matchingProjects.isNotEmpty()
            val currentExpandedIds = if (shouldInitialize) ancestorIds else (currentExpandedState ?: emptySet())

            val visibleProjects = flatList.filter { it.id in visibleIds }
            val displayProjects =
                visibleProjects.map { project ->
                    project.copy(isExpanded = currentExpandedIds.contains(project.id))
                }

            val projectsById = displayProjects.associateBy { it.id }
            val topLevel = mutableListOf<Context>()
            val childMap = mutableMapOf<String, MutableList<Context>>()

            displayProjects.forEach { project ->
                if (hasValidParent(project, projectsById)) {
                    val parentId = project.parentId.normalizedParentId()!!
                    childMap.getOrPut(parentId) { mutableListOf() }.add(project)
                } else {
                    topLevel.add(project)
                }
            }

            return ContextHierarchyData(
                allProjects = flatList,
                topLevelProjects = topLevel.sortedBy { it.order },
                childMap = childMap.mapValues { (_, projects) -> projects.sortedBy { it.order } },
            )
        }

        fun createLongDescendantsMap(hierarchy: ContextHierarchyData): Map<String, Boolean> {
            return createHierarchyDescendantOverflowMap(hierarchy)
        }

        fun createSearchResults(
            filterState: FilterState,
            fullHierarchy: ContextHierarchyData,
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
                    parentPath = buildPathToProject(project.id, fullHierarchy).map { it.name },
                )
            }.sortedBy { it.projectName }
        }

        fun createFilteredListHierarchyForDialog(
            allProjects: List<Context>,
            filterText: String,
            movingId: String?,
        ): ContextHierarchyData {
            if (movingId == null) {
                return ContextHierarchyData()
            }

            val filteredProjects =
                if (filterText.isBlank()) {
                    allProjects
                } else {
                    allProjects.filter {
                        it.name.contains(filterText, ignoreCase = true) ||
                            fuzzyMatch(filterText, it.name)
                    }
                }

            val topLevel = filteredProjects.filter { it.parentId == null }.sortedBy { it.order }
            val childMap = filteredProjects.filter { it.parentId != null }.groupBy { it.parentId!! }

            return ContextHierarchyData(
                allProjects = filteredProjects,
                topLevelProjects = topLevel,
                childMap = childMap,
            )
        }

        private fun hasValidParent(
            project: Context,
            projectsById: Map<String, Context>,
        ): Boolean {
            val firstParentId = project.parentId.normalizedParentId() ?: return false
            if (firstParentId == project.id) return false
            var currentParentId = firstParentId
            val visited = mutableSetOf(project.id)

            while (true) {
                if (!visited.add(currentParentId)) return false
                val parentProject = projectsById[currentParentId] ?: return false
                val nextParentId = parentProject.parentId.normalizedParentId() ?: return true
                if (nextParentId == currentParentId) return false
                currentParentId = nextParentId
            }
        }
    }
