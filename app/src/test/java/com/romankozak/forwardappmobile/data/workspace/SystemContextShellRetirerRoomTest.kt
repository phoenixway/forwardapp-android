package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
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
class SystemContextShellRetirerRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `retirer deletes only active exact reserved shells and is idempotent`() =
        runBlocking {
            val database = database()
            try {
                val definitions = SystemOperationalDefinitions.all
                assertEquals(20, definitions.size)

                val activeReserved =
                    definitions.mapIndexed { index, definition ->
                        ContextEntity(
                            id = definition.id,
                            name = "Legacy ${definition.defaultName}",
                            description = "legacy-$index",
                            parentId = definition.defaultParentId,
                            createdAt = index + 1L,
                            updatedAt = index + 101L,
                            order = index.toLong(),
                        )
                    }

                database.contextDao().insertContexts(activeReserved)

                val historicalSysPrefix =
                    ContextEntity(
                        id = "sys_custom",
                        name = "Historical ordinary sys prefix",
                        description = null,
                        parentId = null,
                        createdAt = 500L,
                        updatedAt = 500L,
                    )
                database.contextDao().insert(historicalSysPrefix)

                val tombstoneId = SystemContexts.INBOX.raw

                val materializer =
                    SystemWorkspaceMaterializer(
                        database = database,
                        contextDao = database.contextDao(),
                        workspaceDao = database.workspaceDao(),
                    )

                materializer.materializeAll(
                    now = 1_000L,
                    seedMissingFactoryCapabilities = false,
                )

                SystemWorkspaceTagSeed(
                    database = database,
                    contextDao = database.contextDao(),
                ).seedMissingCanonicalCollections(now = 2_000L)

                // A reserved tombstone is historical evidence that exists only
                // after canonical ownership/tag convergence. Materializer must
                // never be asked to promote from deleted legacy evidence.
                val tombstone =
                    requireNotNull(database.contextDao().getContextById(tombstoneId))
                        .copy(isDeleted = true)
                database.contextDao().update(tombstone)

                val retirer =
                    SystemContextShellRetirer(
                        database = database,
                        contextDao = database.contextDao(),
                    )

                val first = retirer.retireActiveReservedShells()

                assertEquals(19, first.deletedActiveShells)

                definitions.forEach { definition ->
                    val workspace =
                        requireNotNull(database.workspaceDao().getById(definition.id))
                    assertFalse(workspace.isDeleted)
                    assertEquals(
                        WorkspaceProvenance.CANONICAL_ONLY.name,
                        workspace.provenance,
                    )
                    assertNull(workspace.sourceContextId)

                    val seedState =
                        database.systemWorkspaceTagSeedStateDao()
                            .getByWorkspaceId(definition.id)
                    assertNotNull(seedState)

                    if (definition.id == tombstoneId) {
                        val retainedTombstone =
                            requireNotNull(database.contextDao().getContextById(definition.id))
                        assertTrue(retainedTombstone.isDeleted)
                    } else {
                        assertNull(database.contextDao().getContextById(definition.id))
                    }
                }

                val ordinary =
                    requireNotNull(database.contextDao().getContextById("sys_custom"))
                assertFalse(ordinary.isDeleted)

                val second = retirer.retireActiveReservedShells()
                assertEquals(0, second.deletedActiveShells)

                val db = database.openHelper.writableDatabase
                db.query("PRAGMA foreign_key_check").use { cursor ->
                    assertEquals(0, cursor.count)
                }
                db.query("PRAGMA integrity_check").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("ok", cursor.getString(0))
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun `retirer fails closed before deleting anything when canonical owner is malformed`() =
        runBlocking {
            val database = database()
            try {
                val definitions = SystemOperationalDefinitions.all

                database.contextDao().insertContexts(
                    definitions.map { definition ->
                        ContextEntity(
                            id = definition.id,
                            name = definition.defaultName,
                            description = null,
                            parentId = definition.defaultParentId,
                            createdAt = 1L,
                            updatedAt = 1L,
                        )
                    },
                )

                val materializer =
                    SystemWorkspaceMaterializer(
                        database = database,
                        contextDao = database.contextDao(),
                        workspaceDao = database.workspaceDao(),
                    )
                materializer.materializeAll(
                    now = 1_000L,
                    seedMissingFactoryCapabilities = false,
                )

                SystemWorkspaceTagSeed(
                    database = database,
                    contextDao = database.contextDao(),
                ).seedMissingCanonicalCollections(now = 2_000L)

                val brokenId = SystemContexts.STRATEGIC.raw
                val workspace = requireNotNull(database.workspaceDao().getById(brokenId))
                database.workspaceDao().upsert(
                    listOf(
                        workspace.copy(
                            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                            sourceContextId = brokenId,
                        ),
                    ),
                )

                val result =
                    runCatching {
                        SystemContextShellRetirer(
                            database = database,
                            contextDao = database.contextDao(),
                        ).retireActiveReservedShells()
                    }

                assertTrue(result.isFailure)

                definitions.forEach { definition ->
                    assertNotNull(database.contextDao().getContextById(definition.id))
                }
            } finally {
                database.close()
            }
        }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
