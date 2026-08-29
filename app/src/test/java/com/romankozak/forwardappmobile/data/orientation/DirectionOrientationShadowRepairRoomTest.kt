package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DirectionOrientationShadowRepairRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `linked legacy row quarantine is idempotent and reversible after unlink`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insertContexts(
                listOf(
                    contextEntity("owner", "Owner"),
                    contextEntity("target", "Target"),
                ),
            )
            val row =
                DirectionItemEntity(
                    id = "direction-row",
                    contextId = "owner",
                    text = "Direction",
                    itemOrder = 1,
                    updatedAt = 10L,
                    version = 1L,
                )
            database.directionDao().insert(row)
            val bootstrapper = bootstrapper(database)

            bootstrapper.ensureBootstrapped()
            val initialMapping = directionMapping(database)
            val subjectId = initialMapping.subjectId
            assertFalse(requireNotNull(database.orientationDao().getManagedSubject(subjectId)).isDeleted)

            database.directionDao().update(
                row.copy(
                    linkedContextId = "target",
                    updatedAt = 20L,
                    version = 2L,
                ),
            )
            bootstrapper.ensureBootstrapped()

            val quarantinedMapping = directionMapping(database)
            val quarantinedSubject = requireNotNull(database.orientationDao().getManagedSubject(subjectId))
            assertEquals(LegacySubjectMappingState.QUARANTINED.name, quarantinedMapping.state)
            assertTrue(quarantinedSubject.isDeleted)
            assertTrue(
                database.orientationDao().getOpenBootstrapIssues().any {
                    it.sourceId == row.id && it.code == LINKED_REVIEW_ISSUE
                },
            )
            validateCanonicalPayloadReferences(
                database.orientationDao().exportBundle(database.workspaceDao()),
            )

            bootstrapper.ensureBootstrapped()
            assertEquals(quarantinedMapping.version, directionMapping(database).version)
            assertEquals(
                quarantinedSubject.version,
                database.orientationDao().getManagedSubject(subjectId)?.version,
            )

            database.directionDao().update(
                row.copy(
                    text = "Restored",
                    linkedContextId = null,
                    updatedAt = 30L,
                    version = 3L,
                ),
            )
            bootstrapper.ensureBootstrapped()

            val restoredMapping = directionMapping(database)
            val restoredSubject = requireNotNull(database.orientationDao().getManagedSubject(subjectId))
            assertEquals(LegacySubjectMappingState.MATERIALIZED.name, restoredMapping.state)
            assertFalse(restoredMapping.isDeleted)
            assertFalse(restoredSubject.isDeleted)
            assertEquals("Restored", restoredSubject.title)
            assertTrue(
                database.orientationDao().getOpenBootstrapIssues().none {
                    it.sourceId == row.id && it.code == LINKED_REVIEW_ISSUE
                },
            )
        } finally {
            database.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun OrientationDao.exportBundle(workspaceDao: WorkspaceDao) =
        SnapshotBundle(
            managedSubjects = getAllManagedSubjects(),
            orientations = getAllOrientations(),
            aspects = getAllAspects(),
            orientationAssessments = getAllAssessments(),
            orientationAssessmentRevisions = getAllAssessmentRevisions(),
            legacySubjectMappings = getAllLegacyMappings(),
            orientationRelations = getAllOrientationRelations(),
            aspectOrientationRefs = getAllAspectOrientationRefs(),
            workspaces = workspaceDao.getAll(),
            workspaceBindings = getAllWorkspaceBindings(),
            workspaceCapabilityInstances = getAllWorkspaceCapabilities(),
            savedOrientationViews = getAllSavedViews(),
        )

    private fun bootstrapper(database: AppDatabase): CanonicalOrientationBootstrapper {
        val dayThemeBootstrapper =
            CanonicalDayThemeBootstrapper(
                database = database,
                legacyDao = database.dayThemeDocumentDao(),
                canonicalDao = database.canonicalDayThemeDao(),
            )
        return CanonicalOrientationBootstrapper(
            database = database,
            orientationDao = database.orientationDao(),
            goalDao = database.goalDao(),
            directionDao = database.directionDao(),
            mainBeaconDao = database.mainBeaconDao(),
            arcQuestDao = database.arcQuestDao(),
            canonicalDayThemeDao = database.canonicalDayThemeDao(),
            canonicalDayThemeBootstrapper = dayThemeBootstrapper,
        )
    }

    private suspend fun directionMapping(database: AppDatabase) =
        requireNotNull(
            database.orientationDao().getLegacyMapping(
                LegacyOrientationSourceType.DIRECTION.name,
                "direction-row",
            ),
        )

    private fun contextEntity(
        id: String,
        name: String,
    ) =
        ContextEntity(
            id = id,
            name = name,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 2L,
            roleCode = "default",
        )
}
