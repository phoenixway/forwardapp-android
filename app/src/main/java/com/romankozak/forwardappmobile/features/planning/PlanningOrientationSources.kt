package com.romankozak.forwardappmobile.features.planning

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedMetaChip
import kotlinx.coroutines.flow.combine

/** Explicit SOURCE only; neither participation nor RELATED establishes the basis of an item. */
internal fun projectPlanningOrientationSources(
    rows: List<PlanningReferenceEntity>,
    catalog: List<CanonicalEntitySummary>,
    ownerKind: PlanningReferenceOwnerKind,
): Map<String, CanonicalEntitySummary> {
    val entities = catalog.associateBy { it.ref }
    return rows.filter {
        !it.isDeleted && it.ownerKind == ownerKind.name && it.role == PlanningReferenceRole.SOURCE.name
    }.groupBy { it.ownerId }.mapNotNull { (ownerId, candidates) ->
        if (ownerId.isBlank()) return@mapNotNull null
        if (ownerKind == PlanningReferenceOwnerKind.TACTICAL_MISSION &&
            ownerId.toLongOrNull()?.takeIf { it > 0 }?.toString() != ownerId) return@mapNotNull null
        val row = candidates.singleOrNull()?.takeIf { it.targetKind == PlanningReferenceTargetKind.ORIENTATION.name }
            ?: return@mapNotNull null
        entities[CanonicalEntityRef.Orientation(row.targetId)]?.let { ownerId to it }
    }.toMap()
}

internal data class PlanningOrientationSourceSnapshot(
    val sources: Map<String, CanonicalEntitySummary> = emptyMap(),
    val embodiments: Map<CanonicalEntityRef, CanonicalEntityRef.Workspace> = emptyMap(),
    val related: Map<String, List<CanonicalEntitySummary>> = emptyMap(),
) {
    fun destination(subjectId: String): NavTarget? {
        val ref = CanonicalEntityRef.Orientation(subjectId)
        if (sources.values.none { it.ref == ref } && related.values.none { links -> links.any { it.ref == ref } }) return null
        return (embodiments[ref] ?: ref).planningNavigationTarget()
    }
}

/** RELATED is an explicit Properties reference, not inferred participation or semantic nesting. */
internal fun projectPlanningRelatedOrientations(
    rows: List<PlanningReferenceEntity>,
    catalog: List<CanonicalEntitySummary>,
    ownerKind: PlanningReferenceOwnerKind,
): Map<String, List<CanonicalEntitySummary>> {
    val entities = catalog.associateBy { it.ref }
    return rows.filter {
        !it.isDeleted && it.ownerKind == ownerKind.name && it.role == PlanningReferenceRole.RELATED.name &&
            it.targetKind == PlanningReferenceTargetKind.ORIENTATION.name && it.ownerId.isNotBlank() &&
            (ownerKind != PlanningReferenceOwnerKind.TACTICAL_MISSION ||
                it.ownerId.toLongOrNull()?.takeIf { id -> id > 0 }?.toString() == it.ownerId)
    }.groupBy { it.ownerId }.mapValues { (_, refs) ->
        refs.mapNotNull { entities[CanonicalEntityRef.Orientation(it.targetId)] }.distinctBy { it.ref }
            .sortedWith(compareBy({ it.title.lowercase(java.util.Locale.ROOT) }, { it.ref.id }))
    }.filterValues { it.isNotEmpty() }
}

/** Three bulk reactive snapshots shared by all rows, never per-card queries. */
internal fun observePlanningOrientationSources(
    references: CanonicalPlanningReferenceRepository,
    catalog: CanonicalEntityCatalogRepository,
    orientationDao: OrientationDao,
    ownerKind: PlanningReferenceOwnerKind,
) = combine(references.observeAll(), catalog.observeCatalog(), orientationDao.observeWorkspaceBindings()) { rows, entities, bindings ->
    PlanningOrientationSourceSnapshot(
        projectPlanningOrientationSources(rows, entities, ownerKind),
        resolveOperationalEmbodiments(bindings, entities),
        projectPlanningRelatedOrientations(rows, entities, ownerKind),
    )
}

@Composable
internal fun PlanningOrientationSourceBadge(source: CanonicalEntitySummary, related: Boolean = false, onOpen: () -> Unit) {
    UnifiedMetaChip(text = "${if (related) "Пов’язано" else "На основі"} · ${source.title}", icon = Icons.Outlined.Explore, onClick = onOpen)
}
