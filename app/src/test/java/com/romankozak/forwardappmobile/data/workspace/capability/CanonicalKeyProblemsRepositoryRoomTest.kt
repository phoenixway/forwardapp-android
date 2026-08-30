package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceProblemStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalKeyProblemsRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `create and update persist typed refs without update as create`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            seedTargets(database)
            val repository = repository(database)

            val problemId =
                repository.createProblem(
                    workspaceId = "owner",
                    title = "  Broken pump  ",
                    description = "  Needs inspection  ",
                    status = WorkspaceProblemStatus.BLOCKED,
                    relatedWorkspaceIds = listOf("related"),
                    relatedAttachmentIds = listOf("attachment"),
                    now = 10L,
                )

            var problem = requireNotNull(database.workspaceProblemDao().getProblem(problemId))
            assertEquals("Broken pump", problem.title)
            assertEquals("Needs inspection", problem.description)
            assertEquals(WorkspaceProblemStatus.BLOCKED.name, problem.status)
            assertEquals(0L, problem.problemOrder)
            assertEquals(1L, problem.version)
            assertFalse(problem.isDeleted)

            var workspaceRef =
                database.workspaceProblemDao().getWorkspaceRefs(problemId).single()
            var attachmentRef =
                database.workspaceProblemDao().getAttachmentRefs(problemId).single()

            assertEquals("related", workspaceRef.targetWorkspaceId)
            assertEquals("attachment", attachmentRef.attachmentId)
            assertFalse(workspaceRef.isDeleted)
            assertFalse(attachmentRef.isDeleted)

            val missingFailure =
                runCatching {
                    repository.updateProblem(
                        workspaceId = "owner",
                        problemId = "missing",
                        title = "Must not be created",
                        description = "",
                        status = WorkspaceProblemStatus.OPEN,
                        relatedWorkspaceIds = emptyList(),
                        relatedAttachmentIds = emptyList(),
                        now = 15L,
                    )
                }.exceptionOrNull()

            assertTrue(missingFailure is IllegalArgumentException)
            assertEquals(1, database.workspaceProblemDao().getAllProblems().size)

            repository.updateProblem(
                workspaceId = "owner",
                problemId = problemId,
                title = "  Pump fixed  ",
                description = "  Verified  ",
                status = WorkspaceProblemStatus.RESOLVED,
                relatedWorkspaceIds = emptyList(),
                relatedAttachmentIds = listOf("attachment"),
                now = 20L,
            )

            problem = requireNotNull(database.workspaceProblemDao().getProblem(problemId))
            workspaceRef = database.workspaceProblemDao().getWorkspaceRefs(problemId).single()
            attachmentRef = database.workspaceProblemDao().getAttachmentRefs(problemId).single()

            assertEquals("Pump fixed", problem.title)
            assertEquals("Verified", problem.description)
            assertEquals(WorkspaceProblemStatus.RESOLVED.name, problem.status)
            assertEquals(2L, problem.version)
            assertEquals(20L, problem.updatedAt)
            assertTrue(workspaceRef.isDeleted)
            assertEquals(2L, workspaceRef.version)
            assertFalse(attachmentRef.isDeleted)
            assertEquals(1L, attachmentRef.version)

            val readItem = repository.getItems("owner").single()
            assertEquals(problemId, readItem.problem.id)
            assertTrue(readItem.relatedWorkspaceIds.isEmpty())
            assertEquals(listOf("attachment"), readItem.relatedAttachmentIds)
        } finally {
            database.close()
        }
    }

    @Test
    fun `delete tombstones owned refs and compacts remaining order`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            seedTargets(database)
            val repository = repository(database)

            val first =
                repository.createProblem(
                    workspaceId = "owner",
                    title = "First",
                    relatedWorkspaceIds = listOf("related"),
                    relatedAttachmentIds = listOf("attachment"),
                    now = 10L,
                )
            val second =
                repository.createProblem(
                    workspaceId = "owner",
                    title = "Second",
                    now = 11L,
                )

            repository.deleteProblem(
                workspaceId = "owner",
                problemId = first,
                now = 20L,
            )

            val deleted = requireNotNull(database.workspaceProblemDao().getProblem(first))
            assertTrue(deleted.isDeleted)
            assertEquals(2L, deleted.version)
            assertEquals(20L, deleted.updatedAt)

            val workspaceRef =
                database.workspaceProblemDao().getWorkspaceRefs(first).single()
            val attachmentRef =
                database.workspaceProblemDao().getAttachmentRefs(first).single()
            assertTrue(workspaceRef.isDeleted)
            assertTrue(attachmentRef.isDeleted)
            assertEquals(2L, workspaceRef.version)
            assertEquals(2L, attachmentRef.version)

            val remaining = requireNotNull(database.workspaceProblemDao().getProblem(second))
            assertFalse(remaining.isDeleted)
            assertEquals(0L, remaining.problemOrder)
            assertEquals(2L, remaining.version)
            assertEquals(20L, remaining.updatedAt)

            assertEquals(listOf(second), repository.getItems("owner").map { it.problem.id })
        } finally {
            database.close()
        }
    }

    @Test
    fun `capability deletion preserves owned KEY_PROBLEMS content`() = runBlocking {
        val database = database()
        try {
            seedOwner(database)
            val repository = repository(database)

            val problemId =
                repository.createProblem(
                    workspaceId = "owner",
                    title = "Preserve me",
                    now = 10L,
                )

            repository.deleteCapability("owner", now = 20L)

            val capability =
                database.orientationDao().getAllWorkspaceCapabilities()
                    .single { it.id == "key-problems-owner" }
            assertTrue(capability.isDeleted)

            val preserved = requireNotNull(database.workspaceProblemDao().getProblem(problemId))
            assertFalse(preserved.isDeleted)
            assertEquals("Preserve me", preserved.title)
            assertEquals(1, database.workspaceProblemDao().getAllProblems().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun `context backed Workspace is canonical KEY_PROBLEMS authority after cutover`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(
                listOf(
                    workspace("legacy-owner").copy(
                        provenance = "CONTEXT_BACKED",
                        sourceContextId = "legacy-owner",
                    ),
                ),
            )
            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(capability("legacy-owner")),
            )

            val repository = repository(database)
            val problemId =
                repository.createProblem(
                    workspaceId = "legacy-owner",
                    title = "Canonical after cutover",
                    now = 10L,
                )

            assertNotNull(database.workspaceProblemDao().getProblem(problemId))
        } finally {
            database.close()
        }
    }

    private fun repository(database: AppDatabase) =
        CanonicalKeyProblemsRepository(
            database = database,
            instanceStore =
                CanonicalCapabilityInstanceStore(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    orientationDao = database.orientationDao(),
                ),
            problemDao = database.workspaceProblemDao(),
            workspaceDao = database.workspaceDao(),
            attachmentDao = database.attachmentDao(),
        )

    private suspend fun seedOwner(database: AppDatabase) {
        database.workspaceDao().upsert(listOf(workspace("owner")))
        database.orientationDao().upsertWorkspaceCapabilities(
            listOf(capability("owner")),
        )
    }

    private suspend fun seedTargets(database: AppDatabase) {
        database.workspaceDao().upsert(listOf(workspace("related")))
        database.attachmentDao().insertAttachment(attachment())
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun workspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = "CANONICAL_ONLY",
            sourceContextId = null,
        )

    private fun capability(workspaceId: String) =
        WorkspaceCapabilityInstanceEntity(
            id = "key-problems-$workspaceId",
            workspaceId = workspaceId,
            capabilityType = "KEY_PROBLEMS",
            instanceKey = "default",
            capabilityOrder = 3L,
            state = "ACTIVE",
            configurationVersion = 1,
            configuration = "{}",
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun attachment() =
        AttachmentEntity(
            id = "attachment",
            attachmentType = "FILE",
            entityId = "entity",
            ownerContextId = null,
            roleCode = null,
            isSystem = false,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )
}
