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
class Migration203To204SubjectConversionRoomTest {
    @Test fun `203 to 204 adds only empty history and preserves every existing entity schema and subject`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "conversion-migration-${UUID.randomUUID()}"
        var root = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (!File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/203.json").isFile) root = requireNotNull(root.parentFile)
        val schemaDir = File(root, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase")
        val old = JsonParser.parseString(File(schemaDir, "203.json").readText()).asJsonObject["database"].asJsonObject
        val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(object : SupportSQLiteOpenHelper.Callback(203) {
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
            FrameworkSQLiteOpenHelperFactory().create(config).use { helper ->
                helper.writableDatabase.execSQL("INSERT INTO managed_subjects(id,subjectType,title,createdAt,updatedAt,isDeleted,version) VALUES('aspect','ASPECT','Unchanged',1,2,0,7)")
                helper.writableDatabase.execSQL("INSERT INTO aspects(subjectId,parentAspectId,aspectOrder,archived) VALUES('aspect',NULL,3,0)")
            }
            val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_203_204, MIGRATION_204_205, MIGRATION_205_206).allowMainThreadQueries().build()
            try {
                assertThat(room.openHelper.readableDatabase.version).isEqualTo(206)
                kotlinx.coroutines.runBlocking {
                    assertThat(room.managedSubjectConversionDao().getAll()).isEmpty()
                    assertThat(room.orientationDao().getManagedSubject("aspect")!!.version).isEqualTo(7)
                    assertThat(room.orientationDao().getManagedSubject("aspect")!!.subjectType).isEqualTo("ASPECT")
                    assertThat(room.orientationDao().getAspect("aspect")!!.aspectOrder).isEqualTo(3)
                }
                val current = JsonParser.parseString(File(schemaDir, "204.json").readText()).asJsonObject["database"].asJsonObject
                assertThat(current["entities"].asJsonArray.size()).isEqualTo(old["entities"].asJsonArray.size() + 1)
                old["entities"].asJsonArray.forEach { entity ->
                    assertThat(current["entities"].asJsonArray.single { it.asJsonObject["tableName"] == entity.asJsonObject["tableName"] }).isEqualTo(entity)
                }
            } finally { room.close() }
        } finally { context.deleteDatabase(name) }
    }
}
