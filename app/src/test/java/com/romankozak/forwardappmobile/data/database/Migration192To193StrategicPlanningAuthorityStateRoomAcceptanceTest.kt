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
class Migration192To193StrategicPlanningAuthorityStateRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `schema 192 opens as 193 with inactive local Strategic marker and no synthesized planning`() {
        val dbName = "migration_192_193_strategic_authority"
        createFromExported192(dbName)
        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_192_193)
                .allowMainThreadQueries()
                .build()
        try {
            val db = database.openHelper.writableDatabase
            assertEquals(193L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "strategic_planning_authority_state"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM strategic_planning_authority_state"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM planning_scopes"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM planning_commitments"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM strategic_planning_scope_extensions"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            database.close()
        }
    }

    private fun createFromExported192(dbName: String) {
        context.deleteDatabase(dbName)
        val schema = JsonParser.parseString(findSchema192().readText()).asJsonObject.getAsJsonObject("database")
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(192) {
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

    private fun findSchema192(): File {
        var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        val relative = "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/192.json"
        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 192")
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        scalarLong(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'") == 1L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); requireNotNull(cursor.getString(0)) }
}
