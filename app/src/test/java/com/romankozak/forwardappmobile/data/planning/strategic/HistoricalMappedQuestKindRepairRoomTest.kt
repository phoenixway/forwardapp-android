package com.romankozak.forwardappmobile.data.planning.strategic

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectConversionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.initialOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HistoricalMappedQuestKindRepairRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `retires only unsupported unsynced fact and preserves quest assessment history`() = runBlocking {
        withDatabase { db ->
            val id = seedUnsupportedConversion(db, "quest-one")
            val beforeQuest = db.arcQuestDao().getById("quest-one")
            val beforeCurrent = db.orientationDao().getAllAssessments().single()
            val beforeRevisions = db.orientationDao().getAllAssessmentRevisions()

            assertEquals(1, db.withTransaction { repairHistoricalMappedQuestKinds(db, 100L) })
            assertEquals(0, db.withTransaction { repairHistoricalMappedQuestKinds(db, 200L) })

            assertEquals(OrientationKind.QUEST.name, db.orientationDao().getOrientation(id)?.kind)
            assertEquals(3L, db.orientationDao().getManagedSubject(id)?.version)
            assertTrue(db.managedSubjectConversionDao().getAll().isEmpty())
            assertEquals(beforeQuest, db.arcQuestDao().getById("quest-one"))
            assertEquals(beforeCurrent, db.orientationDao().getAllAssessments().single())
            assertEquals(beforeRevisions, db.orientationDao().getAllAssessmentRevisions())
            assertEquals(OrientationKind.QUEST, CanonicalOrientationRepository(db, db.orientationDao())
                .getOrientationSemantics(id)?.kind)
        }
    }

    @Test
    fun `synced historical conversion prevents every candidate from changing`() = runBlocking {
        withDatabase { db ->
            val safe = seedUnsupportedConversion(db, "quest-safe")
            val unsafe = seedUnsupportedConversion(db, "quest-synced", syncedAt = 20L)

            val failure = runCatching { db.withTransaction { repairHistoricalMappedQuestKinds(db, 100L) } }.exceptionOrNull()

            assertTrue(failure?.message?.contains("conversionHistory=") == true)
            assertEquals(OrientationKind.DIRECTION.name, db.orientationDao().getOrientation(safe)?.kind)
            assertEquals(OrientationKind.DIRECTION.name, db.orientationDao().getOrientation(unsafe)?.kind)
            assertEquals(2, db.managedSubjectConversionDao().getAll().size)
            assertFalse(db.orientationDao().getAllManagedSubjects().any { it.version == 3L })
        }
    }

    private suspend fun seedUnsupportedConversion(db: AppDatabase, questId: String, syncedAt: Long? = null): String {
        val dao = db.orientationDao()
        val id = LegacySubjectUuid.resolve(LegacySubjectRef(LegacyOrientationSourceType.ARC_QUEST, questId))
        val quest = ArcQuestEntity(id = questId, arcKey = "arc", title = "Quest", createdAt = 1L, version = 1L)
        db.arcQuestDao().insert(quest)
        dao.upsertManagedSubjects(listOf(ManagedSubjectEntity(id, "ORIENTATION", "Quest", null, 1L, 10L, null, false, 2L)))
        dao.upsertOrientations(listOf(OrientationEntity(id, OrientationKind.DIRECTION.name, null, "UNSET")))
        dao.upsertLegacyMappings(listOf(LegacySubjectMappingEntity(
            id, LegacyOrientationSourceType.ARC_QUEST.name, questId, id, 1, "CUT_OVER", 1L, 1L, null, false, 1L,
        )))

        val priorId = "$id:initial"
        val resultId = "$id:kind:1:assessment"
        val json = Gson().toJson(initialOrientationAssessment(OrientationKind.QUEST))
        dao.upsertAssessmentRevisions(listOf(
            OrientationAssessmentRevisionEntity(priorId, id, 1L, 1L, "MIGRATION", "Initial Quest", json, 1L, 1L, null, false, 1L),
            OrientationAssessmentRevisionEntity(resultId, id, 10L, 10L, "USER", "Explicit kind change QUEST -> DIRECTION", json, 10L, 10L, null, false, 1L),
        ))
        dao.upsertAssessments(listOf(emptyCurrent(id, resultId)))
        db.managedSubjectConversionDao().insert(ManagedSubjectConversionEntity(
            id = "$id:ORIENTATION_KIND:1",
            subjectId = id,
            formatVersion = 3,
            fromType = "ORIENTATION",
            toType = "ORIENTATION",
            targetOrientationKind = OrientationKind.DIRECTION.name,
            fromSubjectVersion = 1L,
            toSubjectVersion = 2L,
            previousAspectParentId = null,
            previousAspectOrder = 0L,
            previousAspectArchived = false,
            createdAt = 10L,
            updatedAt = 10L,
            version = 1L,
            syncedAt = syncedAt,
            fromOrientationKind = OrientationKind.QUEST.name,
            previousAssessmentRevisionIdsJson = Gson().toJson(listOf(priorId)),
        ))
        return id
    }

    private fun emptyCurrent(id: String, revisionId: String) = OrientationAssessmentEntity(
        orientationId = id,
        revisionId = revisionId,
        importanceValue = null,
        importanceOrigin = "UNSET",
        impactValue = null,
        impactOrigin = "UNSET",
        breadthValue = null,
        breadthOrigin = "UNSET",
        expectedSpanValue = null,
        expectedSpanOrigin = "UNSET",
        targetWindowValue = null,
        targetWindowOrigin = "UNSET",
        attentionTierValue = null,
        attentionTierOrigin = "UNSET",
        commitmentValue = null,
        commitmentOrigin = "UNSET",
        confidenceValue = null,
        confidenceOrigin = "UNSET",
        provenanceJson = "[]",
        createdAt = 1L,
        updatedAt = 10L,
        syncedAt = null,
        isDeleted = false,
        version = 2L,
    )

    private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            block(db)
        } finally {
            db.close()
        }
    }
}
