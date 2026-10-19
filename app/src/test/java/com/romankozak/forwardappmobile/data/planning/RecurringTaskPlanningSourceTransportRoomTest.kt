package com.romankozak.forwardappmobile.data.planning

import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningScopeExtensionSyncStore
import com.romankozak.forwardappmobile.data.recurrence.*
import com.romankozak.forwardappmobile.shared.core.models.recurrence.*
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Before
import org.junit.After
import java.util.TimeZone
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecurringTaskPlanningSourceTransportRoomTest {
    private lateinit var previousZone: TimeZone
    @Before fun useUtc() { previousZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("UTC")) }
    @After fun restoreZone() { TimeZone.setDefault(previousZone) }
    @Test fun `all typed sources and retired history survive snapshot canonical Restore and Merge stores`() = runBlocking<Unit> {
        for (kind in RecurringTaskSourceKind.entries) for (merge in listOf(false, true)) {
            PlanningSourceRoomFixture().use { source -> PlanningSourceRoomFixture().use { target ->
                source.seed(); target.seed()
                val id = when (kind) {
                    RecurringTaskSourceKind.ORIENTATION -> source.base.orientation()
                    RecurringTaskSourceKind.ASPECT -> source.base.aspect()
                    RecurringTaskSourceKind.WORKSPACE -> "workspace"
                }
                val materializer = CanonicalRecurrenceMaterializationAdapter(source.db, Dispatchers.Unconfined, recurrencePlanningSources(source.db))
                val author = CanonicalTaskRecurrenceAuthoringAdapter(source.db, materializer, recurrencePlanningSources(source.db))
                val first = author.createSeriesForPlan("day", "Task", null, null, null, null, emptyList(), emptyList(), TaskPriority.MEDIUM,
                    null, 0, TaskExecutionStrictness.NORMAL, RecurrenceRule(RecurrenceFrequency.DAILY, 1, null),
                    planningSource = RecurringTaskPlanningSource(kind, id))
                author.deleteCurrentOccurrence(first)
                val planning = CanonicalPlanningSyncStore(source.db)
                val bundle = source.base.snapshot().copy(
                    planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                    planningScopes = planning.loadAllScopes(), planningCommitments = planning.loadAllCommitments(),
                    planningReferences = source.db.planningReferenceDao().getAll(),
                    dayPlanningScopeExtensions = CanonicalDayPlanningScopeExtensionSyncStore(source.db).loadAll(),
                    dayPlans = source.db.dayPlanDao().getAllPlansSync().map { it.toSnapshot() },
                    dayTasks = source.db.dayTaskDao().getAllTasksSync().map { CanonicalRecurrenceSnapshotMapper.dayTaskSnapshot(it, it.toSnapshot()) },
                    recurringSeries = source.db.canonicalRecurringSeriesDao().getAllSync().map { it.toSnapshot() },
                )
                val wire = Gson().fromJson(Gson().toJson(bundle), SnapshotBundle::class.java)
                target.db.withTransaction {
                    target.base.admit(wire, merge)
                    wire.dayPlans.forEach { target.db.dayPlanDao().insert(it.toEntity()) }
                    wire.dayTasks.forEach { target.db.dayTaskDao().insert(CanonicalRecurrenceSnapshotMapper.dayTaskEntity(it, it.toEntity())) }
                    target.db.canonicalRecurringSeriesDao().insertAll(wire.recurringSeries.map { it.toEntity() })
                    val store = CanonicalPlanningSyncStore(target.db)
                    if (merge) store.mergeIncoming(wire.planningScopes, wire.planningCommitments)
                    else store.restoreExactDecoded(store.decodeAndValidateForRestore(wire.planningScopes!!, wire.planningCommitments!!, wire.orientations!!.map { it.subjectId }.toSet()))
                    CanonicalDayPlanningScopeExtensionSyncStore(target.db).mergeIncoming(wire.dayPlanningScopeExtensions)
                    val refs = CanonicalPlanningReferenceSyncStore(target.db)
                    if (merge) refs.merge(wire.planningReferences) else refs.restore(wire.planningReferences)
                }
                assertThat(target.db.planningReferenceDao().getAll().single().isDeleted).isTrue()
                assertThat(target.db.dayTaskDao().getTaskById(first.id)!!.isDeleted).isTrue()
                assertThat((target.db.canonicalRecurringSeriesDao().getById(first.recurrenceSeriesId!!)!!.toCanonicalSeries() as RecurringTaskSeries).template.planningSource)
                    .isEqualTo(RecurringTaskPlanningSource(kind, id))
                val plan = DayPlan(id = "future", date = 86_400_000)
                target.db.dayPlanDao().insert(plan)
                target.db.planningDao().upsertScope(PlanningScopeEntity(plan.id, "DAY", "ACTIVE", null, null, null, 1, 1, null, 1, false))
                target.db.dayPlanningScopeExtensionDao().upsert(DayPlanningScopeExtensionEntity(plan.id, plan.date, null, null, 1, 1, null, 1, false))
                val resumed = CanonicalRecurrenceMaterializationAdapter(target.db, Dispatchers.Unconfined, recurrencePlanningSources(target.db))
                val next = resumed.materializeForDate(plan.date, 900).tasksToCreate.single()
                val ref = target.db.planningReferenceDao().getForOwner("DAY_TASK", next.id).single()
                assertThat(ref.targetKind).isEqualTo(kind.name)
                assertThat(ref.targetId).isEqualTo(id)
                assertThat(target.db.planningDao().getAllCommitments().count { !it.isDeleted })
                    .isEqualTo(if (kind == RecurringTaskSourceKind.ORIENTATION) 1 else 0)
                assertThat(target.db.hierarchyPlacementDao().getAll()).isEmpty()
                assertThat(target.base.dao.getAllWorkspaceBindings()).isEmpty()
            } }
        }
    }
}
