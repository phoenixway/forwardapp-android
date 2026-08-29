package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.CanonicalExecutionLogSnapshot
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.sync.datasource.CanonicalExecutionLogSyncVersion
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
class CanonicalExecutionLogSyncStoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `canonical snapshot persists in canonical partition and round trips`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(canonicalWorkspace("workspace")))
            val store = store(database)
            val incoming = canonicalLog(id = "log", version = 3L, updatedAt = 30L)

            store.mergeIncoming(listOf(incoming))

            val persisted = database.contextManagementDao().getAllLogs().single()
            assertNull(persisted.contextId)
            assertEquals("workspace", persisted.workspaceId)
            assertEquals(3L, persisted.version)
            assertEquals(listOf(incoming), store.loadAll())
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical ingress rejects Context backed Workspace and legacy id collision`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(
                listOf(
                    contextBackedWorkspace("legacy-workspace"),
                    canonicalWorkspace("canonical-workspace"),
                ),
            )
            val store = store(database)

            val contextEntity =
                ContextEntity(
                    id = "context",
                    name = "Legacy",
                    description = null,
                    parentId = null,
                    createdAt = 1L,
                    updatedAt = 2L,
                    roleCode = "management",
                )
            database.contextDao().insert(contextEntity)
            database.contextManagementDao().insertLog(
                ContextLog(
                    id = "shared-id",
                    contextId = contextEntity.id,
                    timestamp = 10L,
                    type = "COMMENT",
                    description = "Legacy row",
                    updatedAt = 10L,
                    version = 1L,
                ),
            )

            val provenanceFailure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            canonicalLog(
                                id = "wrong-owner",
                                workspaceId = "legacy-workspace",
                            ),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(provenanceFailure is IllegalArgumentException)

            val collisionFailure =
                runCatching {
                    store.mergeIncoming(
                        listOf(
                            canonicalLog(
                                id = "shared-id",
                                workspaceId = "canonical-workspace",
                            ),
                        ),
                    )
                }.exceptionOrNull()

            assertTrue(collisionFailure is IllegalArgumentException)

            val preserved = database.contextManagementDao().getAllLogs().single()
            assertEquals("context", preserved.contextId)
            assertNull(preserved.workspaceId)
            assertEquals("Legacy row", preserved.description)
        } finally {
            database.close()
        }
    }

    @Test
    fun `equal freshness tombstone wins and equal live cannot resurrect it`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(canonicalWorkspace("workspace")))
            val store = store(database)

            store.mergeIncoming(
                listOf(
                    canonicalLog(
                        id = "log",
                        version = 5L,
                        updatedAt = 50L,
                        deleted = false,
                    ),
                ),
            )

            store.mergeIncoming(
                listOf(
                    canonicalLog(
                        id = "log",
                        version = 5L,
                        updatedAt = 50L,
                        deleted = true,
                    ),
                ),
            )

            var persisted = database.contextManagementDao().getAllLogs().single()
            assertTrue(persisted.isDeleted)

            store.mergeIncoming(
                listOf(
                    canonicalLog(
                        id = "log",
                        version = 5L,
                        updatedAt = 50L,
                        deleted = false,
                    ),
                ),
            )

            persisted = database.contextManagementDao().getAllLogs().single()
            assertTrue(persisted.isDeleted)
            assertEquals(5L, persisted.version)
            assertEquals(50L, persisted.updatedAt)
        } finally {
            database.close()
        }
    }

    @Test
    fun `sync acknowledgement is exact version and canonical partition only`() = runBlocking {
        val database = database()
        try {
            database.workspaceDao().upsert(listOf(canonicalWorkspace("workspace")))
            val store = store(database)

            store.mergeIncoming(
                listOf(
                    canonicalLog(
                        id = "log",
                        version = 7L,
                        updatedAt = 70L,
                    ),
                ),
            )

            store.markSynced(listOf(CanonicalExecutionLogSyncVersion("log", 6L)))
            assertNull(database.contextManagementDao().getAllLogs().single().syncedAt)

            store.markSynced(listOf(CanonicalExecutionLogSyncVersion("log", 7L)))
            assertNotNull(database.contextManagementDao().getAllLogs().single().syncedAt)
            assertFalse(database.contextManagementDao().getAllLogs().single().isDeleted)
        } finally {
            database.close()
        }
    }

    private fun store(database: AppDatabase) =
        CanonicalExecutionLogSyncStore(
            database = database,
            contextManagementDao = database.contextManagementDao(),
            workspaceDao = database.workspaceDao(),
        )

    private fun database() =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private fun canonicalWorkspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = "Canonical",
            descriptionOverride = null,
            parentWorkspaceId = null,
            roleCode = null,
            workspaceOrder = 0L,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun contextBackedWorkspace(id: String) =
        canonicalWorkspace(id).copy(
            nameOverride = "Legacy",
            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
            sourceContextId = id,
        )

    private fun canonicalLog(
        id: String,
        workspaceId: String = "workspace",
        version: Long = 1L,
        updatedAt: Long = 10L,
        deleted: Boolean = false,
    ) =
        CanonicalExecutionLogSnapshot(
            id = id,
            workspaceId = workspaceId,
            timestamp = 5L,
            type = "COMMENT",
            description = "Canonical row",
            details = null,
            updatedAt = updatedAt,
            version = version,
            isDeleted = deleted,
        )
}
