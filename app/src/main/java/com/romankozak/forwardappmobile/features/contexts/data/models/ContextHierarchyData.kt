package com.romankozak.forwardappmobile.features.contexts.data.models

data class ContextHierarchyData(
    val allProjects: List<Context> = emptyList(),
    val topLevelProjects: List<Context> = emptyList(),
    val childMap: Map<String, List<Context>> = emptyMap(),
)
