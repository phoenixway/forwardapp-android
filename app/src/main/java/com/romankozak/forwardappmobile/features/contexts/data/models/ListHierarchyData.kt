package com.romankozak.forwardappmobile.features.contexts.data.models

import com.romankozak.forwardappmobile.features.contexts.data.models.Project

data class ListHierarchyData(
    val allProjects: List<Project> = emptyList(),
    val topLevelProjects: List<Project> = emptyList(),
    val childMap: Map<String, List<Project>> = emptyMap(),
)
