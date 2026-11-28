package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.CURRENT_PLANNING_FORMAT_VERSION
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalSnapshot
import com.romankozak.forwardappmobile.core.sync.createRoomMergeSubject
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.*
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.orientation.*
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningSyncStore
import com.romankozak.forwardappmobile.data.planning.day.*
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.data.recurrence.*
import com.romankozak.forwardappmobile.shared.core.models.recurrence.*
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DayThemeCarryForwardRoomTest {
    private val today = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 19) }.timeInMillis
    private fun previousDay() = Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis

    private suspend fun fixture(keyedTarget: Boolean = false, block: suspend (SemanticDeletionRoomFixture, ThemeOrientationBridge, CanonicalDayThemePlanningAdapter, CanonicalDayPlanningAuthorityActivator) -> Unit) {
        SemanticDeletionRoomFixture().use { f ->
            val db = f.db
            val dao = db.canonicalDayThemeDao()
            db.dayPlanDao().insert(DayPlan(id = "yesterday", date = previousDay(), version = 1))
            if (!keyedTarget) db.dayPlanDao().insert(DayPlan(id = "today", date = today, version = 1))
            for ((id, carry) in listOf("carry" to true, "oneoff" to false)) {
                dao.upsertThemeDefinitions(listOf(ThemeDefinitionSnapshot(id, id, 1, "target", "", carry, false, 1, 1, null, 1, false).toCanonicalEntity()))
                dao.upsertDayThemes(listOf(DayThemeSnapshot(canonicalDayThemeId("yesterday", id), id, "yesterday", 25, 0, true, 1, 1, null, 1, false).toCanonicalEntity()))
            }
            val bootstrapper = CanonicalOrientationBootstrapper(db, db.orientationDao(), db.goalDao(), db.mainBeaconDao(), db.arcQuestDao(), dao,
                CanonicalDayThemeBootstrapper(db, db.dayThemeDocumentDao(), dao))
            val bridge = ThemeOrientationBridge(db.orientationDao(), dao, bootstrapper)
            val authority = CanonicalDayPlanningAuthorityActivator(db, db.dayPlanDao(), dao, db.orientationDao(), db.planningDao(),
                db.dayPlanningScopeExtensionDao(), db.dayPlanningAuthorityStateDao(), bridge)
            authority.ensureActivated(10)
            if (keyedTarget) CanonicalDayPlanningAdapter(db, db.dayPlanDao(), db.planningDao(),
                db.dayPlanningScopeExtensionDao(), authority).createOrUpdate(today, null, today)
            block(f, bridge, CanonicalDayThemePlanningAdapter(db.planningDao(), bridge, authority, db), authority)
        }
    }

    @Test fun `independent carries from the same original theme converge and reject unequal budget or origin`() = runBlocking<Unit> {
        val targetId = PersonalPlanningDay.at(today).scopeId
        suspend fun exportCarry(db: AppDatabase): SnapshotBundle {
            val themes = db.canonicalDayThemeDao().getDayThemesForDay(targetId).map { it.toCanonicalSnapshot() }
            return SnapshotBundle(version = 2, planningFormatVersion = CURRENT_PLANNING_FORMAT_VERSION,
                planningScopes = emptyList(), dayPlanningScopeExtensions = emptyList(),
                tacticalPlanningScopeExtensions = emptyList(), strategicPlanningScopeExtensions = emptyList(),
                planningCommitments = CanonicalPlanningSyncStore(db).loadAllCommitments().filter { it.scopeId == targetId },
                themeDefinitions = emptyList(), dayThemes = themes, dayThemeAssignmentDocuments = emptyList())
        }
        fixture(keyedTarget = true) { a, bridgeA, planningA, _ ->
            fixture(keyedTarget = true) { b, bridgeB, planningB, _ ->
                assertThat(carryForwardDayThemes(a.db, a.db.canonicalDayThemeDao(), bridgeA, planningA, targetId, today + 1)).isEqualTo(1)
                assertThat(carryForwardDayThemes(b.db, b.db.canonicalDayThemeDao(), bridgeB, planningB, targetId, today + 2)).isEqualTo(1)
                val first = createRoomMergeSubject(a.db)
                val second = createRoomMergeSubject(b.db)
                val fromB = exportCarry(b.db)
                val initial = exportCarry(a.db)
                val peerRow = fromB.dayThemes!!.single()
                assertThat(runCatching { first.applySnapshotBundle(fromB.copy(dayThemes = listOf(peerRow.copy(budgetPercent = 99)))) }.isFailure).isTrue()
                val peerCommitment = fromB.planningCommitments!!.single()
                assertThat(runCatching { first.applySnapshotBundle(fromB.copy(planningCommitments = listOf(
                    peerCommitment.copy(version = peerCommitment.version + 1, provenanceSourceId = "different-origin")))) }.isFailure).isTrue()
                assertThat(exportCarry(a.db).dayThemes).isEqualTo(initial.dayThemes)
                first.applySnapshotBundle(fromB)
                second.applySnapshotBundle(exportCarry(a.db))
                first.applySnapshotBundle(exportCarry(b.db))
                assertThat(exportCarry(a.db).planningCommitments).isEqualTo(exportCarry(b.db).planningCommitments)
                assertThat(exportCarry(a.db).dayThemes).isEqualTo(exportCarry(b.db).dayThemes)
                assertThat(exportCarry(a.db).planningCommitments!!.single().updatedAt).isEqualTo(today + 2)
            }
        }
    }

    @Test fun `turning on repeat inherits the chosen theme after late preparation and respects edit scope`() = runBlocking<Unit> {
        fixture { f, bridge, planning, _ ->
            val db = f.db
            val themes = DayThemeRepository(db, db.canonicalDayThemeDao(), bridge, planning)
            val source = DayTask(id = "oneoff", dayPlanId = "yesterday", title = "Task", version = 1)
            db.dayTaskDao().insert(source)
            val yesterdayTheme = canonicalDayThemeId("yesterday", "carry")
            val todayTheme = canonicalDayThemeId("today", "carry")
            themes.toggleEntityTheme("yesterday", source.id, yesterdayTheme)
            val sources = recurrencePlanningSources(db)
            val materializer = CanonicalRecurrenceMaterializationAdapter(db, Dispatchers.Unconfined, sources)
            val author = CanonicalTaskRecurrenceAuthoringAdapter(db, materializer, sources)
            val converted = author.convertOneOffToSeries(source, source.title, null, null, null, null,
                emptyList(), emptyList(), source.priority, null, source.points, source.executionStrictness,
                RecurrenceRule(RecurrenceFrequency.DAILY, 1, null))
            materializer.materializeForDate(today)
            val occurrence = db.dayTaskDao().getTasksForDayIncludingDeletedSync("today").single()
            suspend fun links(plan: String, taskId: String) = themes.observe(plan).first().assignments
                .singleOrNull { it.entityId == taskId }?.themeIds.orEmpty()
            assertThat(links("today", occurrence.id)).isEmpty()
            themes.prepareDay("today", today)
            assertThat(links("today", occurrence.id)).containsExactly(todayTheme)
            val editor = RecurringTaskRealizesEditor(db)
            editor.set(occurrence.id, TaskRealizesKind.THEMES, setOf(todayTheme), false, forward = false)
            materializer.materializeForDate(today)
            assertThat(links("today", occurrence.id)).isEmpty()
            assertThat(links("yesterday", converted.id)).containsExactly(yesterdayTheme)
            editor.set(occurrence.id, TaskRealizesKind.THEMES, setOf(todayTheme), true, forward = true)
            assertThat(links("today", occurrence.id)).containsExactly(todayTheme)
            editor.set(occurrence.id, TaskRealizesKind.THEMES, setOf(todayTheme), false, forward = true)
            assertThat(links("yesterday", converted.id)).containsExactly(yesterdayTheme)
            assertThat(links("today", occurrence.id)).isEmpty()
        }
    }

    @Test fun `recurring priority resolves by series on each day and one-off stays on the converted task`() = runBlocking<Unit> {
        fixture { f, _, _, _ ->
            val db = f.db
            val sources = recurrencePlanningSources(db)
            val materializer = CanonicalRecurrenceMaterializationAdapter(db, Dispatchers.Unconfined, sources)
            val author = CanonicalTaskRecurrenceAuthoringAdapter(db, materializer, sources)
            val rule = RecurrenceRule(RecurrenceFrequency.DAILY, 1, null)
            val prioritySeries = RecurringFocusSeries("focus-series", 1, 1, null, false, 1, rule, "2026-10-18", null,
                RecurringFocusTemplate("Focus", null, emptyList(), 30))
            db.canonicalRecurringSeriesDao().insert(prioritySeries.toAndroidEntity())
            materializer.materializeForDate(previousDay())
            val prior = db.dayFocusItemDao().getItemsForDayPlanSync("yesterday").single()
            val oneOff = DayFocusItem(id = "one-time", dayPlanId = "yesterday", title = "One-time")
            db.dayFocusItemDao().insertAll(listOf(oneOff))
            val source = DayTask(id = "source", dayPlanId = "yesterday", title = "Task", version = 1,
                realizedDayFocusItemIds = listOf(prior.id, oneOff.id))
            db.dayTaskDao().insert(source)
            val first = author.convertOneOffToSeries(source, source.title, null, null, null, null,
                emptyList(), emptyList(), source.priority, null, source.points, source.executionStrictness, rule)
            assertThat(first.realizedDayFocusItemIds).containsExactly(prior.id, oneOff.id)
            materializer.materializeForDate(today)
            val nextPriority = db.dayFocusItemDao().getItemsForDayPlanSync("today").single()
            suspend fun next() = db.dayTaskDao().getTasksForDayIncludingDeletedSync("today").single()
            assertThat(next().realizedDayFocusItemIds).containsExactly(nextPriority.id)
            val editor = RecurringTaskRealizesEditor(db)
            editor.set(first.id, TaskRealizesKind.PRIORITIES, setOf(prior.id), false, forward = true)
            assertThat(next().realizedDayFocusItemIds).isEmpty()
            assertThat(db.dayTaskDao().getTaskById(first.id)?.realizedDayFocusItemIds).containsExactly(oneOff.id)
            val cleared = next()
            materializer.materializeForDate(today)
            assertThat(next()).isEqualTo(cleared)
        }
    }

    @Test fun `only enabled theme carries with new exact participation and same semantic identity`() = runBlocking<Unit> {
        fixture { f, bridge, planning, _ ->
            val sourceRows = f.db.canonicalDayThemeDao().getDayThemesForDay("yesterday")
            val sourceCommitments = planning.getDayThemeCommitments("yesterday")
            val identities = f.dao.getAllManagedSubjects()
            assertThat(carryForwardDayThemes(f.db, f.db.canonicalDayThemeDao(), bridge, planning, "today", today)).isEqualTo(1)
            val row = f.db.canonicalDayThemeDao().getDayThemesForDay("today").single()
            assertThat(row.themeId).isEqualTo("carry")
            assertThat(row.id).isEqualTo(canonicalDayThemeId("today", "carry"))
            val origin = sourceCommitments.single { it.id == canonicalDayThemeCommitmentId(canonicalDayThemeId("yesterday", "carry")) }
            val next = planning.getDayThemeCommitments("today").single()
            assertThat(next.id).isNotEqualTo(origin.id)
            assertThat(next.orientationId).isEqualTo(origin.orientationId)
            assertThat(next.role).isEqualTo(origin.role)
            assertThat(next.provenance.kind.name).isEqualTo("CARRIED_FORWARD")
            assertThat(next.provenance.sourceId).isEqualTo(origin.id.value)
            assertThat(f.db.canonicalDayThemeDao().getDayThemesForDay("yesterday")).isEqualTo(sourceRows)
            assertThat(planning.getDayThemeCommitments("yesterday")).isEqualTo(sourceCommitments)
            assertThat(f.dao.getAllManagedSubjects()).isEqualTo(identities)
            assertThat(f.db.hierarchyPlacementDao().getAll()).isEmpty()
        }
    }

    @Test fun `reopening is inert and removed target theme never resurrects`() = runBlocking<Unit> {
        fixture { f, bridge, planning, _ ->
            val dao = f.db.canonicalDayThemeDao()
            carryForwardDayThemes(f.db, dao, bridge, planning, "today", today)
            val before = f.rows()
            assertThat(carryForwardDayThemes(f.db, dao, bridge, planning, "today", today + 1)).isEqualTo(0)
            assertThat(f.rows()).isEqualTo(before)
            val old = dao.getDayThemesForDay("today").single()
            val dead = DayThemeSnapshot(old.id, old.themeId, old.dayPlanId, old.budgetPercent, old.order, false, old.createdAt, today + 2, null, old.version + 1, true)
            planning.applyProjectionChanges(listOf(dead)); dao.upsertDayThemes(listOf(dead.toCanonicalEntity()))
            val removed = f.rows()
            assertThat(carryForwardDayThemes(f.db, dao, bridge, planning, "today", today + 3)).isEqualTo(0)
            assertThat(f.rows()).isEqualTo(removed)
        }
    }

    @Test fun `browsing a past day never backfills history`() = runBlocking<Unit> {
        fixture { f, bridge, planning, _ ->
            val tomorrow = Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            val before = f.rows()
            assertThat(carryForwardDayThemes(f.db, f.db.canonicalDayThemeDao(), bridge, planning, "today", tomorrow)).isEqualTo(0)
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `late theme insert failure rolls back common participation atomically`() = runBlocking<Unit> {
        fixture { f, bridge, planning, _ ->
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_theme_carry BEFORE INSERT ON day_themes WHEN NEW.dayPlanId='today' BEGIN SELECT RAISE(ABORT, 'late failure'); END")
            val before = f.rows()
            assertThat(runCatching { carryForwardDayThemes(f.db, f.db.canonicalDayThemeDao(), bridge, planning, "today", today) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `carried Theme projects after restore merge compatibility repair and restart without provenance loss`() = runBlocking<Unit> {
        fixture { f, bridge, planning, authority ->
            carryForwardDayThemes(f.db, f.db.canonicalDayThemeDao(), bridge, planning, "today", today)
            val store = CanonicalPlanningSyncStore(f.db)
            val scopes = store.loadAllScopes(); val commitments = store.loadAllCommitments()
            val payload = store.decodeAndValidateForRestore(scopes, commitments, commitments.mapTo(hashSetOf()) { it.orientationId })
            store.restoreExactDecoded(payload)
            store.mergeIncoming(scopes, commitments)
            authority.projectCompatibilityRows()
            assertThat(store.loadAllCommitments()).isEqualTo(commitments)
            assertThat(carryForwardDayThemes(f.db, f.db.canonicalDayThemeDao(), bridge, planning, "today", today)).isEqualTo(0)
            assertThat(planning.getDayThemeCommitments("today").single().provenance.kind.name).isEqualTo("CARRIED_FORWARD")
        }
    }

    @Test fun `legacy provenance still fails closed on mismatched source instead of weakening admission`() = runBlocking<Unit> {
        fixture { f, _, planning, _ ->
            val row = f.db.canonicalDayThemeDao().getDayThemesForDay("yesterday").first()
            val original = planning.getDayThemeCommitments("yesterday").single { it.id == canonicalDayThemeCommitmentId(row.id) }
            val snapshot = DayThemeSnapshot(row.id, row.themeId, row.dayPlanId, row.budgetPercent, row.order,
                row.isActive, row.createdAt, row.updatedAt, row.syncedAt, row.version, row.isDeleted)
            val bad = original.copy(provenance = original.provenance.copy(sourceId = "wrong-source"))
            assertThat(runCatching { planning.applyCanonicalProjection(listOf(snapshot), listOf(bad)) }.isFailure).isTrue()
        }
    }

    @Test fun `read observation is inert and explicit Day preparation materializes carry`() = runBlocking<Unit> {
        fixture { f, bridge, planning, _ ->
            val repository = DayThemeRepository(f.db, f.db.canonicalDayThemeDao(), bridge, planning)
            val before = f.rows()
            assertThat(repository.observe("today").first().themes).isEmpty()
            assertThat(f.rows()).isEqualTo(before)
            repository.prepareDay("today", today)
            assertThat(repository.observe("today").first().themes.single().carryForward).isTrue()
        }
    }
}
