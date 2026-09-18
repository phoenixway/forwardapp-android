package com.romankozak.forwardappmobile.ui.screens.mainscreen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextHierarchyData
import com.romankozak.forwardappmobile.core.data.models.entities.ContextParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconParentLink
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyPresentationData
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.toHierarchyPresentationNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyItem
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.OrientationBeaconInput
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.OrientationHierarchyBuilder
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.HierarchyPresentationTreeBuilder
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildOrientationBreadcrumbs
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases.buildOrientationBreadcrumbsToContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Test

class OrientationHierarchyBuilderTest {
    private val builder = OrientationHierarchyBuilder()

    private fun legacyFixtureBuild(
        rawContexts: List<Context>,
        beacons: List<OrientationBeaconInput>,
        groups: List<MainBeaconGroup> = emptyList(),
        parentLinks: List<ContextParentLink> = emptyList(),
        beaconParentLinks: List<MainBeaconParentLink> = emptyList(),
        workspaces: List<WorkspaceEntity> = emptyList(),
        retiredOrdinaryContextIds: Set<String> = emptySet(),
        presentationHierarchy: HierarchyPresentationData =
            HierarchyPresentationTreeBuilder().build(
                rawContexts.map(Context::toHierarchyPresentationNode),
            ),
    ): List<OrientationHierarchyItem> =
        builder.build(
            presentationHierarchy = presentationHierarchy,
            rawBackedProjectIds = rawContexts.mapTo(linkedSetOf()) { it.id },
            retiredOrdinaryContextIds = retiredOrdinaryContextIds,
            beacons = beacons,
            groups = groups,
            parentLinks = parentLinks,
            beaconParentLinks = beaconParentLinks,
            workspaces = workspaces,
        )

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

        val items = legacyFixtureBuild(rawContexts = hierarchy.allProjects, beacons = listOf(beacon))

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

        val items = legacyFixtureBuild(rawContexts = hierarchy.allProjects, beacons = listOf(beacon))

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
            legacyFixtureBuild(
                rawContexts = hierarchy.allProjects,
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
            legacyFixtureBuild(
                rawContexts = hierarchy.allProjects,
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


    @Test
    fun contextBackedWorkspaceOwnsPlacementWithoutReplacingPresentationPayload() {
        val legacyParent = context(id = "legacy-parent", order = 0)
        val workspaceParent = context(id = "workspace-parent", order = 1)
        val child = context(id = "child", parentId = "legacy-parent", order = 0)
        val hierarchy =
            ContextHierarchyData(
                allProjects = listOf(legacyParent, workspaceParent, child),
                topLevelProjects = listOf(legacyParent, workspaceParent),
                childMap = mapOf("legacy-parent" to listOf(child)),
            )
        val childWorkspace =
            WorkspaceEntity(
                id = "child",
                nameOverride = null,
                descriptionOverride = null,
                parentWorkspaceId = "workspace-parent",
                roleCode = null,
                workspaceOrder = 7L,
                createdAt = 0L,
                updatedAt = 0L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
                provenance = "CONTEXT_BACKED",
                sourceContextId = "child",
            )

        val items =
            legacyFixtureBuild(
                rawContexts = hierarchy.allProjects,
                beacons = emptyList(),
                workspaces = listOf(childWorkspace),
            )

        val childItem = items.single { it.node.id == "child" }
        val childNode = childItem.node as OrientationHierarchyNode.ContextNode

        val childIndex = items.indexOf(childItem)
        assertEquals("workspace-parent", items[childIndex - 1].node.id)
        assertEquals("legacy-parent", childNode.presentation.parentId)
        assertEquals(2, childItem.level)
    }

    @Test
    fun canonicalSystemDisplayUsesCanonicalPresentationWithoutEmbeddingRawContext() {
        val rawSystem =
            context(
                id = SystemContexts.INBOX.raw,
                parentId = "legacy-parent",
                order = 1,
            ).copy(name = "Legacy System name")
        val legacyParent = context(id = "legacy-parent", order = 0)
        val canonicalParent = context(id = "canonical-parent", order = 2)
        val workspace =
            canonicalWorkspace(
                id = rawSystem.id,
                parentId = canonicalParent.id,
                order = 7L,
            ).copy(nameOverride = "Canonical System name")
        val canonicalPresentation =
            HierarchyContextPresentationNode(
                id = rawSystem.id,
                name = "Canonical System name",
                description = null,
                parentId = canonicalParent.id,
                order = 7L,
                roleCode = null,
                tags = emptyList(),
            )
        val hierarchy =
            ContextHierarchyData(
                allProjects = listOf(legacyParent, canonicalParent, rawSystem),
                topLevelProjects = listOf(legacyParent, canonicalParent),
                childMap = mapOf(legacyParent.id to listOf(rawSystem)),
            )

        val items =
            legacyFixtureBuild(
                rawContexts = hierarchy.allProjects,
                beacons = emptyList(),
                workspaces = listOf(workspace),
                presentationHierarchy =
                    HierarchyPresentationData(
                        allProjects =
                            listOf(
                                legacyParent.toHierarchyPresentationNode(),
                                canonicalParent.toHierarchyPresentationNode(),
                                canonicalPresentation,
                            ),
                        topLevelProjects =
                            listOf(
                                legacyParent.toHierarchyPresentationNode(),
                                canonicalParent.toHierarchyPresentationNode(),
                            ),
                        childMap = mapOf(canonicalParent.id to listOf(canonicalPresentation)),
                    ),
            )

        val systemNode =
            items.single { it.node.id == rawSystem.id }.node as OrientationHierarchyNode.WorkspaceNode
        assertEquals("Canonical System name", systemNode.title)
        assertEquals(canonicalParent.id, systemNode.presentation.parentId)
    }

    @Test
    fun normalOrientationDisplayIncludesShellFreeReservedSystemUnderCanonicalParent() {
        val parent = context(id = "parent", order = 0)
        val ordinarySibling = context(id = "ordinary-sibling", parentId = parent.id, order = 1)
        val hierarchy =
            ContextHierarchyData(
                allProjects = listOf(parent, ordinarySibling),
                topLevelProjects = listOf(parent),
                childMap = mapOf(parent.id to listOf(ordinarySibling)),
            )
        val workspace =
            WorkspaceEntity(
                id = SystemContexts.INBOX.raw,
                nameOverride = "Migrated child",
                descriptionOverride = "Preserved description",
                parentWorkspaceId = "parent",
                roleCode = null,
                workspaceOrder = 3L,
                createdAt = 10L,
                updatedAt = 20L,
                syncedAt = null,
                isDeleted = false,
                version = 2L,
                provenance = "CANONICAL_ONLY",
                sourceContextId = null,
            )

        val shellFreeSystem =
            HierarchyContextPresentationNode(
                id = SystemContexts.INBOX.raw,
                name = "Migrated child",
                description = "Preserved description",
                parentId = parent.id,
                order = 3L,
                roleCode = null,
                tags = emptyList(),
            )
        val items =
            legacyFixtureBuild(
                rawContexts = hierarchy.allProjects,
                beacons = emptyList(),
                workspaces = listOf(workspace),
                presentationHierarchy =
                    HierarchyPresentationData(
                        allProjects =
                            listOf(
                                parent.toHierarchyPresentationNode(),
                                ordinarySibling.toHierarchyPresentationNode(),
                                shellFreeSystem,
                            ),
                        topLevelProjects = listOf(parent.toHierarchyPresentationNode()),
                        childMap =
                            mapOf(
                                parent.id to
                                    listOf(
                                        ordinarySibling.toHierarchyPresentationNode(),
                                        shellFreeSystem,
                                    ),
                            ),
                    ),
            )

        assertEquals(
            listOf("virtual:no-beacon", "parent", "ordinary-sibling", SystemContexts.INBOX.raw),
            items.map { it.node.id },
        )
        assertEquals(listOf(0, 1, 2, 2), items.map { it.level })
        val migratedNode = items.last().node as OrientationHierarchyNode.WorkspaceNode
        assertEquals("Migrated child", migratedNode.title)
        assertEquals("parent", migratedNode.presentation.parentId)
        // A CANONICAL_ONLY Workspace is still a ProjectLike hierarchy node.
        // Explicit reveal/navigation must therefore resolve the operational
        // parent path even though the legacy Context row no longer exists.
        val breadcrumbs =
            buildOrientationBreadcrumbsToContext(
                items = items,
                contextId = SystemContexts.INBOX.raw,
            )
        assertEquals(
            listOf("parent", SystemContexts.INBOX.raw),
            breadcrumbs.takeLast(2).map { it.id },
        )
    }

    @Test
    fun shellFreeSystemParentAndChildUseCanonicalPresentationOrderInsideNoBeacon() {
        val parent =
            HierarchyContextPresentationNode(
                id = SystemContexts.STRATEGIC.raw,
                name = "Canonical parent",
                description = null,
                parentId = null,
                order = 2L,
                roleCode = "parent-role",
                tags = listOf("parent-tag"),
            )
        val child =
            HierarchyContextPresentationNode(
                id = SystemContexts.INBOX.raw,
                name = "Canonical child",
                description = "Canonical child description",
                parentId = parent.id,
                order = 1L,
                roleCode = "child-role",
                tags = listOf("child-tag"),
            )
        val workspaces =
            listOf(
                canonicalWorkspace(id = parent.id, parentId = null, order = parent.order),
                canonicalWorkspace(id = child.id, parentId = parent.id, order = child.order),
            )

        val items =
            legacyFixtureBuild(
                rawContexts = emptyList(),
                beacons = emptyList(),
                workspaces = workspaces,
                presentationHierarchy =
                    HierarchyPresentationData(
                        allProjects = listOf(parent, child),
                        topLevelProjects = listOf(parent),
                        childMap = mapOf(parent.id to listOf(child)),
                    ),
            )

        assertEquals(
            listOf("virtual:no-beacon", parent.id, child.id),
            items.map { it.node.id },
        )
        assertEquals(listOf(0, 1, 2), items.map { it.level })
        val parentNode = items[1].node as OrientationHierarchyNode.WorkspaceNode
        val childNode = items[2].node as OrientationHierarchyNode.WorkspaceNode
        assertEquals("Canonical parent", parentNode.presentation.name)
        assertEquals("Canonical child", childNode.presentation.name)
        assertEquals(parent.id, childNode.presentation.parentId)
        assertEquals(listOf("child-tag"), childNode.presentation.tags)
    }

    @Test
    fun standaloneWorkspaceWithoutRawContextIsAdmittedButArbitraryCanonicalWorkspaceIsNot() {
        val standalonePresentation =
            HierarchyContextPresentationNode(
                id = "standalone-user-workspace",
                name = "Standalone",
                description = null,
                parentId = null,
                order = 0L,
                roleCode = null,
                tags = listOf("operations"),
            )
        val arbitraryCanonicalPresentation =
            HierarchyContextPresentationNode(
                id = "canonical-non-system",
                name = "Must stay hidden",
                description = null,
                parentId = null,
                order = 1L,
                roleCode = null,
                tags = emptyList(),
            )

        val standaloneWorkspace =
            canonicalWorkspace(
                id = standalonePresentation.id,
                parentId = null,
                order = standalonePresentation.order,
            ).copy(
                provenance = "STANDALONE",
                sourceContextId = null,
            )
        val arbitraryCanonicalWorkspace =
            canonicalWorkspace(
                id = arbitraryCanonicalPresentation.id,
                parentId = null,
                order = arbitraryCanonicalPresentation.order,
            )

        val items =
            legacyFixtureBuild(
                rawContexts = emptyList(),
                beacons = emptyList(),
                workspaces = listOf(
                    standaloneWorkspace,
                    arbitraryCanonicalWorkspace,
                ),
                presentationHierarchy =
                    HierarchyPresentationData(
                        allProjects =
                            listOf(
                                standalonePresentation,
                                arbitraryCanonicalPresentation,
                            ),
                        topLevelProjects =
                            listOf(
                                standalonePresentation,
                                arbitraryCanonicalPresentation,
                            ),
                        childMap = emptyMap(),
                    ),
            )

        assertEquals(
            listOf(
                "virtual:no-beacon",
                standalonePresentation.id,
            ),
            items.map { it.node.id },
        )

        val standaloneNode =
            items.single { it.node.id == standalonePresentation.id }.node
                as OrientationHierarchyNode.WorkspaceNode
        assertEquals("Standalone", standaloneNode.presentation.name)
        assertEquals(listOf("operations"), standaloneNode.presentation.tags)
    }

    @Test
    fun retiredOrdinaryCanonicalWorkspaceKeepsCanonicalPlacementAndBeaconAdmission() {
        val parent = context(id = "legacy-parent", order = 0)
        val retiredId = "retired-ordinary-owner"
        val retiredPresentation =
            HierarchyContextPresentationNode(
                id = retiredId,
                name = "Restored operational owner",
                description = "Canonical description",
                parentId = parent.id,
                order = 7L,
                roleCode = "operations",
                tags = listOf("restored"),
            )
        val arbitraryCanonicalPresentation =
            HierarchyContextPresentationNode(
                id = "arbitrary-canonical-owner",
                name = "Must stay hidden",
                description = null,
                parentId = null,
                order = 1L,
                roleCode = null,
                tags = emptyList(),
            )
        val retiredWorkspace = canonicalWorkspace(retiredId, parent.id, retiredPresentation.order)
        val arbitraryWorkspace = canonicalWorkspace(arbitraryCanonicalPresentation.id, null, 1L)

        val items =
            legacyFixtureBuild(
                rawContexts = listOf(parent),
                beacons =
                    listOf(
                        OrientationBeaconInput(
                            id = "beacon-1",
                            title = "Restored Beacon",
                            order = 0L,
                            readinessStatus = MainBeaconReadinessStatus.READY,
                            parentBeaconId = null,
                            relatedOwnerIds = listOf(retiredId, arbitraryCanonicalPresentation.id),
                            groupIds = emptyList(),
                        ),
                    ),
                workspaces = listOf(retiredWorkspace, arbitraryWorkspace),
                retiredOrdinaryContextIds = setOf(retiredId),
                presentationHierarchy =
                    HierarchyPresentationData(
                        allProjects =
                            listOf(
                                parent.toHierarchyPresentationNode(),
                                retiredPresentation,
                                arbitraryCanonicalPresentation,
                            ),
                        topLevelProjects = listOf(parent.toHierarchyPresentationNode(), arbitraryCanonicalPresentation),
                        childMap = mapOf(parent.id to listOf(retiredPresentation)),
                    ),
            )

        assertEquals(
            listOf("virtual:no-group", "beacon-1", retiredId, "virtual:no-beacon", parent.id),
            items.map { it.node.id },
        )
        val retiredNode = items.single { it.node.id == retiredId }.node as OrientationHierarchyNode.WorkspaceNode
        assertEquals(parent.id, retiredNode.presentation.parentId)
        assertEquals(7L, retiredNode.presentation.order)
        assertEquals("Restored operational owner", retiredNode.presentation.name)
        assertEquals(false, items.any { it.node.id == arbitraryCanonicalPresentation.id })
    }

    @Test
    fun beaconRetainsShellFreeStandaloneWorkspaceButNotArbitraryCanonicalWorkspace() {
        val standalonePresentation =
            HierarchyContextPresentationNode(
                id = "standalone-beacon-owner",
                name = "Standalone beacon owner",
                description = null,
                parentId = null,
                order = 0L,
                roleCode = null,
                tags = emptyList(),
            )
        val arbitraryCanonicalPresentation =
            HierarchyContextPresentationNode(
                id = "canonical-non-system-beacon-owner",
                name = "Must stay excluded",
                description = null,
                parentId = null,
                order = 1L,
                roleCode = null,
                tags = emptyList(),
            )
        val standaloneWorkspace =
            canonicalWorkspace(
                id = standalonePresentation.id,
                parentId = null,
                order = standalonePresentation.order,
            ).copy(
                provenance = "STANDALONE",
                sourceContextId = null,
            )
        val arbitraryCanonicalWorkspace =
            canonicalWorkspace(
                id = arbitraryCanonicalPresentation.id,
                parentId = null,
                order = arbitraryCanonicalPresentation.order,
            )

        val items =
            legacyFixtureBuild(
                rawContexts = emptyList(),
                beacons =
                    listOf(
                        OrientationBeaconInput(
                            id = "beacon-1",
                            title = "Health",
                            order = 0L,
                            readinessStatus = MainBeaconReadinessStatus.READY,
                            parentBeaconId = null,
                            relatedOwnerIds = listOf(
                                standalonePresentation.id,
                                arbitraryCanonicalPresentation.id,
                            ),
                            groupIds = emptyList(),
                        ),
                    ),
                workspaces = listOf(standaloneWorkspace, arbitraryCanonicalWorkspace),
                presentationHierarchy =
                    HierarchyPresentationData(
                        allProjects = listOf(standalonePresentation, arbitraryCanonicalPresentation),
                        topLevelProjects = listOf(standalonePresentation, arbitraryCanonicalPresentation),
                        childMap = emptyMap(),
                    ),
            )

        assertEquals(
            listOf("virtual:no-group", "beacon-1", standalonePresentation.id),
            items.map { it.node.id },
        )
    }

    @Test
    fun excludesArbitraryCanonicalWorkspaceAndKeepsNonReservedSysContextRawBacked() {
        val ordinarySysContext = context(id = "sys_custom", order = 0)
        val hierarchy =
            ContextHierarchyData(
                allProjects = listOf(ordinarySysContext),
                topLevelProjects = listOf(ordinarySysContext),
                childMap = emptyMap(),
            )
        val unrelatedCanonicalWorkspace =
            WorkspaceEntity(
                id = "canonical-non-system",
                nameOverride = "Must not appear",
                descriptionOverride = null,
                parentWorkspaceId = null,
                roleCode = null,
                workspaceOrder = 0L,
                createdAt = 0L,
                updatedAt = 0L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
                provenance = "CANONICAL_ONLY",
                sourceContextId = null,
            )

        val items =
            legacyFixtureBuild(
                rawContexts = hierarchy.allProjects,
                beacons = emptyList(),
                workspaces = listOf(unrelatedCanonicalWorkspace),
            )

        assertEquals(listOf("virtual:no-beacon", "sys_custom"), items.map { it.node.id })
        val node = items.last().node as OrientationHierarchyNode.ContextNode
        assertEquals("sys_custom", node.presentation.id)
    }

    @Test
    fun malformedOrDeletedCanonicalSystemPresentationDoesNotBecomeProjectLikeNode() {
        val systemPresentation =
            HierarchyContextPresentationNode(
                id = SystemContexts.INBOX.raw,
                name = "Must fail closed",
                description = null,
                parentId = null,
                order = 0L,
                roleCode = null,
                tags = emptyList(),
            )
        val malformed =
            canonicalWorkspace(id = systemPresentation.id, parentId = null, order = 0L)
                .copy(sourceContextId = systemPresentation.id)
        val deleted =
            canonicalWorkspace(id = systemPresentation.id, parentId = null, order = 0L)
                .copy(isDeleted = true)

        listOf(malformed, deleted).forEach { workspace ->
            val items =
                legacyFixtureBuild(
                    rawContexts = emptyList(),
                    beacons = emptyList(),
                    workspaces = listOf(workspace),
                    presentationHierarchy =
                        HierarchyPresentationData(
                            allProjects = listOf(systemPresentation),
                            topLevelProjects = listOf(systemPresentation),
                        ),
                )

            assertEquals(
                emptyList<String>(),
                items.filter { it.node is OrientationHierarchyNode.ProjectLike }.map { it.node.id },
            )
        }
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

    private fun canonicalWorkspace(
        id: String,
        parentId: String?,
        order: Long,
    ) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = parentId,
            roleCode = null,
            workspaceOrder = order,
            createdAt = 0L,
            updatedAt = 0L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = "CANONICAL_ONLY",
            sourceContextId = null,
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
            parentBeaconId = null,
            relatedOwnerIds = relatedContexts.map { it.id },
            groupIds = groupIds,
        )
}
