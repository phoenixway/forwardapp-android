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
class Migration200To201PlanningReferencesRoomTest {
    @Test fun `200 to 201 preserves historical canonical and ambiguous data without fabricating references`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "planning-references-${UUID.randomUUID()}"
        var root = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/200.json").isFile) root = requireNotNull(root.parentFile)
        val schema = JsonParser.parseString(File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/200.json").readText())
            .asJsonObject.getAsJsonObject("database")
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(200) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    schema.getAsJsonArray("entities").forEach { value ->
                        val entity = value.asJsonObject
                        val table = entity.get("tableName").asString
                        db.execSQL(entity.get("createSql").asString.replace("\${TABLE_NAME}", table))
                        entity.getAsJsonArray("indices")?.forEach { db.execSQL(it.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table)) }
                    }
                    schema.getAsJsonArray("views")?.forEach { db.execSQL(it.asJsonObject.get("createSql").asString) }
                    schema.getAsJsonArray("setupQueries")?.forEach { db.execSQL(it.asString) }
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        try {
            FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
                val sql = helper.writableDatabase
                sql.execSQL("INSERT INTO managed_subjects VALUES ('s','ORIENTATION','Meaning',NULL,1,2,NULL,0,1)")
                sql.execSQL("INSERT INTO hierarchy_placements VALUES ('p','GENERAL','MANAGED_SUBJECT','s',NULL,'PRIMARY',0,1,2,NULL,0,1)")
                sql.execSQL("INSERT INTO hierarchy_group_scope_applicability VALUES ('p','GENERAL','LEGACY_UNKNOWN',1,2,NULL,1)")
                sql.execSQL("INSERT INTO planning_scopes VALUES ('day','DAY','ACTIVE','Day',NULL,NULL,1,2,NULL,1,0)")
                sql.execSQL("INSERT INTO planning_commitments VALUES ('c','day','s','DAY.TASK',2,NULL,'PLANNED','MANUAL',NULL,NULL,1,2,NULL,1,0)")
            }
            val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_200_201, MIGRATION_201_202, MIGRATION_202_203)
                .allowMainThreadQueries().build()
            try {
                    val sql = room.openHelper.writableDatabase // Room validates the current schema, through the additive tag migration.
                    fun scalar(query: String) = sql.query(query).use { it.moveToFirst(); it.getString(0) }
                    assertThat(scalar("PRAGMA user_version")).isEqualTo("203")
                    assertThat(scalar("SELECT COUNT(*) FROM planning_references")).isEqualTo("0")
                    assertThat(scalar("SELECT orientationId FROM planning_commitments WHERE id='c'")).isEqualTo("s")
                    assertThat(scalar("SELECT targetId FROM hierarchy_placements WHERE id='p'")).isEqualTo("s")
                    assertThat(scalar("SELECT mode FROM hierarchy_group_scope_applicability WHERE placementId='p'")).isEqualTo("LEGACY_UNKNOWN")
                    assertThat(scalar("SELECT title FROM managed_subjects WHERE id='s'")).isEqualTo("Meaning")
            } finally { room.close() }
        } finally { context.deleteDatabase(name) }
    }
}
