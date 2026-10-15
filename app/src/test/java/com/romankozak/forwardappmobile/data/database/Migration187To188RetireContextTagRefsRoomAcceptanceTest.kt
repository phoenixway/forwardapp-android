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
class Migration187To188RetireContextTagRefsRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `187 to 188 retires Context tag refs and preserves canonical tags and Context data`() {
        val name = "migration_187_188_retire_context_tag_refs"
        createFromExported187(name)
        seed187(name)

        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_187_188)
                .allowMainThreadQueries()
                .build()

        try {
            val db = database.openHelper.writableDatabase

            assertEquals(188L, scalarLong(db, "PRAGMA user_version"))
            assertFalse(tableExists(db, "context_tag_refs"))
            assertFalse(indexExists(db, "index_context_tag_refs_normalized_tag"))

            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM contexts
                    WHERE id = 'ordinary-context'
                      AND name = 'Ordinary Context'
                      AND description = 'must survive'
                      AND createdAt = 100
                      AND updatedAt = 200
                      AND is_deleted = 0
                      AND version = 7
                      AND tags = '["legacy","restore-ingress"]'
                      AND scoring_status = 'CURRENT'
                    """.trimIndent(),
                ),
            )

            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM workspace_tag_refs
                    WHERE workspaceId = 'workspace-owner'
                      AND normalizedTag = 'canonical'
                      AND createdAt = 300
                      AND updatedAt = 400
                      AND syncedAt IS NULL
                      AND isDeleted = 0
                      AND version = 9
                    """.trimIndent(),
                ),
            )

            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private fun seed187(name: String) {
        open187(name).use { helper ->
            val db = helper.writableDatabase

            assertTrue(tableExists(db, "context_tag_refs"))
            assertTrue(tableExists(db, "workspace_tag_refs"))

            db.execSQL(
                """
                INSERT INTO contexts (
                    id, name, description, createdAt, updatedAt,
                    is_deleted, version, tags, scoring_status
                ) VALUES (
                    'ordinary-context',
                    'Ordinary Context',
                    'must survive',
                    100,
                    200,
                    0,
                    7,
                    '["legacy","restore-ingress"]',
                    'CURRENT'
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO context_tag_refs (context_id, normalized_tag)
                VALUES ('ordinary-context', 'legacy')
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO workspaces (
                    id, nameOverride, createdAt, updatedAt,
                    isDeleted, version, provenance
                ) VALUES (
                    'workspace-owner',
                    'Canonical Workspace',
                    300,
                    400,
                    0,
                    5,
                    'STANDALONE'
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO workspace_tag_refs (
                    workspaceId, normalizedTag, createdAt, updatedAt,
                    syncedAt, isDeleted, version
                ) VALUES (
                    'workspace-owner',
                    'canonical',
                    300,
                    400,
                    NULL,
                    0,
                    9
                )
                """.trimIndent(),
            )

            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM context_tag_refs"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM workspace_tag_refs"))
        }
    }

    private fun createFromExported187(name: String) {
        context.deleteDatabase(name)
        open187(name).use { it.writableDatabase }
    }

    private fun open187(name: String): SupportSQLiteOpenHelper {
        val schema =
            JsonParser.parseString(findSchema187().readText())
                .asJsonObject
                .getAsJsonObject("database")

        return FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(187) {
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
                                        index.asJsonObject.get("createSql").asString
                                            .replace("\${TABLE_NAME}", table),
                                    )
                                }
                                entity.get("contentSyncTriggers")
                                    ?.asJsonArray
                                    ?.forEach { db.execSQL(it.asString) }
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
        )
    }

    private fun findSchema187(): File {
        var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        val relative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/187.json"

        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 187")
    }

    private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$name'",
        ) == 1L

    private fun indexExists(db: SupportSQLiteDatabase, name: String): Boolean =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='$name'",
        ) == 1L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use {
            check(it.moveToFirst())
            it.getLong(0)
        }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use {
            check(it.moveToFirst())
            it.getString(0)
        }
}
