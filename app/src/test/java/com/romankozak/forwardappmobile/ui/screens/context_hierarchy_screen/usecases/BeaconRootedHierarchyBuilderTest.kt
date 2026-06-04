package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.BeaconRootedHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.BeaconRootedHierarchyBuilder
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconWithRelations
import org.junit.Assert.assertEquals
import org.junit.Test

class BeaconRootedHierarchyBuilderTest {
    private val builder = BeaconRootedHierarchyBuilder()

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
            MainBeaconWithRelations(
                beacon = MainBeacon(id = "beacon-1", title = "Health", order = 0),
                relatedContexts = listOf(beaconRoot),
                relatedAttachments = emptyList(),
                levelStatuses = emptyList(),
                groupIds = emptyList(),
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
            MainBeaconWithRelations(
                beacon = MainBeacon(id = "beacon-1", title = "Health", order = 0),
                relatedContexts = listOf(linkedChild),
                relatedAttachments = emptyList(),
                levelStatuses = emptyList(),
                groupIds = emptyList(),
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
            (items[2].node as BeaconRootedHierarchyNode.ContextNode).linkedBeaconIds,
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
}
