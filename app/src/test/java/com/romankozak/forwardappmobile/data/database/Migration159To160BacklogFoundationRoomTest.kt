package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration159To160BacklogFoundationRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `159 through 161 keeps canonical Backlog foundation empty and preserves legacy authority`() {
        val dbName = "migration_159_160_backlog_foundation"
        createFixture(dbName)

        val helper = openHistorical161(dbName)
        try {
            val db = helper.writableDatabase
            assertEquals(161L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "workspace_backlog_entries"))
            assertTrue(tableExists(db, "list_items"))
            assertTrue(tableExists(db, "backlog_orders"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM workspace_backlog_entries"))
            db.query("PRAGMA foreign_key_check").use { assertEquals(0, it.count) }
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            helper.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun openHistorical161(dbName: String): SupportSQLiteOpenHelper {
        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(161) {
                        override fun onCreate(db: SupportSQLiteDatabase) =
                            error("Historical fixture must already exist at schema 159")

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {
                            check(oldVersion == 159 && newVersion == 161) {
                                "Unexpected historical Backlog migration $oldVersion->$newVersion"
                            }
                            MIGRATION_159_160.migrate(db)
                            MIGRATION_160_161.migrate(db)
                        }
                    },
                ).build()

        return FrameworkSQLiteOpenHelperFactory().create(configuration)
    }

    private fun createFixture(dbName: String) {
        context.deleteDatabase(dbName)
        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(159) {
                        override fun onCreate(db: SupportSQLiteDatabase) = createSchema(db, 159)

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).use { it.writableDatabase }
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
        database.getAsJsonArray("views")?.forEach { view ->
            db.execSQL(view.asJsonObject.get("createSql").asString)
        }
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        scalarLong(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'") == 1L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getString(0) }
}
