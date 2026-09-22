package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import javax.inject.Inject
import javax.inject.Singleton

data class CanonicalV2HierarchyPresentationProvenance(
    val linkedAppearancePlacementIds: Set<PlacementId> = emptySet(),
)

enum class CanonicalV2TargetNavigationPolicy {
    PRIMARY_THEN_FIRST_VISIBLE,
    FIRST_VISIBLE,
}

/**
 * Dormant production-facing H4.0d read contract.
 *
 * Structural ancestry is occurrence-native and comes only from the V2
 * HierarchyPlacement projection. Synthetic scopes are presentation only.
 */
data class CanonicalV2ProductionHierarchyRead(
    val hierarchy: CanonicalV2HierarchyProjection,
    val presentation: CanonicalV2HierarchyPresentationProjection,
    val presentationProvenance: CanonicalV2HierarchyPresentationProvenance =
        CanonicalV2HierarchyPresentationProvenance(),
) {
    private val presentedOccurrences =
        presentation.entries
            .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()

    private val occurrenceByPlacementId =
        presentedOccurrences.associateBy { it.placementId }

    private val syntheticScopeByPlacementId =
        buildMap<PlacementId, CanonicalV2PresentedHierarchyEntry.SyntheticScope> {
            var activeScope: CanonicalV2PresentedHierarchyEntry.SyntheticScope? = null
            presentation.entries.forEach { entry ->
                when (entry) {
                    is CanonicalV2PresentedHierarchyEntry.SyntheticScope ->
                        activeScope = entry

                    is CanonicalV2PresentedHierarchyEntry.Occurrence ->
                        put(
                            entry.placementId,
                            requireNotNull(activeScope) {
                                "V2 occurrence ${entry.placementId.value} is outside a synthetic scope"
                            },
                        )
                }
            }
        }

    init {
        require(
            presentedOccurrences.map { it.placementId } ==
                hierarchy.occurrences.map { it.placementId },
        ) {
            "Production V2 read must preserve projected occurrence order and membership"
        }
        require(occurrenceByPlacementId.size == presentedOccurrences.size) {
            "Production V2 read contains duplicate PlacementId presentation entries"
        }

        val unknown =
            presentationProvenance.linkedAppearancePlacementIds -
                occurrenceByPlacementId.keys
        require(unknown.isEmpty()) {
            "Linked-appearance provenance references unknown placements: " +
                unknown.joinToString { it.value }
        }
        presentationProvenance.linkedAppearancePlacementIds.forEach { id ->
            require(
                occurrenceByPlacementId.getValue(id).target.type ==
                    HierarchyTargetType.WORKSPACE,
            ) {
                "Linked-appearance provenance is valid only for Workspace occurrences: ${id.value}"
            }
        }
    }

    fun occurrence(
        placementId: PlacementId,
    ): CanonicalV2PresentedHierarchyEntry.Occurrence? =
        occurrenceByPlacementId[placementId]

    fun parentOccurrence(
        placementId: PlacementId,
    ): CanonicalV2PresentedHierarchyEntry.Occurrence? {
        val parentId =
            occurrenceByPlacementId[placementId]?.parentPlacementId
                ?: return null
        return occurrenceByPlacementId[parentId]
    }

    fun occurrencePath(
        placementId: PlacementId,
    ): List<CanonicalV2PresentedHierarchyEntry.Occurrence> {
        val projected = hierarchy.occurrence(placementId) ?: return emptyList()
        return projected.occurrencePath.map { id ->
            requireNotNull(occurrenceByPlacementId[id]) {
                "Projected path references missing presentation ${id.value}"
            }
        }
    }

    fun structuralAncestors(
        placementId: PlacementId,
    ): List<CanonicalV2PresentedHierarchyEntry.Occurrence> =
        occurrencePath(placementId).dropLast(1)

    fun childrenOf(
        parentPlacementId: PlacementId?,
    ): List<CanonicalV2PresentedHierarchyEntry.Occurrence> {
        val ids =
            if (parentPlacementId == null) {
                hierarchy.rootPlacementIds
            } else {
                hierarchy.occurrences
                    .asSequence()
                    .filter { it.parentPlacementId == parentPlacementId }
                    .map { it.placementId }
                    .toList()
            }

        return ids.map { id ->
            requireNotNull(occurrenceByPlacementId[id]) {
                "Projected child ${id.value} is missing from V2 presentation"
            }
        }
    }

    fun workspaceOccurrences(
        targetId: String,
    ): List<CanonicalV2PresentedHierarchyEntry.Occurrence> =
        presentedOccurrences.filter {
            it.target.type == HierarchyTargetType.WORKSPACE &&
                it.target.id == targetId
        }


    fun workspaceOccurrences(): List<CanonicalV2PresentedHierarchyEntry.Occurrence> =
        presentedOccurrences.filter {
            it.target.type == HierarchyTargetType.WORKSPACE
        }

    /**
     * Target navigation is explicitly separate from structural focus.
     */
    fun navigationOccurrence(
        target: HierarchyTargetRef,
        policy: CanonicalV2TargetNavigationPolicy,
    ): CanonicalV2PresentedHierarchyEntry.Occurrence? {
        val candidates = presentedOccurrences.filter { it.target == target }
        return when (policy) {
            CanonicalV2TargetNavigationPolicy.FIRST_VISIBLE ->
                candidates.firstOrNull()

            CanonicalV2TargetNavigationPolicy.PRIMARY_THEN_FIRST_VISIBLE ->
                candidates.firstOrNull {
                    it.placementKind == PlacementKind.PRIMARY
                } ?: candidates.firstOrNull()
        }
    }

    fun syntheticScopeFor(
        placementId: PlacementId,
    ): CanonicalV2PresentedHierarchyEntry.SyntheticScope? =
        syntheticScopeByPlacementId[placementId]

    /**
     * Never infer this from PlacementKind.
     */
    fun isLinkedAppearance(placementId: PlacementId): Boolean =
        placementId in presentationProvenance.linkedAppearancePlacementIds

    fun breadcrumbsToOccurrence(
        placementId: PlacementId,
    ): List<CanonicalV2Breadcrumb> {
        val path = occurrencePath(placementId)
        if (path.isEmpty()) return emptyList()

        val scope =
            requireNotNull(
                syntheticScopeByPlacementId[path.first().placementId],
            ) {
                "V2 occurrence ${path.first().placementId.value} has no synthetic scope"
            }

        return buildList {
            add(
                CanonicalV2Breadcrumb(
                    id = scope.id,
                    title = scope.title,
                    level = 0,
                    target = scope.breadcrumbTarget,
                    placementId = null,
                ),
            )
            path.forEachIndexed { index, occurrence ->
                add(
                    CanonicalV2Breadcrumb(
                        id = occurrence.id,
                        title = occurrence.title,
                        level = index + 1,
                        target = occurrence.breadcrumbTarget,
                        placementId = occurrence.placementId,
                    ),
                )
            }
        }
    }
}

/**
 * Dormant H4.0d adapter. H4.0d introduces no production caller.
 *
 * Structural input is limited to HierarchyPlacement. Workspace presentation
 * parent/order is ignored by the target resolver. Group membership may only
 * arrive already converted to synthetic presentation scopes.
 */
class CanonicalV2ProductionHierarchyReadAdapter(
    private val targetResolver: CanonicalV2HierarchyTargetResolver =
        CanonicalV2HierarchyTargetResolver(),
    private val projector: CanonicalV2HierarchyProjector =
        CanonicalV2HierarchyProjector(),
    private val composer: CanonicalV2HierarchySyntheticPresentationComposer =
        CanonicalV2HierarchySyntheticPresentationComposer(),
) {
    fun read(
        placements: Collection<HierarchyPlacement>,
        admittedWorkspacePresentations: Collection<HierarchyContextPresentationNode>,
        managedSubjects: Collection<ManagedSubjectEntity>,
        legacySubjectMappings: Collection<LegacySubjectMappingEntity> = emptyList(),
        syntheticScopes: List<CanonicalV2SyntheticScopeInput> = emptyList(),
        presentationProvenance: CanonicalV2HierarchyPresentationProvenance =
            CanonicalV2HierarchyPresentationProvenance(),
        hierarchyId: HierarchyId = HierarchyId.GENERAL,
    ): CanonicalV2ProductionHierarchyRead {
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
}

/**
 * Persistence-facing dormant H4.0d entry point.
 *
 * This is the literal future read chain:
 *
 * persisted HierarchyPlacement
 * -> CanonicalV2ProductionHierarchyReadAdapter
 * -> H3.1 occurrence projection
 * -> canonical target presentation
 * -> synthetic Group/NoGroup/NoBeacon composition.
 *
 * No production consumer calls this class in H4.0d.
 */
@Singleton
class CanonicalV2PersistedHierarchyReadAdapter
    @Inject
    constructor(
        private val placementRepository: CanonicalHierarchyPlacementRepository,
    ) {
        private val delegate = CanonicalV2ProductionHierarchyReadAdapter()

        suspend fun read(
            admittedWorkspacePresentations: Collection<HierarchyContextPresentationNode>,
            managedSubjects: Collection<ManagedSubjectEntity>,
            legacySubjectMappings: Collection<LegacySubjectMappingEntity> = emptyList(),
            syntheticScopes: List<CanonicalV2SyntheticScopeInput> = emptyList(),
            presentationProvenance: CanonicalV2HierarchyPresentationProvenance =
                CanonicalV2HierarchyPresentationProvenance(),
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): CanonicalV2ProductionHierarchyRead =
            delegate.read(
                placements = placementRepository.getLiveHierarchy(hierarchyId),
                admittedWorkspacePresentations = admittedWorkspacePresentations,
                managedSubjects = managedSubjects,
                legacySubjectMappings = legacySubjectMappings,
                syntheticScopes = syntheticScopes,
                presentationProvenance = presentationProvenance,
                hierarchyId = hierarchyId,
            )
    }

