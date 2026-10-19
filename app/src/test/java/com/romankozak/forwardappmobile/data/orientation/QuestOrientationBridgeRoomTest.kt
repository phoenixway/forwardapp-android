package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.data.daythemes.CanonicalDayThemeBootstrapper
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningRepository
import com.romankozak.forwardappmobile.data.planning.strategic.CanonicalStrategicPlanningAdapter
import com.romankozak.forwardappmobile.data.planning.strategic.CanonicalStrategicPlanningAuthorityActivator
import com.romankozak.forwardappmobile.data.planning.tactical.CanonicalTacticalPlanningAuthorityActivator
import com.romankozak.forwardappmobile.data.planning.tactical.TacticalPlanningActivationReport
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestRepository
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuestOrientationBridgeRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `bootstrap promotes manual ArcQuest shadow in place and is idempotent`() = runBlocking {
        withDatabase { database ->
            val quest = manualQuest()
            val projection =
                (
                    quest.toCompatibilityProjection(LegacySubjectUuid)
                        as ArcQuestCompatibilityProjection.ManualOrientation
                ).value
            val rows = projection.toCanonicalRows(Gson(), migrationVersion = 2)

            database.arcQuestDao().insert(quest)
            insertRows(database, rows)

            val bootstrapper = bootstrapper(database)
            val first = bootstrapper.ensureBootstrapped()
            val firstSubject = database.orientationDao().getManagedSubject(rows.subject.id)!!
            val firstMapping = mapping(database, quest.id)
            val firstOrientation =
                database.orientationDao().getAllOrientations()
                    .single { it.subjectId == rows.subject.id }

            assertTrue(first.issues.isEmpty())
            assertEquals(rows.subject.id, firstMapping.subjectId)
            assertEquals(LegacySubjectMappingState.CUT_OVER.name, firstMapping.state)
            assertEquals(OrientationKind.QUEST.name, firstOrientation.kind)
            assertNull(firstOrientation.lifecycle)
            assertEquals(ValueOrigin.UNSET.name, firstOrientation.lifecycleOrigin)

            val second = bootstrapper.ensureBootstrapped()

            assertTrue(second.issues.isEmpty())
            assertEquals(firstSubject, database.orientationDao().getManagedSubject(rows.subject.id))
            assertEquals(firstMapping, mapping(database, quest.id))
            assertEquals(
                firstOrientation,
                database.orientationDao().getAllOrientations()
                    .single { it.subjectId == rows.subject.id },
            )
        }
    }

    @Test
    fun `new manual ArcQuest creates canonical QUEST immediately while source-backed row does not`() = runBlocking {
        withDatabase { database ->
            val repository = repository(database)
            val manual = manualQuest(id = "manual-new")

            repository.addQuest(manual)

            val manualMapping = mapping(database, manual.id)
            val subject =
                requireNotNull(
                    database.orientationDao().getManagedSubject(manualMapping.subjectId),
                )
            val orientation =
                database.orientationDao().getAllOrientations()
                    .single { it.subjectId == manualMapping.subjectId }

            assertEquals(LegacySubjectMappingState.CUT_OVER.name, manualMapping.state)
            assertEquals(OrientationKind.QUEST.name, orientation.kind)
            assertNull(orientation.lifecycle)
            assertEquals(ValueOrigin.UNSET.name, orientation.lifecycleOrigin)
            assertEquals(manual.title, subject.title)

            val sourceBacked =
                ArcQuestEntity(
                    id = "source-backed",
                    arcKey = "arc-1",
                    title = "Existing beacon placement",
                    sourceType = ArcQuestSourceType.BEACON.name,
                    sourceId = "beacon-1",
                    createdAt = 30L,
                    updatedAt = 30L,
                    version = 1L,
                )

            repository.addQuest(sourceBacked)

            assertNull(
                database.orientationDao().getLegacyMapping(
                    LegacyOrientationSourceType.ARC_QUEST.name,
                    sourceBacked.id,
                ),
            )
            assertEquals(
                1,
                database.orientationDao().getAllOrientations()
                    .count { it.kind == OrientationKind.QUEST.name },
            )
        }
    }

    @Test
    fun `manual ArcQuest common edits follow canonical QUEST while deletion removes only participation`() = runBlocking {
        withDatabase { database ->
            val repository = repository(database)
            val quest = manualQuest(id = "editable")
            repository.addQuest(quest)

            val stored = requireNotNull(repository.getById(quest.id))
            repository.updateQuest(
                stored.copy(
                    title = "Canonical title",
                    description = "Canonical description",
                    status = "PAUSED",
                ),
            )

            val mapping = mapping(database, quest.id)
            val subject =
                requireNotNull(
                    database.orientationDao().getManagedSubject(mapping.subjectId),
                )
            val orientation =
                database.orientationDao().getAllOrientations()
                    .single { it.subjectId == mapping.subjectId }
            val compatibility = requireNotNull(database.arcQuestDao().getById(quest.id))

            assertEquals("Canonical title", subject.title)
            assertEquals("Canonical description", subject.description)
            assertEquals("Canonical title", compatibility.title)
            assertEquals("Canonical description", compatibility.description)
            assertEquals("PAUSED", compatibility.status)
            assertNull(orientation.lifecycle)
            assertEquals(ValueOrigin.UNSET.name, orientation.lifecycleOrigin)

            repository.deleteQuest(
                requireNotNull(repository.getById(quest.id)),
            )

            val deletedSubject =
                requireNotNull(
                    database.orientationDao().getManagedSubject(mapping.subjectId),
                )
            val deletedAssessment =
                database.orientationDao().getAllAssessments()
                    .single { it.orientationId == mapping.subjectId }
            val deletedMapping = mapping(database, quest.id)
            val deletedCompatibility = requireNotNull(database.arcQuestDao().getById(quest.id))

            assertFalse(deletedSubject.isDeleted)
            assertFalse(deletedAssessment.isDeleted)
            assertFalse(deletedMapping.isDeleted)
            assertTrue(deletedCompatibility.isDeleted)
        }
    }

    @Test
    fun `deleted manual ArcQuest cannot resurrect canonical QUEST`() = runBlocking {
        withDatabase { database ->
            val repository = repository(database)
            val quest = manualQuest(id = "no-resurrection")
            repository.addQuest(quest)

            repository.deleteQuest(
                requireNotNull(repository.getById(quest.id)),
            )

            val deletedCompatibility =
                requireNotNull(database.arcQuestDao().getById(quest.id))
            val mapping = mapping(database, quest.id)
            val subjectBefore =
                requireNotNull(
                    database.orientationDao().getManagedSubject(mapping.subjectId),
                )

            val failure =
                runCatching {
                    repository.updateQuest(
                        deletedCompatibility.copy(
                            title = "Resurrected",
                            isDeleted = false,
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(requireNotNull(database.arcQuestDao().getById(quest.id)).isDeleted)
            assertFalse(mapping(database, quest.id).isDeleted)
            assertEquals(
                subjectBefore,
                database.orientationDao().getManagedSubject(mapping.subjectId),
            )
            assertFalse(subjectBefore.isDeleted)
        }
    }

    @Test
    fun `completed bootstrap stays finite and runtime ignores post-cutover ArcQuest compatibility drift`() = runBlocking {
        withDatabase { database ->
            val repository = repository(database)
            val quest = manualQuest(id = "drift")
            repository.addQuest(quest)

            val mapping = mapping(database, quest.id)
            val canonicalBefore =
                requireNotNull(
                    database.orientationDao().getManagedSubject(mapping.subjectId),
                )
            val compatibilityBefore = requireNotNull(database.arcQuestDao().getById(quest.id))

            database.arcQuestDao().update(
                compatibilityBefore.copy(
                    title = "Incoming drift",
                    description = "Must not win",
                    isDeleted = true,
                    version = compatibilityBefore.version + 5L,
                ),
            )

            val report = bootstrapper(database).ensureBootstrapped()
            val canonicalAfter =
                requireNotNull(
                    database.orientationDao().getManagedSubject(mapping.subjectId),
                )
            val repaired = requireNotNull(database.arcQuestDao().getById(quest.id))

            assertTrue(report.issues.isEmpty())
            assertFalse(report.performed)
            assertEquals(canonicalBefore, canonicalAfter)
            assertEquals("Incoming drift", repaired.title)
            assertTrue(repaired.isDeleted)
            assertEquals(compatibilityBefore.version + 5L, repaired.version)
            val projected = requireNotNull(repository.getById(quest.id))
            assertEquals(canonicalBefore.title, projected.title)
            assertEquals(canonicalBefore.description, projected.description)
            assertFalse(projected.isDeleted)
        }
    }

    @Test
    fun `Strategic runtime reads canonical planning and semantic facts over divergent carrier`() = runBlocking {
        withDatabase { database ->
            val repository = repository(database)
            val quest = manualQuest(id = "canonical-wins")
            repository.addQuest(quest)
            val canonical = requireNotNull(repository.getById(quest.id))

            database.arcQuestDao().update(
                requireNotNull(database.arcQuestDao().getById(quest.id)).copy(
                    title = "Legacy title must lose",
                    status = "DONE",
                    order = 999L,
                ),
            )

            val projected = requireNotNull(repository.getById(quest.id))
            assertEquals(canonical.title, projected.title)
            assertEquals(canonical.status, projected.status)
            assertEquals(canonical.order, projected.order)
            assertEquals(listOf(projected), repository.observeArcQuests(quest.arcKey).first())
        }
    }

    @Test
    fun `Strategic runtime fails closed when eligible canonical participation is missing`() = runBlocking {
        withDatabase { database ->
            val repository = repository(database)
            val quest = manualQuest(id = "missing-focus")
            repository.addQuest(quest)
            database.openHelper.writableDatabase.execSQL(
                "DELETE FROM planning_commitments WHERE role = 'FOCUS'",
            )

            val failure = runCatching { repository.getById(quest.id) }.exceptionOrNull()

            assertTrue(failure is IllegalStateException)
        }
    }

    @Test
    fun `mixed eligible and deferred participants reorder through their exact owners`() = runBlocking {
        withDatabase { database ->
            val repository = repository(database)
            val eligible = manualQuest(id = "eligible-mixed")
            val deferred =
                ArcQuestEntity(
                    id = "deferred-mixed",
                    arcKey = eligible.arcKey,
                    title = "Deferred source",
                    sourceType = ArcQuestSourceType.CONTEXT.name,
                    sourceId = "unmapped-context",
                    createdAt = 40L,
                    updatedAt = 40L,
                    version = 1L,
                )
            repository.addQuest(eligible)
            repository.addQuest(deferred)
            assertEquals(listOf(eligible.arcKey), repository.observeNonEmptyArcKeys().first())

            val before = repository.observeArcQuests(eligible.arcKey).first()
            repository.reorder(before.reversed())
            val reordered = repository.observeArcQuests(eligible.arcKey).first()
            repository.updateQuest(
                requireNotNull(reordered.singleOrNull { it.id == eligible.id }).copy(status = "PAUSED"),
            )

            val commitment =
                database.planningDao().getAllCommitments().single { it.provenanceSourceId == eligible.id }
            val deferredRow = requireNotNull(database.arcQuestDao().getById(deferred.id))
            assertEquals("PAUSED", commitment.status)
            assertEquals(0L, commitment.commitmentOrder)
            assertEquals(1L, deferredRow.order)
            assertTrue(
                database.planningDao().getAllCommitments().none { it.provenanceSourceId == deferred.id },
            )
        }
    }

    @Test
    fun `Strategic runtime rejects missing marker and missing canonical Orientation`() = runBlocking {
        withDatabase { database ->
            val repository = repository(database)
            val quest = manualQuest(id = "runtime-integrity")
            repository.addQuest(quest)
            val mapping = mapping(database, quest.id)
            val subject = requireNotNull(database.orientationDao().getManagedSubject(mapping.subjectId))
            database.orientationDao().upsertManagedSubjects(listOf(subject.copy(isDeleted = true)))

            assertTrue(runCatching { repository.getById(quest.id) }.exceptionOrNull() != null)

            database.orientationDao().upsertManagedSubjects(listOf(subject))
            database.openHelper.writableDatabase.execSQL("DELETE FROM strategic_planning_authority_state")
            assertTrue(
                runCatching { repository.observeArcQuests(quest.arcKey).first() }.exceptionOrNull() != null,
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
                LegacyOrientationSourceType.ARC_QUEST.name,
                sourceId,
            ),
        )

    private suspend fun repository(database: AppDatabase): ArcQuestRepository {
        val bridge =
            QuestOrientationBridge(
                orientationDao = database.orientationDao(),
                bootstrapper = bootstrapper(database),
            )
        val tactical = mockk<CanonicalTacticalPlanningAuthorityActivator>()
        coEvery { tactical.ensureActivated(any()) } returns TacticalPlanningActivationReport(false, 0, 0, 0)
        val authority = CanonicalStrategicPlanningAuthorityActivator(database, bridge, tactical)
        authority.ensureActivated(now = 1L)
        val planningRepository =
            CanonicalPlanningRepository(
                database,
                database.planningDao(),
                database.dayPlanningScopeExtensionDao(),
                database.tacticalPlanningScopeExtensionDao(),
            )
        val adapter =
            CanonicalStrategicPlanningAdapter(
                database = database,
                authorityActivator = authority,
                planningRepository = planningRepository,
                orientationRepository = CanonicalOrientationRepository(database, database.orientationDao()),
                questOrientationBridge = bridge,
            )
        return ArcQuestRepository(
            canonicalAdapter = adapter,
        )
    }

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

    private suspend fun withDatabase(
        block: suspend (AppDatabase) -> Unit,
    ) {
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

    private fun manualQuest(
        id: String = "quest-1",
    ) =
        ArcQuestEntity(
            id = id,
            arcKey = "arc-1",
            title = "Canonical quest",
            description = "Do the hard thing",
            sourceType = ArcQuestSourceType.MANUAL.name,
            status = "ACTIVE",
            order = 7L,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = 21L,
            version = 4L,
            isDeleted = false,
        )
}
