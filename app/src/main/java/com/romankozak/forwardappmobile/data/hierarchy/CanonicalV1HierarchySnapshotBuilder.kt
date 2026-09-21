package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure H2 projection of CURRENT V1 visible occurrence topology.
 *
 * Unlike OrientationHierarchyBuilder, this retains incoming-edge provenance so
 * PRIMARY evidence is never reconstructed from first-seen rendering order.
 */
@Singleton
class CanonicalV1HierarchySnapshotBuilder
    @Inject
    constructor() {
        fun build(
            input: CanonicalV1HierarchySnapshotInput,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): CanonicalV1HierarchySnapshot {
            require(hierarchyId == HierarchyId.GENERAL) {
                "H2 currently supports only ${HierarchyId.GENERAL.value}"
            }
            requireDistinct("Workspace", input.workspaces.map { it.id })
            requireDistinct("Beacon", input.beacons.map { it.legacyBeaconId })
            requireDistinct("Beacon Group", input.groups.map { it.id })
            input.beacons.forEach { beacon ->
                require(beacon.target.type == HierarchyTargetType.MANAGED_SUBJECT) {
                    "Beacon ${beacon.legacyBeaconId} must resolve to MANAGED_SUBJECT"
                }
            }

            val workspacesById = input.workspaces.associateBy { it.id }
            val workspaceComparator =
                compareBy<CanonicalV1WorkspaceSnapshotInput> { it.order }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.sourceOrdinal }
                    .thenBy { it.id }

            val canonicalWorkspaceChildren =
                input.workspaces
                    .mapNotNull { child ->
                        child.parentWorkspaceId
                            ?.takeIf { it in workspacesById }
                            ?.let { it to child }
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, children) -> children.sortedWith(workspaceComparator) }

            val activeContextLinks =
                input.contextParentLinks
                    .filter { it.parentWorkspaceId != it.childWorkspaceId }
                    .filter {
                        it.parentWorkspaceId in workspacesById &&
                            it.childWorkspaceId in workspacesById
                    }
                    .sortedWith(
                        compareBy<CanonicalV1ContextParentLinkSnapshotInput> { it.parentWorkspaceId }
                            .thenBy { it.order }
                            .thenBy { it.sourceOrdinal }
                            .thenBy { it.childWorkspaceId },
                    )

            val additionalWorkspaceChildren =
                activeContextLinks.groupBy(
                    keySelector = { it.parentWorkspaceId },
                    valueTransform = { workspacesById.getValue(it.childWorkspaceId) },
                )
            val additionalWorkspaceParents =
                activeContextLinks.groupBy(
                    keySelector = { it.childWorkspaceId },
                    valueTransform = { it.parentWorkspaceId },
                )

            val beaconComparator =
                compareBy<CanonicalV1BeaconSnapshotInput> { it.order }
                    .thenBy { it.title.lowercase() }
                    .thenBy { it.sourceOrdinal }
                    .thenBy { it.legacyBeaconId }
            val sortedBeacons = input.beacons.sortedWith(beaconComparator)
            val beaconsById = sortedBeacons.associateBy { it.legacyBeaconId }

            val beaconChildren = linkedMapOf<String, MutableList<BeaconEdge>>()
            sortedBeacons.forEach { child ->
                child.parentBeaconId?.let { parentId ->
                    beaconChildren.getOrPut(parentId) { mutableListOf() } +=
                        BeaconEdge(
                            child = child,
                            authority = CanonicalV1HierarchySourceAuthority.MAIN_BEACON_PARENT,
                            canonical = true,
                        )
                }
            }
            input.beaconParentLinks
                .asSequence()
                .filter { it.parentBeaconId != it.childBeaconId }
                .sortedWith(
                    compareBy<CanonicalV1BeaconParentLinkSnapshotInput> { it.parentBeaconId }
                        .thenBy { it.order }
                        .thenBy { it.sourceOrdinal }
                        .thenBy { it.childBeaconId },
                )
                .forEach { link ->
                    val child = beaconsById[link.childBeaconId] ?: return@forEach
                    val siblings = beaconChildren.getOrPut(link.parentBeaconId) { mutableListOf() }
                    if (siblings.none { it.child.legacyBeaconId == child.legacyBeaconId }) {
                        siblings +=
                            BeaconEdge(
                                child = child,
                                authority =
                                    CanonicalV1HierarchySourceAuthority.MAIN_BEACON_PARENT_LINK,
                                canonical = false,
                            )
                    }
                }

            val linkedBeaconParents =
                input.beaconParentLinks
                    .asSequence()
                    .filter { it.parentBeaconId != it.childBeaconId }
                    .groupBy(
                        keySelector = { it.childBeaconId },
                        valueTransform = { it.parentBeaconId },
                    )
                    .mapValues { (_, ids) -> ids.toSet() }

            val knownGroupIds = input.groups.mapTo(hashSetOf()) { it.id }
            val beaconsByGroup =
                sortedBeacons
                    .flatMap { beacon ->
                        beacon.groupIds
                            .filter { it in knownGroupIds }
                            .map { groupId ->
                                groupId to
                                    GroupedBeacon(
                                        beacon,
                                        beacon.groupOrders[groupId] ?: beacon.order,
                                    )
                            }
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, members) ->
                        members
                            .sortedWith(
                                compareBy<GroupedBeacon> { it.order }
                                    .thenBy { it.beacon.title.lowercase() }
                                    .thenBy { it.beacon.sourceOrdinal }
                                    .thenBy { it.beacon.legacyBeaconId },
                            )
                            .map { it.beacon }
                    }

            val beaconLinkedWorkspaceIds =
                sortedBeacons
                    .flatMap { it.relatedOwnerIds }
                    .filterTo(hashSetOf()) { it in workspacesById }

            val raw = mutableListOf<RawOccurrence>()
            val nextSiblingOrder = mutableMapOf<String?, Long>()

            fun appendRaw(
                key: String,
                target: HierarchyTargetRef,
                parentKey: String?,
                evidence: CanonicalV1PrimaryEvidence,
                authority: CanonicalV1HierarchySourceAuthority,
                rootGroupScope: CanonicalV1RootGroupScope? = null,
            ) {
                require(raw.none { it.key == key }) { "Duplicate H2 occurrenceKey=$key" }
                val order = nextSiblingOrder[parentKey] ?: 0L
                nextSiblingOrder[parentKey] = order + 1L
                raw +=
                    RawOccurrence(
                        key = key,
                        target = target,
                        parentKey = parentKey,
                        order = order,
                        evidence = evidence,
                        authority = authority,
                        rootGroupScope = rootGroupScope,
                    )
            }

            fun hasLinkedOwnerAncestor(
                workspace: CanonicalV1WorkspaceSnapshotInput,
                candidates: Set<String>,
            ): Boolean {
                val visited = mutableSetOf<String>()
                val pending = ArrayDeque<String>()
                workspace.parentWorkspaceId?.let(pending::add)
                additionalWorkspaceParents[workspace.id].orEmpty().forEach(pending::add)

                while (pending.isNotEmpty()) {
                    val parentId = pending.removeFirst()
                    if (!visited.add(parentId)) continue
                    if (parentId in candidates) return true
                    val parent = workspacesById[parentId] ?: continue
                    parent.parentWorkspaceId?.let(pending::add)
                    additionalWorkspaceParents[parent.id].orEmpty().forEach(pending::add)
                }
                return false
            }


            fun appendWorkspace(
                workspace: CanonicalV1WorkspaceSnapshotInput,
                parentKey: String?,
                key: String,
                authority: CanonicalV1HierarchySourceAuthority,
                canonicalRoute: Boolean,
                visited: LinkedHashSet<String>,
                skipDirectBeaconLinked: Boolean,
            ) {
                if (!visited.add(workspace.id)) return
                if (skipDirectBeaconLinked && workspace.id in beaconLinkedWorkspaceIds) return

                val evidence =
                    when {
                        !canonicalRoute -> CanonicalV1PrimaryEvidence.NONE
                        authority == CanonicalV1HierarchySourceAuthority.WORKSPACE_ROOT ->
                            CanonicalV1PrimaryEvidence.CANONICAL_ROOT
                        authority == CanonicalV1HierarchySourceAuthority.WORKSPACE_PARENT ->
                            CanonicalV1PrimaryEvidence.CANONICAL_PARENT
                        else -> CanonicalV1PrimaryEvidence.NONE
                    }

                appendRaw(
                    key = key,
                    target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, workspace.id),
                    parentKey = parentKey,
                    evidence = evidence,
                    authority = authority,
                )

                val canonicalChildren = canonicalWorkspaceChildren[workspace.id].orEmpty()
                val canonicalIds = canonicalChildren.mapTo(hashSetOf()) { it.id }

                canonicalChildren.forEach { child ->
                    appendWorkspace(
                        workspace = child,
                        parentKey = key,
                        key = "$key/${segment("workspace-parent", child.id)}",
                        authority = CanonicalV1HierarchySourceAuthority.WORKSPACE_PARENT,
                        canonicalRoute = canonicalRoute,
                        visited = LinkedHashSet(visited),
                        skipDirectBeaconLinked = false,
                    )
                }

                additionalWorkspaceChildren[workspace.id]
                    .orEmpty()
                    .filterNot { it.id in canonicalIds }
                    .forEach { child ->
                        appendWorkspace(
                            workspace = child,
                            parentKey = key,
                            key = "$key/${segment("context-parent-link", child.id)}",
                            authority = CanonicalV1HierarchySourceAuthority.CONTEXT_PARENT_LINK,
                            canonicalRoute = false,
                            visited = LinkedHashSet(visited),
                            skipDirectBeaconLinked = skipDirectBeaconLinked,
                        )
                    }
            }

            fun appendBeacon(
                beacon: CanonicalV1BeaconSnapshotInput,
                parentKey: String?,
                key: String,
                authority: CanonicalV1HierarchySourceAuthority,
                canonicalRoute: Boolean,
                visited: LinkedHashSet<String>,
                rootGroupScope: CanonicalV1RootGroupScope? = null,
            ) {
                if (!visited.add(beacon.legacyBeaconId)) return

                val evidence =
                    when {
                        !canonicalRoute -> CanonicalV1PrimaryEvidence.NONE
                        authority == CanonicalV1HierarchySourceAuthority.MAIN_BEACON_ROOT ->
                            CanonicalV1PrimaryEvidence.CANONICAL_ROOT
                        authority == CanonicalV1HierarchySourceAuthority.MAIN_BEACON_PARENT ->
                            CanonicalV1PrimaryEvidence.CANONICAL_PARENT
                        else -> CanonicalV1PrimaryEvidence.NONE
                    }

                appendRaw(
                    key = key,
                    target = beacon.target,
                    parentKey = parentKey,
                    evidence = evidence,
                    authority = authority,
                    rootGroupScope = rootGroupScope,
                )

                beaconChildren[beacon.legacyBeaconId].orEmpty().forEach { edge ->
                    appendBeacon(
                        beacon = edge.child,
                        parentKey = key,
                        key =
                            "$key/${segment(
                                if (edge.canonical) "beacon-parent" else "beacon-parent-link",
                                edge.child.legacyBeaconId,
                            )}",
                        authority = edge.authority,
                        canonicalRoute = canonicalRoute && edge.canonical,
                        visited = LinkedHashSet(visited),
                    )
                }

                val linkedOwners =
                    beacon.relatedOwnerIds
                        .filter { it in workspacesById }
                        .toCollection(linkedSetOf())

                linkedOwners
                    .asSequence()
                    .map(workspacesById::getValue)
                    .filterNot { hasLinkedOwnerAncestor(it, linkedOwners) }
                    .forEach { owner ->
                        appendWorkspace(
                            workspace = owner,
                            parentKey = key,
                            key = "$key/${segment("beacon-owner", owner.id)}",
                            authority =
                                CanonicalV1HierarchySourceAuthority.BEACON_OPERATIONAL_OWNER_PROJECTION,
                            canonicalRoute = false,
                            visited = linkedSetOf(),
                            skipDirectBeaconLinked = false,
                        )
                    }
            }


            input.groups
                .sortedWith(
                    compareBy<CanonicalV1BeaconGroupSnapshotInput> { it.order }
                        .thenBy { it.title.lowercase() }
                        .thenBy { it.sourceOrdinal }
                        .thenBy { it.id },
                )
                .forEach { group ->
                    val members = beaconsByGroup[group.id].orEmpty()
                    val memberIds = members.mapTo(hashSetOf()) { it.legacyBeaconId }
                    val scope = segment("group", group.id)

                    members
                        .filter { beacon ->
                            beacon.parentBeaconId !in memberIds &&
                                linkedBeaconParents[beacon.legacyBeaconId]
                                    .orEmpty()
                                    .none { it in memberIds }
                        }
                        .forEach { beacon ->
                            val canonicalRoot = beacon.parentBeaconId == null
                            appendBeacon(
                                beacon = beacon,
                                parentKey = null,
                                key = "$scope/${segment("beacon", beacon.legacyBeaconId)}",
                                authority =
                                    if (canonicalRoot) {
                                        CanonicalV1HierarchySourceAuthority.MAIN_BEACON_ROOT
                                    } else {
                                        CanonicalV1HierarchySourceAuthority.MAIN_BEACON_GROUP_ROOT_PROJECTION
                                    },
                                canonicalRoute = canonicalRoot,
                                visited = linkedSetOf(),
                                rootGroupScope =
                                    CanonicalV1RootGroupScope.group(
                                        group.canonicalSubjectId,
                                    ),
                            )
                        }
                }

            val noGroupBeacons =
                sortedBeacons.filter { beacon ->
                    beacon.groupIds.none { it in knownGroupIds }
                }
            val noGroupIds = noGroupBeacons.mapTo(hashSetOf()) { it.legacyBeaconId }

            noGroupBeacons
                .filter { beacon ->
                    beacon.parentBeaconId !in noGroupIds &&
                        linkedBeaconParents[beacon.legacyBeaconId]
                            .orEmpty()
                            .none { it in noGroupIds }
                }
                .forEach { beacon ->
                    val canonicalRoot = beacon.parentBeaconId == null
                    appendBeacon(
                        beacon = beacon,
                        parentKey = null,
                        key = "scope:no-group/${segment("beacon", beacon.legacyBeaconId)}",
                        authority =
                            if (canonicalRoot) {
                                CanonicalV1HierarchySourceAuthority.MAIN_BEACON_ROOT
                            } else {
                                CanonicalV1HierarchySourceAuthority.MAIN_BEACON_NO_GROUP_ROOT_PROJECTION
                            },
                        canonicalRoute = canonicalRoot,
                        visited = linkedSetOf(),
                        rootGroupScope = CanonicalV1RootGroupScope.NoGroup,
                    )
                }

            input.workspaces
                .filter { workspace ->
                    workspace.parentWorkspaceId == null ||
                        workspace.parentWorkspaceId !in workspacesById
                }
                .filter { it.id !in beaconLinkedWorkspaceIds }
                .sortedWith(workspaceComparator)
                .forEach { workspace ->
                    val canonicalRoot = workspace.parentWorkspaceId == null
                    appendWorkspace(
                        workspace = workspace,
                        parentKey = null,
                        key = "scope:no-beacon/${segment("workspace", workspace.id)}",
                        authority =
                            if (canonicalRoot) {
                                CanonicalV1HierarchySourceAuthority.WORKSPACE_ROOT
                            } else {
                                CanonicalV1HierarchySourceAuthority.WORKSPACE_ROOT_PROJECTION
                            },
                        canonicalRoute = canonicalRoot,
                        visited = linkedSetOf(),
                        skipDirectBeaconLinked = true,
                    )
                }

            return finalizeSnapshot(
                hierarchyId = hierarchyId,
                raw = raw,
            )
        }

        private fun finalizeSnapshot(
            hierarchyId: HierarchyId,
            raw: List<RawOccurrence>,
        ): CanonicalV1HierarchySnapshot {
            val candidates =
                raw.filter { it.evidence != CanonicalV1PrimaryEvidence.NONE }
                    .groupBy(
                        keySelector = { it.target },
                        valueTransform = { it.key },
                    )

            val diagnostics =
                candidates.entries
                    .filter { it.value.size > 1 }
                    .sortedWith(
                        compareBy<Map.Entry<HierarchyTargetRef, List<String>>>(
                            { it.key.type.name },
                            { it.key.id },
                        ),
                    )
                    .map { (target, keys) ->
                        CanonicalV1HierarchyDiagnostic(
                            code = CanonicalV1HierarchyDiagnosticCode.AMBIGUOUS_PRIMARY_EVIDENCE,
                            target = target,
                            occurrenceKeys = keys.sorted(),
                            message =
                                "Multiple canonical V1 appearance paths provide PRIMARY evidence for " +
                                    "${target.type}:${target.id}; H2 preserves zero PRIMARY",
                        )
                    }

            val primaryKeyByTarget =
                candidates
                    .mapNotNull { (target, keys) ->
                        keys.singleOrNull()?.let { key -> target to key }
                    }
                    .toMap()

            return CanonicalV1HierarchySnapshot(
                hierarchyId = hierarchyId,
                occurrences =
                    raw.map { occurrence ->
                        CanonicalV1HierarchyOccurrence(
                            occurrenceKey = occurrence.key,
                            target = occurrence.target,
                            parentOccurrenceKey = occurrence.parentKey,
                            siblingOrder = occurrence.order,
                            primaryEvidence = occurrence.evidence,
                            sourceAuthority = occurrence.authority,
                            placementKind =
                                if (primaryKeyByTarget[occurrence.target] == occurrence.key) {
                                    PlacementKind.PRIMARY
                                } else {
                                    PlacementKind.LINK
                                },
                            rootGroupScope = occurrence.rootGroupScope,
                        )
                    },
                diagnostics = diagnostics,
            )
        }

        private data class BeaconEdge(
            val child: CanonicalV1BeaconSnapshotInput,
            val authority: CanonicalV1HierarchySourceAuthority,
            val canonical: Boolean,
        )

        private data class GroupedBeacon(
            val beacon: CanonicalV1BeaconSnapshotInput,
            val order: Long,
        )

        private data class RawOccurrence(
            val key: String,
            val target: HierarchyTargetRef,
            val parentKey: String?,
            val order: Long,
            val evidence: CanonicalV1PrimaryEvidence,
            val authority: CanonicalV1HierarchySourceAuthority,
            val rootGroupScope: CanonicalV1RootGroupScope?,
        )

        private fun segment(
            kind: String,
            value: String,
        ): String = "$kind:${value.length}:$value"

        private fun requireDistinct(
            label: String,
            ids: List<String>,
        ) {
            val duplicate =
                ids.groupingBy { it }
                    .eachCount()
                    .entries
                    .firstOrNull { it.value > 1 }

            require(duplicate == null) {
                "$label id ${duplicate?.key} is duplicated in H2 snapshot input"
            }
        }
    }
