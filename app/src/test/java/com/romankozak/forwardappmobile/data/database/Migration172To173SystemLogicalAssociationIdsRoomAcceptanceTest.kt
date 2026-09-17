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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration172To173SystemLogicalAssociationIdsRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `172 to 173 preserves exact System logical ids after Context shell deletion`() {
        val dbName = "migration_172_173_system_logical_ids"

        createFixture(dbName) { db ->
            insertContext(db, SYSTEM_ID)
            insertCanonicalWorkspace(db, SYSTEM_ID)
            insertInboxCapability(db)
            insertInboxRecord(db)
            insertGoal(db)
            insertScript(db)
            insertInboxLink(db)
            insertBacklogLink(db)
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_172_173)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(173L, scalarLong(db, "PRAGMA user_version"))

            assertEquals(
                emptyList<String>(),
                foreignKeyParents(db, "scripts"),
            )
            assertEquals(
                listOf("workspace_inbox_records"),
                foreignKeyParents(db, "inbox_record_links"),
            )
            assertEquals(
                listOf("goals"),
                foreignKeyParents(db, "backlog_goal_association_links"),
            )

            // This is the Step-11 property: after relational cutover, deleting
            // the legacy System Context shell must not erase logical
            // associations whose identity is now canonical outside Context.
            db.execSQL("DELETE FROM `contexts` WHERE `id` = ?", arrayOf(SYSTEM_ID))

            assertEquals(
                0L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM contexts WHERE id = '$SYSTEM_ID'",
                ),
            )
            assertEquals(
                SYSTEM_ID,
                scalarString(
                    db,
                    "SELECT contextId FROM scripts WHERE id = 'script-system'",
                ),
            )
            assertEquals(
                SYSTEM_ID,
                scalarString(
                    db,
                    "SELECT context_id FROM inbox_record_links WHERE record_id = 'inbox-record'",
                ),
            )
            assertEquals(
                SYSTEM_ID,
                scalarString(
                    db,
                    "SELECT context_id FROM backlog_goal_association_links WHERE goal_id = 'goal-system'",
                ),
            )

            // The real authoritative parent FKs remain active.
            db.execSQL("DELETE FROM `workspace_inbox_records` WHERE `id` = 'inbox-record'")
            assertEquals(
                0L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM inbox_record_links WHERE record_id = 'inbox-record'",
                ),
            )

            db.execSQL("DELETE FROM `goals` WHERE `id` = 'goal-system'")
            assertEquals(
                0L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM backlog_goal_association_links WHERE goal_id = 'goal-system'",
                ),
            )

            // Script logical association is intentionally independent from
            // Context row lifetime.
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM scripts WHERE id = 'script-system'",
                ),
            )
            assertEquals(
                SYSTEM_ID,
                scalarString(
                    db,
                    "SELECT contextId FROM scripts WHERE id = 'script-system'",
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

        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(172) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                createSchema(db, 172)
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
    ) {
        check(
            db.insert(
                "contexts",
                0,
                ContentValues().apply {
                    put("id", id)
                    put("name", "Legacy System shell")
                    put("createdAt", 1L)
                    put("updatedAt", 1L)
                    put("is_deleted", 0)
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
                    put("nameOverride", "Canonical System")
                    put("workspaceOrder", 0L)
                    put("createdAt", 1L)
                    put("updatedAt", 1L)
                    put("isDeleted", 0)
                    put("version", 1L)
                    put("provenance", "CANONICAL_ONLY")
                    putNull("sourceContextId")
                },
            ) != -1L,
        )
    }

    private fun insertInboxCapability(db: SupportSQLiteDatabase) {
        check(
            db.insert(
                "workspace_capability_instances",
                0,
                ContentValues().apply {
                    put("id", "inbox-capability")
                    put("workspaceId", SYSTEM_ID)
                    put("capabilityType", "INBOX")
                    put("instanceKey", "default")
                    put("capabilityOrder", 0L)
                    put("state", "ACTIVE")
                    put("configurationVersion", 1)
                    put("configuration", "{}")
                    put("createdAt", 1L)
                    put("updatedAt", 1L)
                    put("isDeleted", 0)
                    put("version", 1L)
                },
            ) != -1L,
        )
    }

    private fun insertInboxRecord(db: SupportSQLiteDatabase) {
        check(
            db.insert(
                "workspace_inbox_records",
                0,
                ContentValues().apply {
                    put("id", "inbox-record")
                    put("workspaceId", SYSTEM_ID)
                    put("capabilityInstanceId", "inbox-capability")
                    put("text", "Inbox #system")
                    put("createdAt", 1L)
                    put("recordOrder", 0L)
                    put("updatedAt", 1L)
                    put("isDeleted", 0)
                    put("version", 1L)
                },
            ) != -1L,
        )
    }

    private fun insertGoal(db: SupportSQLiteDatabase) {
        check(
            db.insert(
                "goals",
                0,
                ContentValues().apply {
                    put("id", "goal-system")
                    put("text", "Goal #system")
                    put("completed", 0)
                    put("createdAt", 1L)
                    put("updatedAt", 1L)
                    put("is_deleted", 0)
                    put("version", 1L)
                    put("scoring_status", "NOT_ASSESSED")
                },
            ) != -1L,
        )
    }

    private fun insertScript(db: SupportSQLiteDatabase) {
        check(
            db.insert(
                "scripts",
                0,
                ContentValues().apply {
                    put("id", "script-system")
                    put("contextId", SYSTEM_ID)
                    put("name", "System script")
                    put("content", "echo system")
                    put("createdAt", 1L)
                    put("updatedAt", 1L)
                    put("isDeleted", 0)
                    put("version", 1L)
                },
            ) != -1L,
        )
    }

    private fun insertInboxLink(db: SupportSQLiteDatabase) {
        check(
            db.insert(
                "inbox_record_links",
                0,
                ContentValues().apply {
                    put("record_id", "inbox-record")
                    put("context_id", SYSTEM_ID)
                    put("owner_context_id", SYSTEM_ID)
                    put("association_tag", "system")
                    put("linked_at", 1L)
                },
            ) != -1L,
        )
    }

    private fun insertBacklogLink(db: SupportSQLiteDatabase) {
        check(
            db.insert(
                "backlog_goal_association_links",
                0,
                ContentValues().apply {
                    put("projection_id", "projection-system")
                    put("goal_id", "goal-system")
                    put("context_id", SYSTEM_ID)
                    put("owner_context_id", SYSTEM_ID)
                    put("association_tag", "system")
                    put("item_order", 0L)
                    put("linked_at", 1L)
                },
            ) != -1L,
        )
    }

    private fun createSchema(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val database =
            schemaFile(version).reader().use {
                JsonParser.parseReader(it).asJsonObject.getAsJsonObject("database")
            }

        database.getAsJsonArray("entities").forEach { element ->
            val entity = element.asJsonObject
            val table = entity.get("tableName").asString
            db.execSQL(
                entity.get("createSql").asString.replace("\${TABLE_NAME}", table),
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
            ?: error("Room schema $version not found from ${userDir.absolutePath}")
    }

    private fun foreignKeyParents(
        db: SupportSQLiteDatabase,
        table: String,
    ): List<String> =
        db.query("PRAGMA foreign_key_list(`$table`)").use { cursor ->
            val tableColumn = cursor.getColumnIndexOrThrow("table")
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(tableColumn))
                }
            }.sorted()
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

    private companion object {
        const val SYSTEM_ID = "sys_today"
    }
}
