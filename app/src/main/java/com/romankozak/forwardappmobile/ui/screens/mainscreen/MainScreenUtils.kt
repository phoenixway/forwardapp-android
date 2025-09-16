// File: com/romankozak/forwardappmobile/ui/screens/mainscreen/MainScreenUtils.kt
package com.romankozak.forwardappmobile.ui.screens.mainscreen

import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData

/**
 * Utility functions for the main screen functionality
 */
object MainScreenUtils {

    /**
     * Performs fuzzy matching between a query and text
     */
    fun fuzzyMatch(query: String, text: String): Boolean {
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

    /**
     * Builds a path from root to target list
     */
    fun buildPathToList(targetId: String, hierarchy: ListHierarchyData): List<BreadcrumbItem> {
        val path = mutableListOf<BreadcrumbItem>()

        fun findPath(lists: List<GoalList>, level: Int): Boolean {
            val sortedLists = lists.sortedBy { it.order }
            for (list in sortedLists) {
                path.add(BreadcrumbItem(list.id, list.name, level))
                if (list.id == targetId) return true

                val children = hierarchy.childMap[list.id] ?: emptyList()
                if (findPath(children, level + 1)) return true

                path.removeLastOrNull()
            }
            return false
        }

        findPath(hierarchy.topLevelLists, 0)
        return path.toList()
    }

    /**
     * Finds all ancestors of a given list
     */
    fun findAncestorsRecursive(
        listId: String?,
        listLookup: Map<String, GoalList>,
        ids: MutableSet<String>,
        visited: MutableSet<String>,
    ) {
        var currentId = listId
        while (currentId != null && visited.add(currentId)) {
            ids.add(currentId)
            currentId = listLookup[currentId]?.parentId
        }
    }

    /**
     * Finds all descendants of a given list
     */
    fun findDescendantsRecursive(
        listId: String,
        childMap: Map<String, List<GoalList>>,
        ids: MutableSet<String>,
        visited: MutableSet<String>,
    ) {
        if (!visited.add(listId)) return
        ids.add(listId)

        childMap[listId]?.forEach { child ->
            findDescendantsRecursive(child.id, childMap, ids, visited)
        }
    }

    /**
     * Gets all descendant IDs using breadth-first search
     */
    fun getDescendantIds(listId: String, childMap: Map<String, List<GoalList>>): Set<String> {
        val descendants = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(listId)

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            childMap[currentId]?.forEach { child ->
                descendants.add(child.id)
                queue.add(child.id)
            }
        }

        return descendants
    }

    /**
     * Flattens hierarchy for display purposes
     */
    fun flattenHierarchy(
        currentLists: List<GoalList>,
        listMap: Map<String, List<GoalList>>
    ): List<GoalList> {
        val result = mutableListOf<GoalList>()

        for (list in currentLists) {
            result.add(list)
            if (list.isExpanded) {
                val children = listMap[list.id]?.sortedBy { it.order } ?: emptyList()
                if (children.isNotEmpty()) {
                    result.addAll(flattenHierarchy(children, listMap))
                }
            }
        }

        return result
    }

    /**
     * Finds descendants for deletion (returns actual GoalList objects)
     */
    fun findDescendantsForDeletion(
        listId: String,
        childMap: Map<String, List<GoalList>>,
        visited: MutableSet<String> = mutableSetOf(),
    ): List<GoalList> {
        if (!visited.add(listId)) return emptyList()

        val children = childMap[listId] ?: emptyList()
        return children + children.flatMap {
            findDescendantsForDeletion(it.id, childMap, visited)
        }
    }

    /**
     * Checks if any descendant has names longer than the specified limit
     */
    fun hasDescendantsWithLongNames(
        listId: String,
        childMap: Map<String, List<GoalList>>,
        allListsFlat: List<GoalList>,
        characterLimit: Int = 35
    ): Boolean {
        val queue = ArrayDeque<String>()
        childMap[listId]?.forEach { queue.add(it.id) }

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val list = allListsFlat.find { it.id == currentId }

            if (list != null) {
                if (list.name.length > characterLimit) {
                    return true
                }
                childMap[currentId]?.forEach { queue.add(it.id) }
            }
        }

        return false
    }
}

/**
 * Extension functions for GoalList operations
 */
fun List<GoalList>.buildHierarchy(): ListHierarchyData {
    val topLevel = this.filter { it.parentId == null }.sortedBy { it.order }
    val childMap = this.filter { it.parentId != null }.groupBy { it.parentId!! }

    return ListHierarchyData(
        allLists = this,
        topLevelLists = topLevel,
        childMap = childMap
    )
}

/**
 * Extension function to get all expanded list IDs
 */
fun List<GoalList>.getExpandedIds(): Set<String> {
    return this.filter { it.isExpanded }.map { it.id }.toSet()
}

/**
 * Extension function to find list by ID
 */
fun List<GoalList>.findById(id: String): GoalList? {
    return this.find { it.id == id }
}

/**
 * Extension function to get lists by parent ID
 */
fun List<GoalList>.getChildrenOf(parentId: String?): List<GoalList> {
    return this.filter { it.parentId == parentId }.sortedBy { it.order }
}