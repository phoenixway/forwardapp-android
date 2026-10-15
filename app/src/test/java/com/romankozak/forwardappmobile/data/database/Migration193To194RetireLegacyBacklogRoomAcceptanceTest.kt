package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration193To194RetireLegacyBacklogRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `schema 193 upgrades through 194 to 195 with legacy backlog retired`() {
        val dbName = "migration_193_194_retire_legacy_backlog"
        createFromExported193(dbName)
        seedLegacyAndCanonicalBacklog(dbName)

        val database =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(195) {
                            override fun onConfigure(db: SupportSQLiteDatabase) {
                                db.setForeignKeyConstraintsEnabled(true)
                            }

                            override fun onCreate(db: SupportSQLiteDatabase) {
                                error("Historical schema 193 must already exist")
                            }

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) {
                                check(oldVersion == 193 && newVersion == 195)
                                MIGRATION_193_194.migrate(db)
                                MIGRATION_194_195.migrate(db)
                            }
                        },
                    )
                    .build(),
            )
        try {
            val db = database.writableDatabase
            assertEquals(195L, scalarLong(db, "PRAGMA user_version"))
            assertFalse(tableExists(db, "list_items"))
            assertFalse(tableExists(db, "backlog_orders"))
            assertFalse(tableExists(db, "main_beacon_context_cross_ref"))
            assertTrue(tableExists(db, "workspace_backlog_entries"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM workspace_backlog_entries WHERE id = 'canonical-entry'"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM strategic_planning_authority_state WHERE id = 1"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            database.close()
        }
    }

    private fun seedLegacyAndCanonicalBacklog(dbName: String) {
        openExisting(dbName, 193).use { helper ->
            val db = helper.writableDatabase
            db.execSQL(
                "INSERT INTO contexts (id, name, createdAt, scoring_status) VALUES ('legacy-owner', 'Legacy', 1, 'UNASSESSED')",
            )
            db.execSQL(
                "INSERT INTO list_items (id, context_id, itemType, entityId, item_order) " +
                    "VALUES ('legacy-item', 'legacy-owner', 'GOAL', 'goal-1', 4)",
            )
            db.execSQL(
                "INSERT INTO backlog_orders (id, list_id, item_id, item_order) " +
                    "VALUES ('legacy-order', 'legacy-owner', 'legacy-item', 4)",
            )
            db.execSQL(
                "INSERT INTO workspaces " +
                    "(id, createdAt, updatedAt, isDeleted, version, provenance) " +
                    "VALUES ('workspace', 1, 1, 0, 1, 'CANONICAL_ONLY')",
            )
            db.execSQL(
                "INSERT INTO workspace_capability_instances " +
                    "(id, workspaceId, capabilityType, instanceKey, capabilityOrder, state, " +
                    "configurationVersion, configuration, createdAt, updatedAt, isDeleted, version) " +
                    "VALUES ('capability', 'workspace', 'BACKLOG', 'default', 0, 'ACTIVE', 2, '{}', 1, 1, 0, 1)",
            )
            db.execSQL(
                "INSERT INTO workspace_backlog_entries " +
                    "(id, workspaceId, capabilityInstanceId, targetKind, targetId, entryOrder, " +
                    "createdAt, updatedAt, isDeleted, version) " +
                    "VALUES ('canonical-entry', 'workspace', 'capability', 'LINK_ITEM', 'link-1', 7, 1, 1, 0, 1)",
            )
            db.execSQL(
                "INSERT INTO strategic_planning_authority_state (id, version, activatedAt) VALUES (1, 1, 1)",
            )
        }
    }

    private fun createFromExported193(dbName: String) {
        context.deleteDatabase(dbName)
        val schema = JsonParser.parseString(findSchema193().readText()).asJsonObject.getAsJsonObject("database")
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(193) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        schema.getAsJsonArray("entities").forEach { element ->
                            val entity = element.asJsonObject
                            val table = entity.get("tableName").asString
                            db.execSQL(entity.get("createSql").asString.replace("\${TABLE_NAME}", table))
                            entity.getAsJsonArray("indices")?.forEach { index ->
                                db.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table))
                            }
                        }
                        schema.getAsJsonArray("views")?.forEach { db.execSQL(it.asJsonObject.get("createSql").asString) }
                        schema.getAsJsonArray("setupQueries")?.forEach { db.execSQL(it.asString) }
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        ).use { it.writableDatabase }
    }

    private fun openExisting(
        dbName: String,
        version: Int,
    ): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        )

    private fun findSchema193(): File {
        var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        val relative = "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/193.json"
        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 193")
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        scalarLong(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'") == 1L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); requireNotNull(cursor.getString(0)) }
}
