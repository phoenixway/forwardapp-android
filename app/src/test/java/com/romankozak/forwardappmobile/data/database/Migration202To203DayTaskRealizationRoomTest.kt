package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.*
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
class Migration202To203DayTaskRealizationRoomTest {
    @Test fun `adds empty priority references and preserves all prior task text timing and unrelated schema`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "day-realizes-${UUID.randomUUID()}"
        var root = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/202.json").isFile) root = requireNotNull(root.parentFile)
        val schemaDir = File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase")
        val old = JsonParser.parseString(File(schemaDir, "202.json").readText()).asJsonObject["database"].asJsonObject
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(202) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                old["entities"].asJsonArray.forEach { value ->
                    val entity = value.asJsonObject; val table = entity["tableName"].asString
                    db.execSQL(entity["createSql"].asString.replace("\${TABLE_NAME}", table))
                    entity.getAsJsonArray("indices")?.forEach { db.execSQL(it.asJsonObject["createSql"].asString.replace("\${TABLE_NAME}", table)) }
                }
                old["views"]?.asJsonArray?.forEach { db.execSQL(it.asJsonObject["createSql"].asString) }
                old["setupQueries"].asJsonArray.forEach { db.execSQL(it.asString) }
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).build()
        try {
            FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
                helper.writableDatabase.execSQL("INSERT INTO day_plans(id,date,status,totalPlannedMinutes,totalCompletedMinutes,completionPercentage,createdAt,isDeleted,version) VALUES('d',1,'PLANNED',0,0,0,1,0,1)")
                helper.writableDatabase.execSQL("INSERT INTO day_tasks(id,dayPlanId,title,description,notes,`order`,priority,status,completed,scheduledTime,estimatedDurationMinutes,dueTime,createdAt,isDeleted,version) VALUES('t','d','Task','Instruction','Journal',2,'HIGH','NOT_STARTED',0,100,30,300,1,0,4)")
            }
            val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_202_203, MIGRATION_203_204, MIGRATION_204_205, MIGRATION_205_206).allowMainThreadQueries().build()
            try {
                val db = room.openHelper.writableDatabase // Full current Room schema validation.
                fun scalar(sql: String) = db.query(sql).use { it.moveToFirst(); it.getString(0) }
                assertThat(scalar("PRAGMA user_version")).isEqualTo("206")
                assertThat(scalar("SELECT realizedDayFocusItemIds FROM day_tasks WHERE id='t'")).isEqualTo("[]")
                assertThat(scalar("SELECT description || ':' || notes || ':' || version FROM day_tasks WHERE id='t'")).isEqualTo("Instruction:Journal:4")
                assertThat(scalar("SELECT scheduledTime || ':' || estimatedDurationMinutes || ':' || dueTime FROM day_tasks WHERE id='t'")).isEqualTo("100:30:300")
                val current = JsonParser.parseString(File(schemaDir, "203.json").readText()).asJsonObject["database"].asJsonObject
                assertThat(current["entities"].asJsonArray.size()).isEqualTo(old["entities"].asJsonArray.size())
                old["entities"].asJsonArray.filter { it.asJsonObject["tableName"].asString != "day_tasks" }.forEach { entity ->
                    assertThat(current["entities"].asJsonArray.single { it.asJsonObject["tableName"] == entity.asJsonObject["tableName"] }).isEqualTo(entity)
                }
            } finally { room.close() }
        } finally { context.deleteDatabase(name) }
    }
}
