package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class WorkspaceSnapshotResolverTest {
    private val resolver = WorkspaceSnapshotResolver()

    @Test
    fun resolvesDesktopSnapshot() {
        val result = resolver.resolve(DESKTOP_SNAPSHOT_JSON)

        assertNotNull(result)
        assertEquals(WorkspaceSnapshotFormat.Desktop, result?.format)
        assertEquals(2, result?.snapshot?.contexts?.size)
        assertEquals(1, result?.snapshot?.backlogItems?.size)
    }

    @Test
    fun resolvesAndroidSnapshotBundleV2() {
        val result = resolver.resolve(WorkspaceSnapshotFixtures.ANDROID_SNAPSHOT_BUNDLE_JSON)

        assertNotNull(result)
        assertEquals(WorkspaceSnapshotFormat.AndroidSnapshotBundleV2, result?.format)
        assertEquals(2, result?.snapshot?.contexts?.size)
        assertEquals(2, result?.snapshot?.backlogItems?.size)
    }

    @Test
    fun `Android Planning absence remains absent`() {
        val result = resolver.resolve(WorkspaceSnapshotFixtures.ANDROID_SNAPSHOT_BUNDLE_JSON)

        assertNotNull(result)
        assertNull(result?.snapshot?.planningFormatVersion)
        assertNull(result?.snapshot?.planningScopes)
        assertNull(result?.snapshot?.planningCommitments)
    }

    @Test
    fun `Android Planning explicit empty remains explicit empty`() {
        val source =
            WorkspaceSnapshotFixtures.ANDROID_SNAPSHOT_BUNDLE_JSON.replaceFirst(
                "\"contexts\": [",
                """
                "planningFormatVersion": 1,
                "planningScopes": [],
                "planningCommitments": [],
                "contexts": [
                """.trimIndent(),
            )

        val result = resolver.resolve(source)

        assertNotNull(result)
        assertEquals(1, result?.snapshot?.planningFormatVersion)
        assertEquals(emptyList<Any>(), result?.snapshot?.planningScopes)
        assertEquals(emptyList<Any>(), result?.snapshot?.planningCommitments)
    }

    @Test
    fun `Android Planning populated payload preserves scalar wire fields`() {
        val source =
            WorkspaceSnapshotFixtures.ANDROID_SNAPSHOT_BUNDLE_JSON.replaceFirst(
                "\"contexts\": [",
                """
                "planningFormatVersion": 1,
                "planningScopes": [
                  {
                    "id": "scope-1",
                    "kind": "TACTICAL_CYCLE",
                    "lifecycle": "ACTIVE",
                    "title": "Cycle",
                    "startsAt": 100,
                    "endsAt": null,
                    "createdAt": 10,
                    "updatedAt": null,
                    "syncedAt": 20,
                    "version": 3,
                    "isDeleted": false
                  }
                ],
                "planningCommitments": [
                  {
                    "id": "commitment-1",
                    "scopeId": "scope-1",
                    "orientationId": "orientation-1",
                    "role": "DAY.THEME",
                    "order": 7,
                    "priorityLevel": null,
                    "status": "ACTIVE",
                    "provenanceKind": "IMPORTED",
                    "provenanceSourceType": "desktop",
                    "provenanceSourceId": "source-1",
                    "createdAt": 11,
                    "updatedAt": 12,
                    "syncedAt": null,
                    "version": 4,
                    "isDeleted": true
                  }
                ],
                "contexts": [
                """.trimIndent(),
            )

        val result = resolver.resolve(source)

        assertNotNull(result)
        val scope = requireNotNull(result?.snapshot?.planningScopes).single()
        val commitment = requireNotNull(result.snapshot.planningCommitments).single()

        assertEquals("TACTICAL_CYCLE", scope.kind)
        assertEquals("ACTIVE", scope.lifecycle)
        assertNull(scope.updatedAt)
        assertEquals(20L, scope.syncedAt)
        assertEquals("DAY.THEME", commitment.role)
        assertNull(commitment.priorityLevel)
        assertEquals("IMPORTED", commitment.provenanceKind)
        assertEquals("desktop", commitment.provenanceSourceType)
        assertEquals("source-1", commitment.provenanceSourceId)
        assertEquals(4L, commitment.version)
        assertEquals(true, commitment.isDeleted)
    }

    @Test
    fun `Android Planning v2 preserves typed Day extension`() {
        val source = androidDayExtensionPayload(outcome = "COMPLETED")

        val result = resolver.resolve(source)

        assertNotNull(result)
        assertEquals(2, result?.snapshot?.planningFormatVersion)
        val extension = requireNotNull(result?.snapshot?.dayPlanningScopeExtensions).single()
        assertEquals("day-scope", extension.scopeId)
        assertEquals(1_780_000_000_000L, extension.calendarDayStartMillis)
        assertEquals("COMPLETED", extension.outcome)
        assertEquals(480L, extension.plannedBudgetMinutes)
    }

    @Test
    fun `Android Planning v2 unknown Day extension code fails closed`() {
        assertNull(resolver.resolve(androidDayExtensionPayload(outcome = "UNKNOWN")))
    }

    @Test
    fun `Android Planning partial envelope fails closed`() {
        val source =
            WorkspaceSnapshotFixtures.ANDROID_SNAPSHOT_BUNDLE_JSON.replaceFirst(
                "\"contexts\": [",
                """
                "planningFormatVersion": 1,
                "planningScopes": [],
                "contexts": [
                """.trimIndent(),
            )

        assertNull(resolver.resolve(source))
    }

    @Test
    fun `Android Planning v3 preserves typed Tactical extension`() {
        val result = resolver.resolve(androidTacticalExtensionPayload(includeTacticalExtension = true))

        assertNotNull(result)
        assertEquals(3, result?.snapshot?.planningFormatVersion)

        val extension =
            requireNotNull(result?.snapshot?.tacticalPlanningScopeExtensions).single()

        assertEquals("cycle-scope", extension.scopeId)
        assertEquals("TIMEBOXED", extension.iterationType)
        assertEquals("2026-W41", extension.weekKey)
        assertEquals(200L, extension.closedAt)
        assertEquals(3L, extension.version)
    }

    @Test
    fun `Android Planning v3 without Tactical extension fails closed`() {
        assertNull(
            resolver.resolve(
                androidTacticalExtensionPayload(includeTacticalExtension = false),
            ),
        )
    }

    @Test
    fun resolvesAndroidLegacyDatabase() {
        val result = resolver.resolve(WorkspaceSnapshotFixtures.ANDROID_LEGACY_DATABASE_JSON)

        assertNotNull(result)
        assertEquals(WorkspaceSnapshotFormat.AndroidLegacyDatabase, result?.format)
        assertEquals(2, result?.snapshot?.contexts?.size)
        assertEquals(2, result?.snapshot?.backlogItems?.size)
    }

    @Test
    fun returnsNullForUnsupportedPayload() {
        assertNull(resolver.resolve("{\"broken\":true}"))
    }

    @Test
    fun `canonical Android DayTask owner survives Desktop import`() {
        val admittedOwners =
            listOf(
                """{"id":"fk3-owner","isDeleted":false,"provenance":"STANDALONE","sourceContextId":null}""" to "[]",
                """{"id":"fk3-owner","isDeleted":false,"provenance":"CANONICAL_ONLY","sourceContextId":null}""" to "[]",
                """{"id":"fk3-owner","isDeleted":false,"provenance":"CONTEXT_BACKED","sourceContextId":"fk3-owner"}""" to
                    """[{"id":"fk3-owner","name":"Context","isDeleted":false}]""",
                """{"id":"fk3-owner","isDeleted":false,"provenance":"CANONICAL_ONLY","sourceContextId":null}""" to
                    """[{"id":"fk3-owner","name":"Retired","isDeleted":true}]""",
            )

        admittedOwners.forEach { (workspace, contexts) ->
            val result =
                resolver.resolve(
                    fk3AndroidOwnerPayload(
                        workspaces = "[$workspace]",
                        contexts = contexts,
                    ),
                )

            val task = requireNotNull(result).snapshot.dayTasks.single()
            assertEquals("fk3-owner", task.projectId)
            assertEquals(listOf("fk3-owner"), task.linkedProjectIds)
        }
    }

    @Test
    fun `canonical Android DayTask owner rejects missing deleted and unproven Workspace`() {
        val rejectedOwners =
            listOf(
                "[]" to "[]",
                """[{"id":"fk3-owner","isDeleted":true,"provenance":"STANDALONE","sourceContextId":null}]""" to "[]",
                """[{"id":"fk3-owner","isDeleted":false,"provenance":"STANDALONE","sourceContextId":"other"}]""" to "[]",
                """[{"id":"fk3-owner","isDeleted":false,"provenance":"CONTEXT_BACKED","sourceContextId":"other"}]""" to "[]",
                """[{"id":"fk3-owner","isDeleted":false,"provenance":"CANONICAL_ONLY","sourceContextId":null}]""" to
                    """[{"id":"fk3-owner","name":"Live","isDeleted":false}]""",
                """[{"id":"fk3-owner","isDeleted":false,"provenance":"CONTEXT_BACKED","sourceContextId":"fk3-owner"}]""" to
                    """[{"id":"fk3-owner","name":"Retired","isDeleted":true}]""",
            )

        rejectedOwners.forEach { (workspaces, contexts) ->
            assertNull(
                resolver.resolve(
                    fk3AndroidOwnerPayload(
                        workspaces = workspaces,
                        contexts = contexts,
                    ),
                ),
            )
        }
    }

    private fun fk3AndroidOwnerPayload(
        workspaces: String,
        contexts: String,
    ): String {
        val root =
            Json.parseToJsonElement(
                WorkspaceSnapshotFixtures.ANDROID_SNAPSHOT_BUNDLE_JSON,
            ).jsonObject.toMutableMap()

        // The Android backup envelope owns the canonical snapshotBundle.
        // Mutating root fields would leave the imported bundle unchanged.
        val fields =
            requireNotNull(root["snapshotBundle"])
                .jsonObject
                .toMutableMap()

        fields["contexts"] = Json.parseToJsonElement(contexts)
        fields["workspaces"] = Json.parseToJsonElement(workspaces)
        fields["dayPlans"] =
            Json.parseToJsonElement(
                """[{"id":"fk3-plan","date":1}]""",
            )
        fields["dayTasks"] =
            Json.parseToJsonElement(
                """[{"id":"fk3-task","dayPlanId":"fk3-plan","title":"Owner regression","projectId":"fk3-owner","linkedProjectIds":["fk3-owner"]}]""",
            )

        root["snapshotBundle"] = JsonObject(fields)
        return JsonObject(root).toString()
    }

    @Test
    fun `Android Planning v4 preserves typed Strategic extension`() {
        val result =
            resolver.resolve(
                androidStrategicExtensionPayload(includeStrategicExtension = true),
            )

        assertNotNull(result)
        assertEquals(4, result?.snapshot?.planningFormatVersion)

        val extension =
            requireNotNull(
                result?.snapshot?.strategicPlanningScopeExtensions,
            ).single()

        assertEquals("strategic-scope", extension.scopeId)
        assertEquals(" exact/Arc Key ", extension.legacyArcKey)
        assertEquals(4L, extension.version)
    }

    @Test
    fun `Android Planning v4 without Strategic extension fails closed`() {
        assertNull(
            resolver.resolve(
                androidStrategicExtensionPayload(includeStrategicExtension = false),
            ),
        )
    }

    private fun androidStrategicExtensionPayload(
        includeStrategicExtension: Boolean,
    ): String {
        val root =
            Json.parseToJsonElement(
                androidTacticalExtensionPayload(includeTacticalExtension = true),
            ).jsonObject.toMutableMap()

        val bundle =
            requireNotNull(root["snapshotBundle"])
                .jsonObject
                .toMutableMap()

        bundle["planningFormatVersion"] = JsonPrimitive(4)

        val strategicScope =
            Json.parseToJsonElement(
                """
                {
                  "id": "strategic-scope",
                  "kind": "STRATEGIC_ARC",
                  "lifecycle": "ACTIVE",
                  "title": "Strategic",
                  "startsAt": 100,
                  "endsAt": null,
                  "createdAt": 10,
                  "updatedAt": 12,
                  "syncedAt": null,
                  "version": 4,
                  "isDeleted": false
                }
                """.trimIndent(),
            )

        val scopes = requireNotNull(bundle["planningScopes"]) as JsonArray
        bundle["planningScopes"] = JsonArray(scopes + strategicScope)

        if (includeStrategicExtension) {
            bundle["strategicPlanningScopeExtensions"] =
                Json.parseToJsonElement(
                    """
                    [
                      {
                        "scopeId": "strategic-scope",
                        "legacyArcKey": " exact/Arc Key ",
                        "createdAt": 10,
                        "updatedAt": 12,
                        "syncedAt": null,
                        "version": 4,
                        "isDeleted": false
                      }
                    ]
                    """.trimIndent(),
                )
        }

        root["snapshotBundle"] = JsonObject(bundle)
        return JsonObject(root).toString()
    }

    private fun androidTacticalExtensionPayload(
        includeTacticalExtension: Boolean,
    ): String {
        val tacticalExtension =
            if (includeTacticalExtension) {
                """
                "tacticalPlanningScopeExtensions": [
                  {
                    "scopeId": "cycle-scope",
                    "iterationType": "TIMEBOXED",
                    "weekKey": "2026-W41",
                    "closedAt": 200,
                    "createdAt": 10,
                    "updatedAt": 12,
                    "syncedAt": 11,
                    "version": 3,
                    "isDeleted": false
                  }
                ],
                """.trimIndent()
            } else {
                ""
            }

        return WorkspaceSnapshotFixtures.ANDROID_SNAPSHOT_BUNDLE_JSON.replaceFirst(
            "\"contexts\": [",
            """
            "planningFormatVersion": 3,
            "planningScopes": [
              {
                "id": "cycle-scope",
                "kind": "TACTICAL_CYCLE",
                "lifecycle": "ACTIVE",
                "title": "Cycle",
                "startsAt": 100,
                "endsAt": 300,
                "createdAt": 10,
                "updatedAt": 12,
                "syncedAt": null,
                "version": 3,
                "isDeleted": false
              }
            ],
            "planningCommitments": [],
            "dayPlanningScopeExtensions": [],
            $tacticalExtension
            "contexts": [
            """.trimIndent(),
        )
    }

    private fun androidDayExtensionPayload(outcome: String): String =
        WorkspaceSnapshotFixtures.ANDROID_SNAPSHOT_BUNDLE_JSON.replaceFirst(
            "\"contexts\": [",
            """
            "planningFormatVersion": 2,
            "planningScopes": [
              {
                "id": "day-scope",
                "kind": "DAY",
                "lifecycle": "ACTIVE",
                "title": null,
                "startsAt": null,
                "endsAt": null,
                "createdAt": 10,
                "updatedAt": 12,
                "syncedAt": null,
                "version": 2,
                "isDeleted": false
              }
            ],
            "planningCommitments": [],
            "dayPlanningScopeExtensions": [
              {
                "scopeId": "day-scope",
                "calendarDayStartMillis": 1780000000000,
                "outcome": "$outcome",
                "plannedBudgetMinutes": 480,
                "createdAt": 10,
                "updatedAt": 12,
                "syncedAt": null,
                "version": 2,
                "isDeleted": false
              }
            ],
            "contexts": [
            """.trimIndent(),
        )

    private companion object {
        const val DESKTOP_SNAPSHOT_JSON =
            """
            {
              "contexts": [
                {
                  "id": "root",
                  "name": "Root",
                  "description": "Workspace root",
                  "parentId": null,
                  "status": "Planning",
                  "defaultView": "Backlog",
                  "score": 0,
                  "isCompleted": false
                },
                {
                  "id": "project",
                  "name": "Desktop App",
                  "description": "Desktop work",
                  "parentId": "root",
                  "status": "InProgress",
                  "defaultView": "Dashboard",
                  "score": 10,
                  "isCompleted": false
                }
              ],
              "backlogItems": [
                {
                  "id": "item-1",
                  "contextId": "project",
                  "title": "Ship explorer",
                  "details": "Use shared store",
                  "kind": "Task",
                  "priority": "High",
                  "isDone": false
                }
              ]
            }
            """

    }
}
