package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState

/**
 * H3 target-resolution boundary.
 *
 * Workspace eligibility is supplied by the already-admitted hierarchy
 * presentation universe. ManagedSubject liveness/display comes from its
 * canonical owning domain. Parent/order fields from either source are ignored:
 * they are never V2 hierarchy topology.
 */
class CanonicalV2HierarchyTargetResolver {
    fun resolve(
        placements: Collection<HierarchyPlacement>,
        admittedWorkspacePresentations: Collection<HierarchyContextPresentationNode>,
        managedSubjects: Collection<ManagedSubjectEntity>,
        legacySubjectMappings: Collection<LegacySubjectMappingEntity> = emptyList(),
    ): Map<HierarchyTargetRef, CanonicalV2HierarchyTargetPresentation> {
        val duplicateWorkspaceId =
            admittedWorkspacePresentations
                .groupingBy { it.id }
                .eachCount()
                .entries
                .firstOrNull { it.value > 1 }
        require(duplicateWorkspaceId == null) {
            "Admitted Workspace presentation ${duplicateWorkspaceId?.key} is duplicated"
        }

        val workspaceById = admittedWorkspacePresentations.associateBy { it.id }
        val subjectById =
            managedSubjects
                .asSequence()
                .filterNot { it.isDeleted }
                .associateBy { it.id }

        val activeBeaconMappings =
            legacySubjectMappings
                .asSequence()
                .filterNot { it.isDeleted }
                .filter { it.state == LegacySubjectMappingState.CUT_OVER.name }
                .filter { it.sourceType == LegacyOrientationSourceType.MAIN_BEACON.name }
                .toList()
        val duplicateBeaconSubject =
            activeBeaconMappings
                .groupingBy { it.subjectId }
                .eachCount()
                .entries
                .firstOrNull { it.value > 1 }
        require(duplicateBeaconSubject == null) {
            "Canonical subject ${duplicateBeaconSubject?.key} has multiple active Beacon mappings"
        }
        val legacyBeaconIdBySubjectId =
            activeBeaconMappings.associate { it.subjectId to it.sourceId }

        return placements
            .asSequence()
            .filterNot { it.isDeleted }
            .map { it.target }
            .distinct()
            .mapNotNull { target ->
                val resolved =
                    when (target.type) {
                        HierarchyTargetType.WORKSPACE ->
                            workspaceById[target.id]
                                ?.let { presentation ->
                                    presentation.name to target.id
                                }

                        HierarchyTargetType.MANAGED_SUBJECT ->
                            subjectById[target.id]
                                ?.let { subject ->
                                    subject.title to
                                        (legacyBeaconIdBySubjectId[target.id] ?: target.id)
                                }
                    } ?: return@mapNotNull null
                val (title, presentationId) = resolved

                target to
                    CanonicalV2HierarchyTargetPresentation(
                        target = target,
                        title = title,
                        presentationId = presentationId,
                    )
            }.toMap()
    }
}

enum class CanonicalV2SyntheticScopeKind {
    GROUP,
    NO_GROUP,
    NO_BEACON,
}

const val CANONICAL_V2_NO_GROUP_SCOPE_ID = "virtual:no-group"
const val CANONICAL_V2_NO_BEACON_SCOPE_ID = "virtual:no-beacon"

data class CanonicalV2SyntheticScopeInput(
    val kind: CanonicalV2SyntheticScopeKind,
    val id: String,
    val title: String,
    val order: Long,
    /**
     * H3.1 compatibility input. Target identity cannot distinguish duplicate
     * root occurrences and must not be used by the H4.0d production planner.
     */
    val rootTargets: List<HierarchyTargetRef> = emptyList(),
    /**
     * Occurrence-native production input. Non-null selects exact PlacementId
     * assignment, including an explicitly empty synthetic scope.
     */
    val rootPlacementIds: List<PlacementId>? = null,
) {
    init {
        require(kind != CanonicalV2SyntheticScopeKind.NO_BEACON) {
            "NoBeacon is derived from persisted WORKSPACE roots"
        }
        require(id.isNotBlank())
        require(title.isNotBlank())
        require(rootTargets.all { it.type == HierarchyTargetType.MANAGED_SUBJECT }) {
            "Group/NoGroup scopes may contain only MANAGED_SUBJECT root targets"
        }
        require(rootPlacementIds == null || rootTargets.isEmpty()) {
            "Synthetic scope must use either exact rootPlacementIds or compatibility rootTargets"
        }
        require(rootPlacementIds == null || rootPlacementIds.distinct().size == rootPlacementIds.size) {
            "Synthetic scope contains duplicate rootPlacementIds"
        }
    }
}

enum class CanonicalV2BreadcrumbTarget {
    CONTEXT,
    ORIENTATION_NODE,
}

sealed interface CanonicalV2PresentedHierarchyEntry {
    val level: Int
    val id: String
    val title: String
    val breadcrumbTarget: CanonicalV2BreadcrumbTarget

    data class SyntheticScope(
        val kind: CanonicalV2SyntheticScopeKind,
        override val id: String,
        override val title: String,
        override val level: Int = 0,
    ) : CanonicalV2PresentedHierarchyEntry {
        override val breadcrumbTarget =
            CanonicalV2BreadcrumbTarget.ORIENTATION_NODE
    }

    data class Occurrence(
        val placementId: PlacementId,
        val target: HierarchyTargetRef,
        val parentPlacementId: PlacementId?,
        val placementKind: PlacementKind,
        val siblingOrder: Long,
        val presentationId: String,
        override val title: String,
        override val level: Int,
    ) : CanonicalV2PresentedHierarchyEntry {
        override val id: String = presentationId
        override val breadcrumbTarget =
            if (target.type == HierarchyTargetType.WORKSPACE) {
                CanonicalV2BreadcrumbTarget.CONTEXT
            } else {
                CanonicalV2BreadcrumbTarget.ORIENTATION_NODE
            }
    }
}

data class CanonicalV2Breadcrumb(
    val id: String,
    val title: String,
    val level: Int,
    val target: CanonicalV2BreadcrumbTarget,
    val placementId: PlacementId? = null,
)

data class CanonicalV2HierarchyPresentationProjection(
    val entries: List<CanonicalV2PresentedHierarchyEntry>,
) {
    fun firstWorkspaceOccurrence(
        targetId: String,
    ): CanonicalV2PresentedHierarchyEntry.Occurrence? =
        entries
            .asSequence()
            .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
            .firstOrNull {
                it.target.type == HierarchyTargetType.WORKSPACE &&
                    it.target.id == targetId
            }

    fun breadcrumbsToWorkspace(targetId: String): List<CanonicalV2Breadcrumb> {
        val nodeIndex =
            entries.indexOfFirst { entry ->
                entry is CanonicalV2PresentedHierarchyEntry.Occurrence &&
                    entry.target.type == HierarchyTargetType.WORKSPACE &&
                    entry.target.id == targetId
            }
        if (nodeIndex == -1) return emptyList()

        val targetEntry = entries[nodeIndex]
        val ancestors = ArrayDeque<CanonicalV2PresentedHierarchyEntry>()
        var expectedLevel = targetEntry.level - 1

        for (index in nodeIndex - 1 downTo 0) {
            val entry = entries[index]
            if (entry.level == expectedLevel) {
                ancestors.addFirst(entry)
                expectedLevel--
            }
            if (expectedLevel < 0) break
        }

        return (ancestors + targetEntry).mapIndexed { index, entry ->
            CanonicalV2Breadcrumb(
                id = entry.id,
                title = entry.title,
                level = index,
                target = entry.breadcrumbTarget,
                placementId =
                    (entry as? CanonicalV2PresentedHierarchyEntry.Occurrence)
                        ?.placementId,
            )
        }
    }
}

class CanonicalV2HierarchySyntheticPresentationException(
    message: String,
) : IllegalStateException(message)

class CanonicalV2HierarchySyntheticPresentationComposer {
    fun compose(
        hierarchy: CanonicalV2HierarchyProjection,
        scopes: List<CanonicalV2SyntheticScopeInput>,
    ): CanonicalV2HierarchyPresentationProjection {
        val byPlacementId = hierarchy.occurrences.associateBy { it.placementId }
        val childrenByParentId =
            hierarchy.occurrences
                .filter { it.parentPlacementId != null }
                .groupBy { requireNotNull(it.parentPlacementId) }

        val roots =
            hierarchy.rootPlacementIds.map { rootId ->
                requireNotNull(byPlacementId[rootId]) {
                    "Projection root ${rootId.value} is missing from occurrence list"
                }
            }

        val subjectRoots =
            roots.filter { it.target.type == HierarchyTargetType.MANAGED_SUBJECT }
        val workspaceRoots =
            roots.filter { it.target.type == HierarchyTargetType.WORKSPACE }

        val sortedScopes =
            scopes.sortedWith(
                compareBy<CanonicalV2SyntheticScopeInput> {
                    if (it.kind == CanonicalV2SyntheticScopeKind.GROUP) 0 else 1
                }.thenBy { it.order }
                    .thenBy { it.title.lowercase() }
                    .thenBy { it.id },
            )

        val remainingSubjectRoots = subjectRoots.toMutableList()
        val entries = mutableListOf<CanonicalV2PresentedHierarchyEntry>()

        fun appendOccurrence(
            occurrence: CanonicalV2HierarchyOccurrence,
            level: Int,
        ) {
            entries +=
                CanonicalV2PresentedHierarchyEntry.Occurrence(
                    placementId = occurrence.placementId,
                    target = occurrence.target,
                    parentPlacementId = occurrence.parentPlacementId,
                    placementKind = occurrence.placementKind,
                    siblingOrder = occurrence.siblingOrder,
                    presentationId = occurrence.presentation.presentationId,
                    title = occurrence.presentation.title,
                    level = level,
                )

            childrenByParentId[occurrence.placementId].orEmpty().forEach { child ->
                appendOccurrence(child, level + 1)
            }
        }

        sortedScopes.forEach { scope ->
            entries +=
                CanonicalV2PresentedHierarchyEntry.SyntheticScope(
                    kind = scope.kind,
                    id = scope.id,
                    title = scope.title,
                )

            val exactRootPlacementIds = scope.rootPlacementIds
            if (exactRootPlacementIds != null) {
                exactRootPlacementIds.forEach { expectedPlacementId ->
                    val rootIndex =
                        remainingSubjectRoots.indexOfFirst {
                            it.placementId == expectedPlacementId
                        }

                    if (rootIndex == -1) {
                        throw CanonicalV2HierarchySyntheticPresentationException(
                            "Synthetic scope ${scope.id} expects root occurrence " +
                                "${expectedPlacementId.value} but no unassigned persisted " +
                                "MANAGED_SUBJECT root occurrence exists",
                        )
                    }

                    appendOccurrence(
                        remainingSubjectRoots.removeAt(rootIndex),
                        level = 1,
                    )
                }
            } else {
                scope.rootTargets.forEach { expectedTarget ->
                    val rootIndex =
                        remainingSubjectRoots.indexOfFirst { it.target == expectedTarget }

                    if (rootIndex == -1) {
                        throw CanonicalV2HierarchySyntheticPresentationException(
                            "Synthetic scope ${scope.id} expects compatibility root " +
                                "$expectedTarget but no unassigned persisted root occurrence exists",
                        )
                    }

                    appendOccurrence(
                        remainingSubjectRoots.removeAt(rootIndex),
                        level = 1,
                    )
                }
            }
        }

        if (remainingSubjectRoots.isNotEmpty()) {
            throw CanonicalV2HierarchySyntheticPresentationException(
                "Persisted MANAGED_SUBJECT roots remain without synthetic scope: " +
                    remainingSubjectRoots.joinToString { it.placementId.value },
            )
        }

        if (workspaceRoots.isNotEmpty()) {
            entries +=
                CanonicalV2PresentedHierarchyEntry.SyntheticScope(
                    kind = CanonicalV2SyntheticScopeKind.NO_BEACON,
                    id = CANONICAL_V2_NO_BEACON_SCOPE_ID,
                    title = "No beacon",
                )

            workspaceRoots.forEach { root ->
                appendOccurrence(root, level = 1)
            }
        }

        val emittedPlacementIds =
            entries
                .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
                .map { it.placementId }

        if (emittedPlacementIds != hierarchy.occurrences.map { it.placementId }) {
            throw CanonicalV2HierarchySyntheticPresentationException(
                "Synthetic composition changed persisted occurrence order or membership",
            )
        }

        return CanonicalV2HierarchyPresentationProjection(entries)
    }
}
