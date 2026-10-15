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
class Migration181To182DayPlanningScopeExtensionRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `schema 181 opens as 182 with dormant empty extension storage`() {
        val dbName = "migration_181_182_day_extension"
        createFromExported181(dbName)
        seedExistingPlanningScope(dbName)

        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_181_182)
                .allowMainThreadQueries()
                .build()
        try {
            val db = database.openHelper.writableDatabase
            assertEquals(182L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "day_planning_scope_extensions"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM day_planning_scope_extensions"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM planning_scopes"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM day_plans WHERE id = 'legacy-day'"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM planning_scopes WHERE id = 'legacy-day'"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            database.close()
        }
    }

    private fun seedExistingPlanningScope(dbName: String) {
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(181) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build(),
        ).use { helper ->
            helper.writableDatabase.execSQL(
                """
                INSERT INTO planning_scopes
                (id, kind, lifecycle, title, startsAt, endsAt, createdAt, updatedAt, syncedAt, version, isDeleted)
                VALUES ('existing-scope', 'TACTICAL_CYCLE', 'ACTIVE', NULL, NULL, NULL, 1, 2, NULL, 1, 0)
                """.trimIndent(),
            )
            helper.writableDatabase.execSQL(
                """
                INSERT INTO day_plans
                (id, date, name, linkedProjectIds, linkedAttachmentIds, status, reflection,
                 energyLevel, mood, weatherConditions, predictedDurationMinutes,
                 totalPlannedMinutes, totalCompletedMinutes, completionPercentage,
                 createdAt, updatedAt, syncedAt, isDeleted, version)
                VALUES
                ('legacy-day', 1780000000000, 'Legacy day', NULL, NULL, 'COMPLETED', NULL,
                 NULL, NULL, NULL, 480, 300, 240, 80.0, 10, 20, NULL, 0, 7)
                """.trimIndent(),
            )
        }
    }

    private fun createFromExported181(dbName: String) {
        context.deleteDatabase(dbName)
        val schema =
            JsonParser.parseString(findSchema181().readText())
                .asJsonObject.getAsJsonObject("database")
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(181) {
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

    private fun findSchema181(): File {
        var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        val relative = "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/181.json"
        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 181")
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        scalarLong(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'") == 1L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); requireNotNull(cursor.getString(0)) }
}
