package com.romankozak.forwardappmobile.data.planning.strategic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.QuestOrientationBridge
import com.romankozak.forwardappmobile.data.orientation.ThemeOrientationBridge
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayPlanningAuthorityActivator
import com.romankozak.forwardappmobile.data.planning.day.CanonicalDayThemePlanningAdapter
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningAuthorityActivator
import com.romankozak.forwardappmobile.data.planning.tactical.TacticalPlanningActivationReport
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.DayTheme
import com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes.DayThemeRepository
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningRoleCode
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalCreateCrashRoomRegressionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `Strategic quest creation refreshes active observer exactly once without restart`() = runBlocking {
        withDatabase { db ->
            val bridge = QuestOrientationBridge(db.orientationDao(), orientationBootstrapper(db))
            val tactical = mockk<CanonicalTacticalPlanningAuthorityActivator>()
            coEvery { tactical.ensureActivated(any()) } returns
                TacticalPlanningActivationReport(false, 0, 0, 0)
            val authority = CanonicalStrategicPlanningAuthorityActivator(db, bridge, tactical)
            authority.ensureActivated(now = 100L)

            val runtime =
                CanonicalStrategicPlanningAdapter(
                    db,
                    authority,
                    CanonicalPlanningRepository(
                        db,
                        db.planningDao(),
                        db.dayPlanningScopeExtensionDao(),
                        db.tacticalPlanningScopeExtensionDao(),
                    ),
                    CanonicalOrientationRepository(db, db.orientationDao()),
                    bridge,
                )

            val arcKey = "2026-10"
            val quest = ArcQuestEntity(
                id = "regression-strategic-quest",
                arcKey = arcKey,
                title = "Crash regression quest",
            )
            val emissions = mutableListOf<List<ArcQuestEntity>>()
            val observer = launch(start = CoroutineStart.UNDISPATCHED) {
                runtime.observeArcQuests(arcKey).collect { emissions.add(it) }
            }

            try {
                withTimeout(10_000) {
                    while (emissions.isEmpty()) delay(10)
                }
                assertTrue(emissions.first().isEmpty())

                runtime.addQuest(quest)

                withTimeout(10_000) {
                    while (emissions.none { list -> list.any { it.id == quest.id } }) delay(10)
                }

                val immediate = runtime.observeArcQuests(arcKey).first()
                assertEquals(1, immediate.count { it.id == quest.id })
                assertEquals("Crash regression quest", immediate.single().title)
                assertEquals(1, db.arcQuestDao().getAllSync().count { it.id == quest.id })
                assertEquals(
                    1,
                    db.planningDao().getAllCommitments()
                        .count { it.role == PlanningRoleCode.FOCUS.value },
                )
            } finally {
                observer.cancelAndJoin()
            }
        }
    }

    @Test
    fun `Day Theme creation refreshes active observer exactly once without restart`() = runBlocking {
        withDatabase { db ->
            val dayPlanId = "regression-day"
            db.dayPlanDao().insert(
                DayPlan(
                    id = dayPlanId,
                    date = 1_700_000_000_000L,
                    status = DayStatus.PLANNED,
                    createdAt = 1L,
                    version = 1L,
                ),
            )

            val bridge = ThemeOrientationBridge(
                db.orientationDao(),
                db.canonicalDayThemeDao(),
                orientationBootstrapper(db),
            )
            val authority = CanonicalDayPlanningAuthorityActivator(
                db,
                db.dayPlanDao(),
                db.canonicalDayThemeDao(),
                db.orientationDao(),
                db.planningDao(),
                db.dayPlanningScopeExtensionDao(),
                db.dayPlanningAuthorityStateDao(),
                bridge,
            )
            authority.ensureActivated(now = 100L)

            val repository = DayThemeRepository(
                db,
                db.canonicalDayThemeDao(),
                bridge,
                CanonicalDayThemePlanningAdapter(db.planningDao(), bridge, authority, db),
            )

            val temporaryId = "regression-theme-definition"
            val canonicalId = canonicalDayThemeId(dayPlanId, temporaryId)
            val emissions = mutableListOf<List<DayTheme>>()
            val observer = launch(start = CoroutineStart.UNDISPATCHED) {
                repository.observe(dayPlanId).collect { document ->
                    emissions.add(document.themes)
                }
            }

            try {
                withTimeout(10_000) {
                    while (emissions.isEmpty()) delay(10)
                }
                assertTrue(emissions.first().isEmpty())

                repository.update(dayPlanId) { document ->
                    document.copy(
                        themes = document.themes + DayTheme(
                            id = temporaryId,
                            dayPlanId = dayPlanId,
                            title = "Crash regression theme",
                        ),
                    )
                }

                withTimeout(10_000) {
                    while (emissions.none { list -> list.any { it.id == canonicalId } }) {
                        delay(10)
                    }
                }

                val immediate = repository.observe(dayPlanId).first()
                assertEquals(1, immediate.themes.count { it.id == canonicalId })
                assertEquals("Crash regression theme", immediate.themes.single().title)
                assertEquals(
                    1,
                    db.canonicalDayThemeDao().getDayThemesForDay(dayPlanId)
                        .count { it.id == canonicalId },
                )
                assertEquals(
                    1,
                    db.planningDao().getAllCommitments().count {
                        it.role == PlanningRoleCode.DAY_THEME.value && it.scopeId == dayPlanId
                    },
                )
            } finally {
                observer.cancelAndJoin()
            }
        }
    }

    private fun orientationBootstrapper(db: AppDatabase): CanonicalOrientationBootstrapper =
        CanonicalOrientationBootstrapper(
            db,
            db.orientationDao(),
            db.goalDao(),
            db.mainBeaconDao(),
            db.arcQuestDao(),
            db.canonicalDayThemeDao(),
            CanonicalDayThemeBootstrapper(
                db,
                db.dayThemeDocumentDao(),
                db.canonicalDayThemeDao(),
            ),
        )

    private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            block(db)
        } finally {
            db.close()
        }
    }
}
