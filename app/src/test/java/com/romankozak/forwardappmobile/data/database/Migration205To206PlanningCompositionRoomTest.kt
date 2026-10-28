package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.*
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration205To206PlanningCompositionRoomTest {
    @Test fun `205 data preserved and new indexed relation table starts empty`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "composition-migration-${UUID.randomUUID()}"
        var root = File(requireNotNull(System.getProperty("user.dir")))
        while (!File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/205.json").isFile) root = requireNotNull(root.parentFile)
        val dir = File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase")
        val previous = JsonParser.parseString(File(dir, "205.json").readText()).asJsonObject["database"].asJsonObject
        val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(205) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                previous["entities"].asJsonArray.forEach {
                    val entity = it.asJsonObject; val table = entity["tableName"].asString
                    db.execSQL(entity["createSql"].asString.replace("\${TABLE_NAME}", table))
                    entity.getAsJsonArray("indices")?.forEach { index -> db.execSQL(index.asJsonObject["createSql"].asString.replace("\${TABLE_NAME}", table)) }
                }
                previous["views"]?.asJsonArray?.forEach { db.execSQL(it.asJsonObject["createSql"].asString) }
                previous["setupQueries"].asJsonArray.forEach { db.execSQL(it.asString) }
                db.execSQL("INSERT INTO managed_subjects VALUES('subject','ORIENTATION','Preserved',NULL,1,1,NULL,0,1)")
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).build()
        try {
            FrameworkSQLiteOpenHelperFactory().create(config).use { it.writableDatabase }
            val room = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().addMigrations(MIGRATION_205_206).build()
            try {
                assertEquals(206, room.openHelper.readableDatabase.version)
                runBlocking {
                    assertEquals("Preserved", room.orientationDao().getManagedSubject("subject")!!.title)
                    assertTrue(room.planningCommitmentRelationDao().getAll().isEmpty())
                }
                val current = JsonParser.parseString(File(dir, "206.json").readText()).asJsonObject["database"].asJsonObject
                assertEquals(previous["entities"].asJsonArray.size() + 1, current["entities"].asJsonArray.size())
                previous["entities"].asJsonArray.forEach { entity ->
                    assertEquals(entity, current["entities"].asJsonArray.single { it.asJsonObject["tableName"] == entity.asJsonObject["tableName"] })
                }
            } finally { room.close() }
        } finally { context.deleteDatabase(name) }
    }
}
