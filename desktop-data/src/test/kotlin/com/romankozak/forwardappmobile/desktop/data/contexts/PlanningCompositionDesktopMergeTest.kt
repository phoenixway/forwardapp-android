package com.romankozak.forwardappmobile.desktop.data.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.*
import org.junit.Assert.*
import org.junit.Test

class PlanningCompositionDesktopMergeTest {
    private val row = SharedPlanningCommitmentRelationSnapshot("edge", "a", "b", "COMPONENT_OF", 0, 1, 1, null, false, 1)
    private fun snapshot(edge: SharedPlanningCommitmentRelationSnapshot = row) = DesktopWorkspaceSnapshot(emptyList(), emptyList(),
        planningFormatVersion = 5, planningScopes = listOf(SharedPlanningScopeSnapshot("scope", "DAY", "ACTIVE", null, null, null, 1, 1, null, 1, false)),
        planningCommitments = listOf("a", "b").map { SharedPlanningCommitmentSnapshot(it, "scope", it, "FOCUS", 0, null, "ACTIVE", "MANUAL", null, null, 1, 1, null, 1, false) },
        planningCommitmentRelations = listOf(edge), dayPlanningScopeExtensions = emptyList(), tacticalPlanningScopeExtensions = emptyList(), strategicPlanningScopeExtensions = emptyList())
    @Test fun stalePayloadCannotResurrectComposition() {
        val newer = row.copy(isDeleted = true, version = 2, updatedAt = 2)
        val result = DesktopWorkspaceSnapshotMerger().merge(snapshot(newer), snapshot())
        assertEquals(listOf(newer), result.planningCommitmentRelations)
        assertEquals(5, result.planningFormatVersion)
    }
    @Test fun equalVersionDivergenceRejected() {
        assertTrue(runCatching { DesktopWorkspaceSnapshotMerger().merge(snapshot(), snapshot(row.copy(order = 2))) }.exceptionOrNull() is IllegalStateException)
    }
    @Test fun olderTransportDoesNotEraseRelations() {
        val older = snapshot().copy(planningFormatVersion = 4, planningCommitmentRelations = null)
        assertEquals(listOf(row), DesktopWorkspaceSnapshotMerger().merge(snapshot(), older).planningCommitmentRelations)
    }
}
