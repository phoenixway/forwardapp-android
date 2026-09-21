package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementGroupScopeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure production V2 read assembler.
 *
 * GENERAL topology comes only from H1. Group scope comes only from exact
 * GroupScope provenance plus canonical PART_OF. Legacy Group rows contribute
 * display identity/order only.
 */
@Singleton
class CanonicalV2HierarchyReadSnapshotAssembler
    @Inject
    constructor() {
        private val targetResolver = CanonicalV2HierarchyTargetResolver()
        private val projector = CanonicalV2HierarchyProjector()
        private val scopePlanner = CanonicalV2HierarchyScopePlanner()
        private val composer = CanonicalV2HierarchySyntheticPresentationComposer()

        fun assemble(
            placements: Collection<HierarchyPlacement>,
            admittedWorkspacePresentations: Collection<HierarchyContextPresentationNode>,
            managedSubjects: Collection<ManagedSubjectEntity>,
            legacySubjectMappings: Collection<LegacySubjectMappingEntity>,
            relations: Collection<OrientationRelationEntity>,
            groupScopes: Collection<HierarchyPlacementGroupScopeEntity>,
            legacyGroups: Collection<MainBeaconGroup>,
            presentationProvenance: CanonicalV2HierarchyPresentationProvenance,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): CanonicalV2ProductionHierarchyRead {
            require(hierarchyId == HierarchyId.GENERAL) {
                "Canonical V2 production assembler currently supports only GENERAL"
            }

            val admittedTargets =
                targetResolver.resolve(
                    placements = placements,
                    admittedWorkspacePresentations = admittedWorkspacePresentations,
                    managedSubjects = managedSubjects,
                    legacySubjectMappings = legacySubjectMappings,
                )
            val hierarchy =
                projector.project(
                    placements = placements,
                    admittedTargets = admittedTargets,
                    hierarchyId = hierarchyId,
                )
            val groups =
                resolveGroupPresentations(
                    legacyGroups = legacyGroups,
                    managedSubjects = managedSubjects,
                    legacySubjectMappings = legacySubjectMappings,
                )
            val syntheticScopes =
                scopePlanner.plan(
                    hierarchy = hierarchy,
                    groups = groups,
                    relations = relations,
                    groupScopes = groupScopes,
                )
            val presentation =
                composer.compose(
                    hierarchy = hierarchy,
                    scopes = syntheticScopes,
                )

            return CanonicalV2ProductionHierarchyRead(
                hierarchy = hierarchy,
                presentation = presentation,
                presentationProvenance = presentationProvenance,
            )
        }

        private fun resolveGroupPresentations(
            legacyGroups: Collection<MainBeaconGroup>,
            managedSubjects: Collection<ManagedSubjectEntity>,
            legacySubjectMappings: Collection<LegacySubjectMappingEntity>,
        ): List<CanonicalV2HierarchyGroupPresentation> {
            val liveSubjectsById =
                managedSubjects
                    .asSequence()
                    .filterNot { it.isDeleted }
                    .associateBy { it.id }
            val legacyGroupsById = legacyGroups.associateBy { it.id }
            val mappings =
                legacySubjectMappings
                    .asSequence()
                    .filterNot { it.isDeleted }
                    .filter { it.state == LegacySubjectMappingState.CUT_OVER.name }
                    .filter {
                        it.sourceType ==
                            LegacyOrientationSourceType.MAIN_BEACON_GROUP.name
                    }
                    .toList()

            require(mappings.map { it.sourceId }.distinct().size == mappings.size) {
                "Canonical V2 Group presentation has duplicate active legacy Group mappings"
            }
            require(mappings.map { it.subjectId }.distinct().size == mappings.size) {
                "Canonical V2 Group presentation maps one canonical Group subject more than once"
            }

            return mappings.map { mapping ->
                val subject =
                    requireNotNull(liveSubjectsById[mapping.subjectId]) {
                        "Active Group mapping ${mapping.sourceId} references missing canonical subject " +
                            mapping.subjectId
                    }
                val legacyGroup =
                    requireNotNull(legacyGroupsById[mapping.sourceId]) {
                        "Active canonical Group ${mapping.subjectId} has no legacy presentation row " +
                            mapping.sourceId
                    }

                CanonicalV2HierarchyGroupPresentation(
                    id = legacyGroup.id,
                    canonicalSubjectId = subject.id,
                    title = subject.title,
                    order = legacyGroup.order,
                )
            }
        }
    }
