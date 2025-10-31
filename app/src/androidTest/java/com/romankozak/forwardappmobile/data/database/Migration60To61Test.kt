package com.romankozak.forwardappmobile.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration60To61Test {
    private val dbName = "migration_60_61_test"

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrateCustomListsToNoteDocuments() {
        helper
            .createDatabase(dbName, 60)
            .apply {
                execSQL("PRAGMA foreign_keys=OFF")
                execSQL(
                    """
                    INSERT INTO custom_lists (id, projectId, name, createdAt, updatedAt, content, lastCursorPosition)
                    VALUES ('doc_1', 'project_1', 'Legacy List', 0, 0, 'Line 1', 0)
                    """.trimIndent(),
                )
                execSQL(
                    """
                    INSERT INTO custom_list_items (id, listId, parentId, content, isCompleted, itemOrder, createdAt, updatedAt)
                    VALUES ('item_1', 'doc_1', NULL, 'Item content', 0, 0, 0, 0)
                    """.trimIndent(),
                )
                execSQL(
                    """
                    INSERT INTO list_items (id, project_id, itemType, entityId, item_order)
                    VALUES ('list_item_1', 'project_1', 'CUSTOM_LIST', 'doc_1', 0)
                    """.trimIndent(),
                )
                close()
            }

        val migratedDb = helper.runMigrationsAndValidate(dbName, 61, true, MIGRATION_60_61)

        migratedDb.query("SELECT name, content FROM note_documents WHERE id = 'doc_1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Legacy List", cursor.getString(0))
            assertEquals("Line 1", cursor.getString(1))
        }

        migratedDb.query("SELECT COUNT(*) FROM note_document_items WHERE listId = 'doc_1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }

        migratedDb.query("SELECT itemType FROM list_items WHERE id = 'list_item_1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("NOTE_DOCUMENT", cursor.getString(0))
        }

        migratedDb.close()
    }
}
