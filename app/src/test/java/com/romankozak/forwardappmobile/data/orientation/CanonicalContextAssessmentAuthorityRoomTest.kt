package com.romankozak.forwardappmobile.data.orientation

import android.content.Context as AndroidContext
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.AssessmentRevisionSource
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubject
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessmentRevision
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationNode
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.emptyApplicableAssessment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalContextAssessmentAuthorityRoomTest {
    private val androidContext: AndroidContext = ApplicationProvider.getApplicationContext()

    @Test
    fun `CUT_OVER Context assessment reads canonical axes and ignores stale Context shadow`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(
                legacyContext(
                    id = CONTEXT_ID,
                    valueImportance = 12f,
                    valueImpact = 13f,
                ),
            )
            seedCutOverOrientation(
                database = database,
                contextId = CONTEXT_ID,
                importance = axis(ImportanceValue.MEDIUM, ValueOrigin.DERIVED),
                impact = axis(ImpactValue.SMALL, ValueOrigin.INHERITED),
            )

            val repository = CanonicalOrientationRepository(database, database.orientationDao())
            val assessment = repository.getContextAssessment(CONTEXT_ID)

            assertEquals(ImportanceValue.MEDIUM.name, assessment?.importance?.valueCode)
            assertEquals(ValueOrigin.DERIVED, assessment?.importance?.origin)
            assertEquals(ImpactValue.SMALL.name, assessment?.impact?.valueCode)
            assertEquals(ValueOrigin.INHERITED, assessment?.impact?.origin)

            val legacy = requireNotNull(database.contextDao().getContextById(CONTEXT_ID))
            assertEquals(12f, legacy.valueImportance)
            assertEquals(13f, legacy.valueImpact)
        } finally {
            database.close()
        }
    }

    @Test
    fun `Context assessment requires live CUT_OVER Orientation mapping`() = runBlocking {
        val database = database()
        try {
            val repository = CanonicalOrientationRepository(database, database.orientationDao())

            seedCutOverOrientation(
                database = database,
                contextId = "materialized",
                mappingState = LegacySubjectMappingState.MATERIALIZED,
            )
            assertNull(repository.getContextAssessment("materialized"))

            seedCutOverAspect(
                database = database,
                contextId = "aspect",
            )
            assertNull(repository.getContextAssessment("aspect"))

            assertNull(repository.getContextAssessment("workspace-only"))
        } finally {
            database.close()
        }
    }

    @Test
    fun `shell free CUT_OVER Context mapping exposes canonical assessment without Context row`() = runBlocking {
        val database = database()
        try {
            seedCutOverOrientation(
                database = database,
                contextId = CONTEXT_ID,
                importance = axis(ImportanceValue.HIGH, ValueOrigin.EXPLICIT),
                impact = axis(ImpactValue.LARGE, ValueOrigin.EXPLICIT),
            )

            assertNull(database.contextDao().getContextById(CONTEXT_ID))

            val assessment =
                CanonicalOrientationRepository(database, database.orientationDao())
                    .getContextAssessment(CONTEXT_ID)

            assertEquals(ImportanceValue.HIGH.name, assessment?.importance?.valueCode)
            assertEquals(ImpactValue.LARGE.name, assessment?.impact?.valueCode)
            assertNull(database.contextDao().getContextById(CONTEXT_ID))
        } finally {
            database.close()
        }
    }

    @Test
    fun `assessment edit writes canonical revision only and preserves non edited axes`() = runBlocking {
        val database = database()
        try {
            database.contextDao().insert(
                legacyContext(
                    id = CONTEXT_ID,
                    valueImportance = 2f,
                    valueImpact = 2f,
                ),
            )
            val original =
                emptyApplicableAssessment().copy(
                    importance = axis(ImportanceValue.LOW, ValueOrigin.DERIVED),
                    impact = axis(ImpactValue.SMALL, ValueOrigin.INHERITED),
                    breadth = AxisAssessment("SYSTEM", ValueOrigin.EXPLICIT),
                )
            seedCutOverOrientation(
                database = database,
                contextId = CONTEXT_ID,
                assessment = original,
            )

            val repository = CanonicalOrientationRepository(database, database.orientationDao())
            val revisionsBefore = database.orientationDao().getAllAssessmentRevisions()

            val updated =
                repository.updateContextAssessmentAxes(
                    contextId = CONTEXT_ID,
                    importance = axis(ImportanceValue.CRITICAL, ValueOrigin.EXPLICIT),
                    impact = AxisAssessment(valueCode = null, origin = ValueOrigin.UNSET),
                    now = 500L,
                )

            assertTrue(updated)

            val current = requireNotNull(repository.getContextAssessment(CONTEXT_ID))
            assertEquals(ImportanceValue.CRITICAL.name, current.importance.valueCode)
            assertEquals(ValueOrigin.EXPLICIT, current.importance.origin)
            assertNull(current.impact.valueCode)
            assertEquals(ValueOrigin.UNSET, current.impact.origin)
            assertEquals(original.breadth, current.breadth)
            assertEquals(original.expectedSpan, current.expectedSpan)
            assertEquals(original.targetWindow, current.targetWindow)
            assertEquals(original.attentionTier, current.attentionTier)
            assertEquals(original.commitment, current.commitment)
            assertEquals(original.confidence, current.confidence)

            val revisionsAfter = database.orientationDao().getAllAssessmentRevisions()
            assertEquals(revisionsBefore.size + 1, revisionsAfter.size)
            val userRevision = revisionsAfter.single { it.id !in revisionsBefore.map { before -> before.id } }
            assertEquals(AssessmentRevisionSource.USER.name, userRevision.source)
            assertEquals(CONTEXT_ASSESSMENT_EDIT_REASON_FOR_TEST, userRevision.reason)
            assertFalse(userRevision.isDeleted)

            val legacy = requireNotNull(database.contextDao().getContextById(CONTEXT_ID))
            assertEquals(2f, legacy.valueImportance)
            assertEquals(2f, legacy.valueImpact)
        } finally {
            database.close()
        }
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(androidContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private suspend fun seedCutOverOrientation(
        database: AppDatabase,
        contextId: String,
        mappingState: LegacySubjectMappingState = LegacySubjectMappingState.CUT_OVER,
        importance: AxisAssessment = axis(ImportanceValue.MEDIUM, ValueOrigin.DERIVED),
        impact: AxisAssessment = axis(ImpactValue.MEDIUM, ValueOrigin.DERIVED),
        assessment: OrientationAssessment =
            emptyApplicableAssessment().copy(
                importance = importance,
                impact = impact,
            ),
    ) {
        val subjectId = "orientation-$contextId"
        val subject =
            ManagedSubject(
                id = subjectId,
                createdAt = 100L,
                updatedAt = 100L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
                subjectType = ManagedSubjectType.ORIENTATION,
                title = "Canonical $contextId",
                description = null,
            )
        val orientation =
            OrientationNode(
                subjectId = subjectId,
                kind = OrientationKind.GOAL,
                lifecycle = null,
                lifecycleOrigin = ValueOrigin.UNSET,
                assessment = assessment,
            )
        val revision =
            OrientationAssessmentRevision(
                id = "revision-$contextId",
                createdAt = 100L,
                updatedAt = 100L,
                syncedAt = null,
                isDeleted = false,
                version = 1L,
                orientationId = subjectId,
                effectiveFrom = 100L,
                recordedAt = 100L,
                source = AssessmentRevisionSource.MIGRATION,
                reason = "test seed",
                assessment = assessment,
            )

        CanonicalOrientationRepository(database, database.orientationDao())
            .saveOrientation(subject, orientation, revision)

        database.orientationDao().upsertLegacyMappings(
            listOf(
                com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity(
                    id = "mapping-$contextId",
                    sourceType = LegacyOrientationSourceType.CONTEXT.name,
                    sourceId = contextId,
                    subjectId = subjectId,
                    migrationVersion = 1,
                    state = mappingState.name,
                    createdAt = 100L,
                    updatedAt = 100L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private suspend fun seedCutOverAspect(
        database: AppDatabase,
        contextId: String,
    ) {
        val subjectId = "aspect-$contextId"
        database.orientationDao().upsertManagedSubjects(
            listOf(
                com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity(
                    id = subjectId,
                    subjectType = ManagedSubjectType.ASPECT.name,
                    title = "Aspect",
                    description = null,
                    createdAt = 100L,
                    updatedAt = 100L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
        database.orientationDao().upsertAspects(
            listOf(
                com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectEntity(
                    subjectId = subjectId,
                    parentAspectId = null,
                    aspectOrder = 0L,
                    archived = false,
                ),
            ),
        )
        database.orientationDao().upsertLegacyMappings(
            listOf(
                com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity(
                    id = "mapping-$contextId",
                    sourceType = LegacyOrientationSourceType.CONTEXT.name,
                    sourceId = contextId,
                    subjectId = subjectId,
                    migrationVersion = 1,
                    state = LegacySubjectMappingState.CUT_OVER.name,
                    createdAt = 100L,
                    updatedAt = 100L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private fun legacyContext(
        id: String,
        valueImportance: Float,
        valueImpact: Float,
    ): Context =
        Context(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 100L,
            updatedAt = 100L,
            valueImportance = valueImportance,
            valueImpact = valueImpact,
        )

    private fun axis(
        value: Enum<*>,
        origin: ValueOrigin,
    ) = AxisAssessment(valueCode = value.name, origin = origin)

    private companion object {
        const val CONTEXT_ID = "ordinary-context"
        const val CONTEXT_ASSESSMENT_EDIT_REASON_FOR_TEST =
            "Canonical Context assessment edited from Context Settings"
    }
}
