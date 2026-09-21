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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration177To178HierarchyAuthorityActivationRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `177 to 178 adds empty local activation marker without hierarchy mutation`() {
        val dbName = "migration_177_178_hierarchy_authority_activation"
        createFromExported177(dbName)

        val preMigration =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(177) {
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
                    'LINK', 7, 10, 11, 12, 0, 3
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO hierarchy_placement_group_scopes(
                    placementId, hierarchyId, groupSubjectId, createdAt,
                    updatedAt, syncedAt, isDeleted, version
                ) VALUES(
                    'placement-1', 'GENERAL', NULL, 13, 14, 15, 0, 4
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO hierarchy_placement_linked_appearances(
                    placementId, hierarchyId, createdAt, updatedAt,
                    syncedAt, isDeleted, version
                ) VALUES(
                    'placement-1', 'GENERAL', 16, 17, 18, 0, 5
                )
                """.trimIndent(),
            )
        }

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_177_178)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(178L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "hierarchy_authority_activation_state"))
            assertEquals(
                0L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM hierarchy_authority_activation_state",
                ),
            )

            assertEquals(
                "placement-1|GENERAL|WORKSPACE|workspace-1|LINK|7|10|11|12|0|3",
                scalarString(
                    db,
                    """
                    SELECT
                        id || '|' ||
                        hierarchyId || '|' ||
                        targetType || '|' ||
                        targetId || '|' ||
                        placementKind || '|' ||
                        siblingOrder || '|' ||
                        createdAt || '|' ||
                        updatedAt || '|' ||
                        syncedAt || '|' ||
                        isDeleted || '|' ||
                        version
                    FROM hierarchy_placements
                    WHERE id = 'placement-1'
                    """.trimIndent(),
                ),
            )

            assertEquals(
                "placement-1|GENERAL|NULL|13|14|15|0|4",
                scalarString(
                    db,
                    """
                    SELECT
                        placementId || '|' ||
                        hierarchyId || '|' ||
                        COALESCE(groupSubjectId, 'NULL') || '|' ||
                        createdAt || '|' ||
                        updatedAt || '|' ||
                        syncedAt || '|' ||
                        isDeleted || '|' ||
                        version
                    FROM hierarchy_placement_group_scopes
                    WHERE placementId = 'placement-1'
                    """.trimIndent(),
                ),
            )

            assertEquals(
                "placement-1|GENERAL|16|17|18|0|5",
                scalarString(
                    db,
                    """
                    SELECT
                        placementId || '|' ||
                        hierarchyId || '|' ||
                        createdAt || '|' ||
                        updatedAt || '|' ||
                        syncedAt || '|' ||
                        isDeleted || '|' ||
                        version
                    FROM hierarchy_placement_linked_appearances
                    WHERE placementId = 'placement-1'
                    """.trimIndent(),
                ),
            )

            db.query("PRAGMA foreign_key_check").use {
                assertEquals(0, it.count)
            }
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun createFromExported177(dbName: String) {
        context.deleteDatabase(dbName)
        val schema =
            JsonParser.parseString(findSchema177().readText())
                .asJsonObject
                .getAsJsonObject("database")

        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(177) {
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

    private fun findSchema177(): File {
        val rootRelative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/177.json"
        val moduleRelative =
            "schemas/com.romankozak.forwardappmobile.database.AppDatabase/177.json"

        var current: File? = File(System.getProperty("user.dir")).absoluteFile
        while (current != null) {
            File(current, rootRelative).takeIf(File::isFile)?.let { return it }
            File(current, moduleRelative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 177")
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
