package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Production-copy acceptance for the Workspace-tag foundation and its
 * schema-169 System seed-state extension.
 *
 * Requires a consistent schema-167 database through
 * FORWARDAPP_PRODUCTION_DB_FILE. The supplied source is copied and never
 * modified.
 */
@RunWith(RobolectricTestRunner::class)
class ProductionCopy167To168WorkspaceTagsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `production schema 167 migrates to 169 without changing System ownership`() {
        val sourcePath =
            requireNotNull(System.getenv("FORWARDAPP_PRODUCTION_DB_FILE")) {
                "FORWARDAPP_PRODUCTION_DB_FILE is required"
            }
        val source = File(sourcePath)
        require(source.isFile) {
            "Production-copy database does not exist: ${source.absolutePath}"
        }

        val dbName = "production_copy_167_168_workspace_tags"
        context.deleteDatabase(dbName)

        val target = context.getDatabasePath(dbName)
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)

        val room =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                dbName,
            )
                .addMigrations(MIGRATION_167_168)
                .addMigrations(MIGRATION_168_169)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(169L, scalarLong(db, "PRAGMA user_version"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))

            db.query("PRAGMA foreign_key_check").use { cursor ->
                assertEquals(0, cursor.count)
            }

            assertTrue(tableExists(db, "workspace_tag_refs"))
            assertTrue(tableExists(db, "system_workspace_tag_seed_states"))
            assertEquals(
                0L,
                scalarLong(db, "SELECT COUNT(*) FROM workspace_tag_refs"),
            )

            val definitions = SystemOperationalDefinitions.all
            assertEquals(20, definitions.size)

            definitions.forEach { definition ->
                val id = definition.id

                assertEquals(
                    "Reserved System Context changed during 167 -> 168: $id",
                    1L,
                    scalarLong(
                        db,
                        """
                        SELECT COUNT(*)
                        FROM contexts
                        WHERE id = '$id'
                          AND is_deleted = 0
                        """.trimIndent(),
                    ),
                )

                db.query(
                    """
                    SELECT provenance, sourceContextId, isDeleted
                    FROM workspaces
                    WHERE id = ?
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(id),
                ).use { cursor ->
                    assertTrue(
                        "Missing reserved System Workspace after 167 -> 168: $id",
                        cursor.moveToFirst(),
                    )
                    assertEquals(
                        "System Workspace lost CANONICAL_ONLY ownership: $id",
                        "CANONICAL_ONLY",
                        cursor.getString(0),
                    )
                    assertTrue(
                        "System Workspace sourceContextId resurrected: $id",
                        cursor.isNull(1),
                    )
                    assertEquals(
                        "System Workspace was deleted during 167 -> 168: $id",
                        0,
                        cursor.getInt(2),
                    )
                    assertFalse(cursor.moveToNext())
                }
            }
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'",
        ) == 1L

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use {
            check(it.moveToFirst())
            it.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use {
            check(it.moveToFirst())
            it.getString(0)
        }
}
