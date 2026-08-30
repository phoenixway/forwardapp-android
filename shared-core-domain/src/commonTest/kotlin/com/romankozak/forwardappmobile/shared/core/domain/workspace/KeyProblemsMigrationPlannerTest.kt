package com.romankozak.forwardappmobile.shared.core.domain.workspace

import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KeyProblemsMigrationPlannerTest {
    @Test
    fun `typed config rejects unknown version and malformed payload`() {
        assertEquals("{}", KeyProblemsCapabilityConfigurationCodec.encode())
        assertEquals(
            KeyProblemsCapabilityConfigurationV1,
            KeyProblemsCapabilityConfigurationCodec.decode(1, "{}"),
        )
        assertTrue(runCatching { KeyProblemsCapabilityConfigurationCodec.decode(2, "{}") }.isFailure)
        assertTrue(runCatching { KeyProblemsCapabilityConfigurationCodec.decode(1, "{\"x\":1}") }.isFailure)
    }

    @Test
    fun `planner normalizes tracker order and creates typed refs`() {
        val plan =
            KeyProblemsMigrationPlanner.plan(
                sources =
                    listOf(
                        source(
                            """
                            {
                              "issues": [
                                {
                                  "id": "later",
                                  "title": "  Later  ",
                                  "description": " Details ",
                                  "dateTime": null,
                                  "status": "BLOCKED",
                                  "relatedContextIds": ["related", "related"],
                                  "relatedAttachmentIds": ["attachment", "attachment"],
                                  "order": 5,
                                  "createdAt": 10,
                                  "updatedAt": 20
                                },
                                {
                                  "id": "first",
                                  "title": "First",
                                  "status": "OPEN",
                                  "order": 1
                                }
                              ]
                            }
                            """.trimIndent(),
                        ),
                    ),
                bindings = bindings(),
            )

        assertTrue(plan.canApply)
        assertTrue(plan.isFullyAccounted)
        assertEquals(listOf("first", "later"), plan.problems.map { it.id })
        assertEquals(listOf(0L, 1L), plan.problems.map { it.order })
        assertEquals(WorkspaceProblemStatus.BLOCKED, plan.problems.last().status)
        assertEquals("Later", plan.problems.last().title)
        assertEquals("Details", plan.problems.last().description)
        assertEquals(listOf("workspace-related"), plan.workspaceRefs.map { it.targetWorkspaceId })
        assertEquals(listOf("attachment"), plan.attachmentRefs.map { it.attachmentId })
    }

    @Test
    fun `legacy description shape becomes one deterministic problem without synthetic dateTime`() {
        val plan =
            KeyProblemsMigrationPlanner.plan(
                sources =
                    listOf(
                        source(
                            """{"description":"  Broken pump\nNeeds inspection  ","focusContextIds":["related"]}""",
                        ),
                    ),
                bindings = bindings(),
            )

        assertTrue(plan.canApply)
        assertEquals(1, plan.problems.size)
        assertEquals("legacy-owner", plan.problems.single().id)
        assertEquals("Broken pump", plan.problems.single().title)
        assertEquals(100L, plan.problems.single().createdAt)
        assertEquals("workspace-related", plan.workspaceRefs.single().targetWorkspaceId)
    }

    @Test
    fun `explicit dateTime blocks cutover without dropping parsed problem`() {
        val plan =
            KeyProblemsMigrationPlanner.plan(
                sources =
                    listOf(
                        source(
                            """{"issues":[{"id":"dated","title":"Dated","dateTime":1234}]}""",
                        ),
                    ),
                bindings = bindings(),
            )

        assertFalse(plan.canApply)
        assertEquals(listOf("dated"), plan.problems.map { it.id })
        assertTrue(
            plan.issues.any {
                it.code == KeyProblemsMigrationIssueCode.DATE_TIME_REQUIRES_DECISION &&
                    it.issueId == "dated"
            },
        )
    }

    @Test
    fun `unknown status duplicate identity and malformed refs fail closed`() {
        val unknownStatus =
            KeyProblemsMigrationPlanner.plan(
                sources =
                    listOf(
                        source(
                            """{"issues":[{"id":"same","title":"One","status":"UNKNOWN"},{"id":"same","title":"Two"}]}""",
                        ),
                    ),
                bindings = bindings(),
            )
        assertFalse(unknownStatus.canApply)
        assertTrue(unknownStatus.issues.any { it.code == KeyProblemsMigrationIssueCode.UNKNOWN_STATUS })
        assertTrue(unknownStatus.issues.any { it.code == KeyProblemsMigrationIssueCode.DUPLICATE_ISSUE_ID })

        val malformedRefs =
            KeyProblemsMigrationPlanner.plan(
                sources =
                    listOf(
                        source(
                            """{"issues":[{"id":"bad-ref","title":"Bad","relatedContextIds":[""]}]}""",
                        ),
                    ),
                bindings = bindings(),
            )
        assertFalse(malformedRefs.canApply)
        assertTrue(malformedRefs.issues.any { it.code == KeyProblemsMigrationIssueCode.INVALID_ISSUE })
    }

    @Test
    fun `unresolved dependencies and canonical collisions are blocking diagnostics`() {
        val plan =
            KeyProblemsMigrationPlanner.plan(
                sources =
                    listOf(
                        source(
                            """
                            {"issues":[{
                              "id":"collision",
                              "title":"Problem",
                              "relatedContextIds":["missing-context"],
                              "relatedAttachmentIds":["missing-attachment"]
                            }]}
                            """.trimIndent(),
                        ),
                    ),
                bindings = bindings().copy(existingProblemIds = setOf("collision")),
            )

        assertFalse(plan.canApply)
        assertTrue(plan.issues.any { it.code == KeyProblemsMigrationIssueCode.UNRESOLVED_RELATED_WORKSPACE })
        assertTrue(plan.issues.any { it.code == KeyProblemsMigrationIssueCode.UNRESOLVED_ATTACHMENT })
        assertTrue(plan.issues.any { it.code == KeyProblemsMigrationIssueCode.CANONICAL_ID_COLLISION })
    }

    private fun source(payload: String) =
        LegacyKeyProblemsSource(
            contextId = "owner",
            payloadJson = payload,
            updatedAt = 100L,
        )

    private fun bindings() =
        KeyProblemsMigrationBindings(
            workspaceIdByContextId =
                mapOf(
                    "owner" to "workspace-owner",
                    "related" to "workspace-related",
                ),
            capabilityInstanceIdByWorkspaceId = mapOf("workspace-owner" to "capability"),
            knownAttachmentIds = setOf("attachment"),
        )
}
