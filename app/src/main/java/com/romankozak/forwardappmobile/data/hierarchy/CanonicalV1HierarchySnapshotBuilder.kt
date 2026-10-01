package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure deterministic H2 projection from source-neutral establishment evidence.
 *
 * Source adapters own persisted V1/fresh-native acquisition. This builder owns
 * the single occurrence/provenance algorithm, retaining incoming-edge evidence
 * so PRIMARY is never reconstructed from first-seen rendering order.
 */
@Singleton
class CanonicalV1HierarchySnapshotBuilder
    @Inject
    constructor() {
        /**
         * Builds the frozen deterministic H2 occurrence projection from
         * source-neutral establishment evidence.
         *
         * Literal occurrence-key segments such as `context-parent-link` and
         * `beacon-parent-link` are historical identity tokens. They remain
         * unchanged because PlacementId is derived from occurrenceKey.
         */
        fun build(
            input: CanonicalHierarchyEstablishmentInput,
            hierarchyId: HierarchyId = HierarchyId.GENERAL,
        ): CanonicalV1HierarchySnapshot {
            require(hierarchyId == HierarchyId.GENERAL) {
                "H2 currently supports only ${HierarchyId.GENERAL.value}"
            }
            requireDistinct("Workspace", input.workspaces.map { it.id })
            requireDistinct("Beacon", input.beacons.map { it.sourceId })
            requireDistinct("Beacon Group", input.groups.map { it.sourceId })
            input.beacons.forEach { beacon ->
                require(beacon.target.type == HierarchyTargetType.MANAGED_SUBJECT) {
                    "Beacon ${beacon.sourceId} must resolve to MANAGED_SUBJECT"
                }
            }

            val workspacesById = input.workspaces.associateBy { it.id }
            val workspaceComparator =
                compareBy<CanonicalHierarchyEstablishmentWorkspaceInput> { it.order }
                    .thenBy { it.name.lowercase() }
                    .thenBy { it.sourceOrdinal }
                    .thenBy { it.id }

            val canonicalWorkspaceChildren =
                input.workspaces
                    .mapNotNull { child ->
                        child.canonicalParentId
                            ?.takeIf { it in workspacesById }
                            ?.let { it to child }
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, children) -> children.sortedWith(workspaceComparator) }

            val activeContextLinks =
                input.additionalWorkspaceRoutes
                    .filter { it.parentWorkspaceId != it.childWorkspaceId }
                    .filter {
                        it.parentWorkspaceId in workspacesById &&
                            it.childWorkspaceId in workspacesById
                    }
                    .sortedWith(
                        compareBy<CanonicalHierarchyEstablishmentAdditionalWorkspaceRoute> { it.parentWorkspaceId }
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
                compareBy<CanonicalHierarchyEstablishmentBeaconInput> { it.order }
                    .thenBy { it.title.lowercase() }
                    .thenBy { it.sourceOrdinal }
                    .thenBy { it.sourceId }
            val sortedBeacons = input.beacons.sortedWith(beaconComparator)
            val beaconsById = sortedBeacons.associateBy { it.sourceId }

            val beaconChildren = linkedMapOf<String, MutableList<BeaconEdge>>()
            sortedBeacons.forEach { child ->
                child.canonicalParentSourceId?.let { parentId ->
                    beaconChildren.getOrPut(parentId) { mutableListOf() } +=
                        BeaconEdge(
                            child = child,
                            authority = CanonicalV1HierarchySourceAuthority.MAIN_BEACON_PARENT,
                            canonical = true,
                        )
                }
            }
            input.additionalBeaconRoutes
                .asSequence()
                .filter { it.parentSourceId != it.childSourceId }
                .sortedWith(
                    compareBy<CanonicalHierarchyEstablishmentAdditionalBeaconRoute> { it.parentSourceId }
                        .thenBy { it.order }
                        .thenBy { it.sourceOrdinal }
                        .thenBy { it.childSourceId },
                )
                .forEach { link ->
                    val child = beaconsById[link.childSourceId] ?: return@forEach
                    val siblings = beaconChildren.getOrPut(link.parentSourceId) { mutableListOf() }
                    if (siblings.none { it.child.sourceId == child.sourceId }) {
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
                input.additionalBeaconRoutes
                    .asSequence()
                    .filter { it.parentSourceId != it.childSourceId }
                    .groupBy(
                        keySelector = { it.childSourceId },
                        valueTransform = { it.parentSourceId },
                    )
                    .mapValues { (_, ids) -> ids.toSet() }

            val knownGroupIds = input.groups.mapTo(hashSetOf()) { it.sourceId }
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
                                    .thenBy { it.beacon.sourceId },
                            )
                            .map { it.beacon }
                    }

            val beaconLinkedWorkspaceIds =
                sortedBeacons
                    .flatMap { it.operationalOwnerWorkspaceIds }
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
                workspace: CanonicalHierarchyEstablishmentWorkspaceInput,
                candidates: Set<String>,
            ): Boolean {
                val visited = mutableSetOf<String>()
                val pending = ArrayDeque<String>()
                workspace.canonicalParentId?.let(pending::add)
                additionalWorkspaceParents[workspace.id].orEmpty().forEach(pending::add)

                while (pending.isNotEmpty()) {
                    val parentId = pending.removeFirst()
                    if (!visited.add(parentId)) continue
                    if (parentId in candidates) return true
                    val parent = workspacesById[parentId] ?: continue
                    parent.canonicalParentId?.let(pending::add)
                    additionalWorkspaceParents[parent.id].orEmpty().forEach(pending::add)
                }
                return false
            }


            fun appendWorkspace(
                workspace: CanonicalHierarchyEstablishmentWorkspaceInput,
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
                beacon: CanonicalHierarchyEstablishmentBeaconInput,
                parentKey: String?,
                key: String,
                authority: CanonicalV1HierarchySourceAuthority,
                canonicalRoute: Boolean,
                visited: LinkedHashSet<String>,
                rootGroupScope: CanonicalV1RootGroupScope? = null,
            ) {
                if (!visited.add(beacon.sourceId)) return

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

                beaconChildren[beacon.sourceId].orEmpty().forEach { edge ->
                    appendBeacon(
                        beacon = edge.child,
                        parentKey = key,
                        key =
                            "$key/${segment(
                                if (edge.canonical) "beacon-parent" else "beacon-parent-link",
                                edge.child.sourceId,
                            )}",
                        authority = edge.authority,
                        canonicalRoute = canonicalRoute && edge.canonical,
                        visited = LinkedHashSet(visited),
                    )
                }

                val linkedOwners =
                    beacon.operationalOwnerWorkspaceIds
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
                    compareBy<CanonicalHierarchyEstablishmentGroupInput> { it.order }
                        .thenBy { it.title.lowercase() }
                        .thenBy { it.sourceOrdinal }
                        .thenBy { it.sourceId },
                )
                .forEach { group ->
                    val members = beaconsByGroup[group.sourceId].orEmpty()
                    val memberIds = members.mapTo(hashSetOf()) { it.sourceId }
                    val scope = segment("group", group.sourceId)

                    members
                        .filter { beacon ->
                            beacon.canonicalParentSourceId !in memberIds &&
                                linkedBeaconParents[beacon.sourceId]
                                    .orEmpty()
                                    .none { it in memberIds }
                        }
                        .forEach { beacon ->
                            val canonicalRoot = beacon.canonicalParentSourceId == null
                            appendBeacon(
                                beacon = beacon,
                                parentKey = null,
                                key = "$scope/${segment("beacon", beacon.sourceId)}",
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
            val noGroupIds = noGroupBeacons.mapTo(hashSetOf()) { it.sourceId }

            noGroupBeacons
                .filter { beacon ->
                    beacon.canonicalParentSourceId !in noGroupIds &&
                        linkedBeaconParents[beacon.sourceId]
                            .orEmpty()
                            .none { it in noGroupIds }
                }
                .forEach { beacon ->
                    val canonicalRoot = beacon.canonicalParentSourceId == null
                    appendBeacon(
                        beacon = beacon,
                        parentKey = null,
                        key = "scope:no-group/${segment("beacon", beacon.sourceId)}",
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
                    workspace.canonicalParentId == null ||
                        workspace.canonicalParentId !in workspacesById
                }
                .filter { it.id !in beaconLinkedWorkspaceIds }
                .sortedWith(workspaceComparator)
                .forEach { workspace ->
                    val canonicalRoot = workspace.canonicalParentId == null
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
            val child: CanonicalHierarchyEstablishmentBeaconInput,
            val authority: CanonicalV1HierarchySourceAuthority,
            val canonical: Boolean,
        )

        private data class GroupedBeacon(
            val beacon: CanonicalHierarchyEstablishmentBeaconInput,
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
