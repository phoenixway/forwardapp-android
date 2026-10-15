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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration191To192StrategicPlanningScopeExtensionRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `schema 191 opens as 192 with dormant empty Strategic extension storage`() {
        val dbName = "migration_191_192_strategic_extension"
        createFromExported191(dbName)
        seedLegacyAndCanonicalState(dbName)

        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_191_192)
                .allowMainThreadQueries()
                .build()
        try {
            val db = database.openHelper.writableDatabase
            assertEquals(192L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "strategic_planning_scope_extensions"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM strategic_planning_scope_extensions"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM arc_quests"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM planning_scopes"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM planning_scopes WHERE kind = 'STRATEGIC_ARC'"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM planning_commitments"))
            assertEquals(
                1L,
                scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_list('strategic_planning_scope_extensions')"),
            )
            assertEquals(2L, scalarLong(db, "SELECT COUNT(*) FROM pragma_index_list('strategic_planning_scope_extensions') WHERE origin = 'c'"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            database.close()
        }
    }

    private fun seedLegacyAndCanonicalState(dbName: String) {
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(191) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        ).use { helper ->
            helper.writableDatabase.execSQL(
                """
                INSERT INTO planning_scopes
                (id, kind, lifecycle, title, startsAt, endsAt, createdAt, updatedAt, syncedAt, version, isDeleted)
                VALUES ('existing-day', 'DAY', 'ACTIVE', NULL, NULL, NULL, 1, 2, NULL, 1, 0)
                """.trimIndent(),
            )
            helper.writableDatabase.execSQL(
                """
                INSERT INTO arc_quests
                (id, arc_key, title, description, linked_context_id, linked_mission_id, source_type, source_id,
                 status, quest_order, createdAt, updatedAt, synced_at, is_deleted, version)
                VALUES ('legacy-quest', '2026-10', 'Legacy quest', NULL, NULL, NULL, 'MANUAL', NULL,
                        'ACTIVE', 0, 1, 2, NULL, 0, 1)
                """.trimIndent(),
            )
        }
    }

    private fun createFromExported191(dbName: String) {
        context.deleteDatabase(dbName)
        val schema = JsonParser.parseString(findSchema191().readText()).asJsonObject.getAsJsonObject("database")
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(191) {
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

    private fun findSchema191(): File {
        var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        val relative = "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/191.json"
        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 191")
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        scalarLong(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'") == 1L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); requireNotNull(cursor.getString(0)) }
}
