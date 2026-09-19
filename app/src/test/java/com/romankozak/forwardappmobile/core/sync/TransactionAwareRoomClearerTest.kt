package com.romankozak.forwardappmobile.core.sync

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.daymanagement.runtime.data.DayManagementRuntimeRepository
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransactionAwareRoomClearerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `failure after clear rolls back destination state`() =
        runBlocking {
            val database = database()
            try {
                database.workspaceDao().upsert(listOf(sentinel()))

                assertThrows(IllegalStateException::class.java) {
                    runBlocking {
                        restoreDataSource(database) {
                            database.workspaceDao().upsert(requireNotNull(it.workspaces))
                            error("forced restore failure")
                        }.replaceWith(canonicalBundle(restored()))
                    }
                }

                assertNotNull(database.workspaceDao().getById(SENTINEL_ID))
                assertNull(database.workspaceDao().getById(RESTORED_ID))
            } finally {
                database.close()
            }
        }

    @Test
    fun `clear treats FTS4 shadow tables as virtual table implementation details`() =
        runBlocking {
            val database = database()
            try {
                database.withTransaction {
                    val sql = database.openHelper.writableDatabase

                    sql.execSQL(
                        """
                        CREATE VIRTUAL TABLE restore_clearer_fts4
                        USING fts4(body)
                        """.trimIndent(),
                    )
                    sql.execSQL(
                        """
                        INSERT INTO restore_clearer_fts4(body)
                        VALUES ('indexed restore sentinel')
                        """.trimIndent(),
                    )

                    val shadowTables =
                        sql.query(
                            """
                            SELECT name
                            FROM sqlite_master
                            WHERE type = 'table'
                              AND name LIKE 'restore_clearer_fts4_%'
                            ORDER BY name
                            """.trimIndent(),
                        ).use { cursor ->
                            buildList {
                                while (cursor.moveToNext()) {
                                    add(cursor.getString(0))
                                }
                            }
                        }

                    // This is the historical FTS3/FTS4 family that triggered
                    // the real-device restore failure.
                    check(shadowTables.any { it.endsWith("_segdir") }) {
                        "FTS4 regression fixture did not create a _segdir shadow table: $shadowTables"
                    }

                    TransactionAwareRoomClearer(database).clearAllApplicationTables()

                    val logicalCount =
                        sql.query(
                            "SELECT COUNT(*) FROM restore_clearer_fts4",
                        ).use { cursor ->
                            check(cursor.moveToFirst())
                            cursor.getLong(0)
                        }

                    assertEquals(0L, logicalCount)
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun `successful clear removes destination state`() =
        runBlocking {
            val database = database()
            try {
                database.workspaceDao().upsert(listOf(sentinel()))

                restoreDataSource(database) { bundle ->
                    database.workspaceDao().upsert(requireNotNull(bundle.workspaces))
                }.replaceWith(canonicalBundle(restored()))

                assertNull(database.workspaceDao().getById(SENTINEL_ID))
                assertNotNull(database.workspaceDao().getById(RESTORED_ID))
            } finally {
                database.close()
            }
        }

    @Test
    fun `validation failure occurs before destination clear`() =
        runBlocking {
            val database = database()
            try {
                database.workspaceDao().upsert(listOf(sentinel()))

                assertThrows(IllegalArgumentException::class.java) {
                    runBlocking {
                        restoreDataSource(database) { error("writer must not run") }
                            .replaceWith(SnapshotBundle())
                    }
                }

                assertNotNull(database.workspaceDao().getById(SENTINEL_ID))
            } finally {
                database.close()
            }
        }

    private fun sentinel() =
        workspace(SENTINEL_ID)

    private fun restored() =
        workspace(RESTORED_ID)

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
            provenance = WorkspaceProvenance.STANDALONE.name,
            sourceContextId = null,
        )

    private fun canonicalBundle(workspace: WorkspaceEntity) =
        SnapshotBundle(
            contexts = emptyList(),
            managedSubjects = emptyList(),
            orientations = emptyList(),
            aspects = emptyList(),
            orientationAssessments = emptyList(),
            orientationAssessmentRevisions = emptyList(),
            legacySubjectMappings = emptyList(),
            orientationRelations = emptyList(),
            aspectOrientationRefs = emptyList(),
            workspaces = listOf(workspace),
            workspaceBindings = emptyList(),
            workspaceCapabilityInstances = emptyList(),
            savedOrientationViews = emptyList(),
            workspaceBacklogEntries = emptyList(),
            workspaceInboxRecords = emptyList(),
        )

    private fun restoreDataSource(
        database: AppDatabase,
        write: suspend (SnapshotBundle) -> Unit,
    ) = SnapshotRestoreLocalDataSourceImpl(
        database = database,
        writer = CanonicalSnapshotTransactionWriter(write),
        dayManagementRuntimeRepository = mockk<DayManagementRuntimeRepository>(relaxed = true),
    )

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    private companion object {
        const val SENTINEL_ID = "restore-destination-sentinel"
        const val RESTORED_ID = "restore-source-workspace"
    }
}
