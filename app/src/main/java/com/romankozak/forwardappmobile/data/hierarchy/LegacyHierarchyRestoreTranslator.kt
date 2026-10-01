package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyBackupGeneration
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementAuthorityMode
import com.romankozak.forwardappmobile.core.data.models.sync.HierarchyPlacementIngressBoundary
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.classifyHierarchyBackupGeneration
import com.romankozak.forwardappmobile.core.data.models.sync.hasLegacyGeneralHierarchyEvidence
import com.romankozak.forwardappmobile.core.data.models.sync.hierarchyPlacementIngressDecision
import com.romankozak.forwardappmobile.core.data.models.sync.requireCanonicalHierarchyRestoreOutput
import com.romankozak.forwardappmobile.core.data.models.sync.withoutEmbeddedMainBeaconTopology
import com.romankozak.forwardappmobile.core.data.models.sync.withoutEmbeddedWorkspaceTopology
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType

/**
 * Full-restore-only finite compatibility translator.
 *
 * It never runs for peer merge/sync. Complete native V2 hierarchy transport,
 * including H1 + GroupScope + linked-appearance provenance, bypasses legacy
 * reconstruction. A pre-v177 native H1 payload missing linked provenance may
 * use one finite V1 reconstruction only when its deterministic structural H1
 * exactly matches the native graph. PlacementKind never reconstructs display
 * provenance. Legacy semantic relations alone do not trigger H1 creation.
 */
internal class LegacyHierarchyRestoreTranslator(
    private val builder: CanonicalV1HierarchySnapshotBuilder =
        CanonicalV1HierarchySnapshotBuilder(),
) {
    fun translate(
        source: SnapshotBundle,
        canonical: SnapshotBundle,
        authorityMode: HierarchyPlacementAuthorityMode =
            HierarchyPlacementAuthorityMode.CURRENT_PRE_CUTOVER,
        hierarchyBackupGeneration: HierarchyBackupGeneration =
            source.classifyHierarchyBackupGeneration(),
    ): SnapshotBundle {
        // Historical characterization only. Production restore calls this translator
        // explicitly with V2_AUTHORITY and never selects CURRENT at runtime.
        if (authorityMode == HierarchyPlacementAuthorityMode.CURRENT_PRE_CUTOVER) {
            return canonical
        }

        val policy =
            hierarchyPlacementIngressDecision(
                boundary = HierarchyPlacementIngressBoundary.RESTORE_COMPATIBILITY,
                authorityMode = authorityMode,
            )

        // Restore translation is selected by the explicit RESTORE_COMPATIBILITY boundary.
        if (!policy.legacyHierarchyTranslationAllowed) {
            return canonical
        }

        if (
            hierarchyBackupGeneration == HierarchyBackupGeneration.CURRENT_CANONICAL ||
            hierarchyBackupGeneration == HierarchyBackupGeneration.HISTORICAL_CANONICAL
        ) {
            require(source.hierarchyPlacements != null)
            require(source.hierarchyPlacementGroupScopes != null)
            require(source.hierarchyPlacementLinkedAppearances != null)
            return canonical.withoutConsumedLegacyStructuralEvidence(authorityMode)
        }

        var nativeForLinkedRecovery: SnapshotBundle? = null

        if (source.hierarchyPlacements != null) {
            requireCanonicalHierarchyRestoreOutput(
                authorityMode = authorityMode,
                canonicalH1Present = canonical.hierarchyPlacements != null,
            )

            var nativeCanonical = canonical
            if (
                authorityMode == HierarchyPlacementAuthorityMode.V2_AUTHORITY &&
                nativeCanonical.hierarchyPlacementGroupScopes == null
            ) {
                val nativeH1 = requireNotNull(nativeCanonical.hierarchyPlacements)
                val hasLiveRootManagedSubject =
                    nativeH1.any { placement ->
                        !placement.isDeleted &&
                            placement.hierarchyId == "GENERAL" &&
                            placement.parentPlacementId == null &&
                            placement.targetType == "MANAGED_SUBJECT"
                    }
                require(!hasLiveRootManagedSubject) {
                    "Native H1 restore with root MANAGED_SUBJECT occurrences requires explicit " +
                        "Hierarchy GroupScope provenance"
                }
                nativeCanonical =
                    nativeCanonical.copy(
                        hierarchyPlacementGroupScopes = emptyList(),
                    )
            }

            if (
                authorityMode != HierarchyPlacementAuthorityMode.V2_AUTHORITY ||
                nativeCanonical.hierarchyPlacementLinkedAppearances != null
            ) {
                return nativeCanonical.withoutConsumedLegacyStructuralEvidence(authorityMode)
            }

            val nativeH1 = requireNotNull(nativeCanonical.hierarchyPlacements)
            if (nativeH1.none { it.targetType == "WORKSPACE" }) {
                return nativeCanonical
                    .copy(hierarchyPlacementLinkedAppearances = emptyList())
                    .withoutConsumedLegacyStructuralEvidence(authorityMode)
            }

            // Pre-v177 backup compatibility. We may recover sparse display
            // provenance only from exact V1 source-authority evidence, and only
            // after proving that deterministic V1 structure is identical to the
            // native H1 graph. No PlacementKind/parent/target heuristic is used.
            nativeForLinkedRecovery = nativeCanonical
        }

        if (
            source.hierarchyPlacements == null &&
            !source.hasLegacyGeneralHierarchyEvidence()
        ) {
            val explicitEmpty =
                canonical.copy(
                    hierarchyPlacements = emptyList(),
                    hierarchyPlacementGroupScopes = emptyList(),
                    hierarchyPlacementLinkedAppearances = emptyList(),
                )
            requireCanonicalHierarchyRestoreOutput(
                authorityMode = authorityMode,
                canonicalH1Present = true,
            )
            return explicitEmpty.withoutConsumedLegacyStructuralEvidence(authorityMode)
        }

        val liveWorkspaces =
            canonical.workspaces
                .orEmpty()
                .filterNot { it.isDeleted }
        val sourceWorkspaceById = source.workspaces.orEmpty().associateBy { it.id }
        require(sourceWorkspaceById.size == source.workspaces.orEmpty().size) {
            "Restore legacy Workspace topology evidence contains duplicate ids"
        }
        val sourceContextById = source.contexts.associateBy { it.id }
        require(sourceContextById.size == source.contexts.size) {
            "Restore legacy Context topology evidence contains duplicate ids"
        }
        val workspaceInputs =
            liveWorkspaces
                .map { workspace ->
                    val workspaceEvidence = sourceWorkspaceById[workspace.id]
                    val contextEvidence = sourceContextById[workspace.id]
                    val topology =
                        when {
                            workspaceEvidence != null ->
                                LegacyWorkspaceTopologyEvidence(
                                    parentWorkspaceId = workspaceEvidence.parentWorkspaceId,
                                    order = workspaceEvidence.workspaceOrder,
                                )

                            contextEvidence != null ->
                                LegacyWorkspaceTopologyEvidence(
                                    parentWorkspaceId = contextEvidence.parentId,
                                    order = contextEvidence.order.toLong(),
                                )

                            else ->
                                error(
                                    "Canonical Workspace ${workspace.id} has no source topology evidence",
                                )
                        }
                    workspace to topology
                }
                .sortedWith(
                    compareBy(
                        { it.second.order },
                        { (it.first.nameOverride ?: it.first.id).lowercase() },
                        { it.first.id },
                    ),
                )
                .mapIndexed { ordinal, (workspace, topology) ->
                    CanonicalHierarchyEstablishmentWorkspaceInput(
                        id = workspace.id,
                        name = workspace.nameOverride ?: workspace.id,
                        canonicalParentId = topology.parentWorkspaceId,
                        order = topology.order,
                        sourceOrdinal = ordinal,
                    )
                }

        val subjectsById =
            canonical.managedSubjects
                .orEmpty()
                .associateBy { it.id }
        val mappings =
            canonical.legacySubjectMappings
                .orEmpty()
        val duplicateMappingSource =
            mappings
                .groupingBy { it.sourceType to it.sourceId }
                .eachCount()
                .entries
                .firstOrNull { it.value > 1 }
        require(duplicateMappingSource == null) {
            "Restore canonical legacy mapping payload contains duplicate source identity " +
                "${duplicateMappingSource?.key}"
        }
        val mappingsBySource =
            mappings.associateBy { it.sourceType to it.sourceId }

        val ownerRowsByBeacon =
            source.mainBeaconContextCrossRefs.groupBy { it.beaconId }
        ownerRowsByBeacon.forEach { (beaconId, rows) ->
            val duplicate =
                rows.groupingBy { it.contextId }
                    .eachCount()
                    .entries
                    .firstOrNull { it.value > 1 }
            require(duplicate == null) {
                "Restore Main Beacon $beaconId has duplicate operational-owner ref ${duplicate?.key}"
            }
        }

        val groupRowsByBeacon =
            source.mainBeaconGroupMembers.groupBy { it.beaconId }
        groupRowsByBeacon.forEach { (beaconId, rows) ->
            val duplicate =
                rows.groupingBy { it.groupId }
                    .eachCount()
                    .entries
                    .firstOrNull { it.value > 1 }
            require(duplicate == null) {
                "Restore Main Beacon $beaconId has duplicate Group PART_OF ref ${duplicate?.key}"
            }
        }

        val sortedBeacons =
            source.mainBeacons.sortedWith(
                compareBy(
                    { it.order },
                    { it.title.lowercase() },
                    { it.id },
                ),
            )
        val beaconInputs =
            sortedBeacons.mapIndexed { ordinal, beacon ->
                val mapping =
                    mappingsBySource[
                        LegacyOrientationSourceType.MAIN_BEACON.name to beacon.id
                    ]
                val subject =
                    mapping?.subjectId?.let(subjectsById::get)
                val target =
                    requireNotNull(
                        resolveLegacyBeaconHierarchyTarget(
                            legacyBeaconId = beacon.id,
                            mapping = mapping,
                            subject = subject,
                        ),
                    ) {
                        "Restore Main Beacon ${beacon.id} has no live CUT_OVER ManagedSubject target"
                    }
                val resolvedSubject =
                    requireNotNull(subject) {
                        "Restore Main Beacon ${beacon.id} canonical target subject is missing"
                    }

                val ownerRows =
                    ownerRowsByBeacon[beacon.id]
                        .orEmpty()
                        .sortedWith(compareBy({ it.order }, { it.contextId }))
                val groupRows =
                    groupRowsByBeacon[beacon.id]
                        .orEmpty()
                        .sortedWith(compareBy({ it.order }, { it.groupId }))

                CanonicalHierarchyEstablishmentBeaconInput(
                    sourceId = beacon.id,
                    target = target,
                    title = resolvedSubject.title,
                    order = beacon.order,
                    canonicalParentSourceId = beacon.parentBeaconId,
                    operationalOwnerWorkspaceIds = ownerRows.map { it.contextId },
                    groupIds = groupRows.map { it.groupId },
                    groupOrders = groupRows.associate { it.groupId to it.order },
                    sourceOrdinal = ordinal,
                )
            }

        val groupInputs =
            source.mainBeaconGroups
                .sortedWith(
                    compareBy(
                        { it.order },
                        { it.title.lowercase() },
                        { it.id },
                    ),
                )
                .mapIndexed { ordinal, group ->
                    val mapping =
                        mappingsBySource[
                            LegacyOrientationSourceType.MAIN_BEACON_GROUP.name to group.id
                        ]
                    require(mapping != null && !mapping.isDeleted && mapping.state == "CUT_OVER") {
                        "Restore Main Beacon Group ${group.id} has no live CUT_OVER canonical mapping"
                    }
                    val subject =
                        requireNotNull(subjectsById[mapping.subjectId]) {
                            "Restore Main Beacon Group ${group.id} canonical subject is missing"
                        }
                    require(!subject.isDeleted && subject.subjectType == "ORIENTATION") {
                        "Restore Main Beacon Group ${group.id} canonical subject is not a live Orientation"
                    }

                    CanonicalHierarchyEstablishmentGroupInput(
                        sourceId = group.id,
                        title = subject.title,
                        order = group.order,
                        sourceOrdinal = ordinal,
                        canonicalSubjectId = subject.id,
                    )
                }

        val contextParentLinks =
            source.contextParentLinks
                .asSequence()
                .filterNot { it.isDeleted }
                .sortedWith(
                    compareBy(
                        { it.parentContextId },
                        { it.order },
                        { it.childContextId },
                    ),
                )
                .mapIndexed { ordinal, link ->
                    CanonicalHierarchyEstablishmentAdditionalWorkspaceRoute(
                        parentWorkspaceId = link.parentContextId,
                        childWorkspaceId = link.childContextId,
                        order = link.order,
                        sourceOrdinal = ordinal,
                    )
                }
                .toList()

        val beaconParentLinks =
            source.mainBeaconParentLinks
                .sortedWith(
                    compareBy(
                        { it.parentBeaconId },
                        { it.order },
                        { it.childBeaconId },
                    ),
                )
                .mapIndexed { ordinal, link ->
                    CanonicalHierarchyEstablishmentAdditionalBeaconRoute(
                        parentSourceId = link.parentBeaconId,
                        childSourceId = link.childBeaconId,
                        order = link.order,
                        sourceOrdinal = ordinal,
                    )
                }

        val snapshot =
            builder.build(
                input =
                    CanonicalHierarchyEstablishmentInput(
                        workspaces = workspaceInputs,
                        beacons = beaconInputs,
                        groups = groupInputs,
                        additionalWorkspaceRoutes = contextParentLinks,
                        additionalBeaconRoutes = beaconParentLinks,
                    ),
                hierarchyId = HierarchyId.GENERAL,
            )

        require(snapshot.diagnostics.isEmpty()) {
            val targets =
                snapshot.diagnostics
                    .joinToString { "${it.target.type}:${it.target.id}" }
            "Restore legacy hierarchy has ambiguous PRIMARY evidence: $targets"
        }

        val now = source.exportedAt.coerceAtLeast(1L)
        val translated =
            snapshot
                .toDeterministicHierarchyPlacements(now)
                .map { it.toTransportSnapshot() }
        val translatedGroupScopes =
            snapshot
                .toDeterministicHierarchyPlacementGroupScopes(now)
                .map { it.toTransportSnapshot() }
        val translatedLinkedAppearances =
            snapshot
                .toDeterministicHierarchyPlacementLinkedAppearances(now)
                .map { it.toTransportSnapshot() }

        nativeForLinkedRecovery?.let { native ->
            val nativeH1 = requireNotNull(native.hierarchyPlacements)
            require(
                exactStructuralHierarchyMatch(
                    native = nativeH1,
                    reconstructed = translated,
                ),
            ) {
                "Pre-v177 native H1 restore is missing linked-appearance provenance and " +
                    "does not exactly match deterministic legacy V1 structure"
            }

            val recovered =
                native.copy(
                    hierarchyPlacementLinkedAppearances = translatedLinkedAppearances,
                )
            requireCanonicalHierarchyRestoreOutput(
                authorityMode = authorityMode,
                canonicalH1Present = true,
            )
            return recovered.withoutConsumedLegacyStructuralEvidence(authorityMode)
        }

        val result =
            canonical.copy(
                hierarchyPlacements = translated,
                hierarchyPlacementGroupScopes = translatedGroupScopes,
                hierarchyPlacementLinkedAppearances = translatedLinkedAppearances,
            )
        requireCanonicalHierarchyRestoreOutput(
            authorityMode = authorityMode,
            canonicalH1Present = result.hierarchyPlacements != null,
        )
        return result.withoutConsumedLegacyStructuralEvidence(authorityMode)
    }
}

private data class LegacyWorkspaceTopologyEvidence(
    val parentWorkspaceId: String?,
    val order: Long,
)

/**
 * Restore-only V1 structural evidence is consumed while deriving H1 and must
 * not cross the canonicalization boundary into current runtime persistence.
 * Embedded Workspace/MainBeacon topology is likewise consumed, not persisted.
 */
private fun SnapshotBundle.withoutConsumedLegacyStructuralEvidence(
    authorityMode: HierarchyPlacementAuthorityMode,
): SnapshotBundle =
    if (authorityMode == HierarchyPlacementAuthorityMode.V2_AUTHORITY) {
        copy(
            contextParentLinks = emptyList(),
            mainBeaconParentLinks = emptyList(),
            mainBeacons = mainBeacons.map { it.withoutEmbeddedMainBeaconTopology() },
            workspaces =
                workspaces?.map { workspace ->
                    workspace.withoutEmbeddedWorkspaceTopology()
                },
        )
    } else {
        this
    }

private fun exactStructuralHierarchyMatch(
    native: List<com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot>,
    reconstructed: List<com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot>,
): Boolean {
    data class StructuralPlacement(
        val id: String,
        val hierarchyId: String,
        val targetType: String,
        val targetId: String,
        val parentPlacementId: String?,
        val placementKind: String,
        val siblingOrder: Long,
        val isDeleted: Boolean,
    )

    fun com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot.structural() =
        StructuralPlacement(
            id = id,
            hierarchyId = hierarchyId,
            targetType = targetType,
            targetId = targetId,
            parentPlacementId = parentPlacementId,
            placementKind = placementKind,
            siblingOrder = siblingOrder,
            isDeleted = isDeleted,
        )

    if (native.size != reconstructed.size) return false
    if (native.map { it.id }.toSet().size != native.size) return false
    if (reconstructed.map { it.id }.toSet().size != reconstructed.size) return false

    return native.map { it.structural() }.toSet() ==
        reconstructed.map { it.structural() }.toSet()
}
