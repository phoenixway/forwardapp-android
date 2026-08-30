package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.domain.orientation.orientationCapabilityRegistry
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityArchetype
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InboxSortingCapabilityContractTest {
    @Test
    fun `typed configuration round trips and absent rules default to newest`() {
        val configuration =
            InboxSortingCapabilityConfigurationV1(
                listOf(
                    WorkspaceSortingRule(WorkspaceSortingTarget.INBOX, WorkspaceSortingMode.ALPHA),
                    WorkspaceSortingRule(WorkspaceSortingTarget.BACKLOG, WorkspaceSortingMode.OLDEST),
                ),
            )

        val encoded = InboxSortingCapabilityConfigurationCodec.encode(configuration)

        assertEquals(configuration, InboxSortingCapabilityConfigurationCodec.decode(1, encoded))
        assertEquals(
            WorkspaceSortingMode.NEWEST,
            effectiveSortingMode(configuration, WorkspaceSortingTarget.CONNECTIONS),
        )
    }

    @Test
    fun `configuration rejects duplicate targets invalid combinations and unknown fields`() {
        assertFailsWith<IllegalArgumentException> {
            InboxSortingCapabilityConfigurationCodec.encode(
                InboxSortingCapabilityConfigurationV1(
                    listOf(
                        WorkspaceSortingRule(WorkspaceSortingTarget.BACKLOG, WorkspaceSortingMode.NEWEST),
                        WorkspaceSortingRule(WorkspaceSortingTarget.BACKLOG, WorkspaceSortingMode.OLDEST),
                    ),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            InboxSortingCapabilityConfigurationCodec.encode(
                InboxSortingCapabilityConfigurationV1(
                    listOf(WorkspaceSortingRule(WorkspaceSortingTarget.BACKLOG, WorkspaceSortingMode.ALPHA)),
                ),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            InboxSortingCapabilityConfigurationCodec.decode(1, "{\"rules\":[],\"extra\":true}")
        }
    }

    @Test
    fun `legacy planner maps current grammar and attachment alias into typed policy`() {
        val plan =
            InboxSortingLegacyPlanner.plan(
                sources = listOf(source("backlog:oldest\ninbox:alpha\nattachments:type")),
                bindings = bindings(),
            )

        assertTrue(plan.canApply)
        assertTrue(plan.isFullyAccounted)
        val configuration =
            InboxSortingCapabilityConfigurationCodec.decode(
                plan.updates.single().configurationVersion,
                plan.updates.single().configuration,
            )
        assertEquals(WorkspaceSortingMode.OLDEST, effectiveSortingMode(configuration, WorkspaceSortingTarget.BACKLOG))
        assertEquals(WorkspaceSortingMode.ALPHA, effectiveSortingMode(configuration, WorkspaceSortingTarget.INBOX))
        assertEquals(WorkspaceSortingMode.TYPE, effectiveSortingMode(configuration, WorkspaceSortingTarget.CONNECTIONS))
        assertEquals(44L, plan.updates.single().sourceUpdatedAt)
    }

    @Test
    fun `blank legacy policy preserves newest defaults without inventing rules`() {
        val plan = InboxSortingLegacyPlanner.plan(listOf(source("\n  \n")), bindings())

        assertTrue(plan.canApply)
        val configuration =
            InboxSortingCapabilityConfigurationCodec.decode(
                plan.updates.single().configurationVersion,
                plan.updates.single().configuration,
            )
        assertTrue(configuration.rules.isEmpty())
        assertEquals(WorkspaceSortingMode.NEWEST, effectiveSortingMode(configuration, WorkspaceSortingTarget.INBOX))
    }

    @Test
    fun `invalid ambiguous and unresolved legacy policy fails closed`() {
        val invalid =
            InboxSortingLegacyPlanner.plan(
                sources = listOf(source("backlog:alpha\nattachments:type\nconnections:oldest\nwrong")),
                bindings = bindings(),
            )
        assertFalse(invalid.canApply)
        assertTrue(invalid.issues.any { it.code == InboxSortingMigrationIssueCode.UNKNOWN_MODE })
        assertTrue(invalid.issues.any { it.code == InboxSortingMigrationIssueCode.DUPLICATE_TARGET })
        assertTrue(invalid.issues.any { it.code == InboxSortingMigrationIssueCode.INVALID_RULE })

        val unresolved =
            InboxSortingLegacyPlanner.plan(
                sources = listOf(source("inbox:newest", contextId = "missing")),
                bindings = bindings(),
            )
        assertFalse(unresolved.canApply)
        assertTrue(unresolved.issues.any { it.code == InboxSortingMigrationIssueCode.UNRESOLVED_OWNER_WORKSPACE })
    }

    @Test
    fun `multiple legacy owners resolving to one workspace fail closed`() {
        val plan =
            InboxSortingLegacyPlanner.plan(
                sources =
                    listOf(
                        source("inbox:newest", contextId = "context"),
                        source("inbox:oldest", contextId = "context-alias"),
                    ),
                bindings =
                    bindings().copy(
                        workspaceIdByContextId =
                            mapOf(
                                "context" to "workspace",
                                "context-alias" to "workspace",
                            ),
                    ),
            )

        assertFalse(plan.canApply)
        assertTrue(plan.issues.any { it.code == InboxSortingMigrationIssueCode.MULTIPLE_SOURCES_FOR_WORKSPACE })
    }

    @Test
    fun `policy has command scoped dependencies rather than static inbox dependency`() {
        val definition =
            orientationCapabilityRegistry.single { it.type == WorkspaceCapabilityType.INBOX_SORTING }

        assertEquals(WorkspaceCapabilityArchetype.POLICY, definition.archetype)
        assertTrue(definition.requiredTypes.isEmpty())
        assertEquals(
            WorkspaceCapabilityType.BACKLOG,
            requiredCapabilityForSorting(WorkspaceSortingTarget.BACKLOG),
        )
        assertEquals(
            WorkspaceCapabilityType.CONNECTIONS,
            requiredCapabilityForSorting(WorkspaceSortingTarget.CONNECTIONS),
        )
    }

    private fun source(
        rulesText: String,
        contextId: String = "context",
    ) = LegacyInboxSortingSource(contextId = contextId, rulesText = rulesText, updatedAt = 44L)

    private fun bindings() =
        InboxSortingMigrationBindings(
            workspaceIdByContextId = mapOf("context" to "workspace"),
            capabilityInstanceIdByWorkspaceId = mapOf("workspace" to "sorting-capability"),
        )
}
