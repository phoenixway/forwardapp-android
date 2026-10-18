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
class Migration198To199BigPicturePresentationRoomTest {
    private inline fun <T> AppDatabase.use(block: (AppDatabase) -> T): T =
        try { block(this) } finally { close() }

    @Test fun `exported 198 migrates to registered 199 preserving H1 and GroupScope with empty presentation tables`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "r2f2a-${UUID.randomUUID()}"
        val schema = JsonParser.parseString(schema198().readText()).asJsonObject.getAsJsonObject("database")
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(198) {
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
            FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
                helper.writableDatabase.execSQL("""
                    INSERT INTO hierarchy_placements
                    (id,hierarchyId,targetType,targetId,parentPlacementId,placementKind,siblingOrder,
                     createdAt,updatedAt,syncedAt,isDeleted,version)
                    VALUES ('historical','GENERAL','MANAGED_SUBJECT','subject',NULL,'PRIMARY',7,1,2,3,0,4)
                """.trimIndent())
                helper.writableDatabase.execSQL("""
                    INSERT INTO hierarchy_placement_group_scopes
                    (placementId,hierarchyId,groupSubjectId,createdAt,updatedAt,syncedAt,isDeleted,version)
                    VALUES ('historical','GENERAL',NULL,1,2,3,0,4)
                """.trimIndent())
            }
            assertThat(ALL_MIGRATIONS.toList()).contains(MIGRATION_198_199)
            Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(MIGRATION_198_199, MIGRATION_199_200)
                .allowMainThreadQueries().build().use { room ->
                    val sql = room.openHelper.writableDatabase // Room validates the entire exported schema.
                    fun scalar(query: String): String = sql.query(query).use { it.moveToFirst(); it.getString(0) }
                    assertThat(scalar("PRAGMA user_version")).isEqualTo("200")
                    assertThat(scalar("SELECT siblingOrder || ':' || version || ':' || syncedAt FROM hierarchy_placements WHERE id='historical'"))
                        .isEqualTo("7:4:3")
                    assertThat(scalar("SELECT version FROM hierarchy_placement_group_scopes WHERE placementId='historical'"))
                        .isEqualTo("4")
                    assertThat(scalar("SELECT COUNT(*) FROM big_picture_containers")).isEqualTo("0")
                    assertThat(scalar("SELECT COUNT(*) FROM big_picture_memberships")).isEqualTo("0")
                    assertThat(scalar("SELECT COUNT(*) FROM pragma_foreign_key_check")).isEqualTo("0")
                    assertThat(scalar("PRAGMA integrity_check")).isEqualTo("ok")
                }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun schema198(): File {
        var directory: File? = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
        while (directory != null) {
            val candidate = File(directory, "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/198.json")
            if (candidate.isFile) return candidate
            directory = directory.parentFile
        }
        error("Exported schema 198 missing")
    }
}
