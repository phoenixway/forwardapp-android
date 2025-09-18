package com.romankozak.forwardappmobile.data.database.models

// This is the single, correct definition for your hierarchy data.
data class ListHierarchyData(
    val allProjects: List<Project> = emptyList(),
    val topLevelProjects: List<Project> = emptyList(),
    val childMap: Map<String, List<Project>> = emptyMap()
)