package com.romankozak.forwardappmobile.data.hierarchy

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.BigPicturePresentationSnapshot
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class BigPictureStructure(
    val placements: List<HierarchyPlacement>,
    val rootGroupSubjects: Map<PlacementId, String?> = emptyMap(),
    val groupOrder: Map<String, Long> = emptyMap(),
    val presentation: BigPicturePresentationSnapshot = BigPicturePresentationSnapshot(),
)

/** H1 owns topology; GroupScope is optional historical presentation input, not generic containment. */
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
        "hierarchy_placement_group_scopes",
        "big_picture_containers",
        "big_picture_memberships",
    ).map {
        database.withTransaction {
            val live = database.hierarchyPlacementDao()
                .getLiveHierarchy(HierarchyId.GENERAL.value)
                .map { it.toHierarchyPlacementStrict() }

            val liveIds = live.mapTo(hashSetOf()) { it.id.value }
            val scopes = database.hierarchyPlacementGroupScopeDao().getAll()
                .filter {
                    !it.isDeleted &&
                        it.hierarchyId == HierarchyId.GENERAL.value &&
                        it.placementId in liveIds
                }

            val presentationDao = database.bigPicturePresentationDao()
            val presentation = BigPicturePresentationSnapshot(
                containers = presentationDao.containers().map { it.record },
                memberships = presentationDao.memberships().map { it.record },
            )

            BigPictureStructure(
                placements = live,
                rootGroupSubjects = scopes.associate {
                    PlacementId(it.placementId) to it.groupSubjectId
                },
                presentation = presentation,
            )
        }
    }.distinctUntilChanged()

    // Existing compatibility rows contribute group display order only, never membership/topology.
    private val groupOrder = combine(
        database.mainBeaconDao().observeGroups(),
        database.orientationDao().observeLegacyMappings(),
    ) { groups, mappings ->
        val orderByLegacyId = groups.associate { it.id to it.order }
        mappings.filter {
            !it.isDeleted && it.state == LegacySubjectMappingState.CUT_OVER.name &&
                it.sourceType == LegacyOrientationSourceType.MAIN_BEACON_GROUP.name
        }.associate { it.subjectId to (orderByLegacyId[it.sourceId] ?: Long.MAX_VALUE) }
    }.distinctUntilChanged()

    // Historical compatibility contributes display order only. All
    // topology-sensitive inputs are already one atomic structural frame.
    fun observe() = combine(structural, groupOrder) { structure, order ->
        structure.copy(groupOrder = order)
    }.distinctUntilChanged()
}
