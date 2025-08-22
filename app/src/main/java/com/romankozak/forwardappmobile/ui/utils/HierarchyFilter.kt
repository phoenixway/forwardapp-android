package com.romankozak.forwardappmobile.ui.utils

import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData

object HierarchyFilter {

    fun filter(
        originalHierarchy: ListHierarchyData,
        query: String
    ): ListHierarchyData {
        if (query.isBlank()) {
            return originalHierarchy
        }

        val filteredLists = originalHierarchy.allLists.filter {
            it.name.contains(query, ignoreCase = true)
        }

        val allRelevantIds = filteredLists.flatMap { findParentIds(it, originalHierarchy) }
            .plus(filteredLists.map { it.id })
            .toSet()

        val topLevelLists = originalHierarchy.topLevelLists.filter { it.id in allRelevantIds }

        val childMap = originalHierarchy.childMap.mapValues { (_, children) ->
            children.filter { it.id in allRelevantIds }
        }.filterValues { it.isNotEmpty() }

        return ListHierarchyData(
            allLists = filteredLists,
            topLevelLists = topLevelLists,
            childMap = childMap
        )
    }

    private fun findParentIds(
        list: GoalList,
        hierarchy: ListHierarchyData
    ): Set<String> {
        val parents = mutableSetOf<String>()
        var currentParentId = list.parentId
        while (currentParentId != null) {
            parents.add(currentParentId)
            currentParentId = hierarchy.allLists.find { it.id == currentParentId }?.parentId
        }
        return parents
    }
}