package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.ThemeDefinitionEntity
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemeOrientationBridgeRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `bootstrap promotes existing Theme shadow in place and is idempotent`() = runBlocking {
        withDatabase { database ->
            val definition = definition()
            val rows = definition.toEffectiveOrientation(LegacySubjectUuid).toCanonicalRows(Gson(), 2)
            database.canonicalDayThemeDao().upsertThemeDefinitions(listOf(definition))
            insertRows(database, rows)
            val bootstrapper = bootstrapper(database)

            val first = bootstrapper.ensureBootstrapped()
            val firstSubject = database.orientationDao().getManagedSubject(rows.subject.id)!!
            val firstMapping = mapping(database, definition.id)
            val firstRevision = database.orientationDao().getAllAssessmentRevisions().single()

            assertTrue(first.issues.isEmpty())
            assertEquals(rows.subject.id, firstMapping.subjectId)
            assertEquals(LegacySubjectMappingState.CUT_OVER.name, firstMapping.state)
            assertEquals(
                OrientationKind.THEME.name,
                database.orientationDao().getAllOrientations().single().kind,
            )
            assertEquals(rows.revision.id, firstRevision.id)

            val second = bootstrapper.ensureBootstrapped()

            assertTrue(second.issues.isEmpty())
            assertEquals(firstSubject, database.orientationDao().getManagedSubject(rows.subject.id))
            assertEquals(firstMapping, mapping(database, definition.id))
            assertEquals(firstRevision, database.orientationDao().getAllAssessmentRevisions().single())
        }
    }

    @Test
    fun `canonical common edit preserves extension version and projects compatibility fields`() = runBlocking {
        withDatabase { database ->
            val definition = definition()
            database.canonicalDayThemeDao().upsertThemeDefinitions(listOf(definition))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped()
            val bridge =
                ThemeOrientationBridge(
                    orientationDao = database.orientationDao(),
                    dayThemeDao = database.canonicalDayThemeDao(),
                    bootstrapper = bootstrapper,
                )
            val mapping = mapping(database, definition.id)

            bridge.writeCommon(definition, "Canonical", "Owned", now = 100)

            val subject = database.orientationDao().getManagedSubject(mapping.subjectId)!!
            val projected = database.canonicalDayThemeDao().getThemeDefinitionById(definition.id)!!
            assertEquals("Canonical", subject.title)
            assertEquals("Owned", subject.description)
            assertNull(subject.syncedAt)
            assertEquals("Canonical", projected.title)
            assertEquals("Owned", projected.description)
            assertEquals(definition.version, projected.version)
            assertEquals(definition.colorArgb, projected.colorArgb)
            assertEquals(definition.carryForward, projected.carryForward)
        }
    }

    @Test
    fun `COMPLETE fast path skips compatibility repair while canonical projection still wins`() = runBlocking {
        withDatabase { database ->
            val definition = definition()
            database.canonicalDayThemeDao().upsertThemeDefinitions(listOf(definition))
            val bootstrapper = bootstrapper(database)
            bootstrapper.ensureBootstrapped()
            val bridge =
                ThemeOrientationBridge(
                    orientationDao = database.orientationDao(),
                    dayThemeDao = database.canonicalDayThemeDao(),
                    bootstrapper = bootstrapper,
                )
            val mapping = mapping(database, definition.id)
            val canonicalBefore = database.orientationDao().getManagedSubject(mapping.subjectId)!!

            database.canonicalDayThemeDao().upsertThemeDefinitions(
                listOf(
                    definition.copy(
                        title = "Incoming compatibility drift",
                        description = "Must not win",
                        isDeleted = true,
                        version = definition.version + 5,
                    ),
                ),
            )

            val report = bootstrapper.ensureBootstrapped()
            val canonicalAfter = database.orientationDao().getManagedSubject(mapping.subjectId)!!
            val compatibility = database.canonicalDayThemeDao().getThemeDefinitionById(definition.id)!!
            val projected = bridge.projectCanonicalDefinitions(listOf(compatibility)).single()

            assertTrue(report.issues.isEmpty())
            assertTrue(!report.performed)
            assertEquals(canonicalBefore, canonicalAfter)
            assertEquals("Incoming compatibility drift", compatibility.title)
            assertEquals(canonicalBefore.title, projected.title)
            assertEquals(canonicalBefore.description, projected.description)
            assertEquals(canonicalBefore.isDeleted, projected.isDeleted)
            assertEquals(definition.version + 5, projected.version)
        }
    }

    @Test
    fun `new Theme creates authoritative identity immediately without restart`() = runBlocking {
        withDatabase { database ->
            val definition = definition(id = "new-theme")
            database.withTransaction {
                database.canonicalDayThemeDao().upsertThemeDefinitions(listOf(definition))
                ThemeOrientationBridge(
                    orientationDao = database.orientationDao(),
                    dayThemeDao = database.canonicalDayThemeDao(),
                    bootstrapper = bootstrapper(database),
                ).writeCommon(definition, definition.title, definition.description, now = 20)
            }

            val mapping = mapping(database, definition.id)
            assertEquals(LegacySubjectUuid.resolve(definition.toEffectiveOrientation(LegacySubjectUuid).source), mapping.subjectId)
            assertEquals(LegacySubjectMappingState.CUT_OVER.name, mapping.state)
            assertEquals(
                OrientationKind.THEME.name,
                database.orientationDao().getAllOrientations().single().kind,
            )
        }
    }

    private suspend fun insertRows(
        database: AppDatabase,
        rows: CanonicalOrientationRows,
    ) {
        database.orientationDao().upsertManagedSubjects(listOf(rows.subject))
        database.orientationDao().upsertOrientations(listOf(rows.orientation))
        database.orientationDao().upsertAssessmentRevisions(listOf(rows.revision))
        database.orientationDao().upsertAssessments(listOf(rows.assessment))
        database.orientationDao().upsertLegacyMappings(listOf(rows.mapping))
    }

    private suspend fun mapping(
        database: AppDatabase,
        sourceId: String,
    ) =
        requireNotNull(
            database.orientationDao().getLegacyMapping(
                LegacyOrientationSourceType.THEME_DEFINITION.name,
                sourceId,
            ),
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
            mainBeaconDao = database.mainBeaconDao(),
            arcQuestDao = database.arcQuestDao(),
            canonicalDayThemeDao = database.canonicalDayThemeDao(),
            canonicalDayThemeBootstrapper = dayThemeBootstrapper,
        )
    }

    private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
        val database =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun definition(id: String = "theme-1") =
        ThemeDefinitionEntity(
            id = id,
            title = "Focus",
            colorArgb = 99,
            iconKey = "target",
            description = "Deep work",
            carryForward = true,
            archived = false,
            createdAt = 10,
            updatedAt = 20,
            syncedAt = 21,
            version = 4,
            isDeleted = false,
        )
}
