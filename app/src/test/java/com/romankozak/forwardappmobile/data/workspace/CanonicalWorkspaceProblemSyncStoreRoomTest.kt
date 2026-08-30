package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemAttachmentRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemAttachmentRefSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemSyncVersion
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemWorkspaceRefSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceProblemWorkspaceRefSyncVersion
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.sync.datasource.CanonicalWorkspaceProblemSyncAck
import com.romankozak.forwardappmobile.sync.datasource.CanonicalWorkspaceProblemSyncPayload
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceProblemSyncStoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `whole canonical payload merges and exposes unsynced and timestamp deltas`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            val store = store(database)

            val payload =
                CanonicalWorkspaceProblemSyncPayload(
                    problems = listOf(problem(updatedAt = 30L, version = 3L)),
                    workspaceRefs = listOf(workspaceRef(updatedAt = 31L, version = 2L)),
                    attachmentRefs = listOf(attachmentRef(updatedAt = 32L, version = 4L)),
                )

            store.mergeIncoming(payload)

            val all = store.loadAll()
            assertEquals(listOf("problem"), all.problems.map { it.id })
            assertEquals(listOf("problem-workspace-ref"), all.workspaceRefs.map { it.id })
            assertEquals(listOf("problem-attachment-ref"), all.attachmentRefs.map { it.id })

            val unsynced = store.loadUnsynced()
            assertEquals(listOf("problem"), unsynced.problems.map { it.id })
            assertEquals(listOf("problem-workspace-ref"), unsynced.workspaceRefs.map { it.id })
            assertEquals(listOf("problem-attachment-ref"), unsynced.attachmentRefs.map { it.id })

            val changed = store.loadChangedSince(30L)
            assertTrue(changed.problems.isEmpty())
            assertEquals(listOf("problem-workspace-ref"), changed.workspaceRefs.map { it.id })
            assertEquals(listOf("problem-attachment-ref"), changed.attachmentRefs.map { it.id })

            val persisted = requireNotNull(database.workspaceProblemDao().getProblem("problem"))
            assertEquals("owner", persisted.workspaceId)
            assertEquals("key-problems-owner", persisted.capabilityInstanceId)
            assertEquals(3L, persisted.version)
            assertNull(persisted.syncedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `exact version acknowledgement marks only matching canonical rows synced`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            val store = store(database)

            store.mergeIncoming(
                CanonicalWorkspaceProblemSyncPayload(
                    problems = listOf(problem(version = 3L)),
                    workspaceRefs = listOf(workspaceRef(version = 4L)),
                    attachmentRefs = listOf(attachmentRef(version = 5L)),
                ),
            )

            store.markSynced(
                CanonicalWorkspaceProblemSyncAck(
                    problems = listOf(WorkspaceProblemSyncVersion("problem", 2L)),
                    workspaceRefs =
                        listOf(
                            WorkspaceProblemWorkspaceRefSyncVersion(
                                "problem-workspace-ref",
                                3L,
                            ),
                        ),
                    attachmentRefs =
                        listOf(
                            WorkspaceProblemAttachmentRefSyncVersion(
                                "problem-attachment-ref",
                                4L,
                            ),
                        ),
                ),
            )

            assertNull(database.workspaceProblemDao().getProblem("problem")!!.syncedAt)
            assertNull(
                database.workspaceProblemDao()
                    .getWorkspaceRefs("problem")
                    .single()
                    .syncedAt,
            )
            assertNull(
                database.workspaceProblemDao()
                    .getAttachmentRefs("problem")
                    .single()
                    .syncedAt,
            )

            store.markSynced(
                CanonicalWorkspaceProblemSyncAck(
                    problems = listOf(WorkspaceProblemSyncVersion("problem", 3L)),
                    workspaceRefs =
                        listOf(
                            WorkspaceProblemWorkspaceRefSyncVersion(
                                "problem-workspace-ref",
                                4L,
                            ),
                        ),
                    attachmentRefs =
                        listOf(
                            WorkspaceProblemAttachmentRefSyncVersion(
                                "problem-attachment-ref",
                                5L,
                            ),
                        ),
                ),
            )

            assertNotNull(database.workspaceProblemDao().getProblem("problem")!!.syncedAt)
            assertNotNull(
                database.workspaceProblemDao()
                    .getWorkspaceRefs("problem")
                    .single()
                    .syncedAt,
            )
            assertNotNull(
                database.workspaceProblemDao()
                    .getAttachmentRefs("problem")
                    .single()
                    .syncedAt,
            )

            val unsynced = store.loadUnsynced()
            assertTrue(unsynced.problems.isEmpty())
            assertTrue(unsynced.workspaceRefs.isEmpty())
            assertTrue(unsynced.attachmentRefs.isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `equal freshness tombstone wins and equal live cannot resurrect Problem`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            val store = store(database)

            val live = problem(version = 5L, updatedAt = 50L, deleted = false)
            store.mergeIncoming(
                CanonicalWorkspaceProblemSyncPayload(problems = listOf(live)),
            )

            store.mergeIncoming(
                CanonicalWorkspaceProblemSyncPayload(
                    problems = listOf(live.copy(isDeleted = true)),
                ),
            )

            var persisted = requireNotNull(database.workspaceProblemDao().getProblem("problem"))
            assertTrue(persisted.isDeleted)

            store.mergeIncoming(
                CanonicalWorkspaceProblemSyncPayload(problems = listOf(live)),
            )

            persisted = requireNotNull(database.workspaceProblemDao().getProblem("problem"))
            assertTrue(persisted.isDeleted)
            assertEquals(5L, persisted.version)
            assertEquals(50L, persisted.updatedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `immutable ownership and whole contract failures roll back incoming mutation`() = runBlocking {
        val database = database()
        try {
            seedDependencies(database)
            database.workspaceDao().upsert(listOf(workspace("owner-2")))
            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(capability("owner-2")),
            )

            val store = store(database)
            val originalProblem = problem()
            val originalWorkspaceRef = workspaceRef()

            store.mergeIncoming(
                CanonicalWorkspaceProblemSyncPayload(
                    problems = listOf(originalProblem),
                    workspaceRefs = listOf(originalWorkspaceRef),
                ),
            )

            val ownerFailure =
                runCatching {
                    store.mergeIncoming(
                        CanonicalWorkspaceProblemSyncPayload(
                            problems =
                                listOf(
                                    originalProblem.copy(
                                        workspaceId = "owner-2",
                                        capabilityInstanceId = "key-problems-owner-2",
                                        version = 2L,
                                        updatedAt = 20L,
                                    ),
                                ),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(ownerFailure is IllegalArgumentException)

            var preserved = requireNotNull(database.workspaceProblemDao().getProblem("problem"))
            assertEquals("owner", preserved.workspaceId)
            assertEquals(1L, preserved.version)
            assertFalse(preserved.isDeleted)

            val contractFailure =
                runCatching {
                    store.mergeIncoming(
                        CanonicalWorkspaceProblemSyncPayload(
                            problems =
                                listOf(
                                    originalProblem.copy(
                                        isDeleted = true,
                                        version = 2L,
                                        updatedAt = 20L,
                                    ),
                                ),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(contractFailure is IllegalArgumentException)

            preserved = requireNotNull(database.workspaceProblemDao().getProblem("problem"))
            assertFalse(preserved.isDeleted)
            assertEquals(1L, preserved.version)

            val preservedRef =
                database.workspaceProblemDao().getWorkspaceRefs("problem").single()
            assertFalse(preservedRef.isDeleted)
            assertEquals("related", preservedRef.targetWorkspaceId)
        } finally {
            database.close()
        }
    }

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun store(database: AppDatabase) =
        CanonicalWorkspaceProblemSyncStore(
            database = database,
            problemDao = database.workspaceProblemDao(),
            workspaceDao = database.workspaceDao(),
            orientationDao = database.orientationDao(),
            attachmentDao = database.attachmentDao(),
        )

    private suspend fun seedDependencies(database: AppDatabase) {
        database.workspaceDao().upsert(
            listOf(
                workspace("owner"),
                workspace("related"),
            ),
        )
        database.orientationDao().upsertWorkspaceCapabilities(
            listOf(capability("owner")),
        )
        database.attachmentDao().insertAttachment(attachment())
    }

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

    private fun problem(
        workspaceId: String = "owner",
        capabilityInstanceId: String = "key-problems-owner",
        version: Long = 1L,
        updatedAt: Long = 10L,
        deleted: Boolean = false,
    ) =
        WorkspaceProblemSnapshot(
            id = "problem",
            workspaceId = workspaceId,
            capabilityInstanceId = capabilityInstanceId,
            title = "Problem",
            description = "Details",
            status = "OPEN",
            order = 0L,
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = deleted,
        )

    private fun workspaceRef(
        version: Long = 1L,
        updatedAt: Long = 10L,
        deleted: Boolean = false,
    ) =
        WorkspaceProblemWorkspaceRefSnapshot(
            id = "problem-workspace-ref",
            problemId = "problem",
            targetWorkspaceId = "related",
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = deleted,
        )

    private fun attachmentRef(
        version: Long = 1L,
        updatedAt: Long = 10L,
        deleted: Boolean = false,
    ) =
        WorkspaceProblemAttachmentRefSnapshot(
            id = "problem-attachment-ref",
            problemId = "problem",
            attachmentId = "attachment",
            createdAt = 1L,
            updatedAt = updatedAt,
            version = version,
            isDeleted = deleted,
        )
}
