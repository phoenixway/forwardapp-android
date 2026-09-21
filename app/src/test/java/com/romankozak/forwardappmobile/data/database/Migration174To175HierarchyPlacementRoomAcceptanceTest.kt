package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.models.entities.hierarchy.HierarchyPlacementEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration174To175HierarchyPlacementRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `174 to 175 opens through Room with additive hierarchy schema`() {
        val dbName = "migration_174_175_hierarchy"
        createFromExported174(dbName)

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_174_175)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(175L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "hierarchy_placements"))
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM hierarchy_placements"))
            assertEquals(0, db.query("PRAGMA foreign_key_check").use { it.count })
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))

            val createSql =
                scalarString(
                    db,
                    """
                    SELECT sql
                    FROM sqlite_master
                    WHERE type = 'table' AND name = 'hierarchy_placements'
                    """.trimIndent(),
                )
            assertTrue(createSql.contains("DEFERRABLE INITIALLY DEFERRED", ignoreCase = true))
            assertTrue(
                indexIsUnique(
                    db = db,
                    table = "hierarchy_placements",
                    indexName = "index_hierarchy_placements_id_hierarchyId",
                ),
            )
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `fresh hierarchy persistence accepts intended legal shapes`() =
        runBlocking {
            withFreshDatabase { room ->
                val dao = room.hierarchyPlacementDao()

                dao.upsert(row(id = "root", siblingOrder = 0L))
                dao.upsert(row(id = "child", parentPlacementId = "root", siblingOrder = 1L))

                // Physical FK accepts a tombstoned parent. Parent liveness is a domain invariant.
                dao.upsert(row(id = "deleted-parent", isDeleted = true))
                dao.upsert(
                    row(
                        id = "live-child-of-tombstone",
                        parentPlacementId = "deleted-parent",
                    ),
                )

                // H1.2 deliberately does not enforce live PRIMARY uniqueness in SQLite.
                dao.upsert(
                    row(
                        id = "primary-a",
                        targetId = "same-target",
                        placementKind = "PRIMARY",
                        siblingOrder = 2L,
                    ),
                )
                dao.upsert(
                    row(
                        id = "primary-b",
                        targetId = "same-target",
                        placementKind = "PRIMARY",
                        siblingOrder = 3L,
                    ),
                )

                // Duplicate same-target siblings are legal and order ties break by placement id.
                dao.upsert(
                    row(
                        id = "link-a",
                        targetId = "duplicate-target",
                        parentPlacementId = "root",
                        placementKind = "LINK",
                        siblingOrder = 5L,
                    ),
                )
                dao.upsert(
                    row(
                        id = "link-b",
                        targetId = "duplicate-target",
                        parentPlacementId = "root",
                        placementKind = "LINK",
                        siblingOrder = 5L,
                    ),
                )

                assertNotNull(dao.getById("root"))
                assertEquals(
                    listOf("child", "link-a", "link-b"),
                    dao.getLiveChildren("GENERAL", "root").map { it.id },
                )
                assertEquals(
                    listOf("primary-a", "primary-b"),
                    dao.getLiveAppearances(
                        hierarchyId = "GENERAL",
                        targetType = "WORKSPACE",
                        targetId = "same-target",
                    ).map { it.id },
                )

                val db = room.openHelper.writableDatabase
                assertTrue(tableExists(db, "hierarchy_placements"))
                assertEquals(0, db.query("PRAGMA foreign_key_check").use { it.count })
            }
        }

    @Test
    fun `composite self foreign key rejects missing and cross-hierarchy parents`() =
        runBlocking {
            // A deferred FK violation is reported when Room commits its @Upsert
            // transaction. Use a fresh database for each independent rejection
            // so a failed transaction cannot contaminate the next assertion.
            withFreshDatabase { room ->
                assertForeignKeyFailure {
                    room.hierarchyPlacementDao().upsert(
                        row(
                            id = "missing-parent-child",
                            parentPlacementId = "missing",
                        ),
                    )
                }
            }

            withFreshDatabase { room ->
                val dao = room.hierarchyPlacementDao()
                dao.upsert(row(id = "general-parent"))

                assertForeignKeyFailure {
                    dao.upsert(
                        row(
                            id = "other-child",
                            hierarchyId = "OTHER",
                            parentPlacementId = "general-parent",
                        ),
                    )
                }
            }
        }

    @Test
    fun `deferred self foreign key permits child before parent in one transaction`() =
        runBlocking {
            withFreshDatabase { room ->
                val dao = room.hierarchyPlacementDao()

                room.withTransaction {
                    dao.upsert(
                        row(
                            id = "child-first",
                            parentPlacementId = "parent-second",
                        ),
                    )
                    dao.upsert(row(id = "parent-second"))
                }

                assertNotNull(dao.getById("child-first"))
                assertNotNull(dao.getById("parent-second"))
                assertEquals(
                    0,
                    room.openHelper.writableDatabase
                        .query("PRAGMA foreign_key_check")
                        .use { it.count },
                )
            }
        }

    private suspend fun withFreshDatabase(block: suspend (AppDatabase) -> Unit) {
        val room =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            block(room)
        } finally {
            room.close()
        }
    }

    private suspend fun assertForeignKeyFailure(block: suspend () -> Unit) {
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

    private fun row(
        id: String,
        hierarchyId: String = "GENERAL",
        targetType: String = "WORKSPACE",
        targetId: String = id,
        parentPlacementId: String? = null,
        placementKind: String = "PRIMARY",
        siblingOrder: Long = 0L,
        isDeleted: Boolean = false,
    ) = HierarchyPlacementEntity(
        id = id,
        hierarchyId = hierarchyId,
        targetType = targetType,
        targetId = targetId,
        parentPlacementId = parentPlacementId,
        placementKind = placementKind,
        siblingOrder = siblingOrder,
        createdAt = 1L,
        updatedAt = 2L,
        syncedAt = null,
        isDeleted = isDeleted,
        version = 1L,
    )

    private fun createFromExported174(dbName: String) {
        context.deleteDatabase(dbName)
        val schema =
            JsonParser.parseString(findSchema174().readText())
                .asJsonObject
                .getAsJsonObject("database")

        val helper =
            FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context)
                    .name(dbName)
                    .callback(
                        object : SupportSQLiteOpenHelper.Callback(174) {
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

    private fun findSchema174(): File {
        val rootRelative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/174.json"
        val moduleRelative =
            "schemas/com.romankozak.forwardappmobile.database.AppDatabase/174.json"

        var current: File? = File(System.getProperty("user.dir")).absoluteFile
        while (current != null) {
            File(current, rootRelative).takeIf(File::isFile)?.let { return it }
            File(current, moduleRelative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }
        error("Unable to locate exported Room schema 174")
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        db.query(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(table),
        ).use { it.moveToFirst() }

    private fun indexIsUnique(
        db: SupportSQLiteDatabase,
        table: String,
        indexName: String,
    ): Boolean =
        db.query("PRAGMA index_list(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val uniqueIndex = cursor.getColumnIndexOrThrow("unique")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == indexName) {
                    return@use cursor.getInt(uniqueIndex) != 0
                }
            }
            false
        }

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
