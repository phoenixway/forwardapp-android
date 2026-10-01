package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.HierarchyContextPresentationNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyPlacement
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalV2HierarchyPresentationTest {
    private val projector = CanonicalV2HierarchyProjector()
    private val composer = CanonicalV2HierarchySyntheticPresentationComposer()

    @Test
    fun `V2 Workspace admission cannot carry legacy topology`() {
        val source =
            ContextPresentation(
                id = "workspace",
                name = "Workspace",
                description = "Description",
                parentId = "legacy-parent",
                roleCode = "role",
                order = 91L,
                tags = listOf("tag"),
            )

        val presentation = source.toCanonicalV2WorkspacePresentation()
        val compatibilityNode = presentation.toHierarchyPresentationNode()

        assertEquals("workspace", presentation.id)
        assertEquals("Workspace", presentation.name)
        assertFalse(
            CanonicalV2WorkspacePresentation::class.java.declaredFields
                .any { it.name == "parentId" || it.name == "order" },
        )
        assertNull(compatibilityNode.parentId)
        assertEquals(0L, compatibilityNode.order)
    }

    @Test
    fun `resolver uses admitted Workspace presentation and live ManagedSubject`() {
        val workspace = workspaceTarget("workspace")
        val subject = subjectTarget("subject")
        val deleted = subjectTarget("deleted")

        val resolved =
            CanonicalV2HierarchyTargetResolver().resolve(
                placements =
                    listOf(
                        placement("w", workspace),
                        placement("s", subject, order = 1),
                        placement("d", deleted, order = 2),
                    ),
                admittedWorkspacePresentations =
                    listOf(
                        presentation(
                            id = "workspace",
                            name = "Workspace display",
                        ),
                    ),
                managedSubjects =
                    listOf(
                        subject("subject", "Beacon display"),
                        subject("deleted", "Deleted", isDeleted = true),
                    ),
                legacySubjectMappings =
                    listOf(
                        beaconMapping(
                            beaconId = "legacy-beacon",
                            subjectId = "subject",
                        ),
                    ),
            )

        assertEquals("Workspace display", resolved[workspace]?.title)
        assertEquals("workspace", resolved[workspace]?.presentationId)
        assertEquals("Beacon display", resolved[subject]?.title)
        assertEquals("legacy-beacon", resolved[subject]?.presentationId)
        assertNull(resolved[deleted])
    }

    @Test
    fun `ManagedSubject presentation identity may differ from canonical target identity`() {
        val canonicalTarget = subjectTarget("canonical-subject")
        val placement = placement("beacon-placement", canonicalTarget)

        val hierarchy =
            projector.project(
                placements = listOf(placement),
                admittedTargets =
                    mapOf(
                        canonicalTarget to
                            CanonicalV2HierarchyTargetPresentation(
                                target = canonicalTarget,
                                title = "Beacon",
                                presentationId = "legacy-beacon",
                            ),
                    ),
            )

        val presented =
            composer.compose(
                hierarchy = hierarchy,
                scopes =
                    listOf(
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                            id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                            title = "No group",
                            order = 0,
                            rootTargets = listOf(canonicalTarget),
                        ),
                    ),
            )

        val occurrence =
            presented.entries
                .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
                .single()

        assertEquals("legacy-beacon", occurrence.id)
        assertEquals("canonical-subject", occurrence.target.id)
    }

    @Test
    fun `Group NoGroup and NoBeacon wrap persisted roots without changing occurrence order`() {
        val groupBeacon = subjectTarget("group-beacon")
        val freeBeacon = subjectTarget("free-beacon")
        val workspaceRoot = workspaceTarget("workspace-root")
        val workspaceChild = workspaceTarget("workspace-child")

        val hierarchy =
            project(
                placement("group-root", groupBeacon, order = 0),
                placement("free-root", freeBeacon, order = 1),
                placement("workspace-root", workspaceRoot, order = 2),
                placement(
                    "workspace-child",
                    workspaceChild,
                    parentId = "workspace-root",
                ),
            )

        val presented =
            composer.compose(
                hierarchy = hierarchy,
                scopes =
                    listOf(
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.GROUP,
                            id = "group",
                            title = "Core",
                            order = 0,
                            rootTargets = listOf(groupBeacon),
                        ),
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                            id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                            title = "No group",
                            order = 0,
                            rootTargets = listOf(freeBeacon),
                        ),
                    ),
            )

        assertEquals(
            listOf(
                "group",
                "group-beacon",
                CANONICAL_V2_NO_GROUP_SCOPE_ID,
                "free-beacon",
                CANONICAL_V2_NO_BEACON_SCOPE_ID,
                "workspace-root",
                "workspace-child",
            ),
            presented.entries.map { it.id },
        )
        assertEquals(
            listOf("group-root", "free-root", "workspace-root", "workspace-child"),
            presented.entries
                .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
                .map { it.placementId.value },
        )
    }

    @Test
    fun `Group presentation order may reorder root scopes without changing persisted topology`() {
        val first = subjectTarget("first")
        val second = subjectTarget("second")
        val firstChild = workspaceTarget("first-child")
        val secondChild = workspaceTarget("second-child")

        val hierarchy =
            project(
                placement("first-root", first, order = 0),
                placement(
                    "first-child",
                    firstChild,
                    parentId = "first-root",
                    order = 0,
                ),
                placement("second-root", second, order = 1),
                placement(
                    "second-child",
                    secondChild,
                    parentId = "second-root",
                    order = 0,
                ),
            )

        val presented =
            composer.compose(
                hierarchy = hierarchy,
                scopes =
                    listOf(
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.GROUP,
                            id = "group-first",
                            title = "First",
                            order = 1,
                            rootPlacementIds = listOf(PlacementId("first-root")),
                        ),
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.GROUP,
                            id = "group-second",
                            title = "Second",
                            order = 0,
                            rootPlacementIds = listOf(PlacementId("second-root")),
                        ),
                    ),
            )

        assertEquals(
            listOf(
                "group-second",
                "second",
                "second-child",
                "group-first",
                "first",
                "first-child",
            ),
            presented.entries.map { it.id },
        )

        assertEquals(
            setOf("first-root", "first-child", "second-root", "second-child"),
            presented.entries
                .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
                .map { it.placementId.value }
                .toSet(),
        )

        assertEquals(
            listOf("second-root", "second-child", "first-root", "first-child"),
            presented.entries
                .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
                .map { it.placementId.value },
        )
    }

    @Test
    fun `same ManagedSubject root occurrences remain distinct across synthetic scopes`() {
        val target = subjectTarget("shared-subject")
        val presented =
            composer.compose(
                hierarchy =
                    project(
                        placement(
                            id = "group-occurrence",
                            target = target,
                            kind = PlacementKind.LINK,
                            order = 0,
                        ),
                        placement(
                            id = "no-group-occurrence",
                            target = target,
                            kind = PlacementKind.LINK,
                            order = 1,
                        ),
                    ),
                scopes =
                    listOf(
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.GROUP,
                            id = "group",
                            title = "Group",
                            order = 0,
                            rootTargets = listOf(target),
                        ),
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                            id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                            title = "No group",
                            order = 0,
                            rootTargets = listOf(target),
                        ),
                    ),
            )

        assertEquals(
            listOf(
                "group",
                "shared-subject",
                CANONICAL_V2_NO_GROUP_SCOPE_ID,
                "shared-subject",
            ),
            presented.entries.map { it.id },
        )
        assertEquals(
            listOf("group-occurrence", "no-group-occurrence"),
            presented.entries
                .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
                .map { it.placementId.value },
        )
    }

    @Test
    fun `empty Group remains visible`() {
        val presented =
            composer.compose(
                hierarchy = project(placement("workspace", workspaceTarget("workspace"))),
                scopes =
                    listOf(
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.GROUP,
                            id = "empty-group",
                            title = "Empty",
                            order = 0,
                            rootTargets = emptyList(),
                        ),
                    ),
            )

        assertEquals(
            listOf(
                "empty-group",
                CANONICAL_V2_NO_BEACON_SCOPE_ID,
                "workspace",
            ),
            presented.entries.map { it.id },
        )
    }

    @Test
    fun `scope plan fails closed instead of manufacturing or dropping subject roots`() {
        val target = subjectTarget("subject")
        val hierarchy = project(placement("subject-root", target))

        val missingAssignment =
            runCatching {
                composer.compose(
                    hierarchy = hierarchy,
                    scopes = emptyList(),
                )
            }.exceptionOrNull()

        assertTrue(
            missingAssignment is CanonicalV2HierarchySyntheticPresentationException,
        )

        val manufactured =
            runCatching {
                composer.compose(
                    hierarchy = hierarchy,
                    scopes =
                        listOf(
                            CanonicalV2SyntheticScopeInput(
                                kind = CanonicalV2SyntheticScopeKind.GROUP,
                                id = "group",
                                title = "Group",
                                order = 0,
                                rootTargets = listOf(subjectTarget("other")),
                            ),
                        ),
                )
            }.exceptionOrNull()

        assertTrue(
            manufactured is CanonicalV2HierarchySyntheticPresentationException,
        )
    }

    @Test
    fun `focus and breadcrumbs use first visible repeated Workspace occurrence`() {
        val beacon = subjectTarget("beacon")
        val root = workspaceTarget("root")
        val repeated = workspaceTarget("repeated")

        val presented =
            composer.compose(
                hierarchy =
                    project(
                        placement("beacon", beacon, order = 0),
                        placement(
                            "beacon-link",
                            repeated,
                            parentId = "beacon",
                            kind = PlacementKind.LINK,
                        ),
                        placement("root", root, order = 1),
                        placement(
                            "structural",
                            repeated,
                            parentId = "root",
                        ),
                    ),
                scopes =
                    listOf(
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                            id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                            title = "No group",
                            order = 0,
                            rootTargets = listOf(beacon),
                        ),
                    ),
            )

        assertEquals(
            "beacon-link",
            presented.firstWorkspaceOccurrence("repeated")
                ?.placementId
                ?.value,
        )
        assertEquals(
            listOf(
                CANONICAL_V2_NO_GROUP_SCOPE_ID to
                    CanonicalV2BreadcrumbTarget.ORIENTATION_NODE,
                "beacon" to
                    CanonicalV2BreadcrumbTarget.ORIENTATION_NODE,
                "repeated" to
                    CanonicalV2BreadcrumbTarget.CONTEXT,
            ),
            presented
                .breadcrumbsToWorkspace("repeated")
                .map { it.id to it.target },
        )
    }

    @Test
    fun `structural Workspace occurrence remains visible beside Beacon LINK appearance`() {
        val beacon = subjectTarget("beacon")
        val root = workspaceTarget("root")
        val repeated = workspaceTarget("repeated")

        val presented =
            composer.compose(
                hierarchy =
                    project(
                        placement("beacon", beacon, order = 0),
                        placement(
                            "link",
                            repeated,
                            parentId = "beacon",
                            kind = PlacementKind.LINK,
                        ),
                        placement("root", root, order = 1),
                        placement(
                            "structural",
                            repeated,
                            parentId = "root",
                        ),
                    ),
                scopes =
                    listOf(
                        CanonicalV2SyntheticScopeInput(
                            kind = CanonicalV2SyntheticScopeKind.NO_GROUP,
                            id = CANONICAL_V2_NO_GROUP_SCOPE_ID,
                            title = "No group",
                            order = 0,
                            rootTargets = listOf(beacon),
                        ),
                    ),
            )

        assertEquals(
            listOf("link", "structural"),
            presented.entries
                .filterIsInstance<CanonicalV2PresentedHierarchyEntry.Occurrence>()
                .filter { it.target == repeated }
                .map { it.placementId.value },
        )
    }

    private fun project(
        vararg placements: HierarchyPlacement,
    ): CanonicalV2HierarchyProjection {
        val admitted =
            placements
                .asSequence()
                .filterNot { it.isDeleted }
                .map { it.target }
                .distinct()
                .associateWith { target ->
                    CanonicalV2HierarchyTargetPresentation(
                        target = target,
                        title = target.id,
                    )
                }

        return projector.project(
            placements = placements.toList(),
            admittedTargets = admitted,
        )
    }

    private fun workspaceTarget(id: String) =
        HierarchyTargetRef(
            type = HierarchyTargetType.WORKSPACE,
            id = id,
        )

    private fun subjectTarget(id: String) =
        HierarchyTargetRef(
            type = HierarchyTargetType.MANAGED_SUBJECT,
            id = id,
        )

    private fun placement(
        id: String,
        target: HierarchyTargetRef,
        parentId: String? = null,
        kind: PlacementKind = PlacementKind.PRIMARY,
        order: Long = 0,
    ) = HierarchyPlacement(
        id = PlacementId(id),
        hierarchyId = HierarchyId.GENERAL,
        target = target,
        parentPlacementId = parentId?.let(::PlacementId),
        placementKind = kind,
        siblingOrder = order,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun presentation(
        id: String,
        name: String,
    ) = CanonicalV2WorkspacePresentation(
        id = id,
        name = name,
        description = null,
        roleCode = null,
        tags = emptyList(),
    )

    private fun beaconMapping(
        beaconId: String,
        subjectId: String,
    ) = LegacySubjectMappingEntity(
        id = "mapping-$beaconId",
        sourceType = LegacyOrientationSourceType.MAIN_BEACON.name,
        sourceId = beaconId,
        subjectId = subjectId,
        migrationVersion = 1,
        state = LegacySubjectMappingState.CUT_OVER.name,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = false,
        version = 1,
    )

    private fun subject(
        id: String,
        title: String,
        isDeleted: Boolean = false,
    ) = ManagedSubjectEntity(
        id = id,
        subjectType = "ORIENTATION",
        title = title,
        description = null,
        createdAt = 1,
        updatedAt = 1,
        syncedAt = null,
        isDeleted = isDeleted,
        version = 1,
    )
}
