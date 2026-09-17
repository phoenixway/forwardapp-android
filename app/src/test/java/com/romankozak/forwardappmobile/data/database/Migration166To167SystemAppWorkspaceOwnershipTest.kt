package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration166To167SystemAppWorkspaceOwnershipTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `migration preserves SystemApp row and replaces Context FK with live same-id Workspace FK`() =
        withLegacyDatabase("system_app_workspace_success") { db ->
            insertOwnerRows(db, workspaceDeleted = false)
            db.execSQL(
                """
                INSERT INTO system_apps (
                    id, system_key, app_type, context_id, note_document_id,
                    createdAt, updatedAt, version, isDeleted
                ) VALUES (
                    'app-1', 'my-life-current-state', 'NOTE_DOCUMENT', 'sys_strategic', 'doc-1',
                    10, 20, 3, 0
                )
                """.trimIndent(),
            )

            MIGRATION_166_167.migrate(db)

            assertTrue(columnExists(db, "system_apps", "workspace_id"))
            assertFalse(columnExists(db, "system_apps", "context_id"))
            db.query(
                """
                SELECT id, system_key, app_type, workspace_id, note_document_id,
                       createdAt, updatedAt, version, isDeleted
                FROM system_apps
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("app-1", cursor.getString(0))
                assertEquals("my-life-current-state", cursor.getString(1))
                assertEquals("NOTE_DOCUMENT", cursor.getString(2))
                assertEquals("sys_strategic", cursor.getString(3))
                assertEquals("doc-1", cursor.getString(4))
                assertEquals(10L, cursor.getLong(5))
                assertEquals(20L, cursor.getLong(6))
                assertEquals(3L, cursor.getLong(7))
                assertEquals(0, cursor.getInt(8))
            }
            assertForeignKey(db, column = "workspace_id", targetTable = "workspaces", onDelete = "CASCADE")
            assertForeignKey(db, column = "note_document_id", targetTable = "note_documents", onDelete = "SET NULL")

            db.execSQL("PRAGMA foreign_keys = ON")
            db.execSQL("DELETE FROM contexts WHERE id = 'sys_strategic'")
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM system_apps"))
        }

    @Test
    fun `migration fails closed when same-id Workspace is missing`() =
        withLegacyDatabase("system_app_workspace_missing") { db ->
            db.execSQL("INSERT INTO contexts (id) VALUES ('sys_strategic')")
            db.execSQL(
                """
                INSERT INTO system_apps (
                    id, system_key, app_type, context_id, note_document_id,
                    createdAt, updatedAt, version, isDeleted
                ) VALUES ('app-1', 'key', 'NOTE_DOCUMENT', 'sys_strategic', NULL, 1, 2, 1, 0)
                """.trimIndent(),
            )

            assertFailsClosed(db)
        }

    @Test
    fun `migration fails closed when same-id Workspace is deleted`() =
        withLegacyDatabase("system_app_workspace_deleted") { db ->
            insertOwnerRows(db, workspaceDeleted = true)
            db.execSQL(
                """
                INSERT INTO system_apps (
                    id, system_key, app_type, context_id, note_document_id,
                    createdAt, updatedAt, version, isDeleted
                ) VALUES ('app-1', 'key', 'NOTE_DOCUMENT', 'sys_strategic', NULL, 1, 2, 1, 0)
                """.trimIndent(),
            )

            assertFailsClosed(db)
        }

    private fun assertFailsClosed(db: SupportSQLiteDatabase) {
        var failed = false
        try {
            MIGRATION_166_167.migrate(db)
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue(failed)
        assertTrue(columnExists(db, "system_apps", "context_id"))
        assertFalse(columnExists(db, "system_apps", "workspace_id"))
    }

    private fun withLegacyDatabase(
        name: String,
        block: (SupportSQLiteDatabase) -> Unit,
    ) {
        context.deleteDatabase(name)
        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(166) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            db.execSQL("CREATE TABLE contexts (id TEXT NOT NULL PRIMARY KEY)")
                            db.execSQL("CREATE TABLE workspaces (id TEXT NOT NULL PRIMARY KEY, isDeleted INTEGER NOT NULL)")
                            db.execSQL("CREATE TABLE note_documents (id TEXT NOT NULL PRIMARY KEY)")
                            db.execSQL(
                                """
                                CREATE TABLE system_apps (
                                    id TEXT NOT NULL PRIMARY KEY,
                                    system_key TEXT NOT NULL,
                                    app_type TEXT NOT NULL,
                                    context_id TEXT NOT NULL,
                                    note_document_id TEXT,
                                    createdAt INTEGER NOT NULL,
                                    updatedAt INTEGER NOT NULL,
                                    version INTEGER NOT NULL,
                                    isDeleted INTEGER NOT NULL,
                                    FOREIGN KEY(context_id) REFERENCES contexts(id) ON DELETE CASCADE,
                                    FOREIGN KEY(note_document_id) REFERENCES note_documents(id) ON DELETE SET NULL
                                )
                                """.trimIndent(),
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = error("Unexpected fixture upgrade $oldVersion->$newVersion")
                    },
                ).build()

        try {
            FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
                block(helper.writableDatabase)
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun insertOwnerRows(
        db: SupportSQLiteDatabase,
        workspaceDeleted: Boolean,
    ) {
        db.execSQL("INSERT INTO contexts (id) VALUES ('sys_strategic')")
        db.execSQL(
            "INSERT INTO workspaces (id, isDeleted) VALUES ('sys_strategic', ${if (workspaceDeleted) 1 else 0})",
        )
        db.execSQL("INSERT INTO note_documents (id) VALUES ('doc-1')")
    }

    private fun assertForeignKey(
        db: SupportSQLiteDatabase,
        column: String,
        targetTable: String,
        onDelete: String,
    ) {
        var found = false
        db.query("PRAGMA foreign_key_list(system_apps)").use { cursor ->
            val tableIndex = cursor.getColumnIndexOrThrow("table")
            val fromIndex = cursor.getColumnIndexOrThrow("from")
            val deleteIndex = cursor.getColumnIndexOrThrow("on_delete")
            while (cursor.moveToNext()) {
                if (
                    cursor.getString(fromIndex) == column &&
                    cursor.getString(tableIndex) == targetTable &&
                    cursor.getString(deleteIndex) == onDelete
                ) {
                    found = true
                }
            }
        }
        assertTrue("Missing $column -> $targetTable ON DELETE $onDelete foreign key", found)
    }

    private fun columnExists(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
    ): Boolean =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return@use true
            }
            false
        }

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }
}
