package com.romankozak.forwardappmobile.core.sync

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.data.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.HistoricalGroupContainerCutover
import com.romankozak.forwardappmobile.database.AppDatabase

/** Existing H1 transport admission, extracted from Merge's outer transaction.
 * Parent evidence must arrive before checking historical root completeness.
 */
internal suspend fun admitCanonicalHierarchyStreams(db: AppDatabase, bundle: SnapshotBundle, selective: Boolean) = db.withTransaction {
    require(bundle.groupScopeCoverage == null ||
        (bundle.hierarchyPlacements != null && bundle.hierarchyPlacementGroupScopes != null)) {
        "GroupScope coverage requires the canonical H1 and GroupScope streams"
    }
    val placements = CanonicalHierarchyPlacementSyncStore(db)
    val scopes = CanonicalHierarchyPlacementGroupScopeSyncStore(db)
    val linked = CanonicalHierarchyPlacementLinkedAppearanceSyncStore(db)
    val presentation = BigPicturePresentationRepository(db)
    placements.mergeIncoming(bundle.hierarchyPlacements)
    val incomingIds = bundle.hierarchyPlacements.orEmpty().mapTo(linkedSetOf()) { it.id }
    presentation.merge(bundle.bigPicturePresentation ?: BigPicturePresentationSnapshot())
    if (selective) {
        scopes.mergeIncomingSelective(bundle.hierarchyPlacementGroupScopes, incomingIds, bundle.groupScopeCoverage)
        linked.mergeIncomingSelective(bundle.hierarchyPlacementLinkedAppearances, incomingIds)
    } else {
        scopes.mergeIncoming(bundle.hierarchyPlacementGroupScopes, bundle.groupScopeCoverage, incomingIds)
        linked.mergeIncoming(bundle.hierarchyPlacementLinkedAppearances)
    }
    HistoricalGroupContainerCutover(db).materialize(
        if (selective) bundle.hierarchyPlacementGroupScopes.orEmpty().mapNotNullTo(hashSetOf()) { it.groupSubjectId } else null)
    placements.requireCurrentHierarchyValid()
    presentation.requireCurrentUnifiedStructuralGraphValid()
    GroupScopeCoverageStore(db).validateCurrent()
}
