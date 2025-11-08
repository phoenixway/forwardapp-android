package com.romankozak.forwardappmobile.shared.data.database.models

data class GlobalSubprojectSearchResult(
    val subproject: Project,
    val parentProjectId: String,
    val parentProjectName: String,
    val pathSegments: List<String>,
)
