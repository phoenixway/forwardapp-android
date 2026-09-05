package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import javax.inject.Inject

data class OrientationBeaconInput(
    val id: String,
    val title: String,
    val order: Long,
    val readinessStatus: MainBeaconReadinessStatus,
    val parentBeaconId: String?,
    val relatedContexts: List<Context>,
    val groupIds: List<String>,
    val groupOrders: Map<String, Long> = emptyMap(),
)

class OrientationHierarchyBuilder
    @Inject
    constructor() {
        fun build(
            hierarchy: ContextHierarchyData,
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
                    it.provenance == WorkspaceProvenance.CANONICAL_ONLY.name
                }
            val liveContextsById = hierarchy.allProjects.associateBy { it.id }
            val displayHierarchy =
                buildOperationalHierarchyProjection(
                    hierarchy = hierarchy,
                    workspaces = liveWorkspacesById.values,
                )

            if (displayHierarchy.allProjects.isEmpty() && beacons.isEmpty()) return emptyList()

            val contextsById = displayHierarchy.allProjects.associateBy { it.id }
            val additionalChildrenByParentId = buildAdditionalChildrenByParentId(parentLinks, contextsById)
            val additionalParentsByChildId = buildAdditionalParentsByChildId(parentLinks, contextsById)
            val beaconIdsByContextId = buildBeaconIdsByContextId(beacons)
            val linkedContextIds = beaconIdsByContextId.keys
            val result = mutableListOf<OrientationHierarchyItem>()
            val sortedBeacons =
                beacons.sortedWith(
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
                            contextsById = contextsById,
                            additionalChildrenByParentId = additionalChildrenByParentId,
                            additionalParentsByChildId = additionalParentsByChildId,
                            beaconIdsByContextId = beaconIdsByContextId,
                            hierarchy = displayHierarchy,
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
                        contextsById = contextsById,
                        additionalChildrenByParentId = additionalChildrenByParentId,
                        additionalParentsByChildId = additionalParentsByChildId,
                        beaconIdsByContextId = beaconIdsByContextId,
                        hierarchy = displayHierarchy,
                        result = result,
                        visitedBeaconIds = linkedSetOf(),
                    )
                }
            }

            val noBeaconRoots =
                displayHierarchy.topLevelProjects
                    .filter { it.id !in linkedContextIds }
                    .sortedWith(contextSort())

            if (noBeaconRoots.isNotEmpty()) {
                result +=
                    OrientationHierarchyItem(
                        node = OrientationHierarchyNode.NoBeacon,
                        level = 0,
                    )
                noBeaconRoots.forEach { context ->
                    appendContextSubtree(
                        context = context,
                        level = 1,
                        hierarchy = displayHierarchy,
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

                val canonicalWorkspace = canonicalWorkspacesById[contextNode.context.id]
                if (canonicalWorkspace != null) {
                    return@map item.copy(
                        node =
                            OrientationHierarchyNode.WorkspaceNode(
                                workspace = canonicalWorkspace,
                                linkedBeaconIds = contextNode.linkedBeaconIds,
                                isLinkedAppearance = contextNode.isLinkedAppearance,
                            ),
                    )
                }

                val originalContext = liveContextsById[contextNode.context.id]
                    ?: return@map item
                item.copy(
                    node =
                        contextNode.copy(
                            context = originalContext,
                        ),
                )
            }
        }

        private fun buildOperationalHierarchyProjection(
            hierarchy: ContextHierarchyData,
            workspaces: Collection<WorkspaceEntity>,
        ): ContextHierarchyData {
            if (workspaces.isEmpty()) return hierarchy

            val workspacesById = workspaces.associateBy { it.id }
            val liveContextsById = hierarchy.allProjects.associateBy { it.id }

            val projectedContexts =
                hierarchy.allProjects.map { context ->
                    val workspace = workspacesById[context.id]
                    if (workspace == null) {
                        context
                    } else {
                        context.copy(
                            parentId = workspace.parentWorkspaceId,
                            order = workspace.workspaceOrder,
                        )
                    }
                }

            val canonicalWorkspaceContexts =
                workspaces
                    .asSequence()
                    .filter { it.provenance == WorkspaceProvenance.CANONICAL_ONLY.name }
                    .filterNot { it.id in liveContextsById }
                    .map { workspace ->
                        Context(
                            id = workspace.id,
                            name =
                                workspace.nameOverride
                                    ?.trim()
                                    ?.takeIf { it.isNotEmpty() }
                                    ?: workspace.id,
                            description = workspace.descriptionOverride,
                            parentId = workspace.parentWorkspaceId,
                            createdAt = workspace.createdAt,
                            updatedAt = workspace.updatedAt,
                            order = workspace.workspaceOrder,
                        )
                    }
                    .toList()

            val allProjects = projectedContexts + canonicalWorkspaceContexts
            val projectsById = allProjects.associateBy { it.id }
            val topLevelProjects =
                allProjects
                    .filter { project ->
                        project.parentId == null || project.parentId !in projectsById
                    }
                    .sortedWith(contextSort())
            val childMap =
                allProjects
                    .mapNotNull { project ->
                        project.parentId
                            ?.takeIf { it in projectsById }
                            ?.let { parentId -> parentId to project }
                    }
                    .groupBy(
                        keySelector = { it.first },
                        valueTransform = { it.second },
                    )

            return ContextHierarchyData(
                allProjects = allProjects,
                topLevelProjects = topLevelProjects,
                childMap = childMap,
            )
        }

        private fun appendBeaconSubtree(
            details: OrientationBeaconInput,
            level: Int,
            childBeaconsByParentId: Map<String?, List<OrientationBeaconInput>>,
            contextsById: Map<String, Context>,
            additionalChildrenByParentId: Map<String, List<Context>>,
            additionalParentsByChildId: Map<String, List<String>>,
            beaconIdsByContextId: Map<String, Set<String>>,
            hierarchy: ContextHierarchyData,
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
                            relatedContextCount = details.relatedContexts.size,
                        ),
                    level = level,
                )

            childBeaconsByParentId[details.id].orEmpty()
                .forEach { child ->
                    appendBeaconSubtree(
                        details = child,
                        level = level + 1,
                        childBeaconsByParentId = childBeaconsByParentId,
                        contextsById = contextsById,
                        additionalChildrenByParentId = additionalChildrenByParentId,
                        additionalParentsByChildId = additionalParentsByChildId,
                        beaconIdsByContextId = beaconIdsByContextId,
                        hierarchy = hierarchy,
                        result = result,
                        visitedBeaconIds = LinkedHashSet(visitedBeaconIds),
                    )
                }

            val linkedIdsForBeacon = details.relatedContexts.mapTo(linkedSetOf()) { it.id }
            val entryPoints =
                linkedIdsForBeacon
                    .asSequence()
                    .mapNotNull(contextsById::get)
                    .filterNot { context ->
                        hasAncestorInSet(
                            context = context,
                            ancestorCandidates = linkedIdsForBeacon,
                            contextsById = contextsById,
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
                details.relatedContexts.forEach { context ->
                    mutable.getOrPut(context.id) { linkedSetOf() } += details.id
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
            context: Context,
            level: Int,
            hierarchy: ContextHierarchyData,
            additionalChildrenByParentId: Map<String, List<Context>>,
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
                            context = context,
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

            canonicalChildren.sortedWith(contextSort())
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

        private fun hasAncestorInSet(
            context: Context,
            ancestorCandidates: Set<String>,
            contextsById: Map<String, Context>,
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
                val parent = contextsById[parentId] ?: continue
                parent.parentId?.let(pending::add)
                additionalParentsByChildId[parent.id].orEmpty().forEach(pending::add)
            }
            return false
        }

        private fun buildAdditionalChildrenByParentId(
            parentLinks: List<ContextParentLink>,
            contextsById: Map<String, Context>,
        ): Map<String, List<Context>> =
            parentLinks
                .asSequence()
                .filterNot { it.isDeleted }
                .filter { it.parentContextId != it.childContextId }
                .filter { it.parentContextId in contextsById }
                .sortedWith(compareBy<ContextParentLink> { it.parentContextId }.thenBy { it.order })
                .mapNotNull { link -> contextsById[link.childContextId]?.let { link to it } }
                .groupBy(
                    keySelector = { (link, _) -> link.parentContextId },
                    valueTransform = { (_, child) -> child },
                )

        private fun buildAdditionalParentsByChildId(
            parentLinks: List<ContextParentLink>,
            contextsById: Map<String, Context>,
        ): Map<String, List<String>> =
            parentLinks
                .asSequence()
                .filterNot { it.isDeleted }
                .filter { it.parentContextId != it.childContextId }
                .filter { it.parentContextId in contextsById && it.childContextId in contextsById }
                .groupBy(
                    keySelector = { it.childContextId },
                    valueTransform = { it.parentContextId },
                )

        private fun contextSort(): Comparator<Context> =
            compareBy<Context> { it.order }
                .thenBy { it.name.lowercase() }
    }
