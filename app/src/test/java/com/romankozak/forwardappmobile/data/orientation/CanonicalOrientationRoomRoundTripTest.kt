package com.romankozak.forwardappmobile.data.orientation

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.orientation.emptyApplicableAssessment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalOrientationRoomRoundTripTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val gson = Gson()

    @Test
    fun `canonical payload survives json and clean Room restore`() = runBlocking {
        val first = database()
        val second = database()
        try {
            first.orientationDao().storeCanonicalPayload(
                payload(version = 1, deleted = false),
                merge = false,
                workspaceDao = first.workspaceDao(),
            )
            val exported = first.orientationDao().exportBundle(first.workspaceDao())
            val decoded = gson.fromJson(gson.toJson(exported), SnapshotBundle::class.java)

            second.orientationDao().storeCanonicalPayload(decoded, merge = false, workspaceDao = second.workspaceDao())

            assertEquals(first.orientationDao().getAllManagedSubjects(), second.orientationDao().getAllManagedSubjects())
            assertEquals(first.workspaceDao().getAll(), second.workspaceDao().getAll())
            assertEquals(first.orientationDao().getAllAssessments(), second.orientationDao().getAllAssessments())
            assertEquals(first.orientationDao().getAllLegacyMappings(), second.orientationDao().getAllLegacyMappings())
        } finally {
            first.close()
            second.close()
        }
    }

    @Test
    fun `schema 151 Workspace JSON without provenance restores as Context-backed`() = runBlocking {
        val database = database()
        try {
            val legacyJson =
                gson.toJson(
                    payload(version = 1, deleted = false),
                ).replace(
                    "\"provenance\":\"CANONICAL_ONLY\",",
                    "",
                ).replace(
                    "\"sourceContextId\":null,",
                    "",
                )
            val decoded = gson.fromJson(legacyJson, SnapshotBundle::class.java)

            database.orientationDao().storeCanonicalPayload(
                decoded,
                merge = false,
                workspaceDao = database.workspaceDao(),
            )

            val workspace = database.workspaceDao().getById("workspace")
            assertEquals(WorkspaceProvenance.CONTEXT_BACKED.name, workspace?.provenance)
            assertEquals("workspace", workspace?.sourceContextId)
        } finally {
            database.close()
        }
    }

    @Test
    fun `repeated merge cannot resurrect a higher version tombstone`() = runBlocking {
        val database = database()
        try {
            database.orientationDao().storeCanonicalPayload(
                payload(version = 1, deleted = false), merge = true, workspaceDao = database.workspaceDao(),
            )
            database.orientationDao().storeCanonicalPayload(
                payload(version = 2, deleted = true), merge = true, workspaceDao = database.workspaceDao(),
            )
            database.orientationDao().storeCanonicalPayload(
                payload(version = 1, deleted = false), merge = true, workspaceDao = database.workspaceDao(),
            )

            val subject = database.orientationDao().getManagedSubject(SUBJECT_ID)
            assertEquals(2L, subject?.version)
            assertTrue(subject?.isDeleted == true)
            val workspace = database.workspaceDao().getById("workspace")
            assertEquals(2L, workspace?.version)
            assertTrue(workspace?.isDeleted == true)
        } finally {
            database.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun payload(version: Long, deleted: Boolean): SnapshotBundle {
        val updatedAt = version * 100
        val assessment = emptyApplicableAssessment()
        return SnapshotBundle(
            managedSubjects =
                listOf(
                    ManagedSubjectEntity(
                        id = SUBJECT_ID,
                        subjectType = "ORIENTATION",
                        title = "Goal",
                        description = null,
                        createdAt = 10,
                        updatedAt = updatedAt,
                        syncedAt = null,
                        isDeleted = deleted,
                        version = version,
                    ),
                ),
            orientations = listOf(OrientationEntity(SUBJECT_ID, "GOAL", null, "UNSET")),
            aspects = emptyList(),
            workspaces =
                listOf(
                    WorkspaceEntity(
                        id = "workspace",
                        nameOverride = "Workspace",
                        descriptionOverride = null,
                        parentWorkspaceId = null,
                        roleCode = null,
                        workspaceOrder = 0L,
                        createdAt = 10L,
                        updatedAt = updatedAt,
                        syncedAt = null,
                        isDeleted = deleted,
                        version = version,
                        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    ),
                ),
            orientationAssessments = listOf(assessmentEntity(version, updatedAt, deleted)),
            orientationAssessmentRevisions =
                listOf(
                    OrientationAssessmentRevisionEntity(
                        id = REVISION_ID,
                        orientationId = SUBJECT_ID,
                        effectiveFrom = updatedAt,
                        recordedAt = updatedAt,
                        source = "MIGRATION",
                        reason = null,
                        assessmentJson = gson.toJson(assessment),
                        createdAt = 10,
                        updatedAt = updatedAt,
                        syncedAt = null,
                        isDeleted = deleted,
                        version = version,
                    ),
                ),
            legacySubjectMappings =
                listOf(
                    LegacySubjectMappingEntity(
                        id = SUBJECT_ID,
                        sourceType = "GOAL",
                        sourceId = "legacy-goal",
                        subjectId = SUBJECT_ID,
                        migrationVersion = 1,
                        state = "MATERIALIZED",
                        createdAt = 10,
                        updatedAt = updatedAt,
                        syncedAt = null,
                        isDeleted = deleted,
                        version = version,
                    ),
                ),
            orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(),
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = emptyList(),
            savedOrientationViews = emptyList(),
        )
    }

    private fun assessmentEntity(version: Long, updatedAt: Long, deleted: Boolean) =
        OrientationAssessmentEntity(
            orientationId = SUBJECT_ID,
            revisionId = REVISION_ID,
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
            createdAt = 10,
            updatedAt = updatedAt,
            syncedAt = null,
            isDeleted = deleted,
            version = version,
        )

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

    companion object {
        private const val SUBJECT_ID = "subject"
        private const val REVISION_ID = "revision"
    }
}
