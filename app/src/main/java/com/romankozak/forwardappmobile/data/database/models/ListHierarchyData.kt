package com.romankozak.forwardappmobile.data.database.models

import com.romankozak.forwardappmobile.data.database.models.GoalList

data class ListHierarchyData(
    val allLists: List<GoalList> = emptyList(),
    val topLevelLists: List<GoalList> = emptyList(),
    val childMap: Map<String, List<GoalList>> = emptyMap()
)