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
class Migration158To159ConnectionsCutoverRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `158 to 159 moves Context attachment placements to canonical Connections`() {
        val dbName = "migration_158_159_connections_cutover"
        createFixture(dbName) { db ->
            insertContext(db, "owner")
            insertWorkspace(db, "owner")
            insertCapability(db, "connections-cap", "owner")
            insertAttachment(db, "newer", createdAt = 30L)
            insertAttachment(db, "older", createdAt = 10L)
            insertLink(db, "owner", "older", order = 5L, version = 2L, syncedAt = 100L)
            insertLink(db, "owner", "newer", order = 1L, version = 3L, syncedAt = 100L)
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_158_159)
                .allowMainThreadQueries()
                .build()
        try {
            val db = room.openHelper.writableDatabase
            assertEquals(159L, scalarLong(db, "PRAGMA user_version"))
            assertFalse(tableExists(db, "context_attachment_cross_ref"))
            assertEquals(2L, scalarLong(db, "SELECT COUNT(*) FROM workspace_connections"))
            assertEquals(
                listOf("newer", "older"),
                queryStrings(db, "SELECT attachmentId FROM workspace_connections ORDER BY connectionOrder"),
            )
            assertEquals(
                listOf(0L, 1L),
                queryLongs(db, "SELECT connectionOrder FROM workspace_connections ORDER BY connectionOrder"),
            )
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM workspace_connections WHERE syncedAt IS NOT NULL"))
            assertEquals(
                "{}",
                scalarString(db, "SELECT configuration FROM workspace_capability_instances WHERE id = 'connections-cap'"),
            )
            db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `158 to 159 fails closed when live placement targets deleted Attachment`() {
        val dbName = "migration_158_159_connections_deleted_target"
        createFixture(dbName) { db ->
            insertContext(db, "owner")
            insertWorkspace(db, "owner")
            insertAttachment(db, "deleted", createdAt = 10L, deleted = true)
            insertLink(db, "owner", "deleted", order = 0L)
        }

        val failure =
            runCatching {
                Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                    .addMigrations(MIGRATION_158_159)
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
                    object : SupportSQLiteOpenHelper.Callback(158) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchema(db, 158)
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
                put("id", id); put("workspaceId", workspaceId); put("capabilityType", "CONNECTIONS")
                put("instanceKey", "default"); put("capabilityOrder", 0L); put("state", "ACTIVE")
                put("configurationVersion", 1); put("configuration", "{}")
                put("createdAt", 10L); put("updatedAt", 10L); put("isDeleted", 0); put("version", 1L)
            },
        )
    }

    private fun insertAttachment(db: SupportSQLiteDatabase, id: String, createdAt: Long, deleted: Boolean = false) {
        db.insert(
            "attachments",
            0,
            ContentValues().apply {
                put("id", id); put("attachment_type", "LINK_ITEM"); put("entity_id", id)
                put("createdAt", createdAt); put("updatedAt", createdAt)
                put("isDeleted", if (deleted) 1 else 0); put("version", 1L); put("is_system", 0)
            },
        )
    }

    private fun insertLink(
        db: SupportSQLiteDatabase,
        contextId: String,
        attachmentId: String,
        order: Long,
        version: Long = 1L,
        syncedAt: Long? = null,
    ) {
        db.insert(
            "context_attachment_cross_ref",
            0,
            ContentValues().apply {
                put("context_id", contextId); put("attachment_id", attachmentId); put("attachment_order", order)
                put("updatedAt", 20L); put("version", version); put("isDeleted", 0)
                syncedAt?.let { put("syncedAt", it) }
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

    private fun queryStrings(db: SupportSQLiteDatabase, sql: String): List<String> =
        db.query(sql).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getString(0)) } }

    private fun queryLongs(db: SupportSQLiteDatabase, sql: String): List<Long> =
        db.query(sql).use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) } }
}
