package com.romankozak.forwardappmobile.data.planning

import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot as daySnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity as dayEntity
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningScopeExtensionSyncStore
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningScopeExtensionSyncStore
import com.romankozak.forwardappmobile.data.planning.strategic.CanonicalStrategicPlanningScopeExtensionSyncStore
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlanningReferenceRoundTripRoomTest {
    @Test fun `all three source types survive JSON Restore and peer Merge without semantic recreation`() = runBlocking<Unit> {
        for (kind in PlanningReferenceTargetKind.entries) for (merge in listOf(false, true)) {
            PlanningSourceRoomFixture().use { source -> PlanningSourceRoomFixture().use { target ->
                source.seed()
                target.seed()
                val sourceId = when (kind) {
                    PlanningReferenceTargetKind.ORIENTATION -> source.base.orientation()
                    PlanningReferenceTargetKind.ASPECT -> source.base.aspect()
                    PlanningReferenceTargetKind.WORKSPACE -> "workspace"
                }
                val ref = PlanningReferenceTarget(kind, sourceId)
                source.tactical.insertMission(TacticalMission(title = "Mission", description = null, projectId = null,
                    iterationId = "cycle", deadline = 100, createdAt = 10, updatedAt = 10, version = 1), ref)
                source.strategic.addQuest(ArcQuestEntity(id = "quest", arcKey = "2026-10", title = "Quest"), ref)
                source.day.persist(ref) { DayTask(id = "task", dayPlanId = "day", title = "Task").also { source.db.dayTaskDao().insert(it) } }
                val planningStore = CanonicalPlanningSyncStore(source.db)
                val snapshot = source.base.snapshot().copy(
                    planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                    planningScopes = planningStore.loadAllScopes(), planningCommitments = planningStore.loadAllCommitments(),
                    planningReferences = source.db.planningReferenceDao().getAll(),
                    dayPlanningScopeExtensions = CanonicalDayPlanningScopeExtensionSyncStore(source.db).loadAll(),
                    tacticalPlanningScopeExtensions = CanonicalTacticalPlanningScopeExtensionSyncStore(source.db).loadAll(),
                    strategicPlanningScopeExtensions = CanonicalStrategicPlanningScopeExtensionSyncStore(source.db).loadAll(),
                    tacticalMissions = source.db.tacticalMissionDao().getAllMissionsSync().map { it.toSnapshot() },
                    arcQuests = source.db.arcQuestDao().getAllSync(),
                    dayPlans = source.db.dayPlanDao().getAllPlansSync().map { it.daySnapshot() },
                    dayTasks = source.db.dayTaskDao().getAllTasksSync().map { it.daySnapshot() },
                )
                val wire = Gson().fromJson(Gson().toJson(snapshot), SnapshotBundle::class.java)
                val strategicExtensions = source.db.strategicPlanningScopeExtensionDao().getAll()
                target.db.withTransaction {
                    target.base.admit(wire, merge)
                    wire.tacticalMissions.forEach { target.db.tacticalMissionDao().insertMissionRaw(it.toEntity()) }
                    wire.arcQuests.forEach { target.db.arcQuestDao().insert(it) }
                    wire.dayPlans.forEach { target.db.dayPlanDao().insert(it.dayEntity()) }
                    wire.dayTasks.forEach { target.db.dayTaskDao().insert(it.dayEntity()) }
                    val store = CanonicalPlanningSyncStore(target.db)
                    if (merge) store.mergeIncoming(wire.planningScopes, wire.planningCommitments)
                    else store.restoreExactDecoded(store.decodeAndValidateForRestore(wire.planningScopes!!, wire.planningCommitments!!,
                        wire.orientations!!.map { it.subjectId }.toSet()))
                    // The existing extension transports are unchanged; preserve their admitted exact rows after scopes.
                    strategicExtensions.forEach { target.db.strategicPlanningScopeExtensionDao().upsert(it) }
                    val references = CanonicalPlanningReferenceSyncStore(target.db)
                    if (merge) references.merge(wire.planningReferences) else references.restore(wire.planningReferences)
                    target.tacticalAuthority.admitCanonicalIngress()
                    target.strategicAuthority.admitCanonicalIngress()
                }
                assertThat(target.base.dao.getAllManagedSubjects().map { it.id }).containsExactlyElementsIn(source.base.dao.getAllManagedSubjects().map { it.id })
                assertThat(target.db.planningReferenceDao().getAll().map { it.copy(syncedAt = null) })
                    .containsExactlyElementsIn(wire.planningReferences!!.map { it.copy(syncedAt = null) })
                assertThat(target.db.hierarchyPlacementDao().getAll()).isEmpty()
                assertThat(target.base.dao.getAllWorkspaceBindings()).isEmpty()
            } }
        }
    }
}
