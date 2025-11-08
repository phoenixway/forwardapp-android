package com.romankozak.forwardappmobile.data.database.models

import com.romankozak.forwardappmobile.shared.data.database.models.Project

data class ListHierarchyData(
    val allProjects: List<Project> = emptyList(),
    val topLevelProjects: List<Project> = emptyList(),
    val childMap: Map<String, List<Project>> = emptyMap(),
)
