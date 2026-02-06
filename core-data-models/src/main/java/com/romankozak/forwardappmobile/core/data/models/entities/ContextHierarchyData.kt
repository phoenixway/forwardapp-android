package com.romankozak.forwardappmobile.core.data.models.entities

import com.google.gson.annotations.SerializedName

data class ContextHierarchyData(
    @SerializedName("allProjects") val allProjects: List<Context> = emptyList(),
    @SerializedName("topLevelProjects") val topLevelProjects: List<Context> = emptyList(),
    @SerializedName("childMap") val childMap: Map<String, List<Context>> = emptyMap(),
)
