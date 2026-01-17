package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils

import com.romankozak.forwardappmobile.features.contexts.data.models.ListHierarchyData
import com.romankozak.forwardappmobile.features.contexts.data.models.Project
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FlatHierarchyItem

private const val TAG = "SendDebug"


fun fuzzyMatch(
    query: String,
    text: String,
): Boolean {
    if (query.isBlank()) return true
    if (text.isBlank()) return false
    val lowerQuery = query.lowercase()
    val lowerText = text.lowercase()
    var queryIndex = 0
    var textIndex = 0
    while (queryIndex < lowerQuery.length && textIndex < lowerText.length) {
        if (lowerQuery[queryIndex] == lowerText[textIndex]) {
            queryIndex++
        }
        textIndex++
    }
    return queryIndex == lowerQuery.length
}


fun findAncestorsRecursive(
    projectId: String?,
    projectLookup: Map<String, Project>,
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
    childMap: Map<String, List<Project>>,
    visited: MutableSet<String> = mutableSetOf(),
): List<Project> {
    if (!visited.add(projectId)) return emptyList()
    val children = childMap[projectId] ?: emptyList()
    return children + children.flatMap { findDescendantsForDeletion(it.id, childMap, visited) }
}


fun getDescendantIds(
    projectId: String,
    childMap: Map<String, List<Project>>,
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
    hierarchy: ListHierarchyData,
): List<BreadcrumbItem> {
    val path = mutableListOf<BreadcrumbItem>()

    fun findPath(
        projects: List<Project>,
        level: Int,
    ): Boolean {
        val sortedProjects = projects.sortedBy { it.order }
        for (project in sortedProjects) {
            path.add(BreadcrumbItem(project.id, project.name, level))
            if (project.id == targetId) return true
            val children = hierarchy.childMap[project.id] ?: emptyList()
            if (findPath(children, level + 1)) return true
            path.removeLastOrNull()
        }
        return false
    }

    findPath(hierarchy.topLevelProjects, 0)
    return path.toList()
}


fun flattenHierarchy(
    currentProjects: List<Project>,
    projectMap: Map<String, List<Project>>,
): List<Project> {
    val result = mutableListOf<Project>()
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
    projects: List<Project>,
    childMap: Map<String, List<Project>>,
    expandedIds: Set<String>? = null,
    level: Int = 0,
): List<FlatHierarchyItem> {
    val result = mutableListOf<FlatHierarchyItem>()

    fun traverse(
        current: List<Project>,
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
