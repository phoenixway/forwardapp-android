// Файл: app/src/main/java/com/romankozak/forwardappmobile/ui/utils/HierarchyFilter.kt
package com.romankozak.forwardappmobile.ui.utils

import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData

/**
 * Утиліта для фільтрації ієрархічних даних.
 */
object HierarchyFilter {

    /**
     * Фільтрує ієрархію списків за текстовим запитом.
     *
     * Результат містить елементи, що відповідають запиту, всіх їхніх нащадків та всіх їхніх батьків.
     *
     * @param fullHierarchy Повна, невідфільтрована ієрархія.
     * @param filterText Текст для пошуку (регістронезалежний).
     * @return Новий об'єкт `ListHierarchyData`, що представляє відфільтровану ієрархію.
     */
    fun filter(fullHierarchy: ListHierarchyData, filterText: String): ListHierarchyData {
        if (filterText.isBlank() || fullHierarchy.allLists.isEmpty()) {
            return fullHierarchy
        }

        val allListsById = fullHierarchy.allLists.associateBy { it.id }
        // Переконуємось, що childMap побудовано з повного списку
        val fullChildMap = fullHierarchy.allLists.filter { it.parentId != null }.groupBy { it.parentId!! }

        val matchingIds = fullHierarchy.allLists
            .filter { it.name.contains(filterText, ignoreCase = true) }
            .map { it.id }
            .toSet()

        if (matchingIds.isEmpty()) {
            return ListHierarchyData(allLists = fullHierarchy.allLists, topLevelLists = emptyList(), childMap = emptyMap())
        }

        val visibleIds = mutableSetOf<String>()

        // Рекурсивна функція для збору всіх ID нащадків
        fun collectAllDescendants(listId: String, allDescendants: MutableSet<String>) {
            fullChildMap[listId]?.forEach { child ->
                if (allDescendants.add(child.id)) {
                    collectAllDescendants(child.id, allDescendants)
                }
            }
        }

        matchingIds.forEach { id ->
            // Додаємо сам елемент
            visibleIds.add(id)
            // Додаємо всіх його нащадків
            collectAllDescendants(id, visibleIds)
            // Додаємо всіх його батьків до кореня
            var current = allListsById[id]
            while (current?.parentId != null) {
                val parentId = current.parentId!!
                if (visibleIds.contains(parentId)) break
                visibleIds.add(parentId)
                current = allListsById[parentId]
            }
        }

        val finalVisibleLists = fullHierarchy.allLists.filter { it.id in visibleIds }

        val topLevel = finalVisibleLists
            .filter { it.parentId == null || it.parentId !in visibleIds }
            .sortedBy { it.order }

        val childMap = finalVisibleLists
            .filter { it.parentId != null }
            .groupBy { it.parentId!! }

        return ListHierarchyData(fullHierarchy.allLists, topLevel, childMap)
    }
}