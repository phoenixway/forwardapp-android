package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BeaconRootedHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BeaconRootedHierarchyNode
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconWithRelations
import javax.inject.Inject

class BeaconRootedHierarchyBuilder
    @Inject
    constructor() {
        fun build(
            hierarchy: ContextHierarchyData,
            beacons: List<MainBeaconWithRelations>,
            groups: List<MainBeaconGroup> = emptyList(),
            parentLinks: List<ContextParentLink> = emptyList(),
        ): List<BeaconRootedHierarchyItem> {
            if (hierarchy.allProjects.isEmpty() && beacons.isEmpty()) return emptyList()

            val contextsById = hierarchy.allProjects.associateBy { it.id }
            val additionalChildrenByParentId = buildAdditionalChildrenByParentId(parentLinks, contextsById)
            val additionalParentsByChildId = buildAdditionalParentsByChildId(parentLinks, contextsById)
            val beaconIdsByContextId = buildBeaconIdsByContextId(beacons)
            val linkedContextIds = beaconIdsByContextId.keys
            val result = mutableListOf<BeaconRootedHierarchyItem>()
            val sortedBeacons =
                beacons.sortedWith(
                    compareBy<MainBeaconWithRelations> { it.beacon.order }.thenBy { it.beacon.title.lowercase() },
                )
            val knownGroupIds = groups.mapTo(hashSetOf()) { it.id }
            val beaconsByGroupId = sortedBeacons.mapNotNull { details ->
                details.groupIds.firstOrNull { it in knownGroupIds }?.let { groupId -> groupId to details }
            }
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })

            groups
                .sortedWith(compareBy<MainBeaconGroup> { it.order }.thenBy { it.title.lowercase() })
                .forEach { group ->
                    val groupBeacons = beaconsByGroupId[group.id].orEmpty()
                    result +=
                        BeaconRootedHierarchyItem(
                            node =
                                BeaconRootedHierarchyNode.Group(
                                    id = group.id,
                                    title = group.title,
                                    beaconCount = groupBeacons.size,
                                ),
                            level = 0,
                        )
                    groupBeacons.forEach { details ->
                        appendBeaconSubtree(
                            details = details,
                            level = 1,
                            contextsById = contextsById,
                            additionalChildrenByParentId = additionalChildrenByParentId,
                            additionalParentsByChildId = additionalParentsByChildId,
                            beaconIdsByContextId = beaconIdsByContextId,
                            hierarchy = hierarchy,
                            result = result,
                        )
                    }
                }

            val noGroupBeacons = sortedBeacons.filter { details -> details.groupIds.none { it in knownGroupIds } }
            if (noGroupBeacons.isNotEmpty()) {
                result +=
                    BeaconRootedHierarchyItem(
                        node = BeaconRootedHierarchyNode.NoGroup,
                        level = 0,
                    )
                noGroupBeacons.forEach { details ->
                    appendBeaconSubtree(
                        details = details,
                        level = 1,
                        contextsById = contextsById,
                        additionalChildrenByParentId = additionalChildrenByParentId,
                        additionalParentsByChildId = additionalParentsByChildId,
                        beaconIdsByContextId = beaconIdsByContextId,
                        hierarchy = hierarchy,
                        result = result,
                    )
                }
            }

            val noBeaconRoots =
                hierarchy.topLevelProjects
                    .filter { it.id !in linkedContextIds }
                    .sortedWith(contextSort())

            if (noBeaconRoots.isNotEmpty()) {
                result +=
                    BeaconRootedHierarchyItem(
                        node = BeaconRootedHierarchyNode.NoBeacon,
                        level = 0,
                    )
                noBeaconRoots.forEach { context ->
                    appendContextSubtree(
                        context = context,
                        level = 1,
                        hierarchy = hierarchy,
                        additionalChildrenByParentId = additionalChildrenByParentId,
                        beaconIdsByContextId = beaconIdsByContextId,
                        result = result,
                        visited = linkedSetOf(),
                        skipDirectBeaconLinkedContexts = true,
                    )
                }
            }

            return result
        }

        private fun appendBeaconSubtree(
            details: MainBeaconWithRelations,
            level: Int,
            contextsById: Map<String, Context>,
            additionalChildrenByParentId: Map<String, List<Context>>,
            additionalParentsByChildId: Map<String, List<String>>,
            beaconIdsByContextId: Map<String, Set<String>>,
            hierarchy: ContextHierarchyData,
            result: MutableList<BeaconRootedHierarchyItem>,
        ) {
            result +=
                BeaconRootedHierarchyItem(
                    node =
                        BeaconRootedHierarchyNode.Beacon(
                            id = details.beacon.id,
                            title = details.beacon.title,
                            readinessStatus = details.beacon.readinessStatus,
                            relatedContextCount = details.relatedContexts.size,
                        ),
                    level = level,
                )

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
                    .sortedWith(contextSort())
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
                )
            }
        }

        private fun buildBeaconIdsByContextId(beacons: List<MainBeaconWithRelations>): Map<String, Set<String>> {
            val mutable = linkedMapOf<String, MutableSet<String>>()
            beacons.forEach { details ->
                details.relatedContexts.forEach { context ->
                    mutable.getOrPut(context.id) { linkedSetOf() } += details.beacon.id
                }
            }
            return mutable.mapValues { (_, ids) -> ids.toSet() }
        }

        private fun appendContextSubtree(
            context: Context,
            level: Int,
            hierarchy: ContextHierarchyData,
            additionalChildrenByParentId: Map<String, List<Context>>,
            beaconIdsByContextId: Map<String, Set<String>>,
            result: MutableList<BeaconRootedHierarchyItem>,
            visited: LinkedHashSet<String>,
            skipDirectBeaconLinkedContexts: Boolean,
        ) {
            if (!visited.add(context.id)) return
            if (skipDirectBeaconLinkedContexts && beaconIdsByContextId.containsKey(context.id)) return

            result +=
                BeaconRootedHierarchyItem(
                    node =
                        BeaconRootedHierarchyNode.ContextNode(
                            context = context,
                            linkedBeaconIds = beaconIdsByContextId[context.id].orEmpty(),
                        ),
                    level = level,
                )

            val canonicalChildren = hierarchy.childMap[context.id].orEmpty()
            val canonicalChildIds = canonicalChildren.mapTo(hashSetOf()) { it.id }
            val additionalChildren =
                additionalChildrenByParentId[context.id]
                    .orEmpty()
                    .filterNot { it.id in canonicalChildIds }

            (canonicalChildren.sortedWith(contextSort()) + additionalChildren)
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
