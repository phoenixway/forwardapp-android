package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningCommitmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningScopeSnapshot
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection
import org.junit.Assert.assertNull
import org.junit.Test

class CanonicalPlanningSelectiveImportTest {
    @Test
    fun `selective import never inherits canonical Planning without Planning selection surface`() {
        val source =
            SnapshotBundle(
                version = 2,
                planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                planningScopes =
                    listOf(
                        PlanningScopeSnapshot(
                            id = "scope-1",
                            kind = "DAY",
                            lifecycle = "ACTIVE",
                            title = "Today",
                            startsAt = 10L,
                            endsAt = 20L,
                            createdAt = 1L,
                            updatedAt = 2L,
                            syncedAt = null,
                            version = 3L,
                            isDeleted = false,
                        ),
                    ),
                planningCommitments =
                    listOf(
                        PlanningCommitmentSnapshot(
                            id = "commitment-1",
                            scopeId = "scope-1",
                            orientationId = "orientation-1",
                            role = "FOCUS",
                            order = 0L,
                            priorityLevel = "HIGH",
                            status = "ACTIVE",
                            provenanceKind = "MANUAL",
                            provenanceSourceType = null,
                            provenanceSourceId = null,
                            createdAt = 1L,
                            updatedAt = 2L,
                            syncedAt = null,
                            version = 4L,
                            isDeleted = false,
                        ),
                    ),
                dayPlanningScopeExtensions = emptyList(),
                tacticalPlanningScopeExtensions = emptyList(),
                strategicPlanningScopeExtensions = emptyList(),
            )

        val filtered =
            SnapshotBundleSelectiveImportFilter().filter(
                source = source,
                selection = WorkspaceSelectiveImportSelection(),
            )

        assertNull(filtered.planningFormatVersion)
        assertNull(filtered.planningScopes)
        assertNull(filtered.planningCommitments)
        assertNull(filtered.dayPlanningScopeExtensions)
        assertNull(filtered.tacticalPlanningScopeExtensions)
        assertNull(filtered.strategicPlanningScopeExtensions)
    }
}
