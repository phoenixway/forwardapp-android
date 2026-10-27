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
class Migration204To205OrientationKindRoomTest {
    @Test fun `migration preserves existing subtype conversion and adds only nullable kind evidence columns`() {
        val context = ApplicationProvider.getApplicationContext<Context>(); val name = "kind-migration-${UUID.randomUUID()}"
        var root = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/204.json").isFile) root = requireNotNull(root.parentFile)
        val dir = File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase")
        val old = JsonParser.parseString(File(dir, "204.json").readText()).asJsonObject["database"].asJsonObject
        val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(204) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                old["entities"].asJsonArray.forEach { value ->
                    val entity = value.asJsonObject; val table = entity["tableName"].asString
                    db.execSQL(entity["createSql"].asString.replace("\${TABLE_NAME}", table))
                    entity.getAsJsonArray("indices")?.forEach { db.execSQL(it.asJsonObject["createSql"].asString.replace("\${TABLE_NAME}", table)) }
                }
                old["views"]?.asJsonArray?.forEach { db.execSQL(it.asJsonObject["createSql"].asString) }
                old["setupQueries"].asJsonArray.forEach { db.execSQL(it.asString) }
                db.execSQL("INSERT INTO managed_subject_conversions VALUES('id','subject',1,'ASPECT','ORIENTATION','PROJECT',2,3,NULL,5,0,10,10,1,11)")
            }
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }).build()
        try {
            FrameworkSQLiteOpenHelperFactory().create(config).use { it.writableDatabase }
            val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_204_205).allowMainThreadQueries().build()
            try {
                assertThat(room.openHelper.readableDatabase.version).isEqualTo(205)
                kotlinx.coroutines.runBlocking {
                    val row = room.managedSubjectConversionDao().getAll().single()
                    assertThat(row.formatVersion).isEqualTo(1); assertThat(row.previousAspectOrder).isEqualTo(5)
                    assertThat(row.syncedAt).isEqualTo(11); assertThat(row.fromOrientationKind).isNull()
                    assertThat(row.previousAssessmentRevisionIdsJson).isNull()
                }
                val current = JsonParser.parseString(File(dir, "205.json").readText()).asJsonObject["database"].asJsonObject
                assertThat(current["entities"].asJsonArray.size()).isEqualTo(old["entities"].asJsonArray.size())
                old["entities"].asJsonArray.filter { it.asJsonObject["tableName"].asString != "managed_subject_conversions" }.forEach { entity ->
                    assertThat(current["entities"].asJsonArray.single { it.asJsonObject["tableName"] == entity.asJsonObject["tableName"] }).isEqualTo(entity)
                }
            } finally { room.close() }
        } finally { context.deleteDatabase(name) }
    }
}
