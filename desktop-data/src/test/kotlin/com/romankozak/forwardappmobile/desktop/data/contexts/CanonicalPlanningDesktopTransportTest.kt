package com.romankozak.forwardappmobile.desktop.data.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedPlanningCommitmentSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedPlanningScopeSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedTacticalPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedStrategicPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.shared.domain.contexts.WorkspaceSnapshotFixtures
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalPlanningDesktopTransportTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `Desktop merger preserves local Planning when incoming payload is absent`() {
        val local = desktopSnapshot(scopes = listOf(scope("local-scope")))
        val incoming = desktopSnapshot()

        val merged = DesktopWorkspaceSnapshotMerger().merge(local, incoming)

        assertEquals(1, merged.planningFormatVersion)
        assertEquals(listOf("local-scope"), merged.planningScopes?.map { it.id })
        assertEquals(emptyList<Any>(), merged.planningCommitments)
    }

    @Test
    fun `Desktop merger replaces Planning atomically when incoming payload is explicit`() {
        val local = desktopSnapshot(scopes = listOf(scope("local-scope")))
        val incoming =
            desktopSnapshot(
                scopes = listOf(scope("incoming-scope")),
                commitments = listOf(commitment("incoming-scope")),
            )

        val merged = DesktopWorkspaceSnapshotMerger().merge(local, incoming)

        assertEquals(listOf("incoming-scope"), merged.planningScopes?.map { it.id })
        assertEquals(listOf("commitment-1"), merged.planningCommitments?.map { it.id })
    }

    @Test
    fun `Android import re-encoding preserves Planning transport`() {
        val workspaceDir = Files.createTempDirectory("desktop-planning-import")
        val importFile = workspaceDir.resolve("incoming.json")
        importFile.parent.createDirectories()
        importFile.writeText(androidPlanningBackup())

        val store =
            DesktopWorkspaceFileStore(
                workspaceFile = workspaceDir.resolve("workspace.json"),
                fixtureLoader = StaticFixtureLoader(WorkspaceSnapshotFixtures.DESKTOP_MINIMAL_JSON),
            )

        val result = store.importSnapshot(importFile)

        assertTrue(result is ImportResult.Success)
        val persisted =
            json.decodeFromString(
                DesktopWorkspaceSnapshot.serializer(),
                store.readSnapshot(),
            )
        assertEquals(1, persisted.planningFormatVersion)
        assertEquals("scope-1", persisted.planningScopes?.single()?.id)
        assertEquals("DAY.THEME", persisted.planningCommitments?.single()?.role)
        assertNull(persisted.planningCommitments?.single()?.priorityLevel)
        assertEquals("IMPORTED", persisted.planningCommitments?.single()?.provenanceKind)
        assertEquals(true, persisted.planningCommitments?.single()?.isDeleted)
    }

    @Test
    fun `Desktop preserves typed Day extension and explicit empty v2 authority`() {
        val local =
            desktopSnapshot(
                scopes = listOf(scope("day-scope", kind = "DAY")),
                commitments = emptyList(),
                dayExtensions = listOf(dayExtension("day-scope")),
            )
        val incoming =
            desktopSnapshot(
                scopes = listOf(scope("day-scope", kind = "DAY")),
                commitments = emptyList(),
                dayExtensions = emptyList(),
            )

        val merged = DesktopWorkspaceSnapshotMerger().merge(local, incoming)

        assertEquals(2, merged.planningFormatVersion)
        assertEquals(emptyList<Any>(), merged.dayPlanningScopeExtensions)
    }

    @Test
    fun `legacy v1 incoming preserves local v2 Day extension when dependency remains`() {
        val local =
            desktopSnapshot(
                scopes = listOf(scope("day-scope", kind = "DAY")),
                commitments = emptyList(),
                dayExtensions = listOf(dayExtension("day-scope")),
            )
        val incoming =
            desktopSnapshot(
                scopes = listOf(scope("day-scope", kind = "DAY")),
                commitments = emptyList(),
            )

        val merged = DesktopWorkspaceSnapshotMerger().merge(local, incoming)

        assertEquals(2, merged.planningFormatVersion)
        assertEquals("day-scope", merged.dayPlanningScopeExtensions?.single()?.scopeId)
    }

    @Test
    fun `Desktop rejects unknown Day extension stable codes`() {
        val malformed =
            desktopSnapshot(
                scopes = listOf(scope("day-scope", kind = "DAY")),
                commitments = emptyList(),
                dayExtensions = listOf(dayExtension("day-scope").copy(outcome = "UNKNOWN")),
            )

        val failure =
            runCatching {
                DesktopWorkspaceSnapshotMerger().merge(desktopSnapshot(), malformed)
            }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `Desktop preserves typed Tactical extension and authoritative empty v3 state`() {
        val local =
            desktopSnapshot(
                scopes = listOf(scope("cycle")),
                commitments = emptyList(),
                dayExtensions = emptyList(),
                tacticalExtensions = listOf(tacticalExtension("cycle")),
            )
        val incoming =
            desktopSnapshot(
                scopes = listOf(scope("cycle")),
                commitments = emptyList(),
                dayExtensions = emptyList(),
                tacticalExtensions = emptyList(),
            )

        val merged = DesktopWorkspaceSnapshotMerger().merge(local, incoming)

        assertEquals(3, merged.planningFormatVersion)
        assertEquals(emptyList<Any>(), merged.tacticalPlanningScopeExtensions)
    }

    @Test
    fun `Desktop preserves exact Strategic arc key and authoritative empty v4 state`() {
        val local =
            desktopSnapshot(
                scopes = listOf(scope("arc", kind = "STRATEGIC_ARC")),
                commitments = emptyList(),
                dayExtensions = emptyList(),
                tacticalExtensions = emptyList(),
                strategicExtensions = listOf(strategicExtension("arc")),
            )
        val incoming =
            desktopSnapshot(
                scopes = listOf(scope("arc", kind = "STRATEGIC_ARC")),
                commitments = emptyList(),
                dayExtensions = emptyList(),
                tacticalExtensions = emptyList(),
                strategicExtensions = emptyList(),
            )

        assertEquals(" exact/Arc Key ", local.strategicPlanningScopeExtensions?.single()?.legacyArcKey)
        val merged = DesktopWorkspaceSnapshotMerger().merge(local, incoming)
        assertEquals(4, merged.planningFormatVersion)
        assertEquals(emptyList<Any>(), merged.strategicPlanningScopeExtensions)
    }

    private fun desktopSnapshot(
        scopes: List<SharedPlanningScopeSnapshot>? = null,
        commitments: List<SharedPlanningCommitmentSnapshot>? =
            if (scopes == null) null else emptyList(),
        dayExtensions: List<SharedDayPlanningScopeExtensionSnapshot>? = null,
        tacticalExtensions: List<SharedTacticalPlanningScopeExtensionSnapshot>? = null,
        strategicExtensions: List<SharedStrategicPlanningScopeExtensionSnapshot>? = null,
    ) =
        DesktopWorkspaceSnapshot(
            contexts = emptyList(),
            backlogItems = emptyList(),
            planningFormatVersion =
                when {
                    scopes == null && commitments == null -> null
                    strategicExtensions != null -> 4
                    tacticalExtensions != null -> 3
                    dayExtensions != null -> 2
                    else -> 1
                },
            planningScopes = scopes,
            planningCommitments = commitments,
            dayPlanningScopeExtensions = dayExtensions,
            tacticalPlanningScopeExtensions = tacticalExtensions,
            strategicPlanningScopeExtensions = strategicExtensions,
        )

    private fun scope(
        id: String,
        kind: String = "TACTICAL_CYCLE",
    ) =
        SharedPlanningScopeSnapshot(
            id = id,
            kind = kind,
            lifecycle = "ACTIVE",
            title = "Cycle",
            startsAt = 100L,
            endsAt = null,
            createdAt = 10L,
            updatedAt = null,
            syncedAt = 20L,
            version = 3L,
            isDeleted = false,
        )

    private fun dayExtension(scopeId: String) =
        SharedDayPlanningScopeExtensionSnapshot(
            scopeId = scopeId,
            calendarDayStartMillis = 1_780_000_000_000L,
            outcome = "COMPLETED",
            plannedBudgetMinutes = 480L,
            createdAt = 10L,
            updatedAt = 12L,
            syncedAt = null,
            version = 2L,
            isDeleted = false,
        )

    private fun tacticalExtension(scopeId: String) =
        SharedTacticalPlanningScopeExtensionSnapshot(
            scopeId = scopeId,
            iterationType = "TIMEBOXED",
            weekKey = "2026-W41",
            closedAt = null,
            createdAt = 10L,
            updatedAt = 12L,
            syncedAt = null,
            version = 2L,
            isDeleted = false,
        )

    private fun strategicExtension(scopeId: String) =
        SharedStrategicPlanningScopeExtensionSnapshot(
            scopeId = scopeId,
            legacyArcKey = " exact/Arc Key ",
            createdAt = 10L,
            updatedAt = 12L,
            syncedAt = null,
            version = 2L,
            isDeleted = false,
        )

    private fun commitment(scopeId: String) =
        SharedPlanningCommitmentSnapshot(
            id = "commitment-1",
            scopeId = scopeId,
            orientationId = "orientation-1",
            role = "DAY.THEME",
            order = 7L,
            priorityLevel = null,
            status = "ACTIVE",
            provenanceKind = "IMPORTED",
            provenanceSourceType = "desktop",
            provenanceSourceId = "source-1",
            createdAt = 11L,
            updatedAt = 12L,
            syncedAt = null,
            version = 4L,
            isDeleted = true,
        )

    private fun androidPlanningBackup(): String =
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

    private class StaticFixtureLoader(
        private val fixture: String,
    ) : DesktopWorkspaceFixtureLoader {
        override fun loadFixture(): String = fixture
    }
}
