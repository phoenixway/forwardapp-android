package com.romankozak.forwardappmobile.data.database

import android.content.ContentValues
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration167To168WorkspaceTagsRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `167 to 169 adds canonical Workspace tag storage and System seed state`() {
        val dbName = "migration_167_168_workspace_tags"
        createFixture(dbName) { db ->
            insertWorkspace(db, "sys-owner")
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_167_168)
                .addMigrations(MIGRATION_168_169)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(169L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "workspace_tag_refs"))
            assertTrue(tableExists(db, "system_workspace_tag_seed_states"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM workspace_tag_refs"))

            db.execSQL(
                """
                INSERT INTO workspace_tag_refs (
                    workspaceId,
                    normalizedTag,
                    createdAt,
                    updatedAt,
                    syncedAt,
                    isDeleted,
                    version
                ) VALUES ('sys-owner', 'strategic', 10, 10, NULL, 0, 1)
                """.trimIndent(),
            )

            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM workspace_tag_refs
                    WHERE workspaceId = 'sys-owner'
                      AND normalizedTag = 'strategic'
                      AND isDeleted = 0
                      AND version = 1
                    """.trimIndent(),
                ),
            )

            db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun createFixture(
        dbName: String,
        populate: (SupportSQLiteDatabase) -> Unit,
    ) {
        context.deleteDatabase(dbName)
        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(167) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchema(db, 167)
                            populate(db)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()

        FrameworkSQLiteOpenHelperFactory()
            .create(configuration)
            .use { it.writableDatabase }
    }

    private fun insertWorkspace(
        db: SupportSQLiteDatabase,
        id: String,
    ) {
        db.insert(
            "workspaces",
            0,
            ContentValues().apply {
                put("id", id)
                put("workspaceOrder", 0L)
                put("createdAt", 10L)
                put("updatedAt", 10L)
                put("isDeleted", 0)
                put("version", 1L)
                put("provenance", "CANONICAL_ONLY")
            },
        )
    }

    private fun createSchema(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val database =
            JsonParser.parseReader(
                File(
                    "schemas/com.romankozak.forwardappmobile.database.AppDatabase/$version.json",
                ).reader(),
            ).asJsonObject.getAsJsonObject("database")

        database.getAsJsonArray("entities").forEach { element ->
            val entity = element.asJsonObject
            val table = entity.get("tableName").asString
            db.execSQL(
                entity.get("createSql").asString.replace("${'$'}{TABLE_NAME}", table),
            )
            entity.getAsJsonArray("indices")?.forEach { index ->
                db.execSQL(
                    index.asJsonObject.get("createSql").asString
                        .replace("${'$'}{TABLE_NAME}", table),
                )
            }
        }
        database.getAsJsonArray("views")
            ?.forEach { db.execSQL(it.asJsonObject.get("createSql").asString) }
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
