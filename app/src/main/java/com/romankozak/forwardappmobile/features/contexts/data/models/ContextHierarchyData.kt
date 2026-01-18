package com.romankozak.forwardappmobile.features.contexts.data.models

data class ContextHierarchyData(
    val allProjects: List<Project> = emptyList(),
    val topLevelProjects: List<Project> = emptyList(),
    val childMap: Map<String, List<Project>> = emptyMap(),
)
