// Файл: app/src/main/java/com/romankozak/forwardappmobile/data/database/models/ListHierarchyData.kt
package com.romankozak.forwardappmobile.data.database.models

/**
 * Універсальна структура для представлення ієрархії списків.
 * @param allLists Повний плаский список усіх елементів (необхідний для побудови ієрархії).
 * @param topLevelLists Списки верхнього рівня (кореневі) у поточній ієрархії.
 * @param childMap Карта, де ключ — це ID батьківського елемента, а значення — список його прямих нащадків.
 */
data class ListHierarchyData(
    val allLists: List<GoalList> = emptyList(),
    val topLevelLists: List<GoalList> = emptyList(),
    val childMap: Map<String, List<GoalList>> = emptyMap()
)