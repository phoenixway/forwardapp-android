package com.romankozak.forwardappmobile.ui.screens.mainscreen.utils

import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.BreadcrumbItem

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
