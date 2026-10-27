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
class Migration201To202SemanticTagsRoomTest {
    @Test fun `201 to 202 preserves old data and creates empty independent semantic tag authority`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "semantic-tags-${UUID.randomUUID()}"
        var root = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/201.json").isFile) root = requireNotNull(root.parentFile)
        val schema = JsonParser.parseString(File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/201.json").readText())
            .asJsonObject.getAsJsonObject("database")
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(201) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    schema.getAsJsonArray("entities").forEach { value ->
                        val entity = value.asJsonObject
                        val table = entity["tableName"].asString
                        db.execSQL(entity["createSql"].asString.replace("\${TABLE_NAME}", table))
                        entity.getAsJsonArray("indices")?.forEach { db.execSQL(it.asJsonObject["createSql"].asString.replace("\${TABLE_NAME}", table)) }
                    }
                    schema.getAsJsonArray("views")?.forEach { db.execSQL(it.asJsonObject["createSql"].asString) }
                    schema.getAsJsonArray("setupQueries")?.forEach { db.execSQL(it.asString) }
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build()
        try {
            FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
                val sql = helper.writableDatabase
                sql.execSQL("INSERT INTO managed_subjects VALUES ('s','ORIENTATION','Meaning',NULL,1,2,NULL,0,1)")
                sql.execSQL("INSERT INTO workspaces(id,nameOverride,createdAt,updatedAt,isDeleted,version,provenance) VALUES ('w','Operational',1,2,0,1,'STANDALONE')")
                sql.execSQL("INSERT INTO workspace_tag_refs VALUES ('w','health',1,2,NULL,0,1)")
                sql.execSQL("INSERT INTO hierarchy_placements VALUES ('p','GENERAL','MANAGED_SUBJECT','s',NULL,'PRIMARY',0,1,2,NULL,0,1)")
                sql.execSQL("INSERT INTO planning_references VALUES ('r','DAY_TASK','task','ORIENTATION','s','RELATED',1,2,NULL,1,0)")
            }
            val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_201_202, MIGRATION_202_203)
                .allowMainThreadQueries().build()
            try {
                val sql = room.openHelper.writableDatabase // Room validates the complete KSP-exported 202 schema.
                fun scalar(query: String) = sql.query(query).use { it.moveToFirst(); it.getString(0) }
                assertThat(scalar("PRAGMA user_version")).isEqualTo("203")
                assertThat(scalar("SELECT COUNT(*) FROM managed_subject_tag_refs")).isEqualTo("0")
                assertThat(scalar("SELECT normalizedTag FROM workspace_tag_refs WHERE workspaceId='w'")).isEqualTo("health")
                assertThat(scalar("SELECT title FROM managed_subjects WHERE id='s'")).isEqualTo("Meaning")
                assertThat(scalar("SELECT id FROM hierarchy_placements WHERE targetId='s'")).isEqualTo("p")
                assertThat(scalar("SELECT id FROM planning_references")).isEqualTo("r")
                val exported = JsonParser.parseString(File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/202.json").readText()).asJsonObject["database"].asJsonObject
                val oldTables = schema["entities"].asJsonArray.map { it.asJsonObject["tableName"].asString }.toSet()
                val newTables = exported["entities"].asJsonArray.map { it.asJsonObject["tableName"].asString }.toSet()
                assertThat(newTables - oldTables).containsExactly("managed_subject_tag_refs")
                assertThat(oldTables - newTables).isEmpty()
                schema["entities"].asJsonArray.forEach { old ->
                    val table = old.asJsonObject["tableName"].asString
                    assertThat(exported["entities"].asJsonArray.single { it.asJsonObject["tableName"].asString == table }).isEqualTo(old)
                }
            } finally { room.close() }
        } finally { context.deleteDatabase(name) }
    }
}
