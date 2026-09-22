package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.data.repository.ContextRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.toHierarchyPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.hierarchyPlacementSiblingComparator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private data class CanonicalV2ReactiveStructuralState(
    val placements: List<HierarchyPlacement>,
    val groupScopes: List<HierarchyPlacementGroupScopeEntity>,
    val presentationProvenance: CanonicalV2HierarchyPresentationProvenance,
)

private data class CanonicalV2ReactiveOrientationState(
    val managedSubjects: List<ManagedSubjectEntity>,
    val mappings: List<LegacySubjectMappingEntity>,
    val relations: List<OrientationRelationEntity>,
)

private data class CanonicalV2ReactivePresentationState(
    val workspaces: List<HierarchyContextPresentationNode>,
    val groups: List<MainBeaconGroup>,
)

/**
 * Reactive production V2 read source.
 *
 * H1, exact GroupScope, and exact linked-presentation provenance are read from
 * one SQLite statement snapshot. Presentation provenance is never inferred from
 * PlacementKind or target ancestry.
 */
@Singleton
class CanonicalV2ReactiveHierarchyReadSource
    @Inject
    constructor(
        private val database: AppDatabase,
        private val contextRepository: ContextRepository,
        private val systemWorkspacePresentationContextProjector:
            SystemWorkspacePresentationContextProjector,
        private val assembler: CanonicalV2HierarchyReadSnapshotAssembler,
    ) {
        fun observe(
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): Flow<CanonicalV2ProductionHierarchyRead> {
            require(hierarchyId == HierarchyId.GENERAL) {
                "Canonical V2 reactive read currently supports only GENERAL"
            }

            val orientationDao = database.orientationDao()
            val mainBeaconDao = database.mainBeaconDao()

            val structural =
                database.hierarchyPlacementDao()
                    .observeLiveStructuralReadFrame(hierarchyId.value)
                    .map { rows ->
                        CanonicalV2ReactiveStructuralState(
                            placements =
                                rows.map { it.placement.toHierarchyPlacementStrict() }
                                    .sortedWith(hierarchyPlacementSiblingComparator),
                            groupScopes =
                                rows.mapNotNull { it.groupScope }
                                    .distinctBy { it.placementId },
                            presentationProvenance =
                                CanonicalV2HierarchyPresentationProvenance(
                                    linkedAppearancePlacementIds =
                                        rows.mapNotNull { row ->
                                            row.linkedAppearance
                                                ?.placementId
                                                ?.let(::PlacementId)
                                        }.toSet(),
                                ),
                        )
                    }

            val orientation =
                combine(
                    orientationDao.observeManagedSubjects(),
                    orientationDao.observeLegacyMappings(),
                    orientationDao.observeOrientationRelations(),
                ) { managedSubjects, mappings, relations ->
                    CanonicalV2ReactiveOrientationState(
                        managedSubjects = managedSubjects,
                        mappings = mappings,
                        relations = relations,
                    )
                }

            val workspaces =
                systemWorkspacePresentationContextProjector
                    .observePresentationUniverse(contextRepository.getAllContextsFlow())
                    .map { presentations ->
                        presentations.map { it.toHierarchyPresentationNode() }
                    }

            val presentation =
                combine(
                    workspaces,
                    mainBeaconDao.observeGroups(),
                ) { workspacePresentations, groups ->
                    CanonicalV2ReactivePresentationState(
                        workspaces = workspacePresentations,
                        groups = groups,
                    )
                }

            return combine(
                structural,
                orientation,
                presentation,
            ) { structuralState, orientationState, presentationState ->
                assembler.assemble(
                    placements = structuralState.placements,
                    admittedWorkspacePresentations = presentationState.workspaces,
                    managedSubjects = orientationState.managedSubjects,
                    legacySubjectMappings = orientationState.mappings,
                    relations = orientationState.relations,
                    groupScopes = structuralState.groupScopes,
                    legacyGroups = presentationState.groups,
                    presentationProvenance = structuralState.presentationProvenance,
                    hierarchyId = hierarchyId,
                )
            }
        }

        fun observeWorkspacePresentations(): Flow<List<HierarchyContextPresentationNode>> =
            systemWorkspacePresentationContextProjector
                .observePresentationUniverse(contextRepository.getAllContextsFlow())
                .map { presentations ->
                    presentations.map { it.toHierarchyPresentationNode() }
                }
    }
