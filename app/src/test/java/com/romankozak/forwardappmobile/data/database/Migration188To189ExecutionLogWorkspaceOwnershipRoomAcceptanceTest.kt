package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration188To189ExecutionLogWorkspaceOwnershipRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `188 to 189 converges legacy logs and retires Context locator`() {
        val name = "migration_188_189_execution_log_owner"
        createFromExported188(name)
        open188(name).use { helper ->
            val db = helper.writableDatabase
            insertContext(db, "legacy-owner")
            insertWorkspace(db, "legacy-owner", "CONTEXT_BACKED", "legacy-owner")
            insertWorkspace(db, "canonical-owner", "STANDALONE", null)
            insertLog(db, "legacy-log", "legacy-owner", null, deleted = false)
            insertLog(db, "canonical-log", null, "canonical-owner", deleted = false)
        }

        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_188_189)
                .allowMainThreadQueries()
                .build()

        try {
            val db = database.openHelper.writableDatabase
            assertEquals(189L, scalarLong(db, "PRAGMA user_version"))
            assertFalse(columnExists(db, "context_execution_logs", "contextId"))
            assertTrue(columnExists(db, "context_execution_logs", "workspaceId"))
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM pragma_table_info('context_execution_logs') " +
                        "WHERE name = 'workspaceId' AND `notnull` = 1",
                ),
            )
            assertEquals("legacy-owner", scalarString(db, "SELECT workspaceId FROM context_execution_logs WHERE id = 'legacy-log'"))
            assertEquals("canonical-owner", scalarString(db, "SELECT workspaceId FROM context_execution_logs WHERE id = 'canonical-log'"))
            assertEquals(
                2L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM context_execution_logs
                    WHERE timestamp = 10
                      AND type = 'COMMENT'
                      AND details = 'details'
                      AND updatedAt = 20
                      AND synced_at = 15
                      AND is_deleted = 0
                      AND version = 7
                    """.trimIndent(),
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM pragma_foreign_key_list('context_execution_logs') " +
                        "WHERE `table` = 'workspaces' AND `from` = 'workspaceId' AND `to` = 'id'",
                ),
            )
            assertEquals(
                0L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM pragma_foreign_key_list('context_execution_logs') " +
                        "WHERE `table` = 'contexts'",
                ),
            )
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun `188 to 189 fails closed and rolls back when owner is missing`() {
        val name = "migration_188_189_execution_log_missing_owner"
        createFromExported188(name)
        open188(name).use { helper ->
            val db = helper.writableDatabase
            insertContext(db, "missing-owner")
            insertLog(db, "legacy-log", "missing-owner", null, deleted = false)
        }

        assertMigrationFailsAndRollsBack(name, expectedWorkspaceId = null)
    }

    @Test
    fun `188 to 189 fails closed and rolls back on conflicting locators`() {
        val name = "migration_188_189_execution_log_conflict"
        createFromExported188(name)
        open188(name).use { helper ->
            val db = helper.writableDatabase
            insertContext(db, "legacy-owner")
            insertWorkspace(db, "legacy-owner", "CONTEXT_BACKED", "legacy-owner")
            insertWorkspace(db, "other-owner", "STANDALONE", null)
            insertLog(db, "conflict", "legacy-owner", "other-owner", deleted = false)
        }

        assertMigrationFailsAndRollsBack(name, expectedWorkspaceId = "other-owner")
    }

    @Test
    fun `188 to 189 rolls back convergence when canonical owner is deleted`() {
        val name = "migration_188_189_execution_log_deleted_owner"
        createFromExported188(name)
        open188(name).use { helper ->
            val db = helper.writableDatabase
            insertContext(db, "deleted-owner")
            insertWorkspace(
                db = db,
                id = "deleted-owner",
                provenance = "CONTEXT_BACKED",
                sourceContextId = "deleted-owner",
                deleted = true,
            )
            insertLog(db, "live-log", "deleted-owner", null, deleted = false)
        }

        assertMigrationFailsAndRollsBack(name, expectedWorkspaceId = null)
    }

    private fun assertMigrationFailsAndRollsBack(
        name: String,
        expectedWorkspaceId: String?,
    ) {
        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_188_189)
                .allowMainThreadQueries()
                .build()
        try {
            assertTrue(runCatching { database.openHelper.writableDatabase }.exceptionOrNull() != null)
        } finally {
            runCatching { database.close() }
        }

        open188(name).use { helper ->
            val db = helper.writableDatabase
            assertEquals(188L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(columnExists(db, "context_execution_logs", "contextId"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM context_execution_logs"))
            if (expectedWorkspaceId == null) {
                assertEquals(
                    1L,
                    scalarLong(db, "SELECT COUNT(*) FROM context_execution_logs WHERE workspaceId IS NULL"),
                )
            } else {
                assertEquals(
                    expectedWorkspaceId,
                    scalarString(db, "SELECT workspaceId FROM context_execution_logs"),
                )
            }
        }
        context.deleteDatabase(name)
    }

    private fun insertContext(db: SupportSQLiteDatabase, id: String) {
        db.execSQL(
            "INSERT INTO contexts (id, name, createdAt, updatedAt, scoring_status) " +
                "VALUES (?, ?, 1, 1, 'NOT_ASSESSED')",
            arrayOf(id, id),
        )
    }

    private fun insertWorkspace(
        db: SupportSQLiteDatabase,
        id: String,
        provenance: String,
        sourceContextId: String?,
        deleted: Boolean = false,
    ) {
        db.execSQL(
            """
            INSERT INTO workspaces (
                id, nameOverride, createdAt, updatedAt, isDeleted, version,
                provenance, sourceContextId
            ) VALUES (?, ?, 1, 1, ?, 1, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(id, id, if (deleted) 1 else 0, provenance, sourceContextId),
        )
    }

    private fun insertLog(
        db: SupportSQLiteDatabase,
        id: String,
        contextId: String?,
        workspaceId: String?,
        deleted: Boolean,
    ) {
        db.execSQL(
            """
            INSERT INTO context_execution_logs (
                id, contextId, timestamp, type, description, details, updatedAt,
                synced_at, is_deleted, version, workspaceId
            ) VALUES (?, ?, 10, 'COMMENT', ?, 'details', 20, 15, ?, 7, ?)
            """.trimIndent(),
            arrayOf<Any?>(id, contextId, id, if (deleted) 1 else 0, workspaceId),
        )
    }

    private fun createFromExported188(name: String) {
        context.deleteDatabase(name)
        open188(name).use { it.writableDatabase }
    }

    private fun open188(name: String): SupportSQLiteOpenHelper {
        val schema =
            JsonParser.parseString(findSchema188().readText())
                .asJsonObject
                .getAsJsonObject("database")
        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(188) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            schema.getAsJsonArray("entities").forEach { element ->
                                val entity = element.asJsonObject
                                val table = entity.get("tableName").asString
                                db.execSQL(entity.get("createSql").asString.replace("\${TABLE_NAME}", table))
                                entity.getAsJsonArray("indices")?.forEach { index ->
                                    db.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table))
                                }
                                entity.get("contentSyncTriggers")?.asJsonArray?.forEach { db.execSQL(it.asString) }
                            }
                            schema.getAsJsonArray("views")?.forEach { db.execSQL(it.asJsonObject.get("createSql").asString) }
                            schema.getAsJsonArray("setupQueries")?.forEach { db.execSQL(it.asString) }
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                ).build(),
        )
    }

    private fun findSchema188(): File {
        var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        val relative = "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/188.json"
        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 188")
    }

    private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean =
        scalarLong(db, "SELECT COUNT(*) FROM pragma_table_info('$table') WHERE name = '$column'") == 1L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getString(0) }
}
