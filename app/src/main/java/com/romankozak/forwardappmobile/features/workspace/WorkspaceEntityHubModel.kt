package com.romankozak.forwardappmobile.features.workspace

/** Presentation groups only. Direction and authority stay on each original relation. */
internal data class WorkspaceEntityHubGroup(val label: String, val rows: List<WorkspaceRelationItem>)

internal fun WorkspaceRelationsProjection.entityHubGroups(): List<WorkspaceEntityHubGroup> {
    val sources = relations.filter { it.family == WorkspaceRelationFamily.SEMANTIC && "EMBODIES" in it.kinds }
    val direct = relations.filterNot { it in sources }
    val groups = mutableListOf(WorkspaceEntityHubGroup(
        if (embodiedAspect != null || embodiedOrientation != null) "Semantic source · EMBODIES" else "Прямі зв’язки · EMBODIES", sources))
    embodiedAspect?.let { aspect ->
        val taxonomy = aspect.relations.filter { "PARENT_ASPECT" in it.kinds }
        val workspaces = aspect.relations.filter { it.targetType == "WORKSPACE" }
        val planning = aspect.relations.filter { it.family == WorkspaceRelationFamily.PLANNING }
        val others = aspect.relations.filterNot { it in taxonomy || it in workspaces || it in planning }
        groups += WorkspaceEntityHubGroup("Семантичні зв’язки · через ${aspect.title}", others)
        groups += WorkspaceEntityHubGroup("Батьківський Aspect · ${aspect.title}", taxonomy.filter { it.direction == WorkspaceRelationDirection.ASPECT_TO_ENTITY })
        groups += WorkspaceEntityHubGroup("Дочірні Aspects · ${aspect.title}", taxonomy.filter { it.direction == WorkspaceRelationDirection.ENTITY_TO_ASPECT })
        groups += WorkspaceEntityHubGroup("Workspaces · пов’язані з ${aspect.title}", workspaces)
        groups += WorkspaceEntityHubGroup("Planning · через ${aspect.title}", planning)
    }
    embodiedOrientation?.let { orientation ->
        groups += WorkspaceEntityHubGroup("Належність до Aspects · через ${orientation.title}",
            orientation.relations.filter { it.targetType == "ASPECT" })
        groups += WorkspaceEntityHubGroup("Семантичні зв’язки · через ${orientation.title}",
            orientation.relations.filter { it.targetType == "ORIENTATION" })
    }
    groups += WorkspaceEntityHubGroup("Інші прямі зв’язки Workspace", direct.filter { it.family == WorkspaceRelationFamily.SEMANTIC })
    groups += WorkspaceEntityHubGroup("Planning · прямі зв’язки Workspace", direct.filter { it.family == WorkspaceRelationFamily.PLANNING })
    return groups.filter { it.rows.isNotEmpty() }
}

/** Bounded preview, never persisted; full Related entities uses the same rows. */
internal fun List<WorkspaceEntityHubGroup>.entityHubPreview(limit: Int = 6): List<WorkspaceEntityHubGroup> {
    var remaining = limit.coerceAtLeast(0)
    return mapNotNull { group ->
        val rows = group.rows.take(minOf(remaining, 2))
        remaining -= rows.size
        group.copy(rows = rows).takeIf { rows.isNotEmpty() }
    }
}
