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
class Migration182To183RetireContextFtsRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `182 to 183 retires only context fts and preserves Context persistence`() {
        val dbName = "migration_182_183_retire_context_fts"
        createFromExported182(dbName)
        seedRepresentativeContext(dbName)

        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_182_183)
                .allowMainThreadQueries()
                .build()

        try {
            val db = database.openHelper.writableDatabase

            assertEquals(183L, scalarLong(db, "PRAGMA user_version"))

            assertFalse(tableExists(db, "contexts_fts"))
            assertFalse(triggerExists(db, "room_fts_content_sync_contexts_fts_BEFORE_UPDATE"))
            assertFalse(triggerExists(db, "room_fts_content_sync_contexts_fts_BEFORE_DELETE"))
            assertFalse(triggerExists(db, "room_fts_content_sync_contexts_fts_AFTER_UPDATE"))
            assertFalse(triggerExists(db, "room_fts_content_sync_contexts_fts_AFTER_INSERT"))

            assertTrue(tableExists(db, "contexts"))
            assertTrue(tableExists(db, "context_tag_refs"))
            assertTrue(tableExists(db, "context_structures"))
            assertTrue(tableExists(db, "context_structure_items"))

            // Parallel V3 schema content from 182 must survive unchanged.
            assertTrue(tableExists(db, "planning_scopes"))
            assertTrue(tableExists(db, "planning_commitments"))
            assertTrue(tableExists(db, "day_planning_scope_extensions"))

            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM contexts
                    WHERE id = 'ordinary-context'
                      AND name = 'Ordinary Context'
                      AND description = 'Context data must survive FTS retirement'
                      AND createdAt = 100
                      AND updatedAt = 200
                      AND is_deleted = 0
                      AND version = 7
                      AND scoring_status = 'CURRENT'
                    """.trimIndent(),
                ),
            )

            // Prove Context writes remain healthy after all FTS sync triggers are gone.
            db.execSQL(
                """
                UPDATE contexts
                SET name = 'Ordinary Context Updated',
                    updatedAt = 300
                WHERE id = 'ordinary-context'
                """.trimIndent(),
            )
            assertEquals(
                "Ordinary Context Updated",
                scalarString(
                    db,
                    "SELECT name FROM contexts WHERE id = 'ordinary-context'",
                ),
            )

            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            database.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun seedRepresentativeContext(dbName: String) {
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(182) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build(),
        ).use { helper ->
            val db = helper.writableDatabase

            assertTrue(tableExists(db, "contexts_fts"))
            assertTrue(triggerExists(db, "room_fts_content_sync_contexts_fts_BEFORE_UPDATE"))
            assertTrue(triggerExists(db, "room_fts_content_sync_contexts_fts_BEFORE_DELETE"))
            assertTrue(triggerExists(db, "room_fts_content_sync_contexts_fts_AFTER_UPDATE"))
            assertTrue(triggerExists(db, "room_fts_content_sync_contexts_fts_AFTER_INSERT"))

            db.execSQL(
                """
                INSERT INTO contexts (
                    id,
                    name,
                    description,
                    createdAt,
                    updatedAt,
                    is_deleted,
                    version,
                    scoring_status
                )
                VALUES (
                    'ordinary-context',
                    'Ordinary Context',
                    'Context data must survive FTS retirement',
                    100,
                    200,
                    0,
                    7,
                    'CURRENT'
                )
                """.trimIndent(),
            )

            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM contexts WHERE id = 'ordinary-context'",
                ),
            )

            // The schema-182 Room trigger should have populated external-content FTS.
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM contexts_fts WHERE docid = " +
                        "(SELECT rowid FROM contexts WHERE id = 'ordinary-context')",
                ),
            )
        }
    }

    private fun createFromExported182(dbName: String) {
        context.deleteDatabase(dbName)
        val schema =
            JsonParser.parseString(findSchema182().readText())
                .asJsonObject
                .getAsJsonObject("database")

        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(182) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            schema.getAsJsonArray("entities").forEach { element ->
                                val entity = element.asJsonObject
                                val table = entity.get("tableName").asString
                                db.execSQL(
                                    entity.get("createSql").asString
                                        .replace("\${TABLE_NAME}", table),
                                )
                                entity.getAsJsonArray("indices")?.forEach { index ->
                                    db.execSQL(
                                        index.asJsonObject
                                            .get("createSql")
                                            .asString
                                            .replace("\${TABLE_NAME}", table),
                                    )
                                }
                                entity.get("contentSyncTriggers")
                                    ?.asJsonArray
                                    ?.forEach { trigger ->
                                        db.execSQL(trigger.asString)
                                    }
                            }

                            schema.getAsJsonArray("views")
                                ?.forEach { db.execSQL(it.asJsonObject.get("createSql").asString) }

                            schema.getAsJsonArray("setupQueries")
                                ?.forEach { db.execSQL(it.asString) }
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build(),
        ).use { it.writableDatabase }
    }

    private fun findSchema182(): File {
        var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        val relative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/182.json"
        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 182")
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'",
        ) == 1L

    private fun triggerExists(
        db: SupportSQLiteDatabase,
        trigger: String,
    ): Boolean =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type='trigger' AND name='$trigger'",
        ) == 1L

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            requireNotNull(cursor.getString(0))
        }
}
