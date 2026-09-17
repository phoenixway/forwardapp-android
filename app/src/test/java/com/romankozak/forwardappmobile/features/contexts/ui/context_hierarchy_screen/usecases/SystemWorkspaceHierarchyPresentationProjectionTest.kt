package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.usecases

import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.projectPresentationUniverseFromState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.FilterState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningMode
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.toHierarchyPresentationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemWorkspaceHierarchyPresentationProjectionTest {
    @Test
    fun promotedSystemUsesCanonicalPresentationAndHierarchyOrder() {
        val systemId = SystemContexts.INBOX.raw
        val canonicalParentId = SystemContexts.STRATEGIC.raw
        val staleShell =
            context(
                id = systemId,
                name = "stale-inbox",
                description = "stale-description",
                parentId = SystemContexts.TODAY.raw,
                roleCode = "stale-role",
                order = 99L,
            )
        val staleParentShell = context(canonicalParentId, "stale-parent", order = 91L)
        val ordinarySibling =
            context(
                "ordinary-sibling",
                "ordinary",
                parentId = canonicalParentId,
                order = 2L,
            )

        val projected =
            projectPresentationUniverseFromState(
                contexts = listOf(staleShell, staleParentShell, ordinarySibling),
                workspaces =
                    listOf(
                        workspace(
                            id = systemId,
                            name = "canonical-inbox",
                            description = "canonical-description",
                            parentId = canonicalParentId,
                            roleCode = "canonical-role",
                            order = 3L,
                        ),
                        workspace(
                            id = canonicalParentId,
                            name = "canonical-parent",
                            parentId = null,
                            order = 1L,
                        ),
                    ),
                canonicalTagsById =
                    mapOf(
                        systemId to emptyList(),
                        canonicalParentId to emptyList(),
                    ),
            )

        val result = projected.single { it.id == systemId }

        assertEquals("canonical-inbox", result.name)
        assertEquals("canonical-description", result.description)
        assertEquals(canonicalParentId, result.parentId)
        assertEquals("canonical-role", result.roleCode)
        assertEquals(3L, result.order)

        val hierarchy =
            HierarchyUseCase().createProjectHierarchy(
                FilterState(
                    flatList = projected.map { it.toHierarchyPresentationNode() },
                    query = "",
                    searchActive = false,
                    mode = PlanningMode.All,
                    settings = PlanningSettingsState(),
                    isReady = true,
                ),
            )

        assertEquals(
            listOf(canonicalParentId),
            hierarchy.topLevelProjects.map { it.id },
        )
        assertEquals(
            listOf(ordinarySibling.id, systemId),
            hierarchy.childMap[canonicalParentId]?.map { it.id },
        )
    }

    @Test
    fun ordinaryContextPresentationIsUnchanged() {
        val ordinary =
            context(
                id = "ordinary",
                name = "legacy",
                parentId = "legacy-parent",
                order = 7L,
            )

        val result =
            projectPresentationUniverseFromState(
                contexts = listOf(ordinary),
                workspaces =
                    listOf(
                        workspace(
                            id = ordinary.id,
                            name = "canonical-looking",
                            parentId = null,
                            order = 1L,
                        ),
                    ),
                canonicalTagsById = emptyMap(),
            ).single()

        assertEquals(ordinary.id, result.id)
        assertEquals("legacy", result.name)
        assertEquals("legacy-parent", result.parentId)
        assertEquals(7L, result.order)
        assertEquals(ordinary.tags, result.tags)
    }

    @Test
    fun contextBackedReservedSystemRemainsLegacyOwned() {
        val systemId = SystemContexts.INBOX.raw
        val shell =
            context(
                id = systemId,
                name = "legacy-inbox",
                parentId = "legacy-parent",
                order = 8L,
            )

        val result =
            projectPresentationUniverseFromState(
                contexts = listOf(shell),
                workspaces =
                    listOf(
                        workspace(
                            id = systemId,
                            name = "workspace-shadow",
                            parentId = null,
                            order = 0L,
                            provenance = WorkspaceProvenance.CONTEXT_BACKED,
                            sourceContextId = systemId,
                        ),
                    ),
                canonicalTagsById = emptyMap(),
            ).single()

        assertEquals(shell.id, result.id)
        assertEquals("legacy-inbox", result.name)
        assertEquals("legacy-parent", result.parentId)
        assertEquals(8L, result.order)
        assertEquals(shell.tags, result.tags)
    }

    @Test
    fun malformedDeletedAndAbsentSystemOwnersFailClosedWithoutLegacyFallback() {
        val absentId = SystemContexts.INBOX.raw
        val malformedId = SystemContexts.STRATEGIC.raw
        val deletedId = SystemContexts.TODAY.raw
        val blankNameId = SystemContexts.WEEK.raw

        val result =
            projectPresentationUniverseFromState(
                contexts =
                    listOf(
                        context(absentId, "stale-absent"),
                        context(malformedId, "stale-malformed"),
                        context(deletedId, "stale-deleted"),
                        context(blankNameId, "stale-blank"),
                    ),
                workspaces =
                    listOf(
                        workspace(
                            malformedId,
                            "canonical",
                            sourceContextId = malformedId,
                        ),
                        workspace(
                            deletedId,
                            "canonical",
                            isDeleted = true,
                        ),
                        workspace(
                            blankNameId,
                            "   ",
                        ),
                    ),
                canonicalTagsById =
                    mapOf(
                        absentId to emptyList(),
                        malformedId to emptyList(),
                        deletedId to emptyList(),
                        blankNameId to emptyList(),
                    ),
            )

        assertTrue(result.isEmpty())
        assertNull(result.firstOrNull { it.id == absentId })
    }

    private fun context(
        id: String,
        name: String,
        description: String? = null,
        parentId: String? = null,
        roleCode: String? = null,
        order: Long = 0L,
    ) = Context(
        id = id,
        name = name,
        description = description,
        parentId = parentId,
        createdAt = 1L,
        updatedAt = 2L,
        order = order,
        roleCode = roleCode,
    )

    private fun workspace(
        id: String,
        name: String?,
        description: String? = null,
        parentId: String? = null,
        roleCode: String? = null,
        order: Long = 0L,
        provenance: WorkspaceProvenance = WorkspaceProvenance.CANONICAL_ONLY,
        sourceContextId: String? = null,
        isDeleted: Boolean = false,
    ) = WorkspaceEntity(
        id = id,
        nameOverride = name,
        descriptionOverride = description,
        parentWorkspaceId = parentId,
        roleCode = roleCode,
        workspaceOrder = order,
        createdAt = 1L,
        updatedAt = 2L,
        syncedAt = null,
        isDeleted = isDeleted,
        version = 1L,
        provenance = provenance.name,
        sourceContextId = sourceContextId,
    )
}
