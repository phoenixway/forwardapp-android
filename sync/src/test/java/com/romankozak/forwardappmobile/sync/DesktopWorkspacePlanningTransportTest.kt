package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.shared.contracts.contexts.DesktopWorkspaceSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedPlanningCommitmentSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedPlanningScopeSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedTacticalPlanningScopeExtensionSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedStrategicPlanningScopeExtensionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopWorkspacePlanningTransportTest {
    @Test
    fun `Desktop adapter preserves Planning absence`() {
        val bundle =
            DesktopWorkspaceSnapshotSyncAdapter().toSnapshotBundle(
                DesktopWorkspaceSnapshot(
                    contexts = emptyList(),
                    backlogItems = emptyList(),
                ),
                exportedAt = 100L,
            )

        assertNull(bundle.planningFormatVersion)
        assertNull(bundle.planningScopes)
        assertNull(bundle.planningCommitments)
    }

    @Test
    fun `Desktop adapter preserves explicit empty Planning`() {
        val bundle =
            DesktopWorkspaceSnapshotSyncAdapter().toSnapshotBundle(
                DesktopWorkspaceSnapshot(
                    contexts = emptyList(),
                    backlogItems = emptyList(),
                    planningFormatVersion = 1,
                    planningScopes = emptyList(),
                    planningCommitments = emptyList(),
                ),
                exportedAt = 100L,
            )

        assertEquals(1, bundle.planningFormatVersion)
        assertEquals(emptyList<Any>(), bundle.planningScopes)
        assertEquals(emptyList<Any>(), bundle.planningCommitments)
    }

    @Test
    fun `Desktop adapter preserves populated Planning wire fields exactly`() {
        val bundle =
            DesktopWorkspaceSnapshotSyncAdapter().toSnapshotBundle(
                DesktopWorkspaceSnapshot(
                    contexts = emptyList(),
                    backlogItems = emptyList(),
                    planningFormatVersion = 1,
                    planningScopes = listOf(scope()),
                    planningCommitments = listOf(commitment()),
                ),
                exportedAt = 100L,
            )

        val scope = requireNotNull(bundle.planningScopes).single()
        val commitment = requireNotNull(bundle.planningCommitments).single()

        assertEquals("TACTICAL_CYCLE", scope.kind)
        assertEquals("ACTIVE", scope.lifecycle)
        assertNull(scope.updatedAt)
        assertEquals(20L, scope.syncedAt)

        assertEquals("DAY.THEME", commitment.role)
        assertNull(commitment.priorityLevel)
        assertEquals("ACTIVE", commitment.status)
        assertEquals("IMPORTED", commitment.provenanceKind)
        assertEquals("desktop", commitment.provenanceSourceType)
        assertEquals("source-1", commitment.provenanceSourceId)
        assertEquals(4L, commitment.version)
        assertEquals(true, commitment.isDeleted)
    }

    @Test
    fun `Desktop adapter preserves typed Day scope extension exactly`() {
        val extension =
            SharedDayPlanningScopeExtensionSnapshot(
                scopeId = "day-scope",
                calendarDayStartMillis = 1_780_000_000_000L,
                outcome = "MISSED",
                plannedBudgetMinutes = 420L,
                createdAt = 10L,
                updatedAt = 12L,
                syncedAt = 11L,
                version = 3L,
                isDeleted = true,
            )
        val bundle =
            DesktopWorkspaceSnapshotSyncAdapter().toSnapshotBundle(
                DesktopWorkspaceSnapshot(
                    contexts = emptyList(),
                    backlogItems = emptyList(),
                    planningFormatVersion = 2,
                    planningScopes = listOf(scope().copy(id = "day-scope", kind = "DAY")),
                    planningCommitments = emptyList(),
                    dayPlanningScopeExtensions = listOf(extension),
                ),
                exportedAt = 100L,
            )

        val transported = requireNotNull(bundle.dayPlanningScopeExtensions).single()
        assertEquals(extension.scopeId, transported.scopeId)
        assertEquals(extension.calendarDayStartMillis, transported.calendarDayStartMillis)
        assertEquals(extension.outcome, transported.outcome)
        assertEquals(extension.plannedBudgetMinutes, transported.plannedBudgetMinutes)
        assertEquals(extension.createdAt, transported.createdAt)
        assertEquals(extension.updatedAt, transported.updatedAt)
        assertEquals(extension.syncedAt, transported.syncedAt)
        assertEquals(extension.version, transported.version)
        assertEquals(extension.isDeleted, transported.isDeleted)
    }

    @Test
    fun `Desktop adapter preserves typed Tactical scope extension exactly`() {
        val extension =
            SharedTacticalPlanningScopeExtensionSnapshot(
                scopeId = "cycle-scope",
                iterationType = "TIMEBOXED",
                weekKey = "2026-W41",
                closedAt = 200L,
                createdAt = 10L,
                updatedAt = 12L,
                syncedAt = 11L,
                version = 3L,
                isDeleted = true,
            )
        val bundle =
            DesktopWorkspaceSnapshotSyncAdapter().toSnapshotBundle(
                DesktopWorkspaceSnapshot(
                    contexts = emptyList(),
                    backlogItems = emptyList(),
                    planningFormatVersion = 3,
                    planningScopes = listOf(scope().copy(id = "cycle-scope")),
                    planningCommitments = emptyList(),
                    dayPlanningScopeExtensions = emptyList(),
                    tacticalPlanningScopeExtensions = listOf(extension),
                ),
                exportedAt = 100L,
            )

        val transported = requireNotNull(bundle.tacticalPlanningScopeExtensions).single()
        assertEquals(extension.scopeId, transported.scopeId)
        assertEquals(extension.iterationType, transported.iterationType)
        assertEquals(extension.weekKey, transported.weekKey)
        assertEquals(extension.closedAt, transported.closedAt)
        assertEquals(extension.createdAt, transported.createdAt)
        assertEquals(extension.updatedAt, transported.updatedAt)
        assertEquals(extension.syncedAt, transported.syncedAt)
        assertEquals(extension.version, transported.version)
        assertEquals(extension.isDeleted, transported.isDeleted)
    }

    @Test
    fun `Desktop adapter preserves typed Strategic scope extension exactly`() {
        val extension =
            SharedStrategicPlanningScopeExtensionSnapshot(
                scopeId = "arc-scope",
                legacyArcKey = " Exact/Arc Key ",
                createdAt = 10L,
                updatedAt = 12L,
                syncedAt = 11L,
                version = 3L,
                isDeleted = true,
            )
        val bundle =
            DesktopWorkspaceSnapshotSyncAdapter().toSnapshotBundle(
                DesktopWorkspaceSnapshot(
                    contexts = emptyList(),
                    backlogItems = emptyList(),
                    planningFormatVersion = 4,
                    planningScopes = listOf(scope().copy(id = "arc-scope", kind = "STRATEGIC_ARC")),
                    planningCommitments = emptyList(),
                    dayPlanningScopeExtensions = emptyList(),
                    tacticalPlanningScopeExtensions = emptyList(),
                    strategicPlanningScopeExtensions = listOf(extension),
                ),
                exportedAt = 100L,
            )

        val transported = requireNotNull(bundle.strategicPlanningScopeExtensions).single()
        assertEquals(extension.scopeId, transported.scopeId)
        assertEquals(extension.legacyArcKey, transported.legacyArcKey)
        assertEquals(extension.createdAt, transported.createdAt)
        assertEquals(extension.updatedAt, transported.updatedAt)
        assertEquals(extension.syncedAt, transported.syncedAt)
        assertEquals(extension.version, transported.version)
        assertEquals(extension.isDeleted, transported.isDeleted)
    }

    @Test
    fun `Desktop adapter rejects live Tactical MISSION without compatibility carrier`() {
        val tacticalExtension =
            SharedTacticalPlanningScopeExtensionSnapshot(
                scopeId = "scope-1",
                iterationType = "OPEN_ENDED",
                weekKey = null,
                closedAt = null,
                createdAt = 10L,
                updatedAt = 12L,
                syncedAt = 11L,
                version = 3L,
                isDeleted = false,
            )

        val failure =
            runCatching {
                DesktopWorkspaceSnapshotSyncAdapter().toSnapshotBundle(
                    DesktopWorkspaceSnapshot(
                        contexts = emptyList(),
                        backlogItems = emptyList(),
                        planningFormatVersion = 3,
                        planningScopes = listOf(scope()),
                        planningCommitments =
                            listOf(
                                commitment().copy(
                                    role = "MISSION",
                                    isDeleted = false,
                                ),
                            ),
                        dayPlanningScopeExtensions = emptyList(),
                        tacticalPlanningScopeExtensions = listOf(tacticalExtension),
                    ),
                    exportedAt = 100L,
                )
            }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(
            failure?.message?.contains(
                "without TacticalMission compatibility rows",
            ) == true,
        )
    }

    private fun scope() =
        SharedPlanningScopeSnapshot(
            id = "scope-1",
            kind = "TACTICAL_CYCLE",
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

    private fun commitment() =
        SharedPlanningCommitmentSnapshot(
            id = "commitment-1",
            scopeId = "scope-1",
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
}
