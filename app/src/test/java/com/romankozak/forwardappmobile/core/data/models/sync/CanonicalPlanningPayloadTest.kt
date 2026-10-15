package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningCommitmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.planning.PlanningScopeSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanonicalPlanningPayloadTest {
    @Test
    fun `planning payload absence is valid and is not authority presence`() {
        val bundle = SnapshotBundle()

        requireValidCanonicalPlanningPayload(bundle)

        assertFalse(bundle.hasCanonicalPlanningPayload())
    }

    @Test
    fun `authoritative empty planning payload is present and valid`() {
        val bundle =
            SnapshotBundle(
                planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                planningScopes = emptyList(),
                planningCommitments = emptyList(),
                dayPlanningScopeExtensions = emptyList(),
                tacticalPlanningScopeExtensions = emptyList(),
                strategicPlanningScopeExtensions = emptyList(),
            )

        requireValidCanonicalPlanningPayload(bundle)

        assertTrue(bundle.hasCanonicalPlanningPayload())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `scopes without commitments fail closed`() {
        requireValidCanonicalPlanningPayload(
            SnapshotBundle(
                planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                planningScopes = emptyList(),
                planningCommitments = null,
                dayPlanningScopeExtensions = emptyList(),
                tacticalPlanningScopeExtensions = emptyList(),
                strategicPlanningScopeExtensions = emptyList(),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `commitments without scopes fail closed`() {
        requireValidCanonicalPlanningPayload(
            SnapshotBundle(
                planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                planningScopes = null,
                planningCommitments = emptyList(),
                dayPlanningScopeExtensions = emptyList(),
                tacticalPlanningScopeExtensions = emptyList(),
                strategicPlanningScopeExtensions = emptyList(),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `planning collections without generation marker fail closed`() {
        requireValidCanonicalPlanningPayload(
            SnapshotBundle(
                planningScopes = emptyList(),
                planningCommitments = emptyList(),
                dayPlanningScopeExtensions = emptyList(),
                tacticalPlanningScopeExtensions = emptyList(),
                strategicPlanningScopeExtensions = emptyList(),
            ),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unsupported planning generation fails closed`() {
        requireValidCanonicalPlanningPayload(
            SnapshotBundle(
                planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION + 1,
                planningScopes = emptyList(),
                planningCommitments = emptyList(),
                dayPlanningScopeExtensions = emptyList(),
                tacticalPlanningScopeExtensions = emptyList(),
                strategicPlanningScopeExtensions = emptyList(),
            ),
        )
    }

    @Test
    fun `non empty common planning payload is presence regardless of legacy stores`() {
        val bundle =
            SnapshotBundle(
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

        requireValidCanonicalPlanningPayload(bundle)

        assertTrue(bundle.hasCanonicalPlanningPayload())
    }

    @Test
    fun `legacy format v1 remains readable without Day extension authority`() {
        val bundle =
            SnapshotBundle(
                planningFormatVersion = LEGACY_COMMON_PLANNING_FORMAT_VERSION,
                planningScopes = emptyList(),
                planningCommitments = emptyList(),
            )

        requireValidCanonicalPlanningPayload(bundle)
        assertTrue(bundle.hasCanonicalPlanningPayload())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `format v4 without explicit extension collections fails closed`() {
        requireValidCanonicalPlanningPayload(
            SnapshotBundle(
                planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                planningScopes = emptyList(),
                planningCommitments = emptyList(),
            ),
        )
    }

    @Test
    fun `format v2 remains readable with explicit Day extension authority only`() {
        val bundle =
            SnapshotBundle(
                planningFormatVersion = DAY_EXTENSION_PLANNING_FORMAT_VERSION,
                planningScopes = emptyList(),
                planningCommitments = emptyList(),
                dayPlanningScopeExtensions = emptyList(),
            )

        requireValidCanonicalPlanningPayload(bundle)
        assertTrue(bundle.hasCanonicalPlanningPayload())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `format v4 without explicit Tactical extension collection fails closed`() {
        requireValidCanonicalPlanningPayload(
            SnapshotBundle(
                planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                planningScopes = emptyList(),
                planningCommitments = emptyList(),
                dayPlanningScopeExtensions = emptyList(),
                strategicPlanningScopeExtensions = emptyList(),
            ),
        )
    }

    @Test
    fun `format v3 remains readable without Strategic authority`() {
        val bundle =
            SnapshotBundle(
                planningFormatVersion = TACTICAL_EXTENSION_PLANNING_FORMAT_VERSION,
                planningScopes = emptyList(),
                planningCommitments = emptyList(),
                dayPlanningScopeExtensions = emptyList(),
                tacticalPlanningScopeExtensions = emptyList(),
            )

        requireValidCanonicalPlanningPayload(bundle)
        assertTrue(bundle.hasCanonicalPlanningPayload())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `format v4 requires explicit Strategic extension collection`() {
        requireValidCanonicalPlanningPayload(
            SnapshotBundle(
                planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                planningScopes = emptyList(),
                planningCommitments = emptyList(),
                dayPlanningScopeExtensions = emptyList(),
                tacticalPlanningScopeExtensions = emptyList(),
            ),
        )
    }
}
