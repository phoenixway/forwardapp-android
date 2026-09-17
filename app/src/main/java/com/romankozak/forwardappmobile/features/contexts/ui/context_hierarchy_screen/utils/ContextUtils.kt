package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.utils

import com.romankozak.forwardappmobile.core.data.models.entities.Context

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

fun findDescendantIdsForDeletion(
    projectId: String,
    childMap: Map<String, List<Context>>,
    visited: MutableSet<String> = mutableSetOf(),
): List<String> {
    if (!visited.add(projectId)) return emptyList()

    fun visitChildren(parentId: String): List<String> =
        childMap[parentId]
            .orEmpty()
            .flatMap { child ->
                if (!visited.add(child.id)) {
                    emptyList()
                } else {
                    listOf(child.id) + visitChildren(child.id)
                }
            }

    return visitChildren(projectId)
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
