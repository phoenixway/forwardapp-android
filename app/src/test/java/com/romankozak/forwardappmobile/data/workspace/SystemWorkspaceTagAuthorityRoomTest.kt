package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.SystemWorkspaceTagSeedStateEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceTagRefEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SystemWorkspaceTagAuthorityRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `canonical System tags beat stale Context shell tags`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            insertContext(database, id, listOf("legacy"))
            insertCanonicalWorkspace(database, id)
            markEstablished(database, id)
            database.workspaceTagRefDao().upsert(
                listOf(tagRef(id, "canonical")),
            )

            val authority = authority(database)

            val resolution = authority.resolve(id)
            assertTrue(resolution is SystemWorkspaceTagAuthority.Resolution.Canonical)
            assertEquals(
                listOf("canonical"),
                (resolution as SystemWorkspaceTagAuthority.Resolution.Canonical).tags,
            )

            val projected =
                authority.project(requireNotNull(database.contextDao().getContextById(id)))
            assertEquals(listOf("canonical"), projected?.tags)
        } finally {
            database.close()
        }
    }

    @Test
    fun `established canonical empty System tags never fall back to stale Context shell`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                insertContext(database, id, listOf("stale"))
                insertCanonicalWorkspace(database, id)
                markEstablished(database, id)

                val authority = authority(database)

                val resolution = authority.resolve(id)
                assertTrue(resolution is SystemWorkspaceTagAuthority.Resolution.Canonical)
                assertEquals(
                    emptyList<String>(),
                    (resolution as SystemWorkspaceTagAuthority.Resolution.Canonical).tags,
                )
                assertEquals(
                    emptyList<String>(),
                    authority.project(
                        requireNotNull(database.contextDao().getContextById(id)),
                    )?.tags,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `unseeded or malformed reserved System ownership fails closed`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            insertContext(database, id, listOf("legacy"))
            insertCanonicalWorkspace(database, id)

            val authority = authority(database)

            assertEquals(
                SystemWorkspaceTagAuthority.Resolution.Unavailable,
                authority.resolve(id),
            )
            assertNull(
                authority.project(
                    requireNotNull(database.contextDao().getContextById(id)),
                ),
            )

            database.workspaceDao().upsert(
                listOf(
                    requireNotNull(database.workspaceDao().getById(id)).copy(
                        provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
                        sourceContextId = id,
                        version = 2L,
                    ),
                ),
            )
            markEstablished(database, id)

            assertEquals(
                SystemWorkspaceTagAuthority.Resolution.Unavailable,
                authority.resolve(id),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `ordinary Context tags remain legacy owned`() = runBlocking {
        val database = database()
        try {
            val ordinaryId = "ordinary-context"
            insertContext(database, ordinaryId, listOf("ordinary"))

            val authority = authority(database)
            val source = requireNotNull(database.contextDao().getContextById(ordinaryId))

            assertEquals(
                SystemWorkspaceTagAuthority.Resolution.NotSystem,
                authority.resolve(ordinaryId),
            )
            assertEquals(source, authority.project(source))
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical System tag owner participates without Context shell`() = runBlocking {
        val database = database()
        try {
            val id = SystemContexts.INBOX.raw
            insertCanonicalWorkspace(database, id)
            markEstablished(database, id)
            database.workspaceTagRefDao().upsert(
                listOf(tagRef(id, "inbox-tag")),
            )

            val owners = authority(database).effectiveOwners(emptyList())

            assertEquals(
                listOf(
                    SystemWorkspaceTagAuthority.TagOwner(
                        id = id,
                        tags = listOf("inbox-tag"),
                    ),
                ),
                owners.filter { it.id == id },
            )

            assertEquals(
                listOf(
                    SystemWorkspaceTagAuthority.TagMatch(
                        contextId = id,
                        normalizedTag = "inbox-tag",
                    ),
                ),
                authority(database).findCanonicalSystemOwnersByTags(
                    listOf("inbox-tag"),
                ),
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `System tag write authors canonical membership before compatibility projection`() =
        runBlocking {
            val database = database()
            try {
                val id = SystemContexts.INBOX.raw
                insertContext(database, id, listOf("legacy"))
                insertCanonicalWorkspace(database, id)
                markEstablished(database, id)

                val authority = authority(database)
                val requested =
                    requireNotNull(database.contextDao().getContextById(id))
                        .copy(tags = listOf("#New", " new ", "Second"))

                val projected =
                    authority.reconcileBeforeContextWrite(
                        context = requested,
                        tagsWereExplicitlyChanged = true,
                        now = 20L,
                    )

                assertEquals(listOf("new", "second"), projected.tags)
                assertEquals(
                    listOf("new", "second"),
                    CanonicalWorkspaceTagRepository(database).getTags(id),
                )

                val staleShell =
                    requireNotNull(database.contextDao().getContextById(id))
                        .copy(tags = listOf("legacy-again"))

                assertEquals(
                    listOf("new", "second"),
                    authority.project(staleShell)?.tags,
                )
            } finally {
                database.close()
            }
        }

    private fun authority(database: AppDatabase): SystemWorkspaceTagAuthority =
        SystemWorkspaceTagAuthority(
            workspaceDao = database.workspaceDao(),
            workspaceTagRefDao = database.workspaceTagRefDao(),
            systemWorkspaceTagSeedStateDao = database.systemWorkspaceTagSeedStateDao(),
            canonicalWorkspaceTagRepository = CanonicalWorkspaceTagRepository(database),
        )

    private suspend fun insertContext(
        database: AppDatabase,
        id: String,
        tags: List<String>?,
    ) {
        database.contextDao().insert(
            ContextEntity(
                id = id,
                name = id,
                description = null,
                parentId = null,
                createdAt = 1L,
                updatedAt = 1L,
                tags = tags,
            ),
        )
    }

    private suspend fun insertCanonicalWorkspace(
        database: AppDatabase,
        id: String,
    ) {
        database.workspaceDao().upsert(
            listOf(
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
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                ),
            ),
        )
    }

    private suspend fun markEstablished(
        database: AppDatabase,
        id: String,
    ) {
        database.systemWorkspaceTagSeedStateDao().upsert(
            listOf(
                SystemWorkspaceTagSeedStateEntity(
                    workspaceId = id,
                    seededAt = 2L,
                ),
            ),
        )
    }

    private fun tagRef(
        workspaceId: String,
        tag: String,
    ): WorkspaceTagRefEntity =
        WorkspaceTagRefEntity(
            workspaceId = workspaceId,
            normalizedTag = tag,
            createdAt = 3L,
            updatedAt = 3L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
        )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
