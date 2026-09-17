package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceTagRefEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationBootstrapper
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationSyncStore
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.sync.datasource.CanonicalOrientationSyncAck
import com.romankozak.forwardappmobile.sync.datasource.CanonicalWorkspaceTagRefSyncVersion
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceTagTransportStoreRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `merge uses version updatedAt and tombstone freshness without Context rows`() = runBlocking {
        val database = database()
        try {
            insertWorkspace(database)
            val store = CanonicalWorkspaceTagTransportStore(database)
            val local = tag(version = 4L, updatedAt = 40L, isDeleted = false)
            database.workspaceTagRefDao().upsert(listOf(local))

            store.mergeIncoming(listOf(local.copy(version = 3L, updatedAt = 99L, isDeleted = true)))
            assertEquals(local, database.workspaceTagRefDao().getAllForWorkspace(OWNER).single())

            val tombstone = local.copy(version = 4L, updatedAt = 40L, isDeleted = true, syncedAt = null)
            store.mergeIncoming(listOf(tombstone))
            assertEquals(tombstone, database.workspaceTagRefDao().getAllForWorkspace(OWNER).single())
        } finally {
            database.close()
        }
    }

    @Test
    fun `SnapshotBundle distinguishes absent tag transport from authoritative empty collection`() {
        val gson = Gson()
        assertNull(gson.fromJson("{}", SnapshotBundle::class.java).workspaceTagRefs)
        assertEquals(emptyList<WorkspaceTagRefEntity>(), gson.fromJson("{\"workspaceTagRefs\":[]}", SnapshotBundle::class.java).workspaceTagRefs)
    }

    @Test
    fun `orientation sync acknowledgement is exact-version guarded for Workspace tags`() = runBlocking {
        val database = database()
        try {
            insertWorkspace(database)
            val original = tag(version = 1L, updatedAt = 10L, isDeleted = false)
            database.workspaceTagRefDao().upsert(listOf(original))
            val syncStore =
                CanonicalOrientationSyncStore(
                    database = database,
                    dao = database.orientationDao(),
                    bootstrapper = mockk<CanonicalOrientationBootstrapper>(relaxed = true),
                    workspaceDao = database.workspaceDao(),
                    workspaceTagRefDao = database.workspaceTagRefDao(),
                    workspaceBootstrapper = mockk<CanonicalWorkspaceBootstrapper>(relaxed = true),
                )

            assertEquals(listOf(original), syncStore.loadUnsynced().workspaceTagRefs)
            database.workspaceTagRefDao().upsert(listOf(original.copy(version = 2L, updatedAt = 20L)))
            syncStore.markSynced(
                CanonicalOrientationSyncAck(
                    workspaceTagRefs =
                        listOf(CanonicalWorkspaceTagRefSyncVersion(OWNER, TAG, original.version)),
                ),
            )

            val current = database.workspaceTagRefDao().getAllForWorkspace(OWNER).single()
            assertEquals(2L, current.version)
            assertNull(current.syncedAt)
        } finally {
            database.close()
        }
    }

    private suspend fun insertWorkspace(database: AppDatabase) {
        database.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = OWNER,
                    nameOverride = "owner",
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
                ),
            ),
        )
    }

    private fun tag(version: Long, updatedAt: Long, isDeleted: Boolean) =
        WorkspaceTagRefEntity(
            workspaceId = OWNER,
            normalizedTag = TAG,
            createdAt = 1L,
            updatedAt = updatedAt,
            syncedAt = null,
            isDeleted = isDeleted,
            version = version,
        )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        const val OWNER = "canonical-owner"
        const val TAG = "focus"
    }
}
