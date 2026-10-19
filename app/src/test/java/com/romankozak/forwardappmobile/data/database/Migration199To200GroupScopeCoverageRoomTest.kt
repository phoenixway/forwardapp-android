package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import java.util.UUID
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration199To200GroupScopeCoverageRoomTest {
    @Test fun `199 migrates deterministically with NATIVE one HISTORICAL one UNKNOWN two and no Workspace coverage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "coverage-${UUID.randomUUID()}"
        var directory = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(directory, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/199.json").isFile) {
            directory = requireNotNull(directory.parentFile)
        }
        val schema = JsonParser.parseString(File(directory, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/199.json").readText())
            .asJsonObject.getAsJsonObject("database")
        val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(199) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    schema.getAsJsonArray("entities").forEach { item ->
                        val entity = item.asJsonObject
                        val table = entity.get("tableName").asString
                        db.execSQL(entity.get("createSql").asString.replace("\${TABLE_NAME}", table))
                        entity.getAsJsonArray("indices")?.forEach {
                            db.execSQL(it.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table))
                        }
                    }
                    schema.getAsJsonArray("views")?.forEach { db.execSQL(it.asJsonObject.get("createSql").asString) }
                    schema.getAsJsonArray("setupQueries")?.forEach { db.execSQL(it.asString) }
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        try {
            FrameworkSQLiteOpenHelperFactory().create(config).use { helper ->
                val sql = helper.writableDatabase
                listOf("h" to "ORIENTATION", "a" to "ASPECT", "u" to "ORIENTATION", "b" to "ASPECT").forEach { (id, type) ->
                    sql.execSQL("INSERT INTO managed_subjects VALUES ('$id','$type','$id',NULL,1,2,NULL,0,1)")
                    if (type == "ASPECT") sql.execSQL("INSERT INTO aspects VALUES ('$id',NULL,0,0)")
                    sql.execSQL("INSERT INTO hierarchy_placements VALUES ('$id','GENERAL','MANAGED_SUBJECT','$id',NULL,'PRIMARY',0,1,2,NULL,0,1)")
                }
                sql.execSQL("INSERT INTO hierarchy_placements VALUES ('w','GENERAL','WORKSPACE','w',NULL,'PRIMARY',0,1,2,NULL,0,1)")
                sql.execSQL("INSERT INTO hierarchy_placement_group_scopes VALUES ('h','GENERAL',NULL,1,2,NULL,0,1)")
                sql.execSQL("INSERT INTO legacy_subject_mappings VALUES ('mapping','MAIN_BEACON','legacy','b',1,'CUT_OVER',1,2,NULL,0,1)")
            }
            val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_199_200, MIGRATION_200_201).allowMainThreadQueries().build()
            try {
                val sql = db.openHelper.writableDatabase
                fun scalar(query: String) = sql.query(query).use { it.moveToFirst(); it.getString(0) }
                assertThat(scalar("PRAGMA user_version")).isEqualTo("201")
                assertThat(scalar("SELECT COUNT(*) FROM hierarchy_group_scope_applicability WHERE mode='NATIVE'")).isEqualTo("1")
                assertThat(scalar("SELECT COUNT(*) FROM hierarchy_group_scope_applicability WHERE mode='HISTORICAL'")).isEqualTo("1")
                assertThat(scalar("SELECT COUNT(*) FROM hierarchy_group_scope_applicability WHERE mode='LEGACY_UNKNOWN'")).isEqualTo("2")
                assertThat(scalar("SELECT mode FROM hierarchy_group_scope_applicability WHERE placementId='a'")).isEqualTo("NATIVE")
                assertThat(scalar("SELECT COUNT(*) FROM hierarchy_placements")).isEqualTo("5")
                assertThat(scalar("SELECT COUNT(*) FROM hierarchy_placement_group_scopes")).isEqualTo("1")
                assertThat(scalar("SELECT COUNT(*) FROM pragma_foreign_key_check")).isEqualTo("0")
                assertThat(scalar("PRAGMA integrity_check")).isEqualTo("ok")
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
