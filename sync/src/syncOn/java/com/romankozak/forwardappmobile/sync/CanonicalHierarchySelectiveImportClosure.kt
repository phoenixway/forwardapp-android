package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalOrientationPayload
import com.romankozak.forwardappmobile.core.data.models.sync.withoutEmbeddedWorkspaceTopology
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementGroupScopeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementLinkedAppearanceSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType

private const val GENERAL_HIERARCHY_ID = "GENERAL"

private data class CanonicalHierarchySelectiveClosure(
    val placements: List<HierarchyPlacementSnapshot>,
    val groupScopes: List<HierarchyPlacementGroupScopeSnapshot>,
    val linkedAppearances: List<HierarchyPlacementLinkedAppearanceSnapshot>,
    val selectedWorkspaceTargetIds: Set<String>,
    val selectedManagedSubjectTargetIds: Set<String>,
)

internal fun SnapshotBundle.withCanonicalHierarchySelectiveClosure(
    source: SnapshotBundle,
    selectedContextIds: Set<String>,
): SnapshotBundle {
    val sourcePlacements = source.hierarchyPlacements
        ?: return copy(
            hierarchyPlacements = null,
            hierarchyPlacementGroupScopes = null,
            groupScopeCoverage = null,
            hierarchyPlacementLinkedAppearances = null,
        )

    val sourceScopes = requireNotNull(source.hierarchyPlacementGroupScopes) {
        "Canonical hierarchy selective import requires H1 and GroupScope streams together."
    }
    val sourceLinkedAppearances =
        requireNotNull(source.hierarchyPlacementLinkedAppearances) {
            "Canonical hierarchy selective import requires H1, GroupScope, and linked-appearance streams together."
        }

    // H1-aware selective import is a canonical boundary. Old snapshots without
    // H1 keep the pre-cutover compatibility behavior above; native H1 sources
    // must not smuggle a partial canonical Orientation/Workspace envelope.
    requireValidCanonicalOrientationPayload(source)

    val closure = source.buildCanonicalHierarchySelectiveClosure(
        placements = sourcePlacements,
        groupScopes = sourceScopes,
        linkedAppearances = sourceLinkedAppearances,
        selectedContextIds = selectedContextIds,
        transportedMainBeaconIds = mainBeacons.mapTo(linkedSetOf()) { it.id },
    )

    return withCanonicalHierarchyDependencies(
        source = source,
        closure = closure,
    ).copy(
        hierarchyPlacements = closure.placements,
        hierarchyPlacementGroupScopes = closure.groupScopes,
        hierarchyPlacementLinkedAppearances = closure.linkedAppearances,
        groupScopeCoverage = source.groupScopeCoverage?.let { payload ->
            val selected = closure.placements.mapTo(hashSetOf()) { it.id }
            payload.copy(records = payload.records.filter { it.placementId in selected })
        },
    )
}

private fun SnapshotBundle.buildCanonicalHierarchySelectiveClosure(
    placements: List<HierarchyPlacementSnapshot>,
    groupScopes: List<HierarchyPlacementGroupScopeSnapshot>,
    linkedAppearances: List<HierarchyPlacementLinkedAppearanceSnapshot>,
    selectedContextIds: Set<String>,
    transportedMainBeaconIds: Set<String>,
): CanonicalHierarchySelectiveClosure {
    requireDistinctIds(
        label = "Hierarchy placement",
        ids = placements.map { it.id },
    )
    requireDistinctIds(
        label = "Hierarchy GroupScope",
        ids = groupScopes.map { it.placementId },
    )
    requireDistinctIds(
        label = "Hierarchy linked appearance",
        ids = linkedAppearances.map { it.placementId },
    )

    placements.forEach(::requireWellFormedHierarchyPlacement)
    groupScopes.forEach(::requireWellFormedHierarchyGroupScope)
    linkedAppearances.forEach(::requireWellFormedHierarchyLinkedAppearance)

    val livePlacements = placements.filterNot { it.isDeleted }
    val liveById = livePlacements.associateBy { it.id }

    livePlacements.forEach { placement ->
        val parentId = placement.parentPlacementId
        if (parentId != null) {
            val parent = requireNotNull(liveById[parentId]) {
                "Live hierarchy placement ${placement.id} references missing live parent $parentId."
            }
            require(parent.hierarchyId == placement.hierarchyId) {
                "Hierarchy placement ${placement.id} crosses hierarchy boundary through parent $parentId."
            }
        }
        requireCanonicalHierarchyTargetExists(placement)
    }
    requireAcyclicLiveHierarchy(livePlacements, liveById)
    requireUniquePrimaryAppearances(livePlacements)

    val liveScopes = groupScopes.filterNot { it.isDeleted }
    val liveScopeByPlacementId = liveScopes.associateBy { it.placementId }
    val liveRootSubjectIds = livePlacements
        .asSequence()
        .filter {
            it.parentPlacementId == null &&
                it.targetType == HierarchyTargetType.MANAGED_SUBJECT.name
        }
        .mapTo(linkedSetOf()) { it.id }

    liveScopes.forEach { scope ->
        val placement = requireNotNull(liveById[scope.placementId]) {
            "Live hierarchy GroupScope ${scope.placementId} references missing live H1 placement."
        }
        require(scope.hierarchyId == placement.hierarchyId) {
            "Hierarchy GroupScope ${scope.placementId} hierarchyId does not match H1."
        }
        require(
            placement.parentPlacementId == null &&
                placement.targetType == HierarchyTargetType.MANAGED_SUBJECT.name,
        ) {
            "Hierarchy GroupScope ${scope.placementId} must reference a root MANAGED_SUBJECT placement."
        }
    }

    val modes = groupScopeCoverage?.let { coverage ->
        require(coverage.formatVersion == 1) { "Unsupported coverage version" }
        val expected = placements.filter { it.targetType == "MANAGED_SUBJECT" }.mapTo(hashSetOf()) { it.id }
        require(coverage.records.mapTo(hashSetOf()) { it.placementId } == expected && coverage.records.size == expected.size) { "Incomplete applicability coverage" }
        require(coverage.records.all { it.mode in setOf("NATIVE", "HISTORICAL", "LEGACY_UNKNOWN") })
        coverage.records.associate { it.placementId to it.mode }
    }
    require(liveScopeByPlacementId.keys.all { modes?.get(it) != "NATIVE" }) { "Native occurrence has active GroupScope" }
    modes?.filterValues { it == "HISTORICAL" }?.keys?.forEach { id ->
        require(groupScopes.any { it.placementId == id }) { "Historical occurrence lost GroupScope history" }
    }
    val requiredLegacyRoots = liveRootSubjectIds.filterTo(hashSetOf()) { modes == null || modes[it] == "LEGACY_UNKNOWN" }
    val legacyScopeKeys = liveScopeByPlacementId.keys.filterTo(hashSetOf()) { modes == null || modes[it] == "LEGACY_UNKNOWN" }
    require(legacyScopeKeys == requiredLegacyRoots) {
        val missing = (liveRootSubjectIds - liveScopeByPlacementId.keys).take(5)
        val extra = (liveScopeByPlacementId.keys - liveRootSubjectIds).take(5)
        "Canonical hierarchy source has incomplete GroupScope provenance; missing=$missing extra=$extra"
    }

    val liveLinkedAppearances = linkedAppearances.filterNot { it.isDeleted }
    liveLinkedAppearances.forEach { linked ->
        val placement = requireNotNull(liveById[linked.placementId]) {
            "Live hierarchy linked appearance ${linked.placementId} references missing live H1 placement."
        }
        require(linked.hierarchyId == placement.hierarchyId) {
            "Hierarchy linked appearance ${linked.placementId} hierarchyId does not match H1."
        }
        require(placement.targetType == HierarchyTargetType.WORKSPACE.name) {
            "Hierarchy linked appearance ${linked.placementId} must reference a Workspace occurrence."
        }
    }

    val selectedWorkspaceTargets = resolveSelectedWorkspaceTargets(selectedContextIds)
    val selectedManagedSubjectTargets =
        resolveTransportedMainBeaconTargets(transportedMainBeaconIds)
    val selectedTargets =
        buildSet {
            selectedWorkspaceTargets.forEach {
                add(HierarchyTargetType.WORKSPACE.name to it)
            }
            selectedManagedSubjectTargets.forEach {
                add(HierarchyTargetType.MANAGED_SUBJECT.name to it)
            }
        }

    val retainedIds = linkedSetOf<String>()
    livePlacements
        .filter {
            it.parentPlacementId == null &&
                (it.targetType to it.targetId) in selectedTargets
        }
        .forEach { retainedIds += it.id }

    var changed: Boolean
    do {
        changed = false
        livePlacements.forEach { placement ->
            val parentId = placement.parentPlacementId ?: return@forEach
            if (
                placement.id !in retainedIds &&
                parentId in retainedIds &&
                (placement.targetType to placement.targetId) in selectedTargets
            ) {
                retainedIds += placement.id
                changed = true
            }
        }
    } while (changed)

    val selectedPlacements = livePlacements.filter { it.id in retainedIds }
    val selectedScopes =
        groupScopes.filter { scope ->
            scope.placementId in retainedIds
        }
    val selectedLinkedAppearances =
        liveLinkedAppearances.filter { linked ->
            linked.placementId in retainedIds
        }

    val selectedRootSubjectIds =
        selectedPlacements
            .asSequence()
            .filter {
                it.parentPlacementId == null &&
                    it.targetType == HierarchyTargetType.MANAGED_SUBJECT.name
            }
            .mapTo(linkedSetOf()) { it.id }

    require(selectedScopes.filterNot { it.isDeleted }.mapTo(linkedSetOf()) { it.placementId }
        .filterTo(hashSetOf()) { modes == null || modes[it] == "LEGACY_UNKNOWN" } ==
        selectedRootSubjectIds.filterTo(hashSetOf()) { modes == null || modes[it] == "LEGACY_UNKNOWN" }) {
        "Selective hierarchy closure lost exact GroupScope provenance."
    }

    return CanonicalHierarchySelectiveClosure(
        placements = selectedPlacements,
        groupScopes = selectedScopes,
        linkedAppearances = selectedLinkedAppearances,
        selectedWorkspaceTargetIds = selectedWorkspaceTargets,
        selectedManagedSubjectTargetIds = selectedManagedSubjectTargets,
    )
}

private fun SnapshotBundle.resolveSelectedWorkspaceTargets(
    selectedContextIds: Set<String>,
): Set<String> {
    if (selectedContextIds.isEmpty()) return emptySet()

    val liveWorkspaceIds =
        workspaces.orEmpty()
            .asSequence()
            .filterNot { it.isDeleted }
            .mapTo(hashSetOf()) { it.id }

    // Product selection is Context-oriented, not occurrence-oriented.
    // A selected Context is a hierarchy target only when the source proves a
    // live same-id canonical Workspace. Contexts without such a target remain
    // valid product selections and simply contribute no H1 occurrence.
    //
    // Malformed H1 still fails closed earlier because every live H1 Workspace
    // target is validated against the canonical source graph.
    return selectedContextIds
        .filterTo(linkedSetOf()) { it in liveWorkspaceIds }
}

private fun SnapshotBundle.resolveTransportedMainBeaconTargets(
    transportedMainBeaconIds: Set<String>,
): Set<String> {
    if (transportedMainBeaconIds.isEmpty()) return emptySet()

    val mappings = requireNotNull(legacySubjectMappings) {
        "Canonical hierarchy Main Beacon selection requires legacySubjectMappings."
    }
    val subjects = requireNotNull(managedSubjects) {
        "Canonical hierarchy Main Beacon selection requires managedSubjects."
    }
    val orientations = requireNotNull(orientations) {
        "Canonical hierarchy Main Beacon selection requires orientations."
    }

    val liveSubjectById =
        subjects
            .filterNot { it.isDeleted }
            .associateBy { it.id }
    val orientationById = orientations.associateBy { it.subjectId }

    val activeBeaconMappings =
        mappings.filter {
            !it.isDeleted &&
                it.state == LegacySubjectMappingState.CUT_OVER.name &&
                it.sourceType == LegacyOrientationSourceType.MAIN_BEACON.name &&
                it.sourceId in transportedMainBeaconIds
        }

    requireDistinctIds(
        label = "active Main Beacon mapping source",
        ids = activeBeaconMappings.map { it.sourceId },
    )

    val byLegacyId = activeBeaconMappings.associateBy { it.sourceId }

    return transportedMainBeaconIds.mapTo(linkedSetOf()) { beaconId ->
        val mapping = requireNotNull(byLegacyId[beaconId]) {
            "Transported Main Beacon $beaconId has no live CUT_OVER canonical mapping."
        }
        requireNotNull(liveSubjectById[mapping.subjectId]) {
            "Transported Main Beacon $beaconId maps to missing/deleted ManagedSubject ${mapping.subjectId}."
        }
        val orientation = requireNotNull(orientationById[mapping.subjectId]) {
            "Transported Main Beacon $beaconId maps to a subject without Orientation state."
        }
        require(orientation.kind == OrientationKind.MAIN_BEACON.name) {
            "Transported Main Beacon $beaconId maps to non-MAIN_BEACON Orientation ${mapping.subjectId}."
        }
        mapping.subjectId
    }
}

private fun SnapshotBundle.withCanonicalHierarchyDependencies(
    source: SnapshotBundle,
    closure: CanonicalHierarchySelectiveClosure,
): SnapshotBundle {
    val sourceWorkspaces = source.workspaces.orEmpty()
    requireDistinctIds(
        label = "Canonical Workspace",
        ids = sourceWorkspaces.map { it.id },
    )
    val workspaceById = sourceWorkspaces.associateBy { it.id }

    val retainedWorkspaceIds =
        closure.placements
            .asSequence()
            .filter { it.targetType == HierarchyTargetType.WORKSPACE.name }
            .mapTo(linkedSetOf()) { it.targetId }

    val backlogClosurePresent = workspaceBacklogEntries != null
    val backlogWorkspaceIds =
        if (backlogClosurePresent) {
            workspaces.orEmpty().mapTo(linkedSetOf()) { it.id }
        } else {
            emptySet()
        }
    val executionLogWorkspaceIds =
        canonicalExecutionLogs
            .orEmpty()
            .mapTo(linkedSetOf()) { it.workspaceId }

    val requiredWorkspaceIds =
        requireCanonicalWorkspaceDependencies(
            roots =
                backlogWorkspaceIds +
                    executionLogWorkspaceIds +
                    closure.selectedWorkspaceTargetIds +
                    retainedWorkspaceIds,
            workspaceById = workspaceById,
        )

    val workspaceDependencies =
        sourceWorkspaces
            .filter { it.id in requiredWorkspaceIds }
            .map { it.withoutEmbeddedWorkspaceTopology() }

    val transportedBeaconSubjectIds =
        closure.selectedManagedSubjectTargetIds

    val retainedSubjectIds =
        closure.placements
            .asSequence()
            .filter { it.targetType == HierarchyTargetType.MANAGED_SUBJECT.name }
            .mapTo(linkedSetOf()) { it.targetId }
    val scopedGroupSubjectIds =
        closure.groupScopes
            .asSequence()
            .filterNot { it.isDeleted }
            .mapNotNull { it.groupSubjectId }
            .toCollection(linkedSetOf())

    val sourceSubjects = source.managedSubjects.orEmpty()
    val sourceOrientations = source.orientations.orEmpty()
    val sourceMappings = source.legacySubjectMappings.orEmpty()
    val sourceRelations = source.orientationRelations.orEmpty()

    requireDistinctIds(
        label = "Canonical ManagedSubject",
        ids = sourceSubjects.map { it.id },
    )
    requireDistinctIds(
        label = "Canonical Orientation",
        ids = sourceOrientations.map { it.subjectId },
    )
    requireDistinctIds(
        label = "Canonical legacy mapping",
        ids = sourceMappings.map { it.id },
    )
    requireDistinctIds(
        label = "Canonical Orientation relation",
        ids = sourceRelations.map { it.id },
    )

    val liveSubjectById =
        sourceSubjects
            .filterNot { it.isDeleted }
            .associateBy { it.id }
    val orientationBySubjectId =
        sourceOrientations.associateBy { it.subjectId }

    val livePartOfRelations =
        sourceRelations.filter { relation ->
            !relation.isDeleted &&
                relation.relationType == OrientationRelationType.PART_OF.name &&
                relation.fromOrientationId in transportedBeaconSubjectIds
        }
    val semanticGroupSubjectIds =
        livePartOfRelations
            .mapTo(linkedSetOf()) { it.toOrientationId }
    val requiredGroupSubjectIds =
        scopedGroupSubjectIds + semanticGroupSubjectIds

    val requiredSubjectIds =
        buildSet {
            addAll(retainedSubjectIds)
            addAll(transportedBeaconSubjectIds)
            addAll(requiredGroupSubjectIds)
        }.toMutableSet()
    val aspectsById = source.aspects.orEmpty().associateBy { it.subjectId }
    val pendingAspectParents = java.util.ArrayDeque(requiredSubjectIds)
    while (pendingAspectParents.isNotEmpty()) {
        val id = pendingAspectParents.removeFirst()
        aspectsById[id]?.parentAspectId?.let { if (requiredSubjectIds.add(it)) pendingAspectParents.add(it) }
    }

    requiredSubjectIds.forEach { subjectId ->
        val subject = requireNotNull(liveSubjectById[subjectId]) {
            "Canonical hierarchy dependency references missing/deleted ManagedSubject $subjectId."
        }
        require(subject.subjectType in setOf(ManagedSubjectType.ORIENTATION.name, ManagedSubjectType.ASPECT.name)) {
            "Canonical hierarchy dependency $subjectId is not an Orientation ManagedSubject."
        }
        require(if (subject.subjectType == "ASPECT") aspectsById[subjectId] != null else orientationBySubjectId[subjectId] != null) {
            "Canonical hierarchy dependency $subjectId has no Orientation row."
        }
    }

    retainedSubjectIds.forEach { subjectId ->
        require(subjectId in transportedBeaconSubjectIds || source.groupScopeCoverage?.records?.any {
            it.mode == "NATIVE" && source.hierarchyPlacements.orEmpty().any { p -> p.id == it.placementId && p.targetId == subjectId }
        } == true) {
            "Selective H1 MANAGED_SUBJECT target $subjectId is not a transported Main Beacon target."
        }
    }

    transportedBeaconSubjectIds.forEach { subjectId ->
        require(
            orientationBySubjectId[subjectId]?.kind ==
                OrientationKind.MAIN_BEACON.name,
        ) {
            "Transported Main Beacon subject $subjectId is not MAIN_BEACON."
        }
    }
    requiredGroupSubjectIds.forEach { subjectId ->
        require(
            orientationBySubjectId[subjectId]?.kind ==
                OrientationKind.MAIN_BEACON_GROUP.name,
        ) {
            "Hierarchy Group subject $subjectId is not MAIN_BEACON_GROUP."
        }
    }

    val requiredMappings =
        sourceMappings.filter { mapping ->
            !mapping.isDeleted &&
                mapping.state == LegacySubjectMappingState.CUT_OVER.name &&
                mapping.subjectId in requiredSubjectIds &&
                (
                    mapping.sourceType == LegacyOrientationSourceType.MAIN_BEACON.name ||
                        mapping.sourceType ==
                            LegacyOrientationSourceType.MAIN_BEACON_GROUP.name
                )
        }

    requiredSubjectIds.forEach { subjectId ->
        val expectedSourceType =
            if (subjectId in transportedBeaconSubjectIds) {
                LegacyOrientationSourceType.MAIN_BEACON.name
            } else {
                LegacyOrientationSourceType.MAIN_BEACON_GROUP.name
            }
        val matching =
            requiredMappings.filter {
                it.subjectId == subjectId && it.sourceType == expectedSourceType
            }
        require(matching.size == 1) {
            "Canonical hierarchy dependency $subjectId requires exactly one live CUT_OVER " +
                "$expectedSourceType mapping; found=${matching.size}."
        }
    }

    requireSelectedGroupScopeSemantics(
        placements = closure.placements,
        scopes = closure.groupScopes,
        livePartOfRelations = livePartOfRelations,
    )

    val requiredSubjects =
        sourceSubjects.filter { it.id in requiredSubjectIds }
    val requiredOrientations =
        sourceOrientations.filter { it.subjectId in requiredSubjectIds }

    // Canonical BACKLOG already computed its own dependency-minimal envelope.
    // Preserve that proven subset, but never use the source.copy() inheritance
    // as evidence that unrelated canonical rows were selected.
    val baseSubjects =
        if (backlogClosurePresent) managedSubjects.orEmpty() else emptyList()
    val baseOrientations =
        if (backlogClosurePresent) orientations.orEmpty() else emptyList()
    val baseAspects =
        if (backlogClosurePresent) aspects.orEmpty() else emptyList()
    val baseAssessments =
        if (backlogClosurePresent) orientationAssessments.orEmpty() else emptyList()
    val baseRevisions =
        if (backlogClosurePresent) orientationAssessmentRevisions.orEmpty() else emptyList()
    val baseMappings =
        if (backlogClosurePresent) legacySubjectMappings.orEmpty() else emptyList()
    val baseRelations =
        if (backlogClosurePresent) orientationRelations.orEmpty() else emptyList()
    val baseAspectRefs =
        if (backlogClosurePresent) aspectOrientationRefs.orEmpty() else emptyList()
    val baseBindings =
        if (backlogClosurePresent) workspaceBindings.orEmpty() else emptyList()
    val baseCapabilities =
        if (backlogClosurePresent) workspaceCapabilityInstances.orEmpty() else emptyList()
    val baseSavedViews =
        if (backlogClosurePresent) savedOrientationViews.orEmpty() else emptyList()

    return copy(
        managedSubjects =
            mergeCanonicalById(
                baseSubjects,
                requiredSubjects,
            ) { it.id },
        orientations =
            mergeCanonicalById(
                baseOrientations,
                requiredOrientations,
            ) { it.subjectId },
        aspects = mergeCanonicalById(baseAspects, source.aspects.orEmpty().filter { it.subjectId in requiredSubjectIds }) { it.subjectId },
        orientationAssessments = mergeCanonicalById(baseAssessments, source.orientationAssessments.orEmpty().filter { it.orientationId in requiredSubjectIds }) { it.orientationId },
        orientationAssessmentRevisions = mergeCanonicalById(baseRevisions, source.orientationAssessmentRevisions.orEmpty().filter { it.orientationId in requiredSubjectIds }) { it.id },
        legacySubjectMappings =
            mergeCanonicalById(
                baseMappings,
                requiredMappings,
            ) { it.id },
        orientationRelations =
            mergeCanonicalById(
                baseRelations,
                livePartOfRelations,
            ) { it.id },
        aspectOrientationRefs = baseAspectRefs,
        workspaces = workspaceDependencies,
        workspaceBindings = baseBindings,
        workspaceCapabilityInstances = baseCapabilities,
        savedOrientationViews = baseSavedViews,
    )
}

private fun requireCanonicalWorkspaceDependencies(
    roots: Set<String>,
    workspaceById: Map<String, WorkspaceSnapshot>,
): Set<String> {
    roots.forEach { workspaceId ->
        val workspace = requireNotNull(workspaceById[workspaceId]) {
            "Canonical hierarchy Workspace dependency $workspaceId is missing."
        }
        require(!workspace.isDeleted) {
            "Canonical hierarchy Workspace dependency $workspaceId is deleted."
        }
    }
    return roots
}

private fun requireSelectedGroupScopeSemantics(
    placements: List<HierarchyPlacementSnapshot>,
    scopes: List<HierarchyPlacementGroupScopeSnapshot>,
    livePartOfRelations:
        List<com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity>,
) {
    val placementById = placements.associateBy { it.id }
    val scopesByTarget =
        scopes.filterNot { it.isDeleted }.groupBy { scope ->
            requireNotNull(placementById[scope.placementId]) {
                "Selected GroupScope ${scope.placementId} lost its H1 placement."
            }.targetId
        }

    val membershipsByTarget =
        livePartOfRelations
            .groupBy { it.fromOrientationId }
            .mapValues { (_, relations) ->
                relations.mapTo(linkedSetOf()) { it.toOrientationId }
            }

    scopesByTarget.forEach { (targetId, targetScopes) ->
        val hasNoGroup = targetScopes.any { it.groupSubjectId == null }
        val groupedIds =
            targetScopes.mapNotNullTo(linkedSetOf()) { it.groupSubjectId }
        val memberships = membershipsByTarget[targetId].orEmpty()

        require(!(hasNoGroup && groupedIds.isNotEmpty())) {
            "Selected hierarchy target $targetId mixes Group and NoGroup provenance."
        }
        if (hasNoGroup) {
            require(memberships.isEmpty()) {
                "Selected NoGroup target $targetId conflicts with live PART_OF membership."
            }
        } else {
            val retired = scopes.filter { it.isDeleted && placementById[it.placementId]?.targetId == targetId }.mapNotNullTo(hashSetOf()) { it.groupSubjectId }
            require(groupedIds.all { it in memberships } && memberships.all { it in groupedIds || it in retired }) {
                "Selected GroupScope provenance for $targetId does not match PART_OF membership."
            }
        }
    }
}

private fun <T> mergeCanonicalById(
    base: List<T>,
    dependencies: List<T>,
    id: (T) -> String,
): List<T> =
    (base + dependencies)
        .associateBy(id)
        .values
        .toList()

private fun SnapshotBundle.requireCanonicalHierarchyTargetExists(
    placement: HierarchyPlacementSnapshot,
) {
    when (HierarchyTargetType.valueOf(placement.targetType)) {
        HierarchyTargetType.WORKSPACE -> {
            require(
                workspaces.orEmpty().any {
                    it.id == placement.targetId && !it.isDeleted
                },
            ) {
                "Live H1 placement ${placement.id} references missing/deleted Workspace ${placement.targetId}."
            }
        }

        HierarchyTargetType.MANAGED_SUBJECT -> {
            require(
                managedSubjects.orEmpty().any {
                    it.id == placement.targetId && !it.isDeleted
                },
            ) {
                "Live H1 placement ${placement.id} references missing/deleted ManagedSubject ${placement.targetId}."
            }
        }
    }
}

private fun requireWellFormedHierarchyPlacement(
    placement: HierarchyPlacementSnapshot,
) {
    require(placement.id.isNotBlank()) { "Hierarchy placement id must not be blank." }
    require(placement.hierarchyId == GENERAL_HIERARCHY_ID) {
        "Unsupported selective hierarchyId=${placement.hierarchyId}."
    }
    require(placement.targetId.isNotBlank()) {
        "Hierarchy placement ${placement.id} targetId must not be blank."
    }
    val parentPlacementId = placement.parentPlacementId
    require(
        parentPlacementId == null ||
            parentPlacementId.isNotBlank(),
    ) {
        "Hierarchy placement ${placement.id} parentPlacementId must not be blank."
    }
    require(placement.parentPlacementId != placement.id) {
        "Hierarchy placement ${placement.id} cannot parent itself."
    }
    HierarchyTargetType.valueOf(placement.targetType)
    PlacementKind.valueOf(placement.placementKind)
    require(placement.version >= 1L) {
        "Hierarchy placement ${placement.id} version must be positive."
    }
}

private fun requireWellFormedHierarchyLinkedAppearance(
    linked: HierarchyPlacementLinkedAppearanceSnapshot,
) {
    require(linked.placementId.isNotBlank()) {
        "Hierarchy linked appearance placementId must not be blank."
    }
    require(linked.hierarchyId == GENERAL_HIERARCHY_ID) {
        "Unsupported hierarchy linked appearance hierarchyId=${linked.hierarchyId}."
    }
    require(linked.version >= 1L) {
        "Hierarchy linked appearance ${linked.placementId} version must be positive."
    }
}

private fun requireWellFormedHierarchyGroupScope(
    scope: HierarchyPlacementGroupScopeSnapshot,
) {
    require(scope.placementId.isNotBlank()) {
        "Hierarchy GroupScope placementId must not be blank."
    }
    require(scope.hierarchyId == GENERAL_HIERARCHY_ID) {
        "Unsupported hierarchy GroupScope hierarchyId=${scope.hierarchyId}."
    }
    val groupSubjectId = scope.groupSubjectId
    require(groupSubjectId == null || groupSubjectId.isNotBlank()) {
        "Hierarchy GroupScope ${scope.placementId} groupSubjectId must not be blank."
    }
    require(scope.version >= 1L) {
        "Hierarchy GroupScope ${scope.placementId} version must be positive."
    }
}

private fun requireUniquePrimaryAppearances(
    placements: List<HierarchyPlacementSnapshot>,
) {
    val duplicate =
        placements
            .asSequence()
            .filter { it.placementKind == PlacementKind.PRIMARY.name }
            .groupBy { Triple(it.hierarchyId, it.targetType, it.targetId) }
            .entries
            .firstOrNull { it.value.size > 1 }

    require(duplicate == null) {
        val key = duplicate?.key
        "Canonical hierarchy source has multiple PRIMARY appearances for " +
            "${key?.second}:${key?.third} in ${key?.first}."
    }
}

private fun requireAcyclicLiveHierarchy(
    placements: List<HierarchyPlacementSnapshot>,
    byId: Map<String, HierarchyPlacementSnapshot>,
) {
    placements.forEach { start ->
        val seen = linkedSetOf<String>()
        var current: HierarchyPlacementSnapshot? = start
        while (current != null) {
            require(seen.add(current.id)) {
                "Canonical hierarchy source contains a parent cycle through ${current.id}."
            }
            current = current.parentPlacementId?.let(byId::getValue)
        }
    }
}

private fun requireDistinctIds(
    label: String,
    ids: List<String>,
) {
    val duplicate =
        ids.groupingBy { it }
            .eachCount()
            .entries
            .firstOrNull { it.value > 1 }
    require(duplicate == null) {
        "$label ${duplicate?.key} appears ${duplicate?.value} times."
    }
}
