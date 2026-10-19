package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.StartupTrace
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.hierarchyPlacementSiblingComparator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Reactive production V2 read source.
 *
 * H1, exact GroupScope, and exact linked-presentation provenance are read from
 * one Room transaction with target identity and display metadata. Independent
 * Flow combines can mix pre-delete structure with post-delete targets even when
 * the writer is atomic. Provenance is never inferred from placement or ancestry.
 */
@Singleton
class CanonicalV2ReactiveHierarchyReadSource
    @Inject
    constructor(
        private val database: AppDatabase,
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

            return database.invalidationTracker.createFlow(
                "hierarchy_placements", "hierarchy_placement_group_scopes",
                "hierarchy_placement_linked_appearances", "hierarchy_group_scope_applicability",
                "big_picture_containers", "managed_subjects", "legacy_subject_mappings",
                "orientation_relations", "main_beacon_groups", "workspaces", "contexts",
                "workspace_tag_refs", "system_workspace_tag_seed_states",
            ).map {
                database.withTransaction {
                    val rows = database.hierarchyPlacementDao().getLiveStructuralReadFrame(hierarchyId.value)
                    val dao = database.orientationDao()
                    val workspaces = systemWorkspacePresentationContextProjector.projectPresentationUniverse()
                        .map { it.toCanonicalV2WorkspacePresentation() }
                    val subjects = dao.getAllManagedSubjects()
                    val mappings = dao.getAllLegacyMappings()
                    val relations = dao.getAllOrientationRelations()
                    val groups = database.mainBeaconDao().getAllGroupsSync()
                    StartupTrace.measureSync("Hierarchy.project") {
                        assembler.assemble(
                            placements = rows.map { it.placement.toHierarchyPlacementStrict() }
                                .sortedWith(hierarchyPlacementSiblingComparator),
                            admittedWorkspacePresentations = workspaces,
                            managedSubjects = subjects,
                            legacySubjectMappings = mappings,
                            relations = relations,
                            groupScopes = rows.mapNotNull { it.groupScope }.distinctBy { it.placementId },
                            legacyGroups = groups,
                            presentationProvenance = CanonicalV2HierarchyPresentationProvenance(
                                rows.mapNotNull { it.linkedAppearance?.placementId?.let(::PlacementId) }.toSet(),
                            ),
                            hierarchyId = hierarchyId,
                            applicability = rows.mapNotNull { it.applicability },
                            materializedHistoricalGroupSubjects = rows.mapNotNullTo(hashSetOf()) { it.materializedHistoricalGroupSubjectId },
                        )
                    }
                }
            }.distinctUntilChanged()
        }

        fun observeCanonicalWorkspacePresentations(): Flow<List<CanonicalV2WorkspacePresentation>> =
            systemWorkspacePresentationContextProjector
                .observePresentationUniverse()
                .map { presentations ->
                    presentations.map { it.toCanonicalV2WorkspacePresentation() }
                }

        /**
         * Temporary non-structural UI compatibility surface. The returned
         * node has no parent/order claims; occurrence topology is supplied by
         * the canonical V2 read.
         */
        fun observeWorkspacePresentations(): Flow<List<HierarchyContextPresentationNode>> =
            observeCanonicalWorkspacePresentations()
                .map { presentations ->
                    presentations.map { it.toHierarchyPresentationNode() }
                }
    }
