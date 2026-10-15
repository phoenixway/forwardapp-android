package com.romankozak.forwardappmobile.features.mainscreen.core

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconContextCrossRef
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainBeaconDaoWorkspaceOwnerRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `logical Context refs persist exclusively as validated Workspace refs`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.mainBeaconDao()
                dao.insertBeacon(
                    MainBeacon(
                        id = BEACON_ID,
                        title = "Beacon",
                        createdAt = 1L,
                        updatedAt = 1L,
                    ),
                )
                database.contextDao().insertContexts(
                    listOf(context(ORDINARY_ID), context(HISTORICAL_SYS_PREFIX_ID)),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalWorkspace(SYSTEM_ID),
                        contextBackedWorkspace(ORDINARY_ID),
                        contextBackedWorkspace(HISTORICAL_SYS_PREFIX_ID),
                    ),
                )

                dao.insertContextCrossRefs(
                    listOf(
                        MainBeaconContextCrossRef(BEACON_ID, SYSTEM_ID, 4L),
                        MainBeaconContextCrossRef(BEACON_ID, ORDINARY_ID, 2L),
                        MainBeaconContextCrossRef(BEACON_ID, HISTORICAL_SYS_PREFIX_ID, 3L),
                    ),
                )

                assertEquals(
                    listOf(ORDINARY_ID, HISTORICAL_SYS_PREFIX_ID, SYSTEM_ID),
                    dao.getAllWorkspaceCrossRefsSync().map { it.workspaceId },
                )
                assertTrue(retiredMainBeaconContextTableAbsent(database))

                dao.updateContextCrossRefOrder(BEACON_ID, SYSTEM_ID, 0L)
                dao.updateContextCrossRefOrder(BEACON_ID, ORDINARY_ID, 1L)

                assertEquals(
                    listOf(SYSTEM_ID, ORDINARY_ID, HISTORICAL_SYS_PREFIX_ID),
                    dao.getAllContextCrossRefsSync().map { it.contextId },
                )

                dao.deleteContextCrossRefsForContexts(setOf(SYSTEM_ID))

                assertEquals(
                    listOf(ORDINARY_ID, HISTORICAL_SYS_PREFIX_ID),
                    dao.getAllWorkspaceCrossRefsSync().map { it.workspaceId },
                )
                assertTrue(retiredMainBeaconContextTableAbsent(database))
            } finally {
                database.close()
            }
        }

    @Test
    fun `repeated logical insertion preserves one canonical Workspace association`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.mainBeaconDao()
                dao.insertBeacon(MainBeacon(id = BEACON_ID, title = "Beacon"))
                database.contextDao().insertContexts(listOf(context(ORDINARY_ID)))
                database.workspaceDao().upsert(
                    listOf(contextBackedWorkspace(ORDINARY_ID)),
                )

                val ref = MainBeaconContextCrossRef(BEACON_ID, ORDINARY_ID, 7L)

                dao.insertContextCrossRefs(listOf(ref))
                dao.insertContextCrossRefs(listOf(ref))

                assertEquals(
                    listOf(ref),
                    dao.getAllContextCrossRefsSync(),
                )
                assertEquals(
                    listOf(
                        com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconWorkspaceCrossRef(
                            BEACON_ID,
                            ORDINARY_ID,
                            7L,
                        ),
                    ),
                    dao.getAllWorkspaceCrossRefsSync(),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `divergent logical insertion rejects without modifying canonical order`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.mainBeaconDao()
                dao.insertBeacon(MainBeacon(id = BEACON_ID, title = "Beacon"))
                database.contextDao().insertContexts(listOf(context(ORDINARY_ID)))
                database.workspaceDao().upsert(
                    listOf(contextBackedWorkspace(ORDINARY_ID)),
                )

                val originalRef = MainBeaconContextCrossRef(
                    BEACON_ID,
                    ORDINARY_ID,
                    4L,
                )
                dao.insertContextCrossRefs(listOf(originalRef))

                val before = dao.getAllWorkspaceCrossRefsSync()

                val failure = runCatching {
                    dao.insertContextCrossRefs(
                        listOf(originalRef.copy(order = 3L)),
                    )
                }.exceptionOrNull()

                assertTrue(
                    "Expected canonical order conflict",
                    failure is IllegalArgumentException,
                )
                assertEquals(before, dao.getAllWorkspaceCrossRefsSync())
                assertEquals(listOf(originalRef), dao.getAllContextCrossRefsSync())
            } finally {
                database.close()
            }
        }

    @Test
    fun `current ordinary Workspace owner persists only in Workspace relation`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.mainBeaconDao()
                dao.insertBeacon(
                    MainBeacon(
                        id = BEACON_ID,
                        title = "Beacon",
                        createdAt = 1L,
                        updatedAt = 1L,
                    ),
                )
                database.workspaceDao().upsert(
                    listOf(
                        canonicalWorkspace(ORDINARY_ID),
                    ),
                )

                dao.insertWorkspaceCrossRefs(
                    listOf(
                        com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconWorkspaceCrossRef(
                            beaconId = BEACON_ID,
                            workspaceId = ORDINARY_ID,
                            order = 2L,
                        ),
                    ),
                )

                assertEquals(
                    listOf(ORDINARY_ID),
                    dao.getAllWorkspaceCrossRefsSync().map { it.workspaceId },
                )
                assertEquals(
                    0L,
                    scalarLong(
                        database,
                        "SELECT COUNT(*) FROM sqlite_master " +
                            "WHERE type = 'table' " +
                            "AND name = 'main_beacon_context_cross_ref'",
                    ),
                )
            } finally {
                database.close()
            }
        }

    @Test
    fun `logical export and observation project canonical Workspace associations`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.mainBeaconDao()
                dao.insertBeacon(MainBeacon(id = BEACON_ID, title = "Beacon"))
                database.contextDao().insertContexts(listOf(context(ORDINARY_ID)))
                database.workspaceDao().upsert(
                    listOf(contextBackedWorkspace(ORDINARY_ID)),
                )

                val logical = MainBeaconContextCrossRef(
                    BEACON_ID,
                    ORDINARY_ID,
                    7L,
                )
                val canonical =
                    com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconWorkspaceCrossRef(
                        BEACON_ID,
                        ORDINARY_ID,
                        7L,
                    )

                dao.insertWorkspaceCrossRefs(listOf(canonical))

                assertEquals(listOf(logical), dao.getAllContextCrossRefsSync())
                assertEquals(listOf(logical), dao.observeContextCrossRefs().first())
                assertEquals(listOf(canonical), dao.getAllWorkspaceCrossRefsSync())
            } finally {
                database.close()
            }
        }

    @Test
    fun `logical export rejects invalid owner provenance without ref mutation`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.mainBeaconDao()
                dao.insertBeacon(MainBeacon(id = BEACON_ID, title = "Beacon"))
                database.contextDao().insertContexts(listOf(context(ORDINARY_ID)))

                val originalOwner = contextBackedWorkspace(ORDINARY_ID)
                database.workspaceDao().upsert(listOf(originalOwner))

                val ref = MainBeaconContextCrossRef(
                    BEACON_ID,
                    ORDINARY_ID,
                    7L,
                )
                dao.insertContextCrossRefs(listOf(ref))

                val before = dao.getAllWorkspaceCrossRefsSync()

                database.workspaceDao().upsert(
                    listOf(
                        originalOwner.copy(
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                val exportFailure = runCatching {
                    dao.getAllContextCrossRefsSync()
                }.exceptionOrNull()

                val observeFailure = runCatching {
                    dao.observeContextCrossRefs().first()
                }.exceptionOrNull()

                assertTrue(exportFailure is IllegalArgumentException)
                assertTrue(observeFailure is IllegalArgumentException)
                assertEquals(before, dao.getAllWorkspaceCrossRefsSync())
            } finally {
                database.close()
            }
        }

    @Test
    fun `logical observer invalidates when Workspace provenance changes without crossref writes`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.mainBeaconDao()
                val owner = contextBackedWorkspace(ORDINARY_ID)
                val ref = MainBeaconContextCrossRef(BEACON_ID, ORDINARY_ID, 7L)

                dao.insertBeacon(MainBeacon(id = BEACON_ID, title = "Beacon"))
                database.contextDao().insertContexts(listOf(context(ORDINARY_ID)))
                database.workspaceDao().upsert(listOf(owner))
                dao.insertContextCrossRefs(listOf(ref))

                val firstEmission =
                    CompletableDeferred<List<MainBeaconContextCrossRef>>()

                val observer =
                    async(Dispatchers.Default) {
                        runCatching {
                            dao.observeContextCrossRefs()
                                .onEach { rows -> firstEmission.complete(rows) }
                                .collect()
                        }.exceptionOrNull()
                    }

                try {
                    assertEquals(
                        listOf(ref),
                        withTimeout(10_000L) { firstEmission.await() },
                    )

                    // No association-table write occurs after observation
                    // starts. Only the Workspace owner authority changes.
                    database.workspaceDao().upsert(
                        listOf(
                            owner.copy(
                                provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                                sourceContextId = null,
                            ),
                        ),
                    )

                    val failure = withTimeout(10_000L) { observer.await() }

                    assertTrue(
                        "Expected live owner revalidation, got $failure",
                        failure is IllegalArgumentException,
                    )
                    assertEquals(
                        1,
                        dao.getAllWorkspaceCrossRefsSync().size,
                    )
                    assertTrue(retiredMainBeaconContextTableAbsent(database))
                } finally {
                    observer.cancel()
                }
            } finally {
                database.close()
            }
        }

    private fun context(id: String) =
        ContextEntity(
            id = id,
            name = id,
            description = null,
            parentId = null,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun contextBackedWorkspace(id: String) =
        canonicalWorkspace(id).copy(
            provenance = WorkspaceProvenance.CONTEXT_BACKED.name,
            sourceContextId = id,
        )

    private fun canonicalWorkspace(id: String) =
        WorkspaceEntity(
            id = id,
            nameOverride = id,
            descriptionOverride = null,
            roleCode = null,
            createdAt = 1L,
            updatedAt = 1L,
            syncedAt = null,
            isDeleted = false,
            version = 1L,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
            sourceContextId = null,
        )

    private fun scalarLong(
        database: AppDatabase,
        sql: String,
    ): Long =
        database.openHelper.writableDatabase.query(sql).use {
            check(it.moveToFirst())
            it.getLong(0)
        }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        const val BEACON_ID = "beacon"
        val SYSTEM_ID: String = SystemContexts.PERSONAL_MANAGEMENT.raw
        const val ORDINARY_ID = "ordinary-context"
        const val HISTORICAL_SYS_PREFIX_ID = "sys_strategic-beacons"
    }
}

private fun retiredMainBeaconContextTableAbsent(
    database: AppDatabase,
): Boolean =
    database.openHelper.writableDatabase.query(
        "SELECT COUNT(*) FROM sqlite_master " +
            "WHERE type = 'table' AND name = 'main_beacon_context_cross_ref'",
    ).use { cursor ->
        check(cursor.moveToFirst())
        cursor.getLong(0) == 0L
    }
