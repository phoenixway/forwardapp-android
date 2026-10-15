package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.calculateOrientationScoreFromCoordinates
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalScoringAuthorityActivatorRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `activation preserves identity and exact legacy precision then becomes inert`() = runBlocking {
        val db = database()
        try {
            val goal = legacyGoal()
            db.goalDao().insertGoal(goal)
            seedGoalOrientation(db, goal.id)
            val activator = activator(db)

            val first = activator.ensureActivated(now = 500L)
            val override = requireNotNull(db.orientationDao().getScoringOverride(SUBJECT_ID))
            val assessment = db.orientationDao().getAllAssessments().single { it.orientationId == SUBJECT_ID }
            val projected = requireNotNull(db.goalDao().getGoalById(goal.id))

            assertTrue(first.performed)
            assertEquals(SUBJECT_ID, override.orientationId)
            assertEquals(2.5f, override.importanceCoordinate)
            assertEquals(11f, override.impactCoordinate)
            assertEquals(1.7f, override.effortWeight)
            assertEquals(EffortValue.EXTREME.name, assessment.effortValue)
            assertEquals(CostValue.HIGH.name, assessment.costValue)
            assertEquals(RiskValue.VERY_HIGH.name, assessment.riskValue)
            assertEquals(OrientationEvaluationState.ASSESSED.name, assessment.evaluationState)
            val expected =
                calculateOrientationScoreFromCoordinates(
                    OrientationScoringCoordinates(2.5f, 11f, 13f, 4f, 8f),
                    1.7f,
                    0.6f,
                    1.2f,
                )
            assertEquals(expected.raw, projected.rawScore, 0.000001f)
            assertEquals(expected.display, projected.displayScore)
            assertNotNull(db.canonicalScoringAuthorityStateDao().get())

            val second = activator.ensureActivated(now = 900L)
            assertFalse(second.performed)
            assertEquals(override, db.orientationDao().getScoringOverride(SUBJECT_ID))
        } finally {
            db.close()
        }
    }

    @Test
    fun `historical V1 revision with provenance activates and preserves provenance`() = runBlocking {
        val db = database()
        try {
            val goal = legacyGoal()
            db.goalDao().insertGoal(goal)
            seedHistoricalProvenanceRevision(db, goal.id)

            val historicalRevision = db.orientationDao().getAllAssessmentRevisions()
                .single { it.id == "revision-1" }

            val result = activator(db).ensureActivated(now = 500L)
            assertTrue(result.performed)
            assertNotNull(db.canonicalScoringAuthorityStateDao().get())

            // The original immutable revision must remain byte-for-byte unchanged.
            val preservedRevision = db.orientationDao().getAllAssessmentRevisions()
                .single { it.id == "revision-1" }
            assertEquals(historicalRevision.assessmentJson, preservedRevision.assessmentJson)

            val current = db.orientationDao().getAllAssessments()
                .single { it.orientationId == SUBJECT_ID }
            val activeRevision = db.orientationDao().getAllAssessmentRevisions()
                .single { it.id == current.revisionId }
            val activeJson =
                com.google.gson.JsonParser.parseString(activeRevision.assessmentJson).asJsonObject

            assertTrue(activeJson.getAsJsonObject("importance").has("provenance"))
            assertTrue(activeJson.getAsJsonObject("impact").has("provenance"))
            assertTrue(current.provenanceJson?.contains("importance") == true)
            assertTrue(current.provenanceJson?.contains("impact") == true)
        } finally {
            db.close()
        }
    }

    @Test
    fun `historical provenance does not permit real assessment drift`() = runBlocking {
        val db = database()
        try {
            val goal = legacyGoal()
            db.goalDao().insertGoal(goal)
            seedHistoricalProvenanceRevision(db, goal.id)

            val current = db.orientationDao().getAllAssessments()
                .single { it.orientationId == SUBJECT_ID }
            db.orientationDao().upsertAssessments(
                listOf(current.copy(impactValue = "TEST_VALUE_DRIFT")),
            )

            val failure = assertThrows(IllegalArgumentException::class.java) {
                runBlocking { activator(db).ensureActivated(now = 500L) }
            }
            assertTrue(failure.message?.contains("diverges from its immutable revision") == true)
            assertNull(db.canonicalScoringAuthorityStateDao().get())
            assertTrue(db.orientationDao().getAllScoringOverrides().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun `malformed cutover rolls back override and activation marker`() = runBlocking {
        val db = database()
        try {
            val goal = legacyGoal(id = "broken")
            db.goalDao().insertGoal(goal)
            db.orientationDao().upsertManagedSubjects(
                listOf(
                    ManagedSubjectEntity(
                        id = "missing-orientation",
                        subjectType = ManagedSubjectType.ORIENTATION.name,
                        title = "Broken",
                        description = null,
                        createdAt = 100L,
                        updatedAt = 100L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    ),
                ),
            )
            db.orientationDao().upsertLegacyMappings(
                listOf(mapping(goal.id, "missing-orientation")),
            )

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { activator(db).ensureActivated(now = 500L) }
            }
            assertTrue(db.orientationDao().getAllScoringOverrides().isEmpty())
            assertNull(db.canonicalScoringAuthorityStateDao().get())
        } finally {
            db.close()
        }
    }

    @Test
    fun `canonical merge admission reprojects changed scoring despite existing marker`() = runBlocking {
        val db = database()
        try {
            val goal = legacyGoal()
            db.goalDao().insertGoal(goal)
            seedGoalOrientation(db, goal.id)
            val activator = activator(db)
            activator.ensureActivated(now = 500L)

            val original = requireNotNull(db.orientationDao().getScoringOverride(SUBJECT_ID))
            db.withTransaction {
                db.orientationDao().upsertScoringOverrides(
                    listOf(
                        original.copy(
                            importanceCoordinate = 3.5f,
                            updatedAt = 600L,
                            version = original.version + 1L,
                        ),
                    ),
                )
                val report = activator.admitMergedState(now = 700L)
                assertFalse(report.performed)
                assertEquals(1, report.orientations)
            }

            val projected = requireNotNull(db.goalDao().getGoalById(goal.id))
            assertEquals(3.5f, projected.valueImportance, 0.000001f)
            assertNotNull(db.canonicalScoringAuthorityStateDao().get())
        } finally {
            db.close()
        }
    }

    @Test
    fun `canonical merge admission rejects deleted scoring override and rolls back`() = runBlocking {
        val db = database()
        try {
            val goal = legacyGoal()
            db.goalDao().insertGoal(goal)
            seedGoalOrientation(db, goal.id)
            val activator = activator(db)
            activator.ensureActivated(now = 500L)

            val original = requireNotNull(db.orientationDao().getScoringOverride(SUBJECT_ID))
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    db.withTransaction {
                        db.orientationDao().upsertScoringOverrides(
                            listOf(
                                original.copy(
                                    isDeleted = true,
                                    updatedAt = 600L,
                                    version = original.version + 1L,
                                ),
                            ),
                        )
                        activator.admitMergedState(now = 700L)
                    }
                }
            }

            assertEquals(original, db.orientationDao().getScoringOverride(SUBJECT_ID))
            assertNotNull(db.canonicalScoringAuthorityStateDao().get())
        } finally {
            db.close()
        }
    }

    @Test
    fun `canonical merge admission rejects revision drift and rolls back`() = runBlocking {
        val db = database()
        try {
            val goal = legacyGoal()
            db.goalDao().insertGoal(goal)
            seedGoalOrientation(db, goal.id)
            val activator = activator(db)
            activator.ensureActivated(now = 500L)

            val original = db.orientationDao().getAllAssessments()
                .single { it.orientationId == SUBJECT_ID }

            val failure = assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    db.withTransaction {
                        db.orientationDao().upsertAssessments(
                            listOf(
                                original.copy(
                                    impactValue = "MERGED_VALUE_DRIFT",
                                    updatedAt = 600L,
                                    version = original.version + 1L,
                                ),
                            ),
                        )
                        activator.admitMergedState(now = 700L)
                    }
                }
            }

            assertTrue(failure.message?.contains("immutable revision") == true)
            assertEquals(
                original,
                db.orientationDao().getAllAssessments()
                    .single { it.orientationId == SUBJECT_ID },
            )
            assertNotNull(db.canonicalScoringAuthorityStateDao().get())
        } finally {
            db.close()
        }
    }

    @Test
    fun `canonical merge admission rejects physically missing override and rolls back`() = runBlocking {
        val db = database()
        try {
            val goal = legacyGoal()
            db.goalDao().insertGoal(goal)
            seedGoalOrientation(db, goal.id)

            val activator = activator(db)
            activator.ensureActivated(now = 500L)

            val original = requireNotNull(
                db.orientationDao().getScoringOverride(SUBJECT_ID),
            )

            val failure = assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    db.withTransaction {
                        db.openHelper.writableDatabase.execSQL(
                            "DELETE FROM orientation_scoring_overrides WHERE orientationId = ?",
                            arrayOf<Any>(SUBJECT_ID),
                        )
                        assertNull(
                            db.orientationDao().getScoringOverride(SUBJECT_ID),
                        )
                        activator.admitMergedState(now = 700L)
                    }
                }
            }

            assertTrue(
                failure.message?.contains(
                    "missing its policy/override record",
                ) == true,
            )
            assertEquals(
                original,
                db.orientationDao().getScoringOverride(SUBJECT_ID),
            )
            assertNotNull(db.canonicalScoringAuthorityStateDao().get())
        } finally {
            db.close()
        }
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun activator(db: AppDatabase) =
        CanonicalScoringAuthorityActivator(
            db,
            db.orientationDao(),
            db.goalDao(),
            db.contextDao(),
            db.canonicalScoringAuthorityStateDao(),
        )

    private suspend fun seedGoalOrientation(
        db: AppDatabase,
        goalId: String,
        withProvenance: Boolean = false,
    ) {
        val assessment =
            emptyApplicableAssessment().copy(
                importance =
                    AxisAssessment(
                        ImportanceValue.LOW.name,
                        ValueOrigin.EXPLICIT,
                        provenance =
                            if (withProvenance) {
                                ValueProvenance(
                                    sourceType = LegacyOrientationSourceType.GOAL.name,
                                    sourceId = goalId,
                                    fieldName = "importance",
                                    rawValue = ImportanceValue.LOW.name,
                                )
                            } else {
                                null
                            },
                    ),
                impact =
                    AxisAssessment(
                        ImpactValue.TRANSFORMATIVE.name,
                        ValueOrigin.EXPLICIT,
                        provenance =
                            if (withProvenance) {
                                ValueProvenance(
                                    sourceType = LegacyOrientationSourceType.GOAL.name,
                                    sourceId = goalId,
                                    fieldName = "impact",
                                    rawValue = ImpactValue.TRANSFORMATIVE.name,
                                )
                            } else {
                                null
                            },
                    ),
            )
        CanonicalOrientationRepository(db, db.orientationDao()).saveOrientation(
            ManagedSubject(SUBJECT_ID, 100L, 100L, null, false, 1L, ManagedSubjectType.ORIENTATION, "Goal", null),
            OrientationNode(SUBJECT_ID, OrientationKind.GOAL, null, ValueOrigin.UNSET, assessment),
            OrientationAssessmentRevision(
                "revision-1",
                100L,
                100L,
                null,
                false,
                1L,
                SUBJECT_ID,
                100L,
                100L,
                AssessmentRevisionSource.MIGRATION,
                "seed",
                assessment,
            ),
        )
        db.orientationDao().upsertLegacyMappings(listOf(mapping(goalId, SUBJECT_ID)))
    }

    private suspend fun seedHistoricalProvenanceRevision(db: AppDatabase, goalId: String) {
        seedGoalOrientation(db, goalId, withProvenance = true)

        // Construct the historical V1 serialized shape in the test fixture.
        val revision = db.orientationDao().getAllAssessmentRevisions()
            .single { it.id == "revision-1" }
        val root = com.google.gson.JsonParser.parseString(revision.assessmentJson).asJsonObject

        listOf("effort", "cost", "risk", "evaluationState").forEach {
            root.remove(it)
        }
        check(root.getAsJsonObject("importance").has("provenance"))
        check(root.getAsJsonObject("impact").has("provenance"))

        db.orientationDao().upsertAssessmentRevisions(
            listOf(revision.copy(assessmentJson = root.toString())),
        )
    }

    private fun mapping(sourceId: String, subjectId: String) =
        LegacySubjectMappingEntity(
            id = "mapping-$sourceId",
            sourceType = LegacyOrientationSourceType.GOAL.name,
            sourceId = sourceId,
            subjectId = subjectId,
            migrationVersion = 1,
            state = LegacySubjectMappingState.CUT_OVER.name,
            createdAt = 100L,
            updatedAt = 100L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun legacyGoal(id: String = "goal-1") =
        Goal(
            id = id,
            text = "Goal",
            completed = false,
            createdAt = 100L,
            updatedAt = 100L,
            valueImportance = 2.5f,
            valueImpact = 11f,
            effort = 13f,
            cost = 4f,
            risk = 8f,
            weightEffort = 1.7f,
            weightCost = 0.6f,
            weightRisk = 1.2f,
            rawScore = -0.573368f,
            displayScore = 21,
            scoringStatus = ScoringStatusValues.ASSESSED,
        )

    private companion object {
        const val SUBJECT_ID = "orientation-goal-1"
    }
}
