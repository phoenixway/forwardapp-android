package com.romankozak.forwardappmobile.features.semanticcreation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.data.hierarchy.HierarchyPlacementLifecycleCoordinator
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SemanticEntityCreationRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `standalone Orientation and Aspect create only canonical semantic aggregates`() = runBlocking {
        val database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            val orientationRepository =
                CanonicalOrientationRepository(database, database.orientationDao())
            val aspectRepository =
                CanonicalAspectRepository(
                    database,
                    database.orientationDao(),
                    HierarchyPlacementLifecycleCoordinator(database),
                )

            val orientationId =
                orientationRepository.createOrientation(
                    kind = OrientationKind.INTENT,
                    title = "  Deliberate outcome  ",
                    now = 100L,
                )
            val aspectId = aspectRepository.create(title = "Health", now = 101L)

            val orientationSubject = database.orientationDao().getManagedSubject(orientationId)
            val aspectSubject = database.orientationDao().getManagedSubject(aspectId)
            assertEquals("Deliberate outcome", orientationSubject?.title)
            assertEquals(ManagedSubjectType.ORIENTATION.name, orientationSubject?.subjectType)
            assertEquals(OrientationKind.INTENT.name, database.orientationDao().getAllOrientations().single().kind)
            assertEquals(ManagedSubjectType.ASPECT.name, aspectSubject?.subjectType)
            assertNotNull(database.orientationDao().getAspect(aspectId))

            assertTrue(database.orientationDao().getAllWorkspaceBindings().isEmpty())
            assertTrue(database.planningDao().getAllScopes().isEmpty())
            assertTrue(database.planningDao().getAllCommitments().isEmpty())
            assertTrue(database.hierarchyPlacementDao().getAll().isEmpty())
            assertTrue(database.contextDao().getAllRaw().isEmpty())
            assertTrue(database.workspaceDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `semantic rename preserves identity rejects stale version and has no relation side effects`() = runBlocking {
        val database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            val orientations = CanonicalOrientationRepository(database, database.orientationDao())
            val aspects =
                CanonicalAspectRepository(
                    database,
                    database.orientationDao(),
                    HierarchyPlacementLifecycleCoordinator(database),
                )
            val orientationId = orientations.createOrientation(OrientationKind.INTENT, "Purpose", now = 10L)
            val aspectId = aspects.create("Health", now = 11L)

            orientations.updateOrientationSemantics(
                subjectId = orientationId,
                title = "Purpose refined",
                description = null,
                expectedVersion = 1L,
                now = 20L,
            )
            aspects.updateDetails(
                id = aspectId,
                title = "Wellbeing",
                description = null,
                expectedVersion = 1L,
                now = 21L,
            )

            assertEquals("Purpose refined", database.orientationDao().getManagedSubject(orientationId)?.title)
            assertEquals("Wellbeing", database.orientationDao().getManagedSubject(aspectId)?.title)
            assertEquals(setOf(orientationId, aspectId), database.orientationDao().getAllManagedSubjects().map { it.id }.toSet())
            assertEquals(orientationId, database.orientationDao().getAllOrientations().single().subjectId)
            assertEquals(aspectId, database.orientationDao().getAllAspects().single().subjectId)

            val staleFailure =
                runCatching {
                    orientations.updateOrientationSemantics(
                        subjectId = orientationId,
                        title = "Stale overwrite",
                        description = null,
                        expectedVersion = 1L,
                        now = 30L,
                    )
                }.exceptionOrNull()
            assertTrue(staleFailure is IllegalArgumentException)
            assertEquals("Purpose refined", database.orientationDao().getManagedSubject(orientationId)?.title)

            assertTrue(database.orientationDao().getAllWorkspaceBindings().isEmpty())
            assertTrue(database.planningDao().getAllCommitments().isEmpty())
            assertTrue(database.hierarchyPlacementDao().getAll().isEmpty())
            assertTrue(database.contextDao().getAllRaw().isEmpty())
            assertTrue(database.workspaceDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }
}
