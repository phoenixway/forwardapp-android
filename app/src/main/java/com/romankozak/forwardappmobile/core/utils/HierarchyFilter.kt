package com.romankozak.forwardappmobile.core.utils

import com.romankozak.forwardappmobile.features.contexts.data.models.ContextHierarchyData
import com.romankozak.forwardappmobile.features.contexts.data.models.Project

object HierarchyFilter {
    fun filter(
        originalHierarchy: ContextHierarchyData,
        query: String,
    ): ContextHierarchyData {
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

        return ContextHierarchyData(
            allProjects = filteredProjects,
            topLevelProjects = topLevelProjects,
            childMap = childMap,
        )
    }

    private fun findParentIds(
        project: Project,
        hierarchy: ContextHierarchyData,
    ): Set<String> {
        val parents = mutableSetOf<String>()
        var currentParentId = project.parentId
        while (currentParentId != null && parents.add(currentParentId)) {
            currentParentId = hierarchy.allProjects.find { it.id == currentParentId }?.parentId
        }
        return parents
    }
}
