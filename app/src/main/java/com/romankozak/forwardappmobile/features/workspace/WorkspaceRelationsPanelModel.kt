package com.romankozak.forwardappmobile.features.workspace

/** Visual grouping only: uses exactly the existing admitted rows and navigation identities. */
internal data class WorkspaceRelationsPanelModel(
    val source: WorkspaceRelationItem?,
    val structureLabel: String,
    val groups: List<WorkspaceEntityHubGroup>,
)

internal fun WorkspaceRelationsProjection.relationshipPanel(): WorkspaceRelationsPanelModel {
    val sourceId = when {
        embodiedAspect != null && embodiedOrientation == null -> embodiedAspect.aspectId
        embodiedOrientation != null && embodiedAspect == null -> embodiedOrientation.orientationId
        else -> null
    }
    val source = relations.singleOrNull {
        it.family == WorkspaceRelationFamily.SEMANTIC && "EMBODIES" in it.kinds && it.targetId == sourceId
    }
    val groups = entityHubGroups().mapNotNull { group ->
        val rows = group.rows.filterNot { it == source }
        if (rows.isEmpty()) return@mapNotNull null
        val label = when {
            rows.all { "PARENT_ASPECT" in it.kinds && it.direction == WorkspaceRelationDirection.ENTITY_TO_ASPECT } -> "Sub-aspects"
            rows.all { "PARENT_ASPECT" in it.kinds } -> "Parent Aspect"
            rows.all { it.family == WorkspaceRelationFamily.PLANNING } -> group.label
            rows.all { it.targetType == "ORIENTATION" } -> "Orientations"
            rows.all { it.targetType == "ASPECT" } -> "Aspects"
            rows.all { it.targetType == "WORKSPACE" } -> "Workspaces"
            else -> group.label
        }
        group.copy(label = label, rows = rows)
    }
    return WorkspaceRelationsPanelModel(source, when {
        source == null -> "Workspace relations"
        embodiedAspect != null -> "Aspect structure"
        else -> "Orientation relations"
    }, groups)
}

internal fun WorkspaceRelationItem.panelRelationLabel(orientationTitle: String? = null): String {
    if (direction in setOf(WorkspaceRelationDirection.ORIENTATION_TO_ENTITY,
            WorkspaceRelationDirection.ENTITY_TO_ORIENTATION, WorkspaceRelationDirection.ORIENTATION_MUTUAL)) {
        val anchor = "«${orientationTitle ?: "поточний орієнтир"}»"
        val outgoing = direction == WorkspaceRelationDirection.ORIENTATION_TO_ENTITY
        // The card title is the sentence subject; name the other endpoint instead of an ambiguous arrow.
        return kinds.joinToString(" · ") { kind ->
            when (kind) {
                "PART_OF" -> if (outgoing) "Містить $anchor" else "Частина $anchor"
                "SUPPORTS" -> if (outgoing) "Отримує підтримку від $anchor" else "Підтримує $anchor"
                "REALIZES" -> if (outgoing) "Реалізується через $anchor" else "Реалізує $anchor"
                "DEPENDS_ON" -> if (outgoing) "Потрібний для $anchor" else "Залежить від $anchor"
                "CONFLICTS_WITH" -> "Конфліктує з $anchor"
                "PRECEDES" -> if (outgoing) "Після $anchor" else "Передує $anchor"
                "REFINES" -> if (outgoing) "Уточнюється через $anchor" else "Уточнює $anchor"
                "DERIVED_FROM" -> if (outgoing) "Основа для $anchor" else "Походить від $anchor"
                "BELONGS_TO" -> "$anchor належить до цього аспекту"
                "RELEVANT_TO" -> "$anchor стосується цього аспекту"
                else -> kind
            }
        }
    }
    val arrow = when (direction) {
        WorkspaceRelationDirection.ENTITY_TO_WORKSPACE, WorkspaceRelationDirection.ENTITY_TO_ASPECT,
        WorkspaceRelationDirection.ENTITY_TO_ORIENTATION -> "←"
        WorkspaceRelationDirection.ORIENTATION_MUTUAL -> "↔"
        else -> "→"
    }
    return "$arrow ${kinds.joinToString(" · ") { it.lowercase(java.util.Locale.ROOT).replace('_', ' ') }}"
}
