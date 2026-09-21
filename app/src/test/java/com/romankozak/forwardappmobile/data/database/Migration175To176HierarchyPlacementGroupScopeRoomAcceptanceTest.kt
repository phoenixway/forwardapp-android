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
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration175To176HierarchyPlacementGroupScopeRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `175 to 176 adds empty occurrence group scope stream without inference`() {
        val dbName = "migration_175_176_hierarchy_group_scope"
        createFromExported175(dbName)

        val preMigration =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(175) {
                            override fun onCreate(db: SupportSQLiteDatabase) = Unit

                            override fun onUpgrade(
                                db: SupportSQLiteDatabase,
                                oldVersion: Int,
                                newVersion: Int,
                            ) = Unit
                        },
                    ).build(),
            )
        preMigration.use { helper ->
            val db = helper.writableDatabase
            db.execSQL(
                """
                INSERT INTO hierarchy_placements(
                    id, hierarchyId, targetType, targetId, parentPlacementId,
                    placementKind, siblingOrder, createdAt, updatedAt, syncedAt,
                    isDeleted, version
                ) VALUES(
                    'placement-1', 'GENERAL', 'WORKSPACE', 'workspace-1', NULL,
                    'PRIMARY', 0, 1, 1, NULL, 0, 1
                )
                """.trimIndent(),
            )
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_175_176)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(176L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "hierarchy_placement_group_scopes"))
            assertEquals(
                0L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM hierarchy_placement_group_scopes",
                ),
            )

            db.execSQL(
                """
                INSERT INTO hierarchy_placement_group_scopes(
                    placementId, hierarchyId, groupSubjectId, createdAt,
                    updatedAt, syncedAt, isDeleted, version
                ) VALUES(
                    'placement-1', 'GENERAL', NULL, 10, 10, NULL, 0, 1
                )
                """.trimIndent(),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM hierarchy_placement_group_scopes",
                ),
            )

            assertForeignKeyFailure {
                db.execSQL(
                    """
                    INSERT INTO hierarchy_placement_group_scopes(
                        placementId, hierarchyId, groupSubjectId, createdAt,
                        updatedAt, syncedAt, isDeleted, version
                    ) VALUES(
                        'missing-placement', 'GENERAL', NULL, 10, 10, NULL, 0, 1
                    )
                    """.trimIndent(),
                )
            }

            db.query("PRAGMA foreign_key_check").use {
                assertEquals(0, it.count)
            }
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun createFromExported175(dbName: String) {
        context.deleteDatabase(dbName)
        val schema =
            JsonParser.parseString(findSchema175().readText())
                .asJsonObject
                .getAsJsonObject("database")

        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(175) {
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

    private fun findSchema175(): File {
        val rootRelative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/175.json"
        val moduleRelative =
            "schemas/com.romankozak.forwardappmobile.database.AppDatabase/175.json"

        var current: File? = File(System.getProperty("user.dir")).absoluteFile
        while (current != null) {
            File(current, rootRelative).takeIf(File::isFile)?.let { return it }
            File(current, moduleRelative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 175")
    }

    private fun assertForeignKeyFailure(block: () -> Unit) {
        val failure =
            try {
                block()
                null
            } catch (caught: Throwable) {
                caught
            }

        if (failure == null) {
            fail("Expected foreign-key constraint failure")
        }

        assertTrue(
            "Expected SQLite foreign-key failure, got $failure",
            generateSequence(failure) { it.cause }
                .any { it.message?.contains("FOREIGN KEY", ignoreCase = true) == true },
        )
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        db.query(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(table),
        ).use { it.moveToFirst() }

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use {
            check(it.moveToFirst())
            it.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use {
            check(it.moveToFirst())
            it.getString(0)
        }
}
