package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import javax.inject.Inject

data class OrientationBeaconInput(
    val id: String,
    val title: String,
    val order: Long,
    val readinessStatus: MainBeaconReadinessStatus,
    val parentBeaconId: String?,
    val relatedOwnerIds: List<String>,
    val groupIds: List<String>,
    val groupOrders: Map<String, Long> = emptyMap(),
)

class OrientationHierarchyBuilder
    @Inject
    constructor() {
        private data class OperationalPlacementHierarchy(
            val allProjects: List<HierarchyContextPresentationNode>,
            val topLevelProjects: List<HierarchyContextPresentationNode>,
            val childMap: Map<String, List<HierarchyContextPresentationNode>>,
        )

        fun build(
            presentationHierarchy: HierarchyPresentationData,
            rawBackedProjectIds: Set<String>,
            beacons: List<OrientationBeaconInput>,
            groups: List<MainBeaconGroup> = emptyList(),
            parentLinks: List<ContextParentLink> = emptyList(),
            beaconParentLinks: List<MainBeaconParentLink> = emptyList(),
            workspaces: List<WorkspaceEntity> = emptyList(),
        ): List<OrientationHierarchyItem> {
            val liveWorkspacesById =
                workspaces
                    .asSequence()
                    .filterNot { it.isDeleted }
                    .associateBy { it.id }
            val canonicalWorkspacesById =
                liveWorkspacesById.filterValues {
                    it.sourceContextId == null &&
                        (
                            it.provenance == WorkspaceProvenance.CANONICAL_ONLY.name ||
                                (
                                    it.provenance == WorkspaceProvenance.STANDALONE.name &&
                                        !SystemContexts.isSystem(ContextId(it.id))
                                )
                        )
                }
            val basePresentationsById = presentationHierarchy.allProjects.associateBy { it.id }
            val operationalOwnerIds =
                rawBackedProjectIds +
                    liveWorkspacesById
                        .values
                        .asSequence()
                        .filter { workspace ->
                            workspace.sourceContextId == null &&
                                (
                                    (
                                        SystemContexts.isSystem(ContextId(workspace.id)) &&
                                            workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name
                                    ) ||
                                        (
                                            !SystemContexts.isSystem(ContextId(workspace.id)) &&
                                                workspace.provenance == WorkspaceProvenance.STANDALONE.name
                                        )
                                )
                        }.mapTo(linkedSetOf()) { it.id }
            val operationalOwnerPresentations =
                presentationHierarchy.allProjects.filter { it.id in operationalOwnerIds }
            val operationalPlacementHierarchy =
                buildOperationalPlacementHierarchy(
                    presentations = operationalOwnerPresentations,
                    workspaces = liveWorkspacesById.values,
                )
            val operationalPresentationsById =
                operationalPlacementHierarchy.allProjects.associateBy { it.id }

            // Presentation owns display payload. Workspace-backed operational
            // projection owns placement for raw-backed nodes. Shell-free
            // presentation nodes retain their canonical presentation placement.
            val effectivePresentationHierarchy =
                HierarchyPresentationTreeBuilder().build(
                    presentationHierarchy.allProjects.map { presentation ->
                        operationalPresentationsById[presentation.id]?.let { operationalPresentation ->
                            presentation.copy(
                                parentId = operationalPresentation.parentId,
                                order = operationalPresentation.order,
                            )
                        } ?: presentation
                    },
                )

            if (effectivePresentationHierarchy.allProjects.isEmpty() && beacons.isEmpty()) return emptyList()
            val additionalChildrenByParentId = buildAdditionalChildrenByParentId(parentLinks, operationalPresentationsById)
            val additionalParentsByChildId = buildAdditionalParentsByChildId(parentLinks, operationalPresentationsById)
            val operationalBeacons =
                beacons.map { details ->
                    details.copy(relatedOwnerIds = details.relatedOwnerIds.filter { it in operationalPresentationsById })
                }
            val beaconIdsByContextId = buildBeaconIdsByContextId(operationalBeacons)
            val linkedContextIds = beaconIdsByContextId.keys
            val result = mutableListOf<OrientationHierarchyItem>()
            val sortedBeacons =
                operationalBeacons.sortedWith(
                    compareBy<OrientationBeaconInput> { it.order }.thenBy { it.title.lowercase() },
                )
            val childBeaconsByParentId = buildChildBeaconsByParentId(sortedBeacons, beaconParentLinks)
            val linkedParentBeaconIdsByChildId = buildLinkedParentBeaconIdsByChildId(beaconParentLinks)
            val knownGroupIds = groups.mapTo(hashSetOf()) { it.id }
            val beaconsByGroupId =
                sortedBeacons
                    .flatMap { details ->
                        details.groupIds
                            .filter { it in knownGroupIds }
                            .map { groupId -> groupId to GroupedBeacon(details, details.groupOrders[groupId] ?: details.order) }
                    }
                    .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                    .mapValues { (_, groupBeacons) ->
                        groupBeacons
                            .sortedWith(compareBy<GroupedBeacon> { it.order }.thenBy { it.details.title.lowercase() })
                            .map { it.details }
                    }

            groups
                .sortedWith(compareBy<MainBeaconGroup> { it.order }.thenBy { it.title.lowercase() })
                .forEach { group ->
                    val groupBeacons = beaconsByGroupId[group.id].orEmpty()
                    val groupBeaconIds = groupBeacons.mapTo(hashSetOf()) { it.id }
                    val rootGroupBeacons =
                        groupBeacons.filter { details ->
                            details.parentBeaconId !in groupBeaconIds &&
                                linkedParentBeaconIdsByChildId[details.id].orEmpty().none { it in groupBeaconIds }
                        }
                    result +=
                        OrientationHierarchyItem(
                            node =
                                OrientationHierarchyNode.Group(
                                    id = group.id,
                                    title = group.title,
                                    beaconCount = groupBeacons.size,
                                ),
                            level = 0,
                        )
                    rootGroupBeacons.forEach { details ->
                        appendBeaconSubtree(
                            details = details,
                            level = 1,
                            childBeaconsByParentId = childBeaconsByParentId,
                            presentationsById = operationalPresentationsById,
                            additionalChildrenByParentId = additionalChildrenByParentId,
                            additionalParentsByChildId = additionalParentsByChildId,
                            beaconIdsByContextId = beaconIdsByContextId,
                            hierarchy = operationalPlacementHierarchy,
                            result = result,
                            visitedBeaconIds = linkedSetOf(),
                        )
                    }
                }

            val noGroupBeacons = sortedBeacons.filter { details -> details.groupIds.none { it in knownGroupIds } }
            if (noGroupBeacons.isNotEmpty()) {
                val noGroupBeaconIds = noGroupBeacons.mapTo(hashSetOf()) { it.id }
                val rootNoGroupBeacons =
                    noGroupBeacons.filter { details ->
                        details.parentBeaconId !in noGroupBeaconIds &&
                            linkedParentBeaconIdsByChildId[details.id].orEmpty().none { it in noGroupBeaconIds }
                    }
                result +=
                    OrientationHierarchyItem(
                        node = OrientationHierarchyNode.NoGroup,
                        level = 0,
                    )
                rootNoGroupBeacons.forEach { details ->
                    appendBeaconSubtree(
                        details = details,
                        level = 1,
                        childBeaconsByParentId = childBeaconsByParentId,
                        presentationsById = operationalPresentationsById,
                        additionalChildrenByParentId = additionalChildrenByParentId,
                        additionalParentsByChildId = additionalParentsByChildId,
                        beaconIdsByContextId = beaconIdsByContextId,
                        hierarchy = operationalPlacementHierarchy,
                        result = result,
                        visitedBeaconIds = linkedSetOf(),
                    )
                }
            }

            val noBeaconRoots =
                effectivePresentationHierarchy.topLevelProjects
                    .filter { it.id !in linkedContextIds }
                    .sortedWith(presentationSort())

            if (noBeaconRoots.isNotEmpty()) {
                result +=
                    OrientationHierarchyItem(
                        node = OrientationHierarchyNode.NoBeacon,
                        level = 0,
                    )
                noBeaconRoots.forEach { presentation ->
                    appendPresentationSubtree(
                        presentation = presentation,
                        level = 1,
                        hierarchy = effectivePresentationHierarchy,
                        rawBackedProjectIds = rawBackedProjectIds,
                        canonicalWorkspacesById = canonicalWorkspacesById,
                        additionalChildrenByParentId = additionalChildrenByParentId,
                        beaconIdsByContextId = beaconIdsByContextId,
                        result = result,
                        visited = linkedSetOf(),
                        skipDirectBeaconLinkedContexts = true,
                        isLinkedAppearance = false,
                    )
                }
            }

            return result.map { item ->
                val contextNode = item.node as? OrientationHierarchyNode.ContextNode
                    ?: return@map item

                val canonicalWorkspace = canonicalWorkspacesById[contextNode.id]
                if (canonicalWorkspace != null) {
                    return@map item.copy(
                        node =
                            OrientationHierarchyNode.WorkspaceNode(
                                presentation =
                                    effectivePresentationHierarchy.allProjects
                                        .firstOrNull { it.id == contextNode.id }
                                        ?: contextNode.presentation,
                                linkedBeaconIds = contextNode.linkedBeaconIds,
                                isLinkedAppearance = contextNode.isLinkedAppearance,
                            ),
                    )
                }

                item.copy(
                    node =
                        contextNode.copy(
                            presentation =
                                basePresentationsById[contextNode.id]
                                    ?: contextNode.presentation,
                        ),
                )
            }
        }

        private fun buildOperationalPlacementHierarchy(
            presentations: List<HierarchyContextPresentationNode>,
            workspaces: Collection<WorkspaceEntity>,
        ): OperationalPlacementHierarchy {
            val workspacesById = workspaces.associateBy { it.id }
            val operationalPlacementPresentations =
                presentations.map { presentation ->
                    val workspace = workspacesById[presentation.id]
                    if (workspace == null) {
                        presentation
                    } else {
                        presentation.copy(
                            parentId = workspace.parentWorkspaceId,
                            order = workspace.workspaceOrder,
                        )
                    }
                }

            return operationalPlacementPresentations.toOperationalPlacementHierarchy()
        }

        private fun List<HierarchyContextPresentationNode>.toOperationalPlacementHierarchy():
            OperationalPlacementHierarchy {
            val presentationsById = associateBy { it.id }
            val topLevelProjects =
                filter { presentation ->
                    presentation.parentId == null ||
                        presentation.parentId !in presentationsById
                }.sortedWith(presentationSort())
            val childMap =
                mapNotNull { presentation ->
                    presentation.parentId
                        ?.takeIf { it in presentationsById }
                        ?.let { parentId -> parentId to presentation }
                }.groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second },
                )

            return OperationalPlacementHierarchy(
                allProjects = this,
                topLevelProjects = topLevelProjects,
                childMap = childMap,
            )
        }

        private fun appendBeaconSubtree(
            details: OrientationBeaconInput,
            level: Int,
            childBeaconsByParentId: Map<String?, List<OrientationBeaconInput>>,
            presentationsById: Map<String, HierarchyContextPresentationNode>,
            additionalChildrenByParentId: Map<String, List<HierarchyContextPresentationNode>>,
            additionalParentsByChildId: Map<String, List<String>>,
            beaconIdsByContextId: Map<String, Set<String>>,
            hierarchy: OperationalPlacementHierarchy,
            result: MutableList<OrientationHierarchyItem>,
            visitedBeaconIds: LinkedHashSet<String>,
        ) {
            if (!visitedBeaconIds.add(details.id)) return
            result +=
                OrientationHierarchyItem(
                    node =
                        OrientationHierarchyNode.Beacon(
                            id = details.id,
                            title = details.title,
                            readinessStatus = details.readinessStatus,
                            relatedContextCount = details.relatedOwnerIds.size,
                        ),
                    level = level,
                )

            childBeaconsByParentId[details.id].orEmpty()
                .forEach { child ->
                    appendBeaconSubtree(
                        details = child,
                        level = level + 1,
                        childBeaconsByParentId = childBeaconsByParentId,
                        presentationsById = presentationsById,
                        additionalChildrenByParentId = additionalChildrenByParentId,
                        additionalParentsByChildId = additionalParentsByChildId,
                        beaconIdsByContextId = beaconIdsByContextId,
                        hierarchy = hierarchy,
                        result = result,
                        visitedBeaconIds = LinkedHashSet(visitedBeaconIds),
                    )
                }

            val linkedIdsForBeacon = details.relatedOwnerIds.toCollection(linkedSetOf())
            val entryPoints =
                linkedIdsForBeacon
                    .asSequence()
                    .mapNotNull(presentationsById::get)
                    .filterNot { context ->
                        hasAncestorInSet(
                            context = context,
                            ancestorCandidates = linkedIdsForBeacon,
                            presentationsById = presentationsById,
                            additionalParentsByChildId = additionalParentsByChildId,
                        )
                    }
                    .toList()

            entryPoints.forEach { context ->
                appendContextSubtree(
                    context = context,
                    level = level + 1,
                    hierarchy = hierarchy,
                    additionalChildrenByParentId = additionalChildrenByParentId,
                    beaconIdsByContextId = beaconIdsByContextId,
                    result = result,
                    visited = linkedSetOf(),
                    skipDirectBeaconLinkedContexts = false,
                    isLinkedAppearance = false,
                )
            }
        }

        private fun buildBeaconIdsByContextId(beacons: List<OrientationBeaconInput>): Map<String, Set<String>> {
            val mutable = linkedMapOf<String, MutableSet<String>>()
            beacons.forEach { details ->
                details.relatedOwnerIds.forEach { ownerId ->
                    mutable.getOrPut(ownerId) { linkedSetOf() } += details.id
                }
            }
            return mutable.mapValues { (_, ids) -> ids.toSet() }
        }

        private data class GroupedBeacon(
            val details: OrientationBeaconInput,
            val order: Long,
        )

        private fun buildChildBeaconsByParentId(
            beacons: List<OrientationBeaconInput>,
            parentLinks: List<MainBeaconParentLink>,
        ): Map<String?, List<OrientationBeaconInput>> {
            val byId = beacons.associateBy { it.id }
            val childrenByParentId = linkedMapOf<String?, MutableList<OrientationBeaconInput>>()
            beacons.forEach { beacon ->
                childrenByParentId.getOrPut(beacon.parentBeaconId) { mutableListOf() } += beacon
            }
            parentLinks
                .asSequence()
                .filter { it.parentBeaconId != it.childBeaconId }
                .sortedWith(compareBy<MainBeaconParentLink> { it.parentBeaconId }.thenBy { it.order })
                .forEach { link ->
                    val child = byId[link.childBeaconId] ?: return@forEach
                    val parentChildren = childrenByParentId.getOrPut(link.parentBeaconId) { mutableListOf() }
                    if (parentChildren.none { it.id == child.id }) {
                        parentChildren += child
                    }
                }
            return childrenByParentId
        }

        private fun buildLinkedParentBeaconIdsByChildId(
            parentLinks: List<MainBeaconParentLink>,
        ): Map<String, Set<String>> =
            parentLinks
                .asSequence()
                .filter { it.parentBeaconId != it.childBeaconId }
                .groupBy(
                    keySelector = { it.childBeaconId },
                    valueTransform = { it.parentBeaconId },
                )
                .mapValues { (_, parentIds) -> parentIds.toSet() }

        private fun appendContextSubtree(
            context: HierarchyContextPresentationNode,
            level: Int,
            hierarchy: OperationalPlacementHierarchy,
            additionalChildrenByParentId: Map<String, List<HierarchyContextPresentationNode>>,
            beaconIdsByContextId: Map<String, Set<String>>,
            result: MutableList<OrientationHierarchyItem>,
            visited: LinkedHashSet<String>,
            skipDirectBeaconLinkedContexts: Boolean,
            isLinkedAppearance: Boolean,
        ) {
            if (!visited.add(context.id)) return
            if (skipDirectBeaconLinkedContexts && beaconIdsByContextId.containsKey(context.id)) return

            result +=
                OrientationHierarchyItem(
                    node =
                        OrientationHierarchyNode.ContextNode(
                            presentation = context,
                            linkedBeaconIds = beaconIdsByContextId[context.id].orEmpty(),
                            isLinkedAppearance = isLinkedAppearance,
                        ),
                    level = level,
                )

            val canonicalChildren = hierarchy.childMap[context.id].orEmpty()
            val canonicalChildIds = canonicalChildren.mapTo(hashSetOf()) { it.id }
            val additionalChildren =
                additionalChildrenByParentId[context.id]
                    .orEmpty()
                    .filterNot { it.id in canonicalChildIds }

            canonicalChildren.sortedWith(presentationSort())
                .forEach { child ->
                    appendContextSubtree(
                        context = child,
                        level = level + 1,
                        hierarchy = hierarchy,
                        additionalChildrenByParentId = additionalChildrenByParentId,
                        beaconIdsByContextId = beaconIdsByContextId,
                        result = result,
                        visited = LinkedHashSet(visited),
                        skipDirectBeaconLinkedContexts = skipDirectBeaconLinkedContexts,
                        isLinkedAppearance = false,
                    )
                }

            additionalChildren.forEach { child ->
                appendContextSubtree(
                    context = child,
                    level = level + 1,
                    hierarchy = hierarchy,
                    additionalChildrenByParentId = additionalChildrenByParentId,
                    beaconIdsByContextId = beaconIdsByContextId,
                    result = result,
                    visited = LinkedHashSet(visited),
                    skipDirectBeaconLinkedContexts = skipDirectBeaconLinkedContexts,
                    isLinkedAppearance = true,
                )
            }
        }

        private fun appendPresentationSubtree(
            presentation: HierarchyContextPresentationNode,
            level: Int,
            hierarchy: HierarchyPresentationData,
            rawBackedProjectIds: Set<String>,
            canonicalWorkspacesById: Map<String, WorkspaceEntity>,
            additionalChildrenByParentId: Map<String, List<HierarchyContextPresentationNode>>,
            beaconIdsByContextId: Map<String, Set<String>>,
            result: MutableList<OrientationHierarchyItem>,
            visited: LinkedHashSet<String>,
            skipDirectBeaconLinkedContexts: Boolean,
            isLinkedAppearance: Boolean,
        ) {
            if (!visited.add(presentation.id)) return
            if (skipDirectBeaconLinkedContexts && beaconIdsByContextId.containsKey(presentation.id)) return

            val hasRawBacking = presentation.id in rawBackedProjectIds
            val canonicalWorkspace = canonicalWorkspacesById[presentation.id]
            val isStandaloneWorkspace =
                canonicalWorkspace != null &&
                    !canonicalWorkspace.isDeleted &&
                    canonicalWorkspace.provenance == WorkspaceProvenance.STANDALONE.name &&
                    canonicalWorkspace.sourceContextId == null &&
                    !SystemContexts.isSystem(ContextId(presentation.id))
            val isCanonicalSystemWorkspace =
                canonicalWorkspace != null &&
                    SystemContexts.isSystem(ContextId(presentation.id))

            val node =
                if (
                    canonicalWorkspace != null &&
                        (hasRawBacking || isCanonicalSystemWorkspace || isStandaloneWorkspace)
                ) {
                    OrientationHierarchyNode.WorkspaceNode(
                        presentation = presentation,
                        linkedBeaconIds = beaconIdsByContextId[presentation.id].orEmpty(),
                        isLinkedAppearance = isLinkedAppearance,
                    )
                } else {
                    if (!hasRawBacking) return
                    OrientationHierarchyNode.ContextNode(
                        presentation = presentation,
                        linkedBeaconIds = beaconIdsByContextId[presentation.id].orEmpty(),
                        isLinkedAppearance = isLinkedAppearance,
                    )
                }
            result += OrientationHierarchyItem(node = node, level = level)

            val canonicalChildren = hierarchy.childMap[presentation.id].orEmpty()
            val canonicalChildIds = canonicalChildren.mapTo(hashSetOf()) { it.id }
            val additionalChildren =
                additionalChildrenByParentId[presentation.id]
                    .orEmpty()
                    .mapNotNull { child -> hierarchy.allProjects.firstOrNull { it.id == child.id } }
                    .filterNot { it.id in canonicalChildIds }

            canonicalChildren.sortedWith(presentationSort()).forEach { child ->
                appendPresentationSubtree(
                    presentation = child,
                    level = level + 1,
                    hierarchy = hierarchy,
                    rawBackedProjectIds = rawBackedProjectIds,
                    canonicalWorkspacesById = canonicalWorkspacesById,
                    additionalChildrenByParentId = additionalChildrenByParentId,
                    beaconIdsByContextId = beaconIdsByContextId,
                    result = result,
                    visited = LinkedHashSet(visited),
                    skipDirectBeaconLinkedContexts = skipDirectBeaconLinkedContexts,
                    isLinkedAppearance = false,
                )
            }
            additionalChildren.forEach { child ->
                appendPresentationSubtree(
                    presentation = child,
                    level = level + 1,
                    hierarchy = hierarchy,
                    rawBackedProjectIds = rawBackedProjectIds,
                    canonicalWorkspacesById = canonicalWorkspacesById,
                    additionalChildrenByParentId = additionalChildrenByParentId,
                    beaconIdsByContextId = beaconIdsByContextId,
                    result = result,
                    visited = LinkedHashSet(visited),
                    skipDirectBeaconLinkedContexts = skipDirectBeaconLinkedContexts,
                    isLinkedAppearance = true,
                )
            }
        }

        private fun hasAncestorInSet(
            context: HierarchyContextPresentationNode,
            ancestorCandidates: Set<String>,
            presentationsById: Map<String, HierarchyContextPresentationNode>,
            additionalParentsByChildId: Map<String, List<String>>,
        ): Boolean {
            val visited = mutableSetOf<String>()
            val pending = ArrayDeque<String>()
            context.parentId?.let(pending::add)
            additionalParentsByChildId[context.id].orEmpty().forEach(pending::add)

            while (pending.isNotEmpty()) {
                val parentId = pending.removeFirst()
                if (!visited.add(parentId)) continue
                if (parentId in ancestorCandidates) return true
                val parent = presentationsById[parentId] ?: continue
                parent.parentId?.let(pending::add)
                additionalParentsByChildId[parent.id].orEmpty().forEach(pending::add)
            }
            return false
        }

        private fun buildAdditionalChildrenByParentId(
            parentLinks: List<ContextParentLink>,
            presentationsById: Map<String, HierarchyContextPresentationNode>,
        ): Map<String, List<HierarchyContextPresentationNode>> =
            parentLinks
                .asSequence()
                .filterNot { it.isDeleted }
                .filter { it.parentContextId != it.childContextId }
                .filter { it.parentContextId in presentationsById }
                .sortedWith(compareBy<ContextParentLink> { it.parentContextId }.thenBy { it.order })
                .mapNotNull { link -> presentationsById[link.childContextId]?.let { link to it } }
                .groupBy(
                    keySelector = { (link, _) -> link.parentContextId },
                    valueTransform = { (_, child) -> child },
                )

        private fun buildAdditionalParentsByChildId(
            parentLinks: List<ContextParentLink>,
            presentationsById: Map<String, HierarchyContextPresentationNode>,
        ): Map<String, List<String>> =
            parentLinks
                .asSequence()
                .filterNot { it.isDeleted }
                .filter { it.parentContextId != it.childContextId }
                .filter { it.parentContextId in presentationsById && it.childContextId in presentationsById }
                .groupBy(
                    keySelector = { it.childContextId },
                    valueTransform = { it.parentContextId },
                )

        private fun presentationSort(): Comparator<HierarchyContextPresentationNode> =
            compareBy<HierarchyContextPresentationNode> { it.order }
                .thenBy { it.name.lowercase() }
    }
