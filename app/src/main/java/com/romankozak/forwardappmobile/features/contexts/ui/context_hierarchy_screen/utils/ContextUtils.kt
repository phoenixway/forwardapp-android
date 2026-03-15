package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FlatHierarchyItem

fun fuzzyMatch(
    query: String,
    text: String,
): Boolean {
    val lowerQuery = query.lowercase()
    val lowerText = text.lowercase()
    var queryIndex = 0
    var textIndex = 0
    val canMatch = query.isBlank() || text.isNotBlank()
    if (canMatch && query.isNotBlank()) {
        while (queryIndex < lowerQuery.length && textIndex < lowerText.length) {
            if (lowerQuery[queryIndex] == lowerText[textIndex]) {
                queryIndex++
            }
            textIndex++
        }
    }
    return query.isBlank() || (text.isNotBlank() && queryIndex == lowerQuery.length)
}

fun findAncestorsRecursive(
    projectId: String?,
    projectLookup: Map<String, Context>,
    ids: MutableSet<String>,
    visited: MutableSet<String>,
) {
    var currentId = projectId
    while (currentId != null && visited.add(currentId)) {
        ids.add(currentId)
        currentId = projectLookup[currentId]?.parentId
    }
}

fun findDescendantsForDeletion(
    projectId: String,
    childMap: Map<String, List<Context>>,
    visited: MutableSet<String> = mutableSetOf(),
): List<Context> {
    if (!visited.add(projectId)) return emptyList()
    val children = childMap[projectId] ?: emptyList()
    return children + children.flatMap { findDescendantsForDeletion(it.id, childMap, visited) }
}

fun getDescendantIds(
    projectId: String,
    childMap: Map<String, List<Context>>,
): Set<String> {
    val descendants = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    queue.add(projectId)
    while (queue.isNotEmpty()) {
        val currentId = queue.removeFirst()
        childMap[currentId]?.forEach { child ->
            descendants.add(child.id)
            queue.add(child.id)
        }
    }
    return descendants
}

fun buildPathToProject(
    targetId: String,
    hierarchy: ContextHierarchyData,
): List<BreadcrumbItem> {
    val path = mutableListOf<BreadcrumbItem>()

    fun findPath(
        projects: List<Context>,
        level: Int,
    ): Boolean {
        val sortedProjects = projects.sortedBy { it.order }
        var found = false
        for (project in sortedProjects) {
            path.add(BreadcrumbItem(project.id, project.name, level))
            val children = hierarchy.childMap[project.id] ?: emptyList()
            found = project.id == targetId || findPath(children, level + 1)
            if (found) {
                break
            }
            path.removeLastOrNull()
        }
        return found
    }

    findPath(hierarchy.topLevelProjects, 0)
    return path.toList()
}

fun flattenHierarchy(
    currentProjects: List<Context>,
    projectMap: Map<String, List<Context>>,
): List<Context> {
    val result = mutableListOf<Context>()
    for (project in currentProjects) {
        result.add(project)
        if (project.isExpanded) {
            val children = projectMap[project.id]?.sortedBy { it.order } ?: emptyList()
            if (children.isNotEmpty()) {
                result.addAll(flattenHierarchy(children, projectMap))
            }
        }
    }
    return result
}

fun flattenHierarchyWithLevels(
    projects: List<Context>,
    childMap: Map<String, List<Context>>,
    expandedIds: Set<String>? = null,
    level: Int = 0,
): List<FlatHierarchyItem> {
    val result = mutableListOf<FlatHierarchyItem>()

    fun traverse(
        current: List<Context>,
        currentLevel: Int,
    ) {
        val sortedProjects = current.sortedBy { it.order }
        for (project in sortedProjects) {
            result.add(FlatHierarchyItem(project = project, level = currentLevel))
            val isExpanded = expandedIds?.contains(project.id) ?: project.isExpanded
            if (isExpanded) {
                val children = childMap[project.id].orEmpty()
                if (children.isNotEmpty()) {
                    traverse(children, currentLevel + 1)
                }
            }
        }
    }

    traverse(projects, level)
    return result
}
