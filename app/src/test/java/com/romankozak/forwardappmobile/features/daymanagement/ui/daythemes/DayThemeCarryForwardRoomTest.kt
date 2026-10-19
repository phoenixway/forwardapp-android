package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toCanonicalEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.*
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.orientation.*
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningSyncStore
import com.romankozak.forwardappmobile.data.planning.day.*
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId
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

    private suspend fun fixture(block: suspend (SemanticDeletionRoomFixture, ThemeOrientationBridge, CanonicalDayThemePlanningAdapter, CanonicalDayPlanningAuthorityActivator) -> Unit) {
        SemanticDeletionRoomFixture().use { f ->
            val db = f.db
            val dao = db.canonicalDayThemeDao()
            db.dayPlanDao().insert(DayPlan(id = "yesterday", date = previousDay(), version = 1))
            db.dayPlanDao().insert(DayPlan(id = "today", date = today, version = 1))
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
            block(f, bridge, CanonicalDayThemePlanningAdapter(db.planningDao(), bridge, authority), authority)
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
