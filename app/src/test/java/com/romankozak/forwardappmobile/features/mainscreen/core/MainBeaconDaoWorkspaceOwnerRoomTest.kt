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
    fun `typed routing stores exact reserved owner only in Workspace branch and exports logical union`() =
        runBlocking {
            val database = database()
            try {
                val dao = database.mainBeaconDao()
                dao.insertBeacon(MainBeacon(id = BEACON_ID, title = "Beacon", createdAt = 1L, updatedAt = 1L))
                database.contextDao().insertContexts(
                    listOf(context(ORDINARY_ID), context(HISTORICAL_SYS_PREFIX_ID)),
                )
                database.workspaceDao().upsert(listOf(canonicalWorkspace(SYSTEM_ID)))

                dao.insertContextCrossRefs(
                    listOf(
                        MainBeaconContextCrossRef(BEACON_ID, SYSTEM_ID, order = 4L),
                        MainBeaconContextCrossRef(BEACON_ID, ORDINARY_ID, order = 2L),
                        MainBeaconContextCrossRef(BEACON_ID, HISTORICAL_SYS_PREFIX_ID, order = 3L),
                    ),
                )

                assertEquals(
                    listOf(SYSTEM_ID),
                    dao.getAllWorkspaceCrossRefsSync().map { it.workspaceId },
                )
                assertEquals(
                    listOf(ORDINARY_ID, HISTORICAL_SYS_PREFIX_ID, SYSTEM_ID),
                    dao.getAllContextCrossRefsSync().map { it.contextId },
                )
                assertEquals(
                    0L,
                    scalarLong(
                        database,
                        "SELECT COUNT(*) FROM main_beacon_context_cross_ref WHERE context_id = '$SYSTEM_ID'",
                    ),
                )
                assertEquals(
                    1L,
                    scalarLong(
                        database,
                        """
                        SELECT COUNT(*) FROM main_beacon_context_cross_ref
                        WHERE context_id = '$HISTORICAL_SYS_PREFIX_ID'
                        """.trimIndent(),
                    ),
                )

                dao.updateContextCrossRefOrder(BEACON_ID, SYSTEM_ID, order = 0L)
                dao.updateContextCrossRefOrder(BEACON_ID, ORDINARY_ID, order = 1L)
                assertEquals(
                    listOf(SYSTEM_ID, ORDINARY_ID, HISTORICAL_SYS_PREFIX_ID),
                    dao.getAllContextCrossRefsSync().map { it.contextId },
                )

                dao.deleteContextCrossRefsForContexts(setOf(SYSTEM_ID))
                assertTrue(dao.getAllWorkspaceCrossRefsSync().isEmpty())
                assertEquals(
                    listOf(ORDINARY_ID, HISTORICAL_SYS_PREFIX_ID),
                    dao.getAllContextCrossRefsSync().map { it.contextId },
                )
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

    private fun canonicalWorkspace(id: String) =
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
