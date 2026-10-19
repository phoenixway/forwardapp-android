package com.romankozak.forwardappmobile.data.planning

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot
import com.romankozak.forwardappmobile.data.recurrence.*
import com.romankozak.forwardappmobile.data.planning.day.dayTaskParticipationId
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
class RecurringTaskPlanningSourceRoomTest {
    private lateinit var previousZone: TimeZone
    @Before fun useUtc() { previousZone = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("UTC")) }
    @After fun restoreZone() { TimeZone.setDefault(previousZone) }
    private val daily = RecurrenceRule(RecurrenceFrequency.DAILY, 1, null)
    private fun materializer(f: PlanningSourceRoomFixture) = CanonicalRecurrenceMaterializationAdapter(f.db, Dispatchers.Unconfined, recurrencePlanningSources(f.db))
    private fun author(f: PlanningSourceRoomFixture) = CanonicalTaskRecurrenceAuthoringAdapter(f.db, materializer(f), recurrencePlanningSources(f.db))
    private suspend fun task(f: PlanningSourceRoomFixture, kind: PlanningReferenceTargetKind = PlanningReferenceTargetKind.ORIENTATION): DayTask {
        val id = when (kind) {
            PlanningReferenceTargetKind.ORIENTATION -> f.base.orientation()
            PlanningReferenceTargetKind.ASPECT -> f.base.aspect()
            PlanningReferenceTargetKind.WORKSPACE -> "workspace"
        }
        return f.day.persist(PlanningReferenceTarget(kind, id)) {
            DayTask(id = "source", dayPlanId = "day", title = "Task").also { f.db.dayTaskDao().insert(it) }
        }
    }
    private suspend fun nextDay(f: PlanningSourceRoomFixture, id: String = "tomorrow", date: Long = 86_400_000): DayPlan {
        val plan = DayPlan(id = id, date = date)
        f.db.dayPlanDao().insert(plan)
        f.db.planningDao().upsertScope(PlanningScopeEntity(id, "DAY", "ACTIVE", null, null, null, 1, 1, null, 1, false))
        f.db.dayPlanningScopeExtensionDao().upsert(DayPlanningScopeExtensionEntity(id, date, null, null, 1, 1, null, 1, false))
        return plan
    }
    private suspend fun convert(f: PlanningSourceRoomFixture, t: DayTask) = author(f).convertOneOffToSeries(
        t, t.title, t.description, t.goalId, t.logicalProjectId, t.taskType, t.linkedProjectIds.orEmpty(),
        t.linkedAttachmentIds.orEmpty(), t.priority, t.estimatedDurationMinutes, t.points, t.executionStrictness, daily,
    )
    private suspend fun split(f: PlanningSourceRoomFixture, t: DayTask) = author(f).splitSeriesFromOccurrence(
        t, "Split", t.description, t.goalId, t.logicalProjectId, t.taskType, t.linkedProjectIds.orEmpty(),
        t.linkedAttachmentIds.orEmpty(), t.priority, t.estimatedDurationMinutes, t.points, t.executionStrictness, daily,
    )
    private suspend fun refs(f: PlanningSourceRoomFixture, id: String) = f.db.planningReferenceDao().getForOwner("DAY_TASK", id).filterNot { it.isDeleted }
    private suspend fun occurrence(f: PlanningSourceRoomFixture, seriesId: String, planId: String) =
        f.db.canonicalRecurringSeriesDao().getTaskOccurrencesForSeries(seriesId).single { it.dayPlanId == planId }

    @Test fun `conversion preserves source and related retires old participation and keeps identity separate`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val old = task(f)
            f.references.attach(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, old.id), PlanningReferenceTarget(PlanningReferenceTargetKind.WORKSPACE, "workspace"))
            val subjects = f.base.dao.getAllManagedSubjects()
            val beforeRefs = refs(f, old.id)
            val allowed = setOf("day_tasks", "canonical_recurring_series", "planning_commitments", "planning_references")
            val isolated = f.base.rows().filterKeys { it !in allowed }
            val next = convert(f, old)
            assertThat(next.id).isNotEqualTo(old.id)
            assertThat(f.db.dayTaskDao().getTaskById(old.id)!!.isDeleted).isTrue()
            assertThat(refs(f, old.id)).isEmpty()
            assertThat(refs(f, next.id).map { Triple(it.targetKind, it.targetId, it.role) }).containsExactlyElementsIn(beforeRefs.map { Triple(it.targetKind, it.targetId, it.role) })
            val oldCommitment = f.db.planningDao().getCommitment(dayTaskParticipationId(old.id).value)!!
            val nextCommitment = f.db.planningDao().getCommitment(dayTaskParticipationId(next.id).value)!!
            assertThat(oldCommitment.isDeleted).isTrue()
            assertThat(nextCommitment.orientationId).isEqualTo(oldCommitment.orientationId)
            assertThat(f.base.dao.getAllManagedSubjects()).isEqualTo(subjects)
            assertThat(f.db.hierarchyPlacementDao().getAll()).isEmpty()
            assertThat(f.base.rows().filterKeys { it !in allowed }).isEqualTo(isolated)
        }
    }

    @Test fun `future orientation occurrence has same source new day participation and inert replay`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val old = task(f); val target = nextDay(f); val first = convert(f, old)
            materializer(f).materializeForDate(target.date, 500)
            val next = occurrence(f, first.recurrenceSeriesId!!, target.id)
            val source = refs(f, first.id).single()
            assertThat(refs(f, next.id).single().targetId).isEqualTo(source.targetId)
            val participation = f.db.planningDao().getCommitment(dayTaskParticipationId(next.id).value)!!
            assertThat(participation.scopeId).isEqualTo(target.id)
            assertThat(participation.orientationId).isEqualTo(source.targetId)
            assertThat(participation.role).isEqualTo("DAY.TASK")
            val before = f.base.rows()
            materializer(f).materializeForDate(target.date, 600)
            assertThat(f.base.rows()).isEqualTo(before)
        }
    }

    @Test fun `aspect and workspace source propagate without orientation participation or semantic writes`() = runBlocking<Unit> {
        for (kind in listOf(PlanningReferenceTargetKind.ASPECT, PlanningReferenceTargetKind.WORKSPACE)) {
            PlanningSourceRoomFixture().use { f ->
                f.seed(); val old = task(f, kind); val target = nextDay(f); val first = convert(f, old)
                val subjects = f.base.dao.getAllManagedSubjects()
                materializer(f).materializeForDate(target.date, 500)
                val next = occurrence(f, first.recurrenceSeriesId!!, target.id)
                assertThat(refs(f, next.id).single().targetKind).isEqualTo(kind.name)
                assertThat(f.db.planningDao().getAllCommitments()).isEmpty()
                assertThat(f.base.dao.getAllManagedSubjects()).isEqualTo(subjects)
            }
        }
    }

    @Test fun `split transfers exact owner references and tombstones old participations`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val first = convert(f, task(f)); val tomorrow = nextDay(f)
            materializer(f).materializeForDate(tomorrow.date, 500)
            val oldFuture = occurrence(f, first.recurrenceSeriesId!!, tomorrow.id)
            val replacement = split(f, oldFuture)
            assertThat(refs(f, oldFuture.id)).isEmpty()
            assertThat(refs(f, replacement.id)).hasSize(1)
            assertThat(f.db.planningDao().getCommitment(dayTaskParticipationId(oldFuture.id).value)!!.isDeleted).isTrue()
            assertThat(f.db.planningDao().getCommitment(dayTaskParticipationId(replacement.id).value)!!.isDeleted).isFalse()
            assertThat((author(f).getSeriesForOccurrence(replacement)!!).template.planningSource).isEqualTo(author(f).getSeriesForOccurrence(first)!!.template.planningSource)
            assertThat(f.db.dayTaskDao().getTaskById(first.id)!!.isDeleted).isFalse()
        }
    }

    @Test fun `delete and stop retire references participation and block rematerialization`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val first = convert(f, task(f)); val target = nextDay(f)
            materializer(f).materializeForDate(target.date, 500)
            val next = occurrence(f, first.recurrenceSeriesId!!, target.id)
            author(f).deleteCurrentOccurrence(next)
            assertThat(refs(f, next.id)).isEmpty()
            assertThat(f.db.planningDao().getCommitment(dayTaskParticipationId(next.id).value)!!.isDeleted).isTrue()
            materializer(f).materializeForDate(target.date, 600)
            assertThat(occurrence(f, first.recurrenceSeriesId!!, target.id).isDeleted).isTrue()
            author(f).stopSeriesFromOccurrence(first)
            assertThat(refs(f, first.id)).isEmpty()
            assertThat(f.db.planningDao().getAllCommitments().all { it.isDeleted }).isTrue()
        }
    }

    @Test fun `detach preserves source on standalone replacement only`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val first = convert(f, task(f)); val next = author(f).detachCurrentOccurrence(first)
            assertThat(next.recurrenceSeriesId).isNull()
            assertThat(refs(f, first.id)).isEmpty()
            assertThat(refs(f, next.id)).hasSize(1)
            assertThat(f.db.planningDao().getCommitment(dayTaskParticipationId(first.id).value)!!.isDeleted).isTrue()
            assertThat(f.db.planningDao().getCommitment(dayTaskParticipationId(next.id).value)!!.isDeleted).isFalse()
        }
    }

    @Test fun `late planning failure rolls back series conversion task and all references`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val old = task(f)
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_recurring BEFORE INSERT ON planning_commitments WHEN NEW.id != '${dayTaskParticipationId(old.id).value}' BEGIN SELECT RAISE(ABORT, 'late failure'); END")
            val before = f.base.rows()
            assertThat(runCatching { convert(f, old) }.isFailure).isTrue()
            assertThat(f.base.rows()).isEqualTo(before)
        }
    }

    @Test fun `snapshot json preserves typed template and old missing field remains untyped`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val first = convert(f, task(f, PlanningReferenceTargetKind.WORKSPACE))
            val entity = f.db.canonicalRecurringSeriesDao().getById(first.recurrenceSeriesId!!)!!
            val gson = Gson()
            val wire = gson.fromJson(gson.toJson(entity.toSnapshot()), CanonicalRecurringSeriesSnapshot::class.java)
            assertThat((wire.toEntity().toCanonicalSeries() as RecurringTaskSeries).template.planningSource).isEqualTo(RecurringTaskPlanningSource(RecurringTaskSourceKind.WORKSPACE, "workspace"))
            val legacy = (entity.toCanonicalSeries() as RecurringTaskSeries).copy(template = (entity.toCanonicalSeries() as RecurringTaskSeries).template.copy(planningSource = null)).toAndroidEntity()
            assertThat((legacy.toCanonicalSeries() as RecurringTaskSeries).template.planningSource).isNull()
            val invalid = entity.copy(templateJson = entity.templateJson.replace("WORKSPACE", "CONTEXT"))
            assertThat(runCatching { invalid.toCanonicalSeries() }.isFailure).isTrue()
        }
    }

    @Test fun `series edit keeps source for future occurrences and current edit leaves references alone`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val first = convert(f, task(f)); val target = nextDay(f)
            val refs = refs(f, first.id)
            val edited = author(f).updateCurrentOccurrence(first, "Override", null, null, null, null, emptyList(), emptyList(), first.priority, null, 0, first.executionStrictness)
            assertThat(refs(f, first.id)).isEqualTo(refs)
            author(f).updateSeriesTemplate(edited, "Series", null, null, null, null, emptyList(), emptyList(), first.priority, null, 0, first.executionStrictness)
            materializer(f).materializeForDate(target.date, 500)
            val future = occurrence(f, first.recurrenceSeriesId!!, target.id)
            assertThat(future.title).isEqualTo("Series")
            assertThat(refs(f, future.id).single().targetId).isEqualTo(refs.single().targetId)
        }
    }

    @Test fun `missing source prevents future occurrence without inventing identity`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val first = convert(f, task(f, PlanningReferenceTargetKind.WORKSPACE)); val target = nextDay(f)
            f.db.openHelper.writableDatabase.execSQL("UPDATE workspaces SET isDeleted=1 WHERE id='workspace'")
            val result = materializer(f).materializeForDate(target.date, 500)
            assertThat(result.tasksToCreate).isEmpty()
            assertThat(f.db.canonicalRecurringSeriesDao().getTaskOccurrencesForSeries(first.recurrenceSeriesId!!)).hasSize(1)
            assertThat(f.db.planningDao().getAllCommitments()).isEmpty()
        }
    }

    @Test fun `direct native series source and requested order are fused with participation`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val id = f.base.orientation()
            val first = author(f).createSeriesForPlan("day", "Native", null, null, null, null, emptyList(), emptyList(), TaskPriority.MEDIUM,
                null, 0, TaskExecutionStrictness.NORMAL, daily, order = -12,
                planningSource = RecurringTaskPlanningSource(RecurringTaskSourceKind.ORIENTATION, id))
            assertThat(refs(f, first.id).single().targetId).isEqualTo(id)
            assertThat(f.db.planningDao().getCommitment(dayTaskParticipationId(first.id).value)!!.commitmentOrder).isEqualTo(-12)
        }
    }

    @Test fun `materialization planning conflict rolls back task creation`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val first = convert(f, task(f)); val target = nextDay(f)
            val original = f.db.planningDao().getCommitment(dayTaskParticipationId(first.id).value)!!
            f.db.planningDao().upsertCommitment(original.copy(id = "conflict", scopeId = target.id))
            val before = f.base.rows()
            assertThat(runCatching { materializer(f).materializeForDate(target.date, 500) }.isFailure).isTrue()
            assertThat(f.base.rows()).isEqualTo(before)
        }
    }

    @Test fun `retirement remains dirty after stale reference participation and series ACK`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val first = convert(f, task(f))
            val reference = refs(f, first.id).single()
            val participation = f.db.planningDao().getCommitment(dayTaskParticipationId(first.id).value)!!
            val series = f.db.canonicalRecurringSeriesDao().getById(first.recurrenceSeriesId!!)!!
            author(f).stopSeriesFromOccurrence(first)
            f.db.planningReferenceDao().acknowledge(reference.id, reference.version, Long.MAX_VALUE)
            f.db.planningDao().markCommitmentSyncedIfVersionMatches(participation.id, participation.version, Long.MAX_VALUE)
            f.db.canonicalRecurringSeriesDao().markSyncedIfVersionMatches(series.id, series.version, Long.MAX_VALUE)
            assertThat(f.db.planningReferenceDao().get(reference.id)!!.syncedAt).isNull()
            assertThat(f.db.planningDao().getCommitment(participation.id)!!.syncedAt).isNull()
            assertThat(f.db.canonicalRecurringSeriesDao().getById(series.id)!!.syncedAt).isNull()
            assertThat(f.db.planningReferenceDao().get(reference.id)!!.isDeleted).isTrue()
        }
    }

    @Test fun `existing imported occurrence receives missing explicit template source without duplicate task`() = runBlocking<Unit> {
        PlanningSourceRoomFixture().use { f ->
            f.seed(); val first = convert(f, task(f)); val target = nextDay(f)
            val imported = first.copy(id = "recurrence:TASK:${first.recurrenceSeriesId}:1970-01-02", dayPlanId = target.id, recurrenceOccurrenceDayKey = "1970-01-02")
            f.db.dayTaskDao().insert(imported)
            assertThat(materializer(f).materializeForDate(target.date, 500).tasksToCreate).isEmpty()
            assertThat(refs(f, imported.id).single().targetId).isEqualTo(refs(f, first.id).single().targetId)
            val rows = f.base.rows()
            materializer(f).materializeForDate(target.date, 600)
            assertThat(f.base.rows()).isEqualTo(rows)
            val edge = refs(f, imported.id).single()
            f.references.remove(edge.id, edge.version, 700)
            val retired = f.base.rows()
            materializer(f).materializeForDate(target.date, 800)
            assertThat(f.base.rows()).isEqualTo(retired)
        }
    }
}
