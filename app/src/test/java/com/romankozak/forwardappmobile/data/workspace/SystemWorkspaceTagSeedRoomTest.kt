package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ContextTagRef
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceTagRefEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SystemWorkspaceTagSeedRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `seed normalizes System shell tags once and leaves Context shells untouched`() =
        runBlocking {
            val database = database()
            try {
                installPromotedSystemRows(
                    database = database,
                    tagsById = mapOf(SystemContexts.INBOX.raw to listOf("#Alpha", " alpha ", "Beta")),
                )
                val before = requireNotNull(database.contextDao().getContextById(SystemContexts.INBOX.raw))
                val seed = SystemWorkspaceTagSeed(database, database.contextDao())

                seed.seedMissingCanonicalCollections(now = 10L)

                val first = database.workspaceTagRefDao().getAllForWorkspace(SystemContexts.INBOX.raw)
                assertEquals(listOf("alpha", "beta"), first.map { it.normalizedTag })
                assertTrue(first.all { it.version == 1L && it.createdAt == 10L && !it.isDeleted })
                assertEquals(before, database.contextDao().getContextById(SystemContexts.INBOX.raw))

                seed.seedMissingCanonicalCollections(now = 20L)

                assertEquals(first, database.workspaceTagRefDao().getAllForWorkspace(SystemContexts.INBOX.raw))
                assertEquals(
                    SystemOperationalDefinitions.all.size,
                    database.systemWorkspaceTagSeedStateDao().getAll().size,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `unmarked canonical rows and malformed System ownership fail closed without partial seed`() =
        runBlocking {
            val database = database()
            try {
                installPromotedSystemRows(
                    database = database,
                    tagsById = mapOf(SystemContexts.INBOX.raw to listOf("legacy")),
                )
                val existing =
                    WorkspaceTagRefEntity(
                        workspaceId = SystemContexts.INBOX.raw,
                        normalizedTag = "canonical",
                        createdAt = 1L,
                        updatedAt = 99L,
                        syncedAt = 2L,
                        isDeleted = false,
                        version = 9L,
                    )
                database.workspaceTagRefDao().upsert(listOf(existing))
                database.contextTagRefDao().insertAll(
                    listOf(
                        ContextTagRef(
                            contextId = SystemContexts.INBOX.raw,
                            normalizedTag = "legacy-index",
                        ),
                    ),
                )

                val failure =
                    runCatching {
                        SystemWorkspaceTagSeed(database, database.contextDao())
                            .seedMissingCanonicalCollections(now = 100L)
                    }.exceptionOrNull()
                assertTrue(failure is IllegalArgumentException)

                assertEquals(
                    listOf(existing),
                    database.workspaceTagRefDao().getAllForWorkspace(SystemContexts.INBOX.raw),
                )
                assertTrue(database.systemWorkspaceTagSeedStateDao().getAll().isEmpty())
                assertEquals(
                    listOf(SystemContexts.INBOX.raw),
                    database.contextTagRefDao().findContextIdsByTag("legacy-index"),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `direct canonical tag authoring establishes the collection before a later legacy seed`() =
        runBlocking {
            val database = database()
            try {
                installPromotedSystemRows(
                    database = database,
                    tagsById = mapOf(SystemContexts.INBOX.raw to listOf("legacy")),
                )
                CanonicalWorkspaceTagRepository(database).replaceTags(
                    workspaceId = SystemContexts.INBOX.raw,
                    tags = listOf("canonical"),
                    now = 10L,
                )

                SystemWorkspaceTagSeed(database, database.contextDao())
                    .seedMissingCanonicalCollections(now = 20L)

                assertEquals(
                    listOf("canonical"),
                    database.workspaceTagRefDao()
                        .getAllForWorkspace(SystemContexts.INBOX.raw)
                        .map { it.normalizedTag },
                )
                assertEquals(
                    SystemOperationalDefinitions.all.map { it.id }.sorted(),
                    database.systemWorkspaceTagSeedStateDao()
                        .getAll()
                        .map { it.workspaceId }
                        .sorted(),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `deleted promoted System Workspace fails before any Context tags are copied`() = runBlocking {
        val database = database()
        try {
            installPromotedSystemRows(
                database = database,
                tagsById = mapOf(SystemContexts.INBOX.raw to listOf("legacy")),
            )
            val deleted = requireNotNull(database.workspaceDao().getById(SystemContexts.INBOX.raw))
                .copy(isDeleted = true, version = 2L)
            database.workspaceDao().upsert(listOf(deleted))

            val failure =
                runCatching {
                    SystemWorkspaceTagSeed(database, database.contextDao())
                        .seedMissingCanonicalCollections(now = 10L)
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(database.workspaceTagRefDao().getAll().isEmpty())
            assertTrue(database.systemWorkspaceTagSeedStateDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `malformed promoted System ownership fails before any Context tags are copied`() = runBlocking {
        val database = database()
        try {
            installPromotedSystemRows(
                database = database,
                tagsById = mapOf(SystemContexts.INBOX.raw to listOf("legacy")),
            )
            val malformed =
                requireNotNull(database.workspaceDao().getById(SystemContexts.INBOX.raw))
                    .copy(sourceContextId = SystemContexts.INBOX.raw, version = 2L)
            database.workspaceDao().upsert(listOf(malformed))

            val failure =
                runCatching {
                    SystemWorkspaceTagSeed(database, database.contextDao())
                        .seedMissingCanonicalCollections(now = 10L)
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(database.workspaceTagRefDao().getAll().isEmpty())
            assertTrue(database.systemWorkspaceTagSeedStateDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `missing promoted System Workspace fails before any Context tags are copied`() = runBlocking {
        val database = database()
        try {
            installPromotedSystemRows(
                database = database,
                tagsById = mapOf(SystemContexts.INBOX.raw to listOf("legacy")),
            )
            database.openHelper.writableDatabase.execSQL(
                "DELETE FROM workspaces WHERE id = ?",
                arrayOf(SystemContexts.INBOX.raw),
            )

            val failure =
                runCatching {
                    SystemWorkspaceTagSeed(database, database.contextDao())
                        .seedMissingCanonicalCollections(now = 10L)
                }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(database.workspaceTagRefDao().getAll().isEmpty())
            assertTrue(database.systemWorkspaceTagSeedStateDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }


    @Test
    fun `null legacy System tags establish authoritative empty canonical collections`() =
        runBlocking {
            val database = database()
            try {
                installPromotedSystemRows(
                    database = database,
                    tagsById = emptyMap(),
                )

                SystemWorkspaceTagSeed(database, database.contextDao())
                    .seedMissingCanonicalCollections(now = 10L)

                assertTrue(database.workspaceTagRefDao().getAll().isEmpty())
                assertEquals(
                    SystemOperationalDefinitions.all.map { it.id }.sorted(),
                    database.systemWorkspaceTagSeedStateDao().getAll()
                        .map { it.workspaceId }
                        .sorted(),
                )

                SystemWorkspaceTagSeed(database, database.contextDao())
                    .seedMissingCanonicalCollections(now = 20L)

                assertTrue(database.workspaceTagRefDao().getAll().isEmpty())
                assertEquals(
                    SystemOperationalDefinitions.all.size,
                    database.systemWorkspaceTagSeedStateDao().getAll().size,
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `successful System tag seed retires reserved legacy tag refs but preserves ordinary Context refs`() =
        runBlocking {
            val database = database()
            try {
                installPromotedSystemRows(
                    database = database,
                    tagsById = emptyMap(),
                )
                val ordinaryContextId = "ordinary-context"
                database.contextDao().insert(
                    ContextEntity(
                        id = ordinaryContextId,
                        name = "Ordinary",
                        description = null,
                        parentId = null,
                        createdAt = 1L,
                        updatedAt = 1L,
                        tags = listOf("legacy-index"),
                    ),
                )
                database.contextTagRefDao().insertAll(
                    listOf(
                        ContextTagRef(
                            contextId = SystemContexts.INBOX.raw,
                            normalizedTag = "legacy-index",
                        ),
                        ContextTagRef(
                            contextId = ordinaryContextId,
                            normalizedTag = "legacy-index",
                        ),
                    ),
                )

                SystemWorkspaceTagSeed(database, database.contextDao())
                    .seedMissingCanonicalCollections(now = 10L)

                assertEquals(
                    listOf(ordinaryContextId),
                    database.contextTagRefDao().findContextIdsByTag("legacy-index"),
                )
                assertEquals(
                    SystemOperationalDefinitions.all.size,
                    database.systemWorkspaceTagSeedStateDao().getAll().size,
                )
            } finally {
                database.close()
            }
        }

    private suspend fun installPromotedSystemRows(
        database: AppDatabase,
        tagsById: Map<String, List<String>>,
    ) {
        database.contextDao().insertContexts(
            SystemOperationalDefinitions.all.map { definition ->
                ContextEntity(
                    id = definition.id,
                    name = definition.defaultName,
                    description = null,
                    parentId = definition.defaultParentId,
                    createdAt = 1L,
                    updatedAt = 1L,
                    tags = tagsById[definition.id],
                )
            },
        )
        database.workspaceDao().upsert(
            SystemOperationalDefinitions.all.map { definition ->
                WorkspaceEntity(
                    id = definition.id,
                    nameOverride = definition.defaultName,
                    descriptionOverride = null,
                    parentWorkspaceId = definition.defaultParentId,
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
            },
        )
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
