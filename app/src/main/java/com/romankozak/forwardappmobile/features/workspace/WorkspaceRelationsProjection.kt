package com.romankozak.forwardappmobile.features.workspace

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceRole
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.planning.PlanningWorkspaceRelationRow
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.entitybrowser.displayLabel
import java.util.Locale

enum class WorkspaceRelationFamily(val label: String) { SEMANTIC("Семантичні"), PLANNING("Planning") }
enum class WorkspaceRelationDirection {
    WORKSPACE_TO_ENTITY, ENTITY_TO_WORKSPACE, ASPECT_TO_ENTITY, ENTITY_TO_ASPECT,
    ORIENTATION_TO_ENTITY, ENTITY_TO_ORIENTATION, ORIENTATION_MUTUAL,
}

data class WorkspaceRelationItem(
    val family: WorkspaceRelationFamily,
    val targetType: String,
    val targetId: String,
    val title: String,
    val kinds: List<String>,
    val direction: WorkspaceRelationDirection,
    val navigation: NavTarget?,
    val typeLabel: String,
) {
    val key: String get() = "${family.name}:$targetType:$targetId:${direction.name}"
    fun compactDescription(): String {
        val arrow = when (direction) {
            WorkspaceRelationDirection.ORIENTATION_TO_ENTITY -> "→ "
            WorkspaceRelationDirection.ENTITY_TO_ORIENTATION -> "← "
            WorkspaceRelationDirection.ORIENTATION_MUTUAL -> "↔ "
            else -> ""
        }
        return "$typeLabel · $arrow${kinds.joinToString(", ")}"
    }
    fun description(): String {
        val relation = kinds.joinToString(", ")
        return "$typeLabel · " + when (direction) {
            WorkspaceRelationDirection.WORKSPACE_TO_ENTITY -> "Workspace → $relation → сутність"
            WorkspaceRelationDirection.ENTITY_TO_WORKSPACE -> "сутність → $relation → Workspace"
            WorkspaceRelationDirection.ASPECT_TO_ENTITY -> "Aspect → $relation → сутність"
            WorkspaceRelationDirection.ENTITY_TO_ASPECT -> "сутність → $relation → Aspect"
            WorkspaceRelationDirection.ORIENTATION_TO_ENTITY -> "Orientation → $relation → сутність"
            WorkspaceRelationDirection.ENTITY_TO_ORIENTATION -> "сутність → $relation → Orientation"
            WorkspaceRelationDirection.ORIENTATION_MUTUAL -> "Orientation ↔ $relation ↔ сутність"
        }
    }
}

data class WorkspaceRelationsProjection(
    val workspaceId: String,
    val relations: List<WorkspaceRelationItem>,
    val embodiedAspect: EmbodiedAspectRelations? = null,
    val embodiedOrientation: EmbodiedOrientationRelations? = null,
)

internal fun projectWorkspaceRelations(
    workspaceId: String,
    bindings: List<WorkspaceBindingEntity>,
    catalog: List<CanonicalEntitySummary>,
    planning: List<PlanningWorkspaceRelationRow>,
): WorkspaceRelationsProjection {
    val semantic = projectWorkspaceSemanticLinks(bindings.filter { it.workspaceId == workspaceId }, catalog)
        .filter { it.isNavigable }.mapNotNull { link ->
            link.ref?.let { ref -> WorkspaceRelationItem(WorkspaceRelationFamily.SEMANTIC, ref.entityClass.name, ref.id,
                link.title, link.bindingTypes, WorkspaceRelationDirection.WORKSPACE_TO_ENTITY,
                link.navigationTarget(), ref.entityClass.displayLabel()) }
        }
    val planningItems = projectPlanningRelationItems(planning, WorkspaceRelationDirection.ENTITY_TO_WORKSPACE)
    return WorkspaceRelationsProjection(workspaceId, (semantic + planningItems).sortedWith(
        compareBy({ it.family.ordinal }, { it.title.lowercase(Locale.ROOT) }, { it.targetType }, { it.targetId })))
}

internal fun projectPlanningRelationItems(
    planning: List<PlanningWorkspaceRelationRow>,
    direction: WorkspaceRelationDirection,
): List<WorkspaceRelationItem> = planning.filter { row ->
        row.ownerId.isNotBlank() && row.ownerKind in PlanningReferenceOwnerKind.entries.map { it.name } &&
            row.role in PlanningReferenceRole.entries.map { it.name }
    }.groupBy { it.ownerKind to it.ownerId }.map { (owner, rows) ->
        val kind = PlanningReferenceOwnerKind.valueOf(owner.first)
        val typeLabel = when (kind) {
            PlanningReferenceOwnerKind.SCOPE -> "Planning scope"
            PlanningReferenceOwnerKind.TACTICAL_MISSION -> "Mission"
            PlanningReferenceOwnerKind.ARC_QUEST -> "Quest"
            PlanningReferenceOwnerKind.DAY_TASK -> "Day task"
            PlanningReferenceOwnerKind.DAY_FOCUS -> "Day focus"
        }
        WorkspaceRelationItem(WorkspaceRelationFamily.PLANNING, kind.name, owner.second, rows.first().title,
            rows.map { it.role }.distinct().sorted(), direction,
            if (kind == PlanningReferenceOwnerKind.DAY_TASK) NavTarget.EditTask(owner.second) else null, typeLabel)
    }
