package com.romankozak.forwardappmobile.data.repository

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.hierarchy.*
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.hierarchyPlacementSiblingComparator

/** Search must pass exact persisted coverage/history, just like the production hierarchy reader. */
internal suspend fun loadCanonicalV2SearchHierarchyRead(
    database: AppDatabase,
    presentations: List<ContextPresentation>,
    assembler: CanonicalV2HierarchyReadSnapshotAssembler,
): CanonicalV2ProductionHierarchyRead = database.withTransaction {
    val rows = database.hierarchyPlacementDao().getLiveStructuralReadFrame(HierarchyId.GENERAL.value)
    assembler.assemble(
        placements = rows.map { it.placement.toHierarchyPlacementStrict() }.sortedWith(hierarchyPlacementSiblingComparator),
        admittedWorkspacePresentations = presentations.map { it.toCanonicalV2WorkspacePresentation() },
        managedSubjects = database.orientationDao().getAllManagedSubjects(),
        legacySubjectMappings = database.orientationDao().getAllLegacyMappings(),
        relations = database.orientationDao().getAllOrientationRelations(),
        groupScopes = rows.mapNotNull { it.groupScope },
        legacyGroups = database.mainBeaconDao().getAllGroupsSync(),
        presentationProvenance = CanonicalV2HierarchyPresentationProvenance(
            rows.mapNotNull { it.linkedAppearance?.placementId?.let(::PlacementId) }.toSet(),
        ),
        applicability = rows.mapNotNull { it.applicability },
        materializedHistoricalGroupSubjects = rows.mapNotNullTo(hashSetOf()) { it.materializedHistoricalGroupSubjectId },
    )
}
