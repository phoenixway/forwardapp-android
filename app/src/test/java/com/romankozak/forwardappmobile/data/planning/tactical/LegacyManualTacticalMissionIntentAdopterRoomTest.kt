package com.romankozak.forwardappmobile.data.planning.tactical

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.NO_DEADLINE
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LegacyManualTacticalMissionIntentAdopterRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `first adoption creates complete deterministic INTENT aggregate and rerun is idempotent`() = runBlocking {
        withDatabase { database ->
            database.tacticalMissionDao().insertMissionRaw(mission(7L))
            val adopter = adopter(database)
            val expectedId = legacyManualTacticalMissionSubjectId(7L)

            assertEquals(LegacyManualIntentAdoptionResult.Created(expectedId), adopter.adopt(7L))
            val firstSubject = database.orientationDao().getManagedSubject(expectedId)!!
            val firstMapping = mapping(database, 7L)
            val firstAssessment = database.orientationDao().getAllAssessments().single()
            val firstRevision = database.orientationDao().getAllAssessmentRevisions().single()

            assertEquals(OrientationKind.INTENT.name, database.orientationDao().getAllOrientations().single().kind)
            assertEquals(LegacyOrientationSourceType.TACTICAL_MISSION.name, firstMapping.sourceType)
            assertEquals(LegacySubjectMappingState.CUT_OVER.name, firstMapping.state)
            assertEquals(expectedId, firstMapping.subjectId)
            assertEquals(firstRevision.id, firstAssessment.revisionId)

            assertEquals(LegacyManualIntentAdoptionResult.AlreadyAdopted(expectedId), adopter.adopt(7L))
            assertEquals(firstSubject, database.orientationDao().getManagedSubject(expectedId))
            assertEquals(firstMapping, mapping(database, 7L))
            assertEquals(firstAssessment, database.orientationDao().getAllAssessments().single())
            assertEquals(firstRevision, database.orientationDao().getAllAssessmentRevisions().single())
        }
    }

    @Test
    fun `different manual missions adopt different semantic identities`() = runBlocking {
        withDatabase { database ->
            database.tacticalMissionDao().insertMissionRaw(mission(7L))
            database.tacticalMissionDao().insertMissionRaw(mission(8L))
            val adopter = adopter(database)

            val first = adopter.adopt(7L).orientationId
            val second = adopter.adopt(8L).orientationId

            assertNotEquals(first, second)
            assertEquals(setOf(first, second), database.orientationDao().getAllOrientations().map { it.subjectId }.toSet())
        }
    }

    @Test
    fun `canonical semantic edits remain authoritative on adoption rerun`() = runBlocking {
        withDatabase { database ->
            database.tacticalMissionDao().insertMissionRaw(mission(7L))
            val adopter = adopter(database)
            val subjectId = adopter.adopt(7L).orientationId
            val canonical = database.orientationDao().getManagedSubject(subjectId)!!
            database.orientationDao().upsertManagedSubjects(
                listOf(
                    canonical.copy(
                        title = "Canonical edit",
                        description = "Canonical authority",
                        updatedAt = 200L,
                        version = canonical.version + 1L,
                    ),
                ),
            )

            assertEquals(LegacyManualIntentAdoptionResult.AlreadyAdopted(subjectId), adopter.adopt(7L))
            val after = database.orientationDao().getManagedSubject(subjectId)!!
            assertEquals("Canonical edit", after.title)
            assertEquals("Canonical authority", after.description)
        }
    }

    @Test
    fun `conflicting mapping and divergent deterministic target fail closed`() = runBlocking {
        withDatabase { database ->
            database.tacticalMissionDao().insertMissionRaw(mission(7L))
            val repository = repository(database)
            repository.ensureOrientationWithId("different", OrientationKind.INTENT, "Other", now = 1L)
            database.orientationDao().upsertLegacyMappings(
                listOf(
                    LegacySubjectMappingEntity(
                        id = "mission-7-conflict",
                        sourceType = LegacyOrientationSourceType.TACTICAL_MISSION.name,
                        sourceId = "7",
                        subjectId = "different",
                        migrationVersion = TACTICAL_MISSION_INTENT_ADOPTION_VERSION,
                        state = LegacySubjectMappingState.CUT_OVER.name,
                        createdAt = 1L,
                        updatedAt = 1L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                ),
            )

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { adopter(database).adopt(7L) }
            }
        }
        withDatabase { database ->
            database.tacticalMissionDao().insertMissionRaw(mission(7L))
            repository(database).ensureOrientationWithId(
                legacyManualTacticalMissionSubjectId(7L),
                OrientationKind.GOAL,
                "Occupied",
                now = 1L,
            )

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { adopter(database).adopt(7L) }
            }
        }
    }

    @Test
    fun `materialized mapping tombstoned target and deleted mission fail closed`() = runBlocking {
        withDatabase { database ->
            database.tacticalMissionDao().insertMissionRaw(mission(7L))
            val adopter = adopter(database)
            val subjectId = adopter.adopt(7L).orientationId
            val mapping = mapping(database, 7L)
            database.orientationDao().upsertLegacyMappings(
                listOf(mapping.copy(state = LegacySubjectMappingState.MATERIALIZED.name)),
            )
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { adopter.adopt(7L) }
            }

            database.orientationDao().upsertLegacyMappings(listOf(mapping))
            val subject = database.orientationDao().getManagedSubject(subjectId)!!
            database.orientationDao().upsertManagedSubjects(listOf(subject.copy(isDeleted = true)))
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { adopter.adopt(7L) }
            }
        }
        withDatabase { database ->
            database.tacticalMissionDao().insertMissionRaw(mission(9L, isDeleted = true))
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { adopter(database).adopt(9L) }
            }
            assertNull(database.orientationDao().getManagedSubject(legacyManualTacticalMissionSubjectId(9L)))
        }
    }

    private fun adopter(database: AppDatabase) =
        LegacyManualTacticalMissionIntentAdopter(database, repository(database))

    private fun repository(database: AppDatabase) =
        CanonicalOrientationRepository(database, database.orientationDao())

    private suspend fun mapping(database: AppDatabase, missionId: Long) =
        requireNotNull(
            database.orientationDao().getLegacyMapping(
                LegacyOrientationSourceType.TACTICAL_MISSION.name,
                missionId.toString(),
            ),
        )

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

    private fun mission(
        id: Long,
        isDeleted: Boolean = false,
    ) = TacticalMission(
        id = id,
        title = "Mission $id",
        description = "Description $id",
        deadline = NO_DEADLINE,
        projectId = null,
        sourceType = MissionSourceType.MANUAL,
        createdAt = 10L,
        updatedAt = 20L,
        version = 3L,
        isDeleted = isDeleted,
    )
}
