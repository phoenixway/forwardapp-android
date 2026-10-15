package com.romankozak.forwardappmobile.data.planning.day

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalDayThemeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.data.orientation.ThemeOrientationBridge
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalDayPlanningAuthorityActivatorRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `activation materializes exact state marker-last and reruns inert`() = runBlocking {
        val db = database()
        try {
            seed(db, includeMapping = true)
            val activator = activator(db)

            val first = activator.ensureActivated(now = 100L)
            val second = activator.ensureActivated(now = 200L)

            assertEquals(true, first.performed)
            assertFalse(second.performed)
            assertEquals(1, first.scopes)
            assertEquals(1, first.commitments)
            assertEquals(1, db.planningDao().getAllScopes().size)
            assertEquals(1, db.dayPlanningScopeExtensionDao().getAll().size)
            assertEquals(1, db.planningDao().getAllCommitments().size)
            assertEquals(100L, requireNotNull(db.dayPlanningAuthorityStateDao().get()).activatedAt)
        } finally {
            db.close()
        }
    }

    @Test
    fun `invalid Theme dependency fails without marker or partial planning`() = runBlocking {
        val db = database()
        try {
            seed(db, includeMapping = false)
            runCatching { activator(db).ensureActivated() }
                .onSuccess { error("Activation unexpectedly succeeded") }

            assertEquals(null, db.dayPlanningAuthorityStateDao().get())
            assertEquals(0, db.planningDao().getAllScopes().size)
            assertEquals(0, db.dayPlanningScopeExtensionDao().getAll().size)
            assertEquals(0, db.planningDao().getAllCommitments().size)
        } finally {
            db.close()
        }
    }

    private fun activator(db: AppDatabase): CanonicalDayPlanningAuthorityActivator {
        val themeBridge = mockk<ThemeOrientationBridge>()
        coEvery { themeBridge.ensureCutOver() } returns Unit
        return CanonicalDayPlanningAuthorityActivator(
            database = db,
            dayPlanDao = db.dayPlanDao(),
            dayThemeDao = db.canonicalDayThemeDao(),
            orientationDao = db.orientationDao(),
            planningDao = db.planningDao(),
            extensionDao = db.dayPlanningScopeExtensionDao(),
            markerDao = db.dayPlanningAuthorityStateDao(),
            themeOrientationBridge = themeBridge,
        )
    }

    private suspend fun seed(db: AppDatabase, includeMapping: Boolean) {
        db.dayPlanDao().insert(
            DayPlan(
                id = "day",
                date = 1_700_000_000_000L,
                status = DayStatus.PLANNED,
                createdAt = 1L,
                version = 1L,
            ),
        )
        db.orientationDao().upsertManagedSubjects(
            listOf(
                ManagedSubjectEntity(
                    id = "theme-subject",
                    subjectType = ManagedSubjectType.ORIENTATION.name,
                    title = "Theme",
                    description = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        db.orientationDao().upsertOrientations(
            listOf(OrientationEntity("theme-subject", OrientationKind.THEME.name, null, "NONE")),
        )
        if (includeMapping) {
            db.orientationDao().upsertLegacyMappings(
                listOf(
                    LegacySubjectMappingEntity(
                        id = "mapping",
                        sourceType = LegacyOrientationSourceType.THEME_DEFINITION.name,
                        sourceId = "definition",
                        subjectId = "theme-subject",
                        migrationVersion = 1,
                        state = LegacySubjectMappingState.CUT_OVER.name,
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                ),
            )
        }
        db.canonicalDayThemeDao().upsertDayThemes(
            listOf(
                CanonicalDayThemeEntity(
                    id = "participation",
                    themeId = "definition",
                    dayPlanId = "day",
                    budgetPercent = 50,
                    order = 0L,
                    isActive = true,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    version = 1L,
                    isDeleted = false,
                ),
            ),
        )
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
