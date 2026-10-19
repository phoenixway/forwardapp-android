package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class BigPictureStructure(
    val placements: List<HierarchyPlacement>,
    val presentation: BigPicturePresentationSnapshot = BigPicturePresentationSnapshot(),
    val embodimentBindings: List<WorkspaceBindingEntity> = emptyList(),
)

/** Only persisted H1 topology feeds Big Picture. GroupScope is not a structural read input. */
class BigPictureStructureRepository @Inject constructor(database: AppDatabase) {
    /**
     * One invalidation source, one Room transaction, one coherent frame.
     *
     * Independently combining H1 and presentation Flows can emit a mixed
     * state between the two invalidation deliveries of one committed move.
     * Do not split these reads back into independently combined streams.
     */
    private val structural = database.invalidationTracker.createFlow(
        "hierarchy_placements",
        "big_picture_containers",
        "big_picture_memberships",
        "workspace_bindings",
        "workspaces",
    ).map {
        database.withTransaction {
            val live = database.hierarchyPlacementDao()
                .getLiveHierarchy(HierarchyId.GENERAL.value)
                .map { it.toHierarchyPlacementStrict() }

            val presentationDao = database.bigPicturePresentationDao()
            val presentation = BigPicturePresentationSnapshot(
                containers = presentationDao.containers().map { it.record },
                memberships = presentationDao.memberships().map { it.record },
            )

            BigPictureStructure(
                placements = live,
                presentation = presentation,
                embodimentBindings = database.orientationDao().getLiveEmbodimentBindings(),
            )
        }
    }.distinctUntilChanged()

    fun observe() = structural
}
