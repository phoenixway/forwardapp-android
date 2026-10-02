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
class Migration180To181PlanningPersistenceRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `schema 180 opens as schema 181 with empty canonical planning storage`() {
        val dbName = "migration_180_181_planning"
        createFromExported180(dbName)

        val database =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_180_181)
                .allowMainThreadQueries()
                .build()
        try {
            val db = database.openHelper.writableDatabase
            assertEquals(181L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "planning_scopes"))
            assertTrue(tableExists(db, "planning_commitments"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM planning_scopes"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM planning_commitments"))
            assertEquals(2L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_list('planning_commitments')"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))

            val indices = indexNames(db, "planning_commitments")
            assertTrue(
                "missing deterministic-order index: $indices",
                "index_planning_commitments_scopeId_role_isDeleted_commitmentOrder_id" in indices,
            )
            assertTrue(
                "missing live-participation lookup index: $indices",
                "index_planning_commitments_scopeId_orientationId_role_isDeleted" in indices,
            )
        } finally {
            database.close()
        }
    }

    private fun createFromExported180(dbName: String) {
        context.deleteDatabase(dbName)
        val schema =
            JsonParser.parseString(findSchema180().readText())
                .asJsonObject
                .getAsJsonObject("database")
        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(180) {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                schema.getAsJsonArray("entities").forEach { element ->
                                    val entity = element.asJsonObject
                                    val table = entity.get("tableName").asString
                                    db.execSQL(
                                        entity.get("createSql").asString
                                            .replace("\${TABLE_NAME}", table),
                                    )
                                    entity.getAsJsonArray("indices")?.forEach { index ->
                                        db.execSQL(
                                            index.asJsonObject.get("createSql").asString
                                                .replace("\${TABLE_NAME}", table),
                                        )
                                    }
                                }
                                schema.getAsJsonArray("views")?.forEach {
                                    db.execSQL(it.asJsonObject.get("createSql").asString)
                                }
                                schema.getAsJsonArray("setupQueries")?.forEach {
                                    db.execSQL(it.asString)
                                }
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

    private fun findSchema180(): File {
        val paths =
            listOf(
                "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/180.json",
                "schemas/com.romankozak.forwardappmobile.database.AppDatabase/180.json",
            )
        var current: File? = File(System.getProperty("user.dir")).absoluteFile
        while (current != null) {
            paths.forEach { relative ->
                File(current, relative).takeIf(File::isFile)?.let { return it }
            }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 180")
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '$table'",
        ) == 1L

    private fun indexNames(
        db: SupportSQLiteDatabase,
        table: String,
    ): Set<String> =
        db.query("PRAGMA index_list(`$table`)").use { cursor ->
            buildSet {
                val nameColumn = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) add(cursor.getString(nameColumn))
            }
        }

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long = db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String = db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getString(0) }
}
