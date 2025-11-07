package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.core.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.FilterState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.SearchResult
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.buildPathToProject
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.findAncestorsRecursive
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.fuzzyMatch
import javax.inject.Inject

class HierarchyUseCase @Inject constructor() {

    private fun String?.normalizedParentId(): String? =
        this
            ?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

    fun createProjectHierarchy(
        filterState: FilterState,
        expandedDaily: Set<String>?,
        expandedMedium: Set<String>?,
        expandedLong: Set<String>?,
    ): ListHierarchyData {
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
                ListHierarchyData()
            }
        return result
    }

    private fun createRegularHierarchy(flatList: List<Project>): ListHierarchyData {
        if (flatList.isEmpty()) {
            HierarchyDebugLogger.d { "createRegularHierarchy: empty flat list" }
            return ListHierarchyData(
                allProjects = flatList,
                topLevelProjects = emptyList(),
                childMap = emptyMap(),
            )
        }

        val projectsById = flatList.associateBy { it.id }
        val topLevel = mutableListOf<Project>()
        val childMap = mutableMapOf<String, MutableList<Project>>()
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

        return ListHierarchyData(
            allProjects = flatList,
            topLevelProjects = topLevel.sortedBy { it.order },
            childMap = childMap.mapValues { (_, projects) -> projects.sortedBy { it.order } },
        )
    }

    private fun createPlanningHierarchy(
        flatList: List<Project>,
        mode: PlanningMode,
        settings: PlanningSettingsState,
        expandedDaily: Set<String>?,
        expandedMedium: Set<String>?,
        expandedLong: Set<String>?,
    ): ListHierarchyData {
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

        val childrenByParentId: Map<String?, List<Project>> =
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
        val topLevel = mutableListOf<Project>()
        val childMap = mutableMapOf<String, MutableList<Project>>()

        displayProjects.forEach { project ->
            if (hasValidParent(project, projectsById)) {
                val parentId = project.parentId.normalizedParentId()!!
                childMap.getOrPut(parentId) { mutableListOf() }.add(project)
            } else {
                topLevel.add(project)
            }
        }

        return ListHierarchyData(
            allProjects = flatList,
            topLevelProjects = topLevel.sortedBy { it.order },
            childMap = childMap.mapValues { (_, projects) -> projects.sortedBy { it.order } },
        )
    }

    
    fun createLongDescendantsMap(allProjects: List<Project>): Map<String, Boolean> {
        if (allProjects.isEmpty()) return emptyMap()

        val childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
        val resultMap = mutableMapOf<String, Boolean>()
        val memo = mutableMapOf<String, Boolean>()
        val characterLimit = 35

        fun hasLongDescendantsRecursive(
            projectId: String,
            visitedInPath: Set<String>,
        ): Boolean {
            
            if (projectId in visitedInPath) {
                return false
            }

            if (memo.containsKey(projectId)) return memo[projectId]!!

            val children = childMap[projectId] ?: emptyList()
            for (child in children) {
                if (child.name.length > characterLimit) {
                    memo[projectId] = true
                    return true
                }
                if (hasLongDescendantsRecursive(child.id, visitedInPath + projectId)) {
                    memo[projectId] = true
                    return true
                }
            }
            memo[projectId] = false
            return false
        }

        allProjects.forEach { project ->
            resultMap[project.id] = hasLongDescendantsRecursive(project.id, emptySet())
        }
        return resultMap
    }

    
    fun createSearchResults(
        filterState: FilterState,
        fullHierarchy: ListHierarchyData,
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
        allProjects: List<Project>,
        filterText: String,
        movingId: String?,
    ): ListHierarchyData {
        if (movingId == null) {
            return ListHierarchyData()
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

        return ListHierarchyData(
            allProjects = filteredProjects,
            topLevelProjects = topLevel,
            childMap = childMap,
        )
    }

    private fun hasValidParent(
        project: Project,
        projectsById: Map<String, Project>,
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
