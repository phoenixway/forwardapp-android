package com.romankozak.forwardappmobile.ui.utils

import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project

object HierarchyFilter {
    fun filter(
        originalHierarchy: ListHierarchyData,
        query: String,
    ): ListHierarchyData {
        if (query.isBlank()) {
            return originalHierarchy
        }

        val filteredProjects =
            originalHierarchy.allProjects.filter {
                it.name.contains(query, ignoreCase = true)
            }

        val allRelevantIds =
            filteredProjects
                .flatMap { findParentIds(it, originalHierarchy) }
                .plus(filteredProjects.map { it.id })
                .toSet()

        val topLevelProjects = originalHierarchy.topLevelProjects.filter { it.id in allRelevantIds }

        val childMap =
            originalHierarchy.childMap
                .mapValues { (_, children) ->
                    children.filter { it.id in allRelevantIds }
                }.filterValues { it.isNotEmpty() }

        return ListHierarchyData(
            allProjects = filteredProjects,
            topLevelProjects = topLevelProjects,
            childMap = childMap,
        )
    }

    private fun findParentIds(
        project: Project,
        hierarchy: ListHierarchyData,
    ): Set<String> {
        val parents = mutableSetOf<String>()
        var currentParentId = project.parentId
        while (currentParentId != null && parents.add(currentParentId)) {
            currentParentId = hierarchy.allProjects.find { it.id == currentParentId }?.parentId
        }
        return parents
    }
}
