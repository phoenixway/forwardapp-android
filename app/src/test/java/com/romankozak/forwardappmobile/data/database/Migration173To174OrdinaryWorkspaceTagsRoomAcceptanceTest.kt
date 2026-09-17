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
class Migration173To174OrdinaryWorkspaceTagsRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `173 to 174 backfills only retired ordinary Workspaces without canonical tag state`() {
        val dbName = "migration_173_174_ordinary_workspace_tags"

        createFixture(dbName) { db ->
            insertContext(db, BACKFILL_ID, deleted = true)
            insertCanonicalWorkspace(db, BACKFILL_ID)
            insertLegacyTag(db, BACKFILL_ID, "alpha")
            insertLegacyTag(db, BACKFILL_ID, "beta")

            insertContext(db, CANONICAL_ID, deleted = true)
            insertCanonicalWorkspace(db, CANONICAL_ID)
            insertLegacyTag(db, CANONICAL_ID, "legacy-stale")
            insertCanonicalTag(
                db = db,
                workspaceId = CANONICAL_ID,
                tag = "canonical-live",
                isDeleted = false,
                version = 7L,
            )

            insertContext(db, CLEARED_ID, deleted = true)
            insertCanonicalWorkspace(db, CLEARED_ID)
            insertLegacyTag(db, CLEARED_ID, "must-not-resurrect")
            insertCanonicalTag(
                db = db,
                workspaceId = CLEARED_ID,
                tag = "previous-canonical",
                isDeleted = true,
                version = 8L,
            )

            insertContext(db, ACTIVE_ID, deleted = false)
            insertCanonicalWorkspace(db, ACTIVE_ID)
            insertLegacyTag(db, ACTIVE_ID, "active-legacy")
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_173_174)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(174L, scalarLong(db, "PRAGMA user_version"))

            assertEquals(
                listOf(
                    TagRow("alpha", isDeleted = false, version = 1L),
                    TagRow("beta", isDeleted = false, version = 1L),
                ),
                tagRows(db, BACKFILL_ID),
            )

            assertEquals(
                listOf(
                    TagRow(
                        "canonical-live",
                        isDeleted = false,
                        version = 7L,
                    ),
                ),
                tagRows(db, CANONICAL_ID),
            )

            assertEquals(
                listOf(
                    TagRow(
                        "previous-canonical",
                        isDeleted = true,
                        version = 8L,
                    ),
                ),
                tagRows(db, CLEARED_ID),
            )

            assertEquals(
                emptyList<TagRow>(),
                tagRows(db, ACTIVE_ID),
            )

            db.query("PRAGMA foreign_key_check").use {
                assertEquals(0, it.count)
            }
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

        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(173) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                createSchema(db, 173)
                                populate(db)
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    ).build(),
            )

        helper.use { it.writableDatabase }
    }

    private fun insertContext(
        db: SupportSQLiteDatabase,
        id: String,
        deleted: Boolean,
    ) {
        check(
            db.insert(
                "contexts",
                0,
                ContentValues().apply {
                    put("id", id)
                    put("name", "Legacy $id")
                    put("createdAt", 1L)
                    put("updatedAt", 2L)
                    put("is_deleted", if (deleted) 1 else 0)
                    put("version", 1L)
                    put("scoring_status", "NOT_ASSESSED")
                },
            ) != -1L,
        )
    }

    private fun insertCanonicalWorkspace(
        db: SupportSQLiteDatabase,
        id: String,
    ) {
        check(
            db.insert(
                "workspaces",
                0,
                ContentValues().apply {
                    put("id", id)
                    put("nameOverride", "Canonical $id")
                    put("workspaceOrder", 0L)
                    put("createdAt", 1L)
                    put("updatedAt", 2L)
                    put("isDeleted", 0)
                    put("version", 1L)
                    put("provenance", "CANONICAL_ONLY")
                    putNull("sourceContextId")
                },
            ) != -1L,
        )
    }

    private fun insertLegacyTag(
        db: SupportSQLiteDatabase,
        contextId: String,
        tag: String,
    ) {
        check(
            db.insert(
                "context_tag_refs",
                0,
                ContentValues().apply {
                    put("context_id", contextId)
                    put("normalized_tag", tag)
                },
            ) != -1L,
        )
    }

    private fun insertCanonicalTag(
        db: SupportSQLiteDatabase,
        workspaceId: String,
        tag: String,
        isDeleted: Boolean,
        version: Long,
    ) {
        check(
            db.insert(
                "workspace_tag_refs",
                0,
                ContentValues().apply {
                    put("workspaceId", workspaceId)
                    put("normalizedTag", tag)
                    put("createdAt", 1L)
                    put("updatedAt", 2L)
                    putNull("syncedAt")
                    put("isDeleted", if (isDeleted) 1 else 0)
                    put("version", version)
                },
            ) != -1L,
        )
    }

    private fun tagRows(
        db: SupportSQLiteDatabase,
        workspaceId: String,
    ): List<TagRow> =
        db.query(
            """
            SELECT normalizedTag, isDeleted, version
            FROM workspace_tag_refs
            WHERE workspaceId = '$workspaceId'
            ORDER BY normalizedTag
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        TagRow(
                            tag = cursor.getString(0),
                            isDeleted = cursor.getInt(1) != 0,
                            version = cursor.getLong(2),
                        ),
                    )
                }
            }
        }

    private fun createSchema(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val database =
            schemaFile(version).reader().use {
                JsonParser.parseReader(it)
                    .asJsonObject
                    .getAsJsonObject("database")
            }

        database.getAsJsonArray("entities").forEach { element ->
            val entity = element.asJsonObject
            val table = entity.get("tableName").asString
            db.execSQL(
                entity.get("createSql")
                    .asString
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
        }

        database.getAsJsonArray("views")?.forEach { view ->
            db.execSQL(view.asJsonObject.get("createSql").asString)
        }
    }

    private fun schemaFile(version: Int): File {
        val relative =
            "schemas/com.romankozak.forwardappmobile.database.AppDatabase/$version.json"
        val userDir = File(System.getProperty("user.dir"))

        return listOf(
            File(relative),
            File("app/$relative"),
            File(userDir, relative),
            File(userDir, "app/$relative"),
        ).firstOrNull { it.isFile }
            ?: error(
                "Room schema $version not found from ${userDir.absolutePath}",
            )
    }

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

    private data class TagRow(
        val tag: String,
        val isDeleted: Boolean,
        val version: Long,
    )

    private companion object {
        const val BACKFILL_ID = "retired-backfill"
        const val CANONICAL_ID = "retired-canonical"
        const val CLEARED_ID = "retired-cleared"
        const val ACTIVE_ID = "active-context"
    }
}
