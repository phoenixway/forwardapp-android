package com.romankozak.forwardappmobile.data.hierarchy

data class ChooserHierarchyItem(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val order: Long,
    val occurrence: HierarchyOccurrenceRef,
)
