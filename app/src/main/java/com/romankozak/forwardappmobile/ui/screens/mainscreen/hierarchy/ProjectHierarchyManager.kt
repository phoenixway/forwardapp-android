package com.romankozak.forwardappmobile.ui.screens.mainscreen.hierarchy

import android.util.Log
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.FilterState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.SearchResult
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.buildPathToProject
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.findAncestorsRecursive
import com.romankozak.forwardappmobile.ui.screens.mainscreen.utils.fuzzyMatch

class ProjectHierarchyManager {

    /**
     * Створює ієрархію проектів на основі поточного стану фільтрації
     */
    fun createProjectHierarchy(
        filterState: FilterState,
        expandedDaily: Set<String>?,
        expandedMedium: Set<String>?,
        expandedLong: Set<String>?
    ): ListHierarchyData {
        return try {
            val (flatList, _, _, mode, settings) = filterState
            val isPlanningModeActive = mode != PlanningMode.All

            if (!isPlanningModeActive) {
                return createRegularHierarchy(flatList)
            }

            createPlanningHierarchy(
                flatList, mode, settings,
                expandedDaily, expandedMedium, expandedLong
            )
        } catch (e: Exception) {
            Log.e("ProjectHierarchyManager", "Exception in createProjectHierarchy", e)
            ListHierarchyData()
        }
    }

    private fun createRegularHierarchy(flatList: List<Project>): ListHierarchyData {
        val topLevel = flatList.filter { it.parentId == null }.sortedBy { it.order }
        val childMap = flatList.filter { it.parentId != null }.groupBy { it.parentId!! }
        return ListHierarchyData(
            allProjects = flatList,
            topLevelProjects = topLevel,
            childMap = childMap
        )
    }

    private fun createPlanningHierarchy(
        flatList: List<Project>,
        mode: PlanningMode,
        settings: PlanningSettingsState,
        expandedDaily: Set<String>?,
        expandedMedium: Set<String>?,
        expandedLong: Set<String>?
    ): ListHierarchyData {
        val projectLookup = flatList.associateBy { it.id }

        val targetTag = when (mode) {
            PlanningMode.Daily -> settings.dailyTag
            PlanningMode.Medium -> settings.mediumTag
            PlanningMode.Long -> settings.longTag
            else -> null
        }

        val matchingProjects = if (targetTag != null) {
            flatList.filter { it.tags?.contains(targetTag) == true }
        } else {
            emptyList()
        }

        val ancestorIds = mutableSetOf<String>()
        val visitedAncestors = mutableSetOf<String>()
        matchingProjects.forEach { project ->
            findAncestorsRecursive(project.id, projectLookup, ancestorIds, visitedAncestors)
        }

        val visibleIds = ancestorIds + matchingProjects.map { it.id }

        val currentExpandedState = when (mode) {
            PlanningMode.Daily -> expandedDaily
            PlanningMode.Medium -> expandedMedium
            PlanningMode.Long -> expandedLong
            else -> null
        }

        val shouldInitialize = currentExpandedState == null && matchingProjects.isNotEmpty()
        val currentExpandedIds = if (shouldInitialize) ancestorIds else (currentExpandedState ?: emptySet())

        val visibleProjects = flatList.filter { it.id in visibleIds }
        val displayProjects = visibleProjects.map { project ->
            project.copy(isExpanded = currentExpandedIds.contains(project.id))
        }
        val topLevel = displayProjects.filter {
            it.parentId == null || it.parentId !in visibleIds
        }.sortedBy { it.order }
        val childMap = displayProjects.filter { it.parentId != null }.groupBy { it.parentId!! }

        return ListHierarchyData(
            allProjects = flatList,
            topLevelProjects = topLevel,
            childMap = childMap
        )
    }

    /**
     * Обчислює карту проектів з довгими нащадками
     */
    fun createLongDescendantsMap(allProjects: List<Project>): Map<String, Boolean> {
        if (allProjects.isEmpty()) return emptyMap()

        val childMap = allProjects.filter { it.parentId != null }.groupBy { it.parentId!! }
        val resultMap = mutableMapOf<String, Boolean>()
        val memo = mutableMapOf<String, Boolean>()
        val characterLimit = 35

        fun hasLongDescendantsRecursive(projectId: String, visitedInPath: Set<String>): Boolean {
            // Перевіряємо на цикл
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

    /**
     * Створює результати пошуку на основі фільтрів
     */
    fun createSearchResults(
        filterState: FilterState,
        fullHierarchy: ListHierarchyData
    ): List<SearchResult> {
        if (!filterState.searchActive || filterState.query.isBlank()) {
            return emptyList()
        }

        val matchingProjects = if (filterState.query.length > 3) {
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
                // **FIXED**: Converted List<BreadcrumbItem> to List<String> using .map.
                parentPath = buildPathToProject(project.id, fullHierarchy).map { it.name }
            )
        }.sortedBy { it.projectName }
    }

    /**
     * Створює ієрархію для діалогу вибору списку
     */
    fun createFilteredListHierarchyForDialog(
        allProjects: List<Project>,
        filterText: String,
        movingId: String?
    ): ListHierarchyData {
        if (movingId == null) {
            return ListHierarchyData()
        }

        val filteredProjects = if (filterText.isBlank()) {
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
            childMap = childMap
        )
    }
}