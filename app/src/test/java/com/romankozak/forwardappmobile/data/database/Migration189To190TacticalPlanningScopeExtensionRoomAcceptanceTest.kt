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
class Migration189To190TacticalPlanningScopeExtensionRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `schema 189 opens as 190 with dormant empty Tactical extension storage`() {
        val dbName = "migration_189_190_tactical_extension"
        createFromExported189(dbName)
        seedLegacyAndCanonicalState(dbName)

        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_189_190)
                .allowMainThreadQueries()
                .build()
        try {
            val db = database.openHelper.writableDatabase
            assertEquals(190L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "tactical_planning_scope_extensions"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM tactical_planning_scope_extensions"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM tactical_iterations"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM planning_scopes"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM planning_scopes WHERE id = 'legacy-cycle'"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM planning_commitments"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_list('tactical_planning_scope_extensions')"))
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
                .callback(object : SupportSQLiteOpenHelper.Callback(189) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        ).use { helper ->
            helper.writableDatabase.execSQL(
                """
                INSERT INTO planning_scopes
                (id, kind, lifecycle, title, startsAt, endsAt, createdAt, updatedAt, syncedAt, version, isDeleted)
                VALUES ('existing-scope', 'TACTICAL_CYCLE', 'ACTIVE', 'Existing', 1, 2, 1, 2, NULL, 1, 0)
                """.trimIndent(),
            )
            helper.writableDatabase.execSQL(
                """
                INSERT INTO tactical_iterations
                (id, title, started_at, planned_end_at, closed_at, status, iteration_type, week_key,
                 created_at, updated_at, synced_at, is_deleted, version)
                VALUES ('legacy-cycle', 'Legacy cycle', 1, 2, NULL, 'ACTIVE', 'TIMEBOXED', '2026-W41',
                        1, 2, NULL, 0, 1)
                """.trimIndent(),
            )
        }
    }

    private fun createFromExported189(dbName: String) {
        context.deleteDatabase(dbName)
        val schema =
            JsonParser.parseString(findSchema189().readText())
                .asJsonObject.getAsJsonObject("database")
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(189) {
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

    private fun findSchema189(): File {
        var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        val relative = "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/189.json"
        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 189")
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        scalarLong(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'") == 1L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); requireNotNull(cursor.getString(0)) }
}
