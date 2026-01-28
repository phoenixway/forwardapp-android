package com.romankozak.forwardappmobile.features.contexts.data.models

import com.romankozak.forwardappmobile.core.data.models.Context

data class ContextHierarchyData(
    val allProjects: List<Context> = emptyList(),
    val topLevelProjects: List<Context> = emptyList(),
    val childMap: Map<String, List<Context>> = emptyMap(),
)
