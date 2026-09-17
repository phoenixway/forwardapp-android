package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceTagRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `replaceTags normalizes membership and versions remove and restore independently`() =
        runBlocking {
            val database =
                Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()

            try {
                database.workspaceDao().upsert(
                    listOf(
                        WorkspaceEntity(
                            id = "sys-owner",
                            nameOverride = "Standalone owner",
                            descriptionOverride = null,
                            parentWorkspaceId = null,
                            roleCode = null,
                            workspaceOrder = 0L,
                            createdAt = 1L,
                            updatedAt = 1L,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                            provenance = WorkspaceProvenance.STANDALONE.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                val repository = CanonicalWorkspaceTagRepository(database)

                repository.replaceTags(
                    workspaceId = "sys-owner",
                    tags = listOf("#Alpha", " alpha ", "Beta"),
                    now = 10L,
                )

                assertEquals(
                    listOf("alpha", "beta"),
                    repository.getTags("sys-owner"),
                )

                repository.replaceTags(
                    workspaceId = "sys-owner",
                    tags = listOf("beta", "Gamma"),
                    now = 20L,
                )

                val afterRemoval =
                    database.workspaceTagRefDao()
                        .getAllForWorkspace("sys-owner")
                        .associateBy { it.normalizedTag }

                assertTrue(afterRemoval.getValue("alpha").isDeleted)
                assertEquals(2L, afterRemoval.getValue("alpha").version)
                assertNull(afterRemoval.getValue("alpha").syncedAt)

                assertFalse(afterRemoval.getValue("beta").isDeleted)
                assertEquals(1L, afterRemoval.getValue("beta").version)

                assertFalse(afterRemoval.getValue("gamma").isDeleted)
                assertEquals(1L, afterRemoval.getValue("gamma").version)

                repository.replaceTags(
                    workspaceId = "sys-owner",
                    tags = listOf("alpha", "beta", "gamma"),
                    now = 30L,
                )

                val restored =
                    database.workspaceTagRefDao()
                        .getAllForWorkspace("sys-owner")
                        .associateBy { it.normalizedTag }

                assertFalse(restored.getValue("alpha").isDeleted)
                assertEquals(3L, restored.getValue("alpha").version)
                assertEquals(
                    listOf("alpha", "beta", "gamma"),
                    repository.getTags("sys-owner"),
                )
            } finally {
                database.close()
            }
        }
}
