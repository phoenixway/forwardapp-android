package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.sync.CanonicalSnapshotTransactionWriter
import com.romankozak.forwardappmobile.core.sync.SnapshotRestoreLocalDataSourceImpl
import com.romankozak.forwardappmobile.data.orientation.storeCanonicalPayload
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningScopeExtensionSyncStore
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningScopeExtensionSyncStore
import com.romankozak.forwardappmobile.data.planning.strategic.CanonicalStrategicPlanningScopeExtensionSyncStore
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlanningReferenceExactRestoreRoomTest {
    private val owner = PlanningReferenceOwner(PlanningReferenceOwnerKind.SCOPE, "day")

    private fun restorer(f: PlanningSourceRoomFixture) = SnapshotRestoreLocalDataSourceImpl(
        database = f.db,
        writer = CanonicalSnapshotTransactionWriter { payload ->
            assertThat(payload.planningReferences).isNull() // Root replacement owns this stream after its endpoints.
            f.db.orientationDao().storeCanonicalPayload(payload, false, f.db.workspaceDao(), false)
            payload.dayPlans.forEach { f.db.dayPlanDao().insert(it.toEntity()) }
        },
        dayManagementRuntimeRepository = mockk(relaxed = true), dayPlanningAuthorityActivator = mockk(relaxed = true),
        tacticalPlanningAuthorityActivator = mockk(relaxed = true), strategicPlanningAuthorityActivator = mockk(relaxed = true),
        canonicalScoringAuthorityActivator = mockk(relaxed = true),
    )

    private suspend fun snapshot(f: PlanningSourceRoomFixture): SnapshotBundle {
        val store = CanonicalPlanningSyncStore(f.db)
        return f.base.snapshot().copy(
            planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
            planningScopes = store.loadAllScopes(), planningCommitments = store.loadAllCommitments(),
            planningReferences = f.db.planningReferenceDao().getAll(),
            dayPlanningScopeExtensions = CanonicalDayPlanningScopeExtensionSyncStore(f.db).loadAll(),
            tacticalPlanningScopeExtensions = CanonicalTacticalPlanningScopeExtensionSyncStore(f.db).loadAll(),
            strategicPlanningScopeExtensions = CanonicalStrategicPlanningScopeExtensionSyncStore(f.db).loadAll(),
            dayPlans = f.db.dayPlanDao().getAllPlansSync().map { it.toSnapshot() },
            workspaceBacklogEntries = emptyList(), workspaceInboxRecords = emptyList(),
        )
    }

    @Test fun `production exact Restore preserves references and distinguishes absent versus empty stream`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { source -> PlanningSourceRoomFixture().use { target ->
            source.seed()
            val id = source.references.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, source.base.orientation()), now = 20)
            source.db.planningReferenceDao().acknowledge(id, 1, 30)
            val bundle = snapshot(source)
            restorer(target).replaceWith(bundle)
            assertThat(target.db.planningReferenceDao().getAll()).isEqualTo(bundle.planningReferences)
            restorer(target).replaceWith(bundle.copy(planningReferences = null))
            assertThat(target.db.planningReferenceDao().getAll()).isEqualTo(bundle.planningReferences)
            restorer(target).replaceWith(bundle.copy(planningReferences = emptyList()))
            assertThat(target.db.planningReferenceDao().getAll()).isEmpty()
        } }
    }

    @Test fun `late reference validation failure rolls back the entire production replacement`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed()
            f.references.attach(owner, PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, f.base.orientation()), now = 20)
            val bundle = snapshot(f)
            val before = f.base.rows()
            val invalid = bundle.copy(planningReferences = bundle.planningReferences!!.map { it.copy(ownerId = "missing") })
            assertThat(runCatching { restorer(f).replaceWith(invalid) }.isFailure).isTrue()
            assertThat(f.base.rows()).isEqualTo(before)
        }
    }
}
