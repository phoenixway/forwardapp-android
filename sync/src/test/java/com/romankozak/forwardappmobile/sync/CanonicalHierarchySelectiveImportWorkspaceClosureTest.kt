package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementLinkedAppearanceSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.HierarchyPlacementSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalHierarchySelectiveImportWorkspaceClosureTest {
    private val filter = SnapshotBundleSelectiveImportFilter()

    @Test
    fun `root Workspace selection retains only selected source H1 occurrence`() {
        val source =
            source(
                workspaces = listOf(workspace("selected"), workspace("unrelated")),
                placements =
                    listOf(
                        h1("selected-root", "selected"),
                        h1("unrelated-root", "unrelated", order = 999L),
                    ),
            )

        val filtered = filter.filter(source, selection("selected"))

        assertEquals(listOf("selected-root"), filtered.hierarchyPlacements?.map { it.id })
        assertEquals(listOf("selected"), filtered.workspaces?.map { it.id })
        assertNotNull(filtered.hierarchyPlacementGroupScopes)
        assertTrue(filtered.hierarchyPlacementGroupScopes.orEmpty().isEmpty())
    }

    @Test
    fun `linked presentation provenance follows exact retained PlacementId only`() {
        val source =
            source(
                workspaces =
                    listOf(
                        workspace("root-a"),
                        workspace("root-b"),
                        workspace("shared"),
                    ),
                placements =
                    listOf(
                        h1("root-a-p", "root-a"),
                        h1("root-b-p", "root-b"),
                        h1("shared-primary", "shared"),
                        h1(
                            id = "shared-link-a",
                            targetId = "shared",
                            parentId = "root-a-p",
                            kind = "LINK",
                        ),
                        h1(
                            id = "shared-link-b",
                            targetId = "shared",
                            parentId = "root-b-p",
                            kind = "LINK",
                        ),
                    ),
                linkedAppearances =
                    listOf(
                        linkedAppearance("shared-link-a"),
                        linkedAppearance("shared-link-b"),
                    ),
            )

        val filtered = filter.filter(source, selection("root-a", "shared"))

        assertEquals(
            listOf("shared-link-a"),
            requireNotNull(filtered.hierarchyPlacementLinkedAppearances)
                .map { it.placementId },
        )
        assertEquals(
            setOf("shared-primary", "shared-link-a"),
            filtered.hierarchyPlacements
                .orEmpty()
                .filter { it.targetId == "shared" }
                .mapTo(linkedSetOf()) { it.id },
        )
    }

    @Test
    fun `linked presentation provenance with missing exact H1 placement fails closed`() {
        val source =
            source(
                workspaces = listOf(workspace("selected")),
                placements = listOf(h1("selected-root", "selected")),
                linkedAppearances = listOf(linkedAppearance("missing-placement")),
            )

        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(source, selection("selected"))
        }
    }

    @Test
    fun `child is retained only through its exact retained parent and child alone pulls no ancestor`() {
        val source =
            source(
                workspaces =
                    listOf(
                        workspace("root"),
                        workspace("child", parentId = "decoy-parent"),
                        workspace("decoy-parent"),
                    ),
                placements =
                    listOf(
                        h1("root-p", "root"),
                        h1("child-p", "child", parentId = "root-p"),
                        h1("decoy-p", "decoy-parent"),
                    ),
            )

        val together = filter.filter(source, selection("root", "child"))
        assertEquals(
            listOf("root-p", "child-p"),
            together.hierarchyPlacements?.map { it.id },
        )
        assertEquals("root-p", together.hierarchyPlacements?.single { it.id == "child-p" }?.parentPlacementId)

        val childOnly = filter.filter(source, selection("child"))
        assertTrue(childOnly.hierarchyPlacements.orEmpty().isEmpty())
    }

    @Test
    fun `same target keeps every valid occurrence but excludes occurrence under unselected parent`() {
        val source =
            source(
                workspaces =
                    listOf(
                        workspace("root-a"),
                        workspace("root-b"),
                        workspace("shared"),
                    ),
                placements =
                    listOf(
                        h1("root-a-p", "root-a"),
                        h1("root-b-p", "root-b"),
                        h1("shared-primary", "shared"),
                        h1(
                            id = "shared-link-a",
                            targetId = "shared",
                            parentId = "root-a-p",
                            kind = "LINK",
                        ),
                        h1(
                            id = "shared-link-b",
                            targetId = "shared",
                            parentId = "root-b-p",
                            kind = "LINK",
                        ),
                    ),
            )

        val bothParents = filter.filter(source, selection("root-a", "root-b", "shared"))
        assertEquals(
            setOf("shared-primary", "shared-link-a", "shared-link-b"),
            bothParents.hierarchyPlacements
                .orEmpty()
                .filter { it.targetId == "shared" }
                .mapTo(linkedSetOf()) { it.id },
        )
        assertEquals(
            setOf("PRIMARY", "LINK"),
            bothParents.hierarchyPlacements
                .orEmpty()
                .filter { it.targetId == "shared" }
                .mapTo(linkedSetOf()) { it.placementKind },
        )

        val oneParent = filter.filter(source, selection("root-a", "shared"))
        assertEquals(
            setOf("shared-primary", "shared-link-a"),
            oneParent.hierarchyPlacements
                .orEmpty()
                .filter { it.targetId == "shared" }
                .mapTo(linkedSetOf()) { it.id },
        )
    }

    @Test
    fun `duplicate LINK occurrences remain distinct and LINK occurrence owns its exact selected subtree`() {
        val source =
            source(
                workspaces =
                    listOf(
                        workspace("root-a"),
                        workspace("root-b"),
                        workspace("shared"),
                        workspace("leaf"),
                    ),
                placements =
                    listOf(
                        h1("root-a-p", "root-a"),
                        h1("root-b-p", "root-b"),
                        h1("shared-primary", "shared"),
                        h1("leaf-primary", "leaf"),
                        h1("shared-link-a", "shared", "root-a-p", "LINK"),
                        h1("shared-link-b", "shared", "root-b-p", "LINK"),
                        h1("leaf-under-link-a", "leaf", "shared-link-a", "LINK"),
                        h1("leaf-under-link-b", "leaf", "shared-link-b", "LINK"),
                    ),
            )

        val filtered =
            filter.filter(
                source,
                selection("root-a", "root-b", "shared", "leaf"),
            )

        assertEquals(
            setOf("shared-link-a", "shared-link-b"),
            filtered.hierarchyPlacements
                .orEmpty()
                .filter { it.targetId == "shared" && it.placementKind == "LINK" }
                .mapTo(linkedSetOf()) { it.id },
        )
        assertEquals(
            mapOf(
                "leaf-under-link-a" to "shared-link-a",
                "leaf-under-link-b" to "shared-link-b",
            ),
            filtered.hierarchyPlacements
                .orEmpty()
                .filter { it.id.startsWith("leaf-under-link-") }
                .associate { it.id to it.parentPlacementId },
        )
    }

    @Test
    fun `Workspace parent fields and source order never manufacture H1 topology`() {
        val source =
            source(
                workspaces =
                    listOf(
                        workspace("claimed-parent"),
                        workspace("selected", parentId = "claimed-parent"),
                    ),
                placements =
                    listOf(
                        h1(
                            id = "selected-root",
                            targetId = "selected",
                            order = 1000L,
                        ),
                        h1(
                            id = "claimed-parent-root",
                            targetId = "claimed-parent",
                            order = -1000L,
                        ),
                    ).reversed(),
            )

        val filtered = filter.filter(source, selection("selected"))

        val selected = requireNotNull(filtered.hierarchyPlacements).single()
        assertEquals("selected-root", selected.id)
        assertEquals(null, selected.parentPlacementId)

        // parentWorkspaceId is retained only as a canonical validator dependency.
        // It does not become an H1 ancestor.
        assertEquals(
            listOf("claimed-parent", "selected"),
            filtered.workspaces?.map { it.id },
        )
    }

    @Test
    fun `selected Context without canonical Workspace target imports product row but selects no H1 occurrence`() {
        val selectedContext =
            com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot(
                id = "context-only",
                name = "context-only",
                parentId = null,
                description = null,
                createdAt = 1L,
                updatedAt = 2L,
                isExpanded = false,
                isDeleted = false,
                version = 1L,
                tags = emptyList(),
                relatedLinks = emptyList(),
                order = 0,
                isAttachmentsExpanded = false,
                defaultViewModeName = "DIRECTION",
                isCompleted = false,
                isContextManagementEnabled = false,
                contextStatus = "NO_PLAN",
                contextStatusText = null,
                contextLogLevel = null,
                totalTimeSpentMinutes = 0L,
                valueImportance = 0,
                valueImpact = 0,
                effort = 0,
                cost = 0,
                risk = 0,
                weightEffort = 1f,
                weightCost = 1f,
                weightRisk = 1f,
                rawScore = 0.0,
                displayScore = 0.0,
                scoringStatus = "NOT_ASSESSED",
                showCheckboxes = false,
                roleCode = null,
            )
        val source =
            source(
                workspaces = listOf(workspace("unrelated")),
                placements = listOf(h1("unrelated-root", "unrelated")),
            ).copy(
                contexts = listOf(selectedContext),
            )

        val filtered = filter.filter(source, selection("context-only"))

        assertEquals(listOf("context-only"), filtered.contexts.map { it.id })
        assertNotNull(filtered.hierarchyPlacements)
        assertTrue(filtered.hierarchyPlacements.orEmpty().isEmpty())
        assertNotNull(filtered.hierarchyPlacementGroupScopes)
        assertTrue(filtered.hierarchyPlacementGroupScopes.orEmpty().isEmpty())
        assertTrue(filtered.workspaces.orEmpty().isEmpty())
    }

    @Test
    fun `native H1 with zero hierarchy targets emits explicit empty streams instead of inheriting source`() {
        val source =
            source(
                workspaces = listOf(workspace("unselected")),
                placements = listOf(h1("unselected-root", "unselected")),
            )

        val filtered =
            filter.filter(
                source,
                WorkspaceSelectiveImportSelection(),
            )

        assertNotNull(filtered.hierarchyPlacements)
        assertTrue(filtered.hierarchyPlacements.orEmpty().isEmpty())
        assertNotNull(filtered.hierarchyPlacementGroupScopes)
        assertTrue(filtered.hierarchyPlacementGroupScopes.orEmpty().isEmpty())
        assertTrue(filtered.workspaces.orEmpty().isEmpty())
    }

    @Test
    fun `invalid source H1 fails closed even when malformed occurrence is not selected`() {
        val missingTarget =
            source(
                workspaces = listOf(workspace("selected")),
                placements =
                    listOf(
                        h1("selected-root", "selected"),
                        h1("bad-root", "missing"),
                    ),
            )
        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(missingTarget, selection("selected"))
        }

        val missingParent =
            source(
                workspaces = listOf(workspace("selected")),
                placements =
                    listOf(
                        h1("selected-child", "selected", parentId = "missing-parent"),
                    ),
            )
        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(missingParent, selection("selected"))
        }

        val duplicateId =
            source(
                workspaces = listOf(workspace("selected"), workspace("other")),
                placements =
                    listOf(
                        h1("duplicate", "selected"),
                        h1("duplicate", "other", kind = "LINK"),
                    ),
            )
        assertThrows(IllegalArgumentException::class.java) {
            filter.filter(duplicateId, selection("selected"))
        }
    }

    private fun selection(vararg ids: String) =
        WorkspaceSelectiveImportSelection(selectedContextIds = ids.toSet())

    private fun source(
        workspaces: List<WorkspaceEntity>,
        placements: List<HierarchyPlacementSnapshot>,
        linkedAppearances: List<HierarchyPlacementLinkedAppearanceSnapshot> = emptyList(),
    ) =
        SnapshotBundle(
            version = 2,
            managedSubjects = emptyList(),
            orientations = emptyList(),
            aspects = emptyList(),
            orientationAssessments = emptyList(),
            orientationAssessmentRevisions = emptyList(),
            legacySubjectMappings = emptyList(),
            orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(),
            workspaces = workspaces,
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = emptyList(),
            savedOrientationViews = emptyList(),
            hierarchyPlacements = placements,
            hierarchyPlacementGroupScopes = emptyList(),
            hierarchyPlacementLinkedAppearances = linkedAppearances,
        )

    private fun linkedAppearance(
        placementId: String,
    ) =
        HierarchyPlacementLinkedAppearanceSnapshot(
            placementId = placementId,
            hierarchyId = "GENERAL",
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = 3L,
            isDeleted = false,
            version = 1L,
        )

    private fun workspace(
        id: String,
        parentId: String? = null,
    ) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = parentId,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun h1(
        id: String,
        targetId: String,
        parentId: String? = null,
        kind: String = "PRIMARY",
        order: Long = 0L,
    ) =
        HierarchyPlacementSnapshot(
            id = id,
            hierarchyId = "GENERAL",
            targetType = "WORKSPACE",
            targetId = targetId,
            parentPlacementId = parentId,
            placementKind = kind,
            siblingOrder = order,
            createdAt = 1L,
            updatedAt = 2L,
            syncedAt = 3L,
            isDeleted = false,
            version = 1L,
        )
}
