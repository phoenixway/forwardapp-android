package com.romankozak.forwardappmobile.shared.core.domain.hierarchy

data class HierarchyId(val value: String) {
    init {
        require(value.isNotBlank()) { "HierarchyId must not be blank" }
    }

    companion object {
        val GENERAL = HierarchyId("GENERAL")
    }
}

data class PlacementId(val value: String) {
    init {
        require(value.isNotBlank()) { "PlacementId must not be blank" }
    }
}

enum class HierarchyTargetType {
    MANAGED_SUBJECT,
    WORKSPACE,
}

data class HierarchyTargetRef(
    val type: HierarchyTargetType,
    val id: String,
) {
    init {
        require(id.isNotBlank()) { "Hierarchy target id must not be blank" }
    }
}

enum class PlacementKind {
    PRIMARY,
    LINK,
}

data class HierarchyPlacement(
    val id: PlacementId,
    val hierarchyId: HierarchyId,
    val target: HierarchyTargetRef,
    val parentPlacementId: PlacementId?,
    val placementKind: PlacementKind,
    val siblingOrder: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long?,
    val isDeleted: Boolean,
    val version: Long,
)

val hierarchyPlacementSiblingComparator: Comparator<HierarchyPlacement> =
    compareBy<HierarchyPlacement> { it.siblingOrder }
        .thenBy { it.id.value }
