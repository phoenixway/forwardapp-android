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
class Migration157To158InboxCutoverRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `157 to 158 moves Inbox content config and cache to canonical ownership`() {
        val dbName = "migration_157_158_inbox_cutover"
        createFixture(dbName) { db ->
            insertContext(db, "owner")
            insertContext(db, "tagged")
            insertWorkspace(db, "owner")
            insertWorkspace(db, "tagged")
            insertCapability(db, "inbox-cap", "owner")
            insertConfiguration(db, "owner", hideWhenAssociated = true)
            insertInbox(db, "later", "owner", "Later #tag", 20L, -20L, version = 3L)
            insertInbox(db, "first", "owner", "First", 10L, -10L, version = 2L)
            insertLink(db, "later", "tagged", "owner")
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_157_158, MIGRATION_158_159)
                .allowMainThreadQueries()
                .build()
        try {
            val db = room.openHelper.writableDatabase
            assertEquals(159L, scalarLong(db, "PRAGMA user_version"))
            assertFalse(tableExists(db, "inbox_records"))
            assertEquals(2L, scalarLong(db, "SELECT COUNT(*) FROM workspace_inbox_records"))
            assertEquals(
                listOf("first", "later"),
                queryStrings(db, "SELECT id FROM workspace_inbox_records ORDER BY recordOrder"),
            )
            assertEquals(
                listOf(0L, 1L),
                queryLongs(db, "SELECT recordOrder FROM workspace_inbox_records ORDER BY recordOrder"),
            )
            assertEquals(
                "{\"ownerVisibility\":\"HIDE_WHEN_ASSOCIATED\"}",
                scalarString(
                    db,
                    "SELECT configuration FROM workspace_capability_instances WHERE id = 'inbox-cap'",
                ),
            )
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM inbox_record_links"))
            db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `157 to 158 fails closed on live legacy hide flag`() {
        val dbName = "migration_157_158_inbox_hide_flag"
        createFixture(dbName) { db ->
            insertContext(db, "owner")
            insertWorkspace(db, "owner")
            insertCapability(db, "inbox-cap", "owner")
            insertInbox(db, "hidden", "owner", "Hidden", 10L, -10L, hide = true)
        }

        val failure =
            runCatching {
                Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                    .addMigrations(MIGRATION_157_158)
                    .allowMainThreadQueries()
                    .build()
                    .openHelper.writableDatabase
            }.exceptionOrNull()
        assertTrue(failure != null)
        context.deleteDatabase(dbName)
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
                    object : SupportSQLiteOpenHelper.Callback(157) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchema(db, 157)
                            populate(db)
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                ).build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).use { it.writableDatabase }
    }

    private fun insertContext(db: SupportSQLiteDatabase, id: String) {
        db.insert(
            "contexts",
            0,
            ContentValues().apply {
                put("id", id); put("name", id); put("createdAt", 10L); put("updatedAt", 10L)
                put("is_deleted", 0); put("version", 1L); put("scoring_status", "UNASSESSED")
            },
        )
    }

    private fun insertWorkspace(db: SupportSQLiteDatabase, id: String) {
        db.insert(
            "workspaces",
            0,
            ContentValues().apply {
                put("id", id); put("workspaceOrder", 0L); put("createdAt", 10L); put("updatedAt", 10L)
                put("isDeleted", 0); put("version", 1L); put("provenance", "CONTEXT_BACKED")
                put("sourceContextId", id)
            },
        )
    }

    private fun insertCapability(db: SupportSQLiteDatabase, id: String, workspaceId: String) {
        db.insert(
            "workspace_capability_instances",
            0,
            ContentValues().apply {
                put("id", id); put("workspaceId", workspaceId); put("capabilityType", "INBOX")
                put("instanceKey", "default"); put("capabilityOrder", 0L); put("state", "ACTIVE")
                put("configurationVersion", 1); put("configuration", "{}")
                put("createdAt", 10L); put("updatedAt", 10L); put("isDeleted", 0); put("version", 1L)
            },
        )
    }

    private fun insertConfiguration(db: SupportSQLiteDatabase, contextId: String, hideWhenAssociated: Boolean) {
        db.insert(
            "context_structures",
            0,
            ContentValues().apply {
                put("id", "config-$contextId"); put("contextId", contextId)
                put("experimental_capability_ids", "[]"); put("apply_mode", "ADDITIVE")
                put("remove_inbox_entry_after_tag_autocopy", if (hideWhenAssociated) 1 else 0)
                put("updatedAt", 10L); put("version", 1L); put("isDeleted", 0)
            },
        )
    }

    private fun insertInbox(
        db: SupportSQLiteDatabase,
        id: String,
        contextId: String,
        text: String,
        createdAt: Long,
        order: Long,
        version: Long = 1L,
        hide: Boolean = false,
    ) {
        db.insert(
            "inbox_records",
            0,
            ContentValues().apply {
                put("id", id); put("contextId", contextId); put("text", text); put("createdAt", createdAt)
                put("item_order", order); put("updatedAt", createdAt); put("is_deleted", 0)
                put("hide_in_owner_inbox", if (hide) 1 else 0); put("version", version)
            },
        )
    }

    private fun insertLink(db: SupportSQLiteDatabase, recordId: String, contextId: String, ownerId: String) {
        db.insert(
            "inbox_record_links",
            0,
            ContentValues().apply {
                put("record_id", recordId); put("context_id", contextId); put("owner_context_id", ownerId)
                put("association_tag", "tag"); put("linked_at", 20L)
            },
        )
    }

    private fun createSchema(db: SupportSQLiteDatabase, version: Int) {
        val database =
            JsonParser.parseReader(
                File("schemas/com.romankozak.forwardappmobile.database.AppDatabase/$version.json").reader(),
            ).asJsonObject.getAsJsonObject("database")
        database.getAsJsonArray("entities").forEach { element ->
            val entity = element.asJsonObject
            val table = entity.get("tableName").asString
            db.execSQL(entity.get("createSql").asString.replace("\${TABLE_NAME}", table))
            entity.getAsJsonArray("indices")?.forEach { index ->
                db.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table))
            }
        }
        database.getAsJsonArray("views")?.forEach { db.execSQL(it.asJsonObject.get("createSql").asString) }
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String) =
        scalarLong(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'") == 1L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String) =
        db.query(sql).use { check(it.moveToFirst()); it.getLong(0) }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String) =
        db.query(sql).use { check(it.moveToFirst()); it.getString(0) }

    private fun queryStrings(db: SupportSQLiteDatabase, sql: String) =
        db.query(sql).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    private fun queryLongs(db: SupportSQLiteDatabase, sql: String) =
        db.query(sql).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) } }
}
