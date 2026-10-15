package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration186To187CanonicalScoringAuthorityRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `schema 186 opens as 187 with additive dormant scoring authority storage`() {
        val name = "migration_186_187_canonical_scoring"
        createFromExported186(name)
        val helper =
            openHistoricalMigrationDatabase(
                context = context,
                dbName = name,
                MIGRATION_186_187,
            )
        try {
            val db = helper.writableDatabase
            assertEquals(187L, scalarLong(db, "PRAGMA user_version"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='orientation_scoring_overrides'"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='canonical_scoring_authority_state'"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM orientation_scoring_overrides"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM canonical_scoring_authority_state"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }

    private fun createFromExported186(name: String) {
        context.deleteDatabase(name)
        val schema = JsonParser.parseString(findSchema186().readText()).asJsonObject.getAsJsonObject("database")
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(186) {
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
                    },
                ).build(),
        ).use { it.writableDatabase }
    }

    private fun findSchema186(): File {
        var current: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        val relative = "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/186.json"
        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 186")
    }

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor -> check(cursor.moveToFirst()); cursor.getLong(0) }
}
