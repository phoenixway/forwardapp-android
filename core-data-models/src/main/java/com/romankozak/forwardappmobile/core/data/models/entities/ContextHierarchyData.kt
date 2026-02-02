package com.romankozak.forwardappmobile.core.data.models.entities

data class ContextHierarchyData(
    val allProjects: List<Context> = emptyList(),
    val topLevelProjects: List<Context> = emptyList(),
    val childMap: Map<String, List<Context>> = emptyMap(),
)
