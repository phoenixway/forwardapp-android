package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.OrientationBeaconInput
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.OrientationHierarchyBuilder
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildOrientationBreadcrumbs
import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationHierarchyBuilderTest {
    private val builder = OrientationHierarchyBuilder()

    @Test
    fun buildsBeaconRootsAndNoBeaconFallback() {
        val beaconRoot = context(id = "beacon-root", order = 0)
        val beaconChild = context(id = "beacon-child", parentId = "beacon-root", order = 0)
        val unassignedRoot = context(id = "unassigned-root", order = 1)
        val unassignedChild = context(id = "unassigned-child", parentId = "unassigned-root", order = 0)

        val hierarchy =
            ContextHierarchyData(
                allProjects = listOf(beaconRoot, beaconChild, unassignedRoot, unassignedChild),
                topLevelProjects = listOf(beaconRoot, unassignedRoot),
                childMap =
                    mapOf(
                        "beacon-root" to listOf(beaconChild),
                        "unassigned-root" to listOf(unassignedChild),
                    ),
            )
        val beacon =
            beacon(
                id = "beacon-1",
                title = "Health",
                order = 0,
                relatedContexts = listOf(beaconRoot),
            )

        val items = builder.build(hierarchy = hierarchy, beacons = listOf(beacon))

        assertEquals(
            listOf(
                "virtual:no-group",
                "beacon-1",
                "beacon-root",
                "beacon-child",
                "virtual:no-beacon",
                "unassigned-root",
                "unassigned-child",
            ),
            items.map { it.node.id },
        )
        assertEquals(listOf(0, 1, 2, 3, 0, 1, 2), items.map { it.level })
    }

    @Test
    fun skipsBeaconLinkedContextsUnderNoBeaconToAvoidDuplicatePrimaryRows() {
        val unassignedRoot = context(id = "unassigned-root", order = 0)
        val linkedChild = context(id = "linked-child", parentId = "unassigned-root", order = 0)
        val unlinkedChild = context(id = "unlinked-child", parentId = "unassigned-root", order = 1)
        val hierarchy =
            ContextHierarchyData(
                allProjects = listOf(unassignedRoot, linkedChild, unlinkedChild),
                topLevelProjects = listOf(unassignedRoot),
                childMap = mapOf("unassigned-root" to listOf(linkedChild, unlinkedChild)),
            )
        val beacon =
            beacon(
                id = "beacon-1",
                title = "Health",
                order = 0,
                relatedContexts = listOf(linkedChild),
            )

        val items = builder.build(hierarchy = hierarchy, beacons = listOf(beacon))

        assertEquals(
            listOf(
                "virtual:no-group",
                "beacon-1",
                "linked-child",
                "virtual:no-beacon",
                "unassigned-root",
                "unlinked-child",
            ),
            items.map { it.node.id },
        )
        assertEquals(
            setOf("beacon-1"),
            (items[2].node as OrientationHierarchyNode.ContextNode).linkedBeaconIds,
        )
    }

    @Test
    fun includesContextUnderAdditionalParentLink() {
        val firstRoot = context(id = "first-root", order = 0)
        val secondRoot = context(id = "second-root", order = 1)
        val sharedChild = context(id = "shared-child", parentId = "first-root", order = 0)
        val hierarchy =
            ContextHierarchyData(
                allProjects = listOf(firstRoot, secondRoot, sharedChild),
                topLevelProjects = listOf(firstRoot, secondRoot),
                childMap = mapOf("first-root" to listOf(sharedChild)),
            )

        val items =
            builder.build(
                hierarchy = hierarchy,
                beacons = emptyList(),
                parentLinks =
                    listOf(
                        ContextParentLink(
                            parentContextId = "second-root",
                            childContextId = "shared-child",
                            createdAt = 0L,
                        ),
                    ),
            )

        assertEquals(
            listOf(
                "virtual:no-beacon",
                "first-root",
                "shared-child",
                "second-root",
                "shared-child",
            ),
            items.map { it.node.id },
        )
        assertEquals(listOf(0, 1, 2, 1, 2), items.map { it.level })
    }

    @Test
    fun buildsBreadcrumbsThroughGroupBeaconAndDeepContext() {
        val context1 = context(id = "context-1", order = 0)
        val context2 = context(id = "context-2", parentId = "context-1", order = 0)
        val context3 = context(id = "context-3", parentId = "context-2", order = 0)
        val hierarchy =
            ContextHierarchyData(
                allProjects = listOf(context1, context2, context3),
                topLevelProjects = listOf(context1),
                childMap =
                    mapOf(
                        "context-1" to listOf(context2),
                        "context-2" to listOf(context3),
                    ),
            )

        val items =
            builder.build(
                hierarchy = hierarchy,
                beacons =
                    listOf(
                        beacon(
                            id = "beacon-1",
                            title = "Health",
                            order = 0,
                            relatedContexts = listOf(context1),
                            groupIds = listOf("group-1"),
                        ),
                    ),
                groups = listOf(MainBeaconGroup(id = "group-1", title = "Core", order = 0)),
            )

        val breadcrumbs = buildOrientationBreadcrumbs(items = items, nodeId = "context-3")

        assertEquals(
            listOf("group-1", "beacon-1", "context-1", "context-2", "context-3"),
            breadcrumbs.map { it.id },
        )
        assertEquals(listOf(0, 1, 2, 3, 4), breadcrumbs.map { it.level })
    }

    private fun context(
        id: String,
        parentId: String? = null,
        order: Long = 0,
    ): Context =
        Context(
            id = id,
            name = id,
            description = null,
            parentId = parentId,
            createdAt = 0L,
            updatedAt = 0L,
            order = order,
        )

    private fun beacon(
        id: String,
        title: String,
        order: Long,
        relatedContexts: List<Context>,
        groupIds: List<String> = emptyList(),
    ): OrientationBeaconInput =
        OrientationBeaconInput(
            id = id,
            title = title,
            order = order,
            readinessStatus = MainBeaconReadinessStatus.READY,
            relatedContexts = relatedContexts,
            groupIds = groupIds,
        )
}
