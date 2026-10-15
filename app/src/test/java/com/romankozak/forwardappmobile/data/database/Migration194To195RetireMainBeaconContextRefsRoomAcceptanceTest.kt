package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration194To195RetireMainBeaconContextRefsRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `legacy only valid owners converge without losing canonical associations`() {
        val name = "main_beacon_194_195_valid"
        seed194(name) { db ->
            beacon(db)

            historicalContext(db, "ordinary")
            historicalContext(db, "sys_today")
            historicalContext(db, "retired", deleted = true)

            workspace(db, "ordinary", "CONTEXT_BACKED", "ordinary")
            workspace(db, "sys_today", "CANONICAL_ONLY")
            workspace(db, "retired", "CANONICAL_ONLY")
            workspace(db, "independent", "STANDALONE")

            legacyRef(db, "ordinary", 2L)
            legacyRef(db, "sys_today", 3L)
            legacyRef(db, "retired", 4L)
            canonicalRef(db, "independent", 5L)
        }

        withMigratedDatabase(name) { room ->
            val db = room.writableDatabase

            assertEquals(195L, scalar(db, "PRAGMA user_version"))
            assertFalse(tableExists(db, "main_beacon_context_cross_ref"))

            assertEquals(
                4L,
                scalar(db, "SELECT COUNT(*) FROM main_beacon_workspace_cross_ref"),
            )

            listOf(
                "ordinary" to 2L,
                "sys_today" to 3L,
                "retired" to 4L,
                "independent" to 5L,
            ).forEach { (ownerId, order) ->
                assertEquals(
                    1L,
                    scalar(
                        db,
                        "SELECT COUNT(*) FROM main_beacon_workspace_cross_ref " +
                            "WHERE workspace_id = '$ownerId' AND ref_order = $order",
                    ),
                )
            }

            assertEquals(0L, scalar(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
            assertEquals(4, formalContextFkCount(db))
        }
    }

    @Test
    fun `equivalent dual physical refs converge to one canonical row`() {
        val name = "main_beacon_194_195_equivalent"
        seed194(name) { db ->
            beacon(db)
            historicalContext(db, "ordinary")
            workspace(db, "ordinary", "CONTEXT_BACKED", "ordinary")
            legacyRef(db, "ordinary", 7L)
            canonicalRef(db, "ordinary", 7L)
        }

        withMigratedDatabase(name) { room ->
            val db = room.writableDatabase

            assertFalse(tableExists(db, "main_beacon_context_cross_ref"))
            assertEquals(
                1L,
                scalar(db, "SELECT COUNT(*) FROM main_beacon_workspace_cross_ref"),
            )
            assertEquals(
                7L,
                scalar(db, "SELECT ref_order FROM main_beacon_workspace_cross_ref"),
            )
        }
    }

    @Test
    fun `divergent orders reject upgrade without partial convergence`() {
        val name = "main_beacon_194_195_divergent"
        seed194(name) { db ->
            beacon(db)

            historicalContext(db, "a-valid")
            historicalContext(db, "z-conflict")
            workspace(db, "a-valid", "CONTEXT_BACKED", "a-valid")
            workspace(db, "z-conflict", "CONTEXT_BACKED", "z-conflict")

            legacyRef(db, "a-valid", 2L)
            legacyRef(db, "z-conflict", 3L)
            canonicalRef(db, "z-conflict", 4L)
        }

        assertRejectedAndUnchanged(
            name = name,
            legacyCount = 2L,
            canonicalCount = 1L,
        )
    }

    @Test
    fun `missing Workspace rejects entire upgrade`() {
        val name = "main_beacon_194_195_missing"
        seed194(name) { db ->
            beacon(db)
            historicalContext(db, "a-valid")
            historicalContext(db, "z-missing")
            workspace(db, "a-valid", "CONTEXT_BACKED", "a-valid")
            legacyRef(db, "a-valid", 2L)
            legacyRef(db, "z-missing", 3L)
        }

        assertRejectedAndUnchanged(name, legacyCount = 2L)
    }

    @Test
    fun `deleted Workspace rejects entire upgrade`() {
        val name = "main_beacon_194_195_deleted"
        seed194(name) { db ->
            beacon(db)
            historicalContext(db, "ordinary")
            workspace(
                db,
                "ordinary",
                "CONTEXT_BACKED",
                "ordinary",
                deleted = true,
            )
            legacyRef(db, "ordinary", 3L)
        }

        assertRejectedAndUnchanged(name, legacyCount = 1L)
    }

    @Test
    fun `unproven live Context lineage rejects entire upgrade`() {
        val name = "main_beacon_194_195_bad_lineage"
        seed194(name) { db ->
            beacon(db)
            historicalContext(db, "ordinary")
            workspace(db, "ordinary", "CONTEXT_BACKED", "wrong-source")
            legacyRef(db, "ordinary", 3L)
        }

        assertRejectedAndUnchanged(name, legacyCount = 1L)
    }

    @Test
    fun `deleted ordinary Context cannot retain Context backed owner`() {
        val name = "main_beacon_194_195_retired_bad"
        seed194(name) { db ->
            beacon(db)
            historicalContext(db, "retired", deleted = true)
            workspace(db, "retired", "CONTEXT_BACKED", "retired")
            legacyRef(db, "retired", 3L)
        }

        assertRejectedAndUnchanged(name, legacyCount = 1L)
    }

    private fun assertRejectedAndUnchanged(
        name: String,
        legacyCount: Long,
        canonicalCount: Long = 0L,
    ) {
        val room = migrate(name)
        val failure =
            try {
                runCatching { room.writableDatabase }.exceptionOrNull()
            } finally {
                room.close()
            }

        assertNotNull("Expected fail-closed Room migration: $name", failure)

        openExisting(name, 194).use { helper ->
            val db = helper.writableDatabase

            assertEquals(194L, scalar(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "main_beacon_context_cross_ref"))
            assertEquals(
                legacyCount,
                scalar(db, "SELECT COUNT(*) FROM main_beacon_context_cross_ref"),
            )
            assertEquals(
                canonicalCount,
                scalar(db, "SELECT COUNT(*) FROM main_beacon_workspace_cross_ref"),
            )
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        }
    }

    private fun seed194(
        name: String,
        seed: (SupportSQLiteDatabase) -> Unit,
    ) {
        createFromExported194(name)
        openExisting(name, 194).use { helper ->
            seed(helper.writableDatabase)
        }
    }

    private fun beacon(
        db: SupportSQLiteDatabase,
        id: String = "beacon",
    ) {
        db.execSQL(
            "INSERT INTO main_beacons " +
                "(id, title, readiness_status, beacon_order, is_expanded, updatedAt, createdAt) " +
                "VALUES (?, ?, 'NOT_ASSESSED', 0, 0, 1, 1)",
            arrayOf(id, "Beacon"),
        )
    }

    private fun historicalContext(
        db: SupportSQLiteDatabase,
        id: String,
        deleted: Boolean = false,
    ) {
        db.execSQL(
            "INSERT INTO contexts " +
                "(id, name, createdAt, scoring_status, is_deleted) " +
                "VALUES (?, ?, 1, 'UNASSESSED', ?)",
            arrayOf<Any>(id, id, if (deleted) 1 else 0),
        )
    }

    private fun workspace(
        db: SupportSQLiteDatabase,
        id: String,
        provenance: String,
        source: String? = null,
        deleted: Boolean = false,
    ) {
        db.execSQL(
            "INSERT INTO workspaces " +
                "(id, createdAt, updatedAt, isDeleted, version, provenance, sourceContextId) " +
                "VALUES (?, 1, 1, ?, 1, ?, ?)",
            arrayOf<Any?>(id, if (deleted) 1 else 0, provenance, source),
        )
    }

    private fun legacyRef(
        db: SupportSQLiteDatabase,
        ownerId: String,
        order: Long,
    ) {
        db.execSQL(
            "INSERT INTO main_beacon_context_cross_ref " +
                "(beacon_id, context_id, ref_order) VALUES ('beacon', ?, ?)",
            arrayOf<Any>(ownerId, order),
        )
    }

    private fun canonicalRef(
        db: SupportSQLiteDatabase,
        ownerId: String,
        order: Long,
    ) {
        db.execSQL(
            "INSERT INTO main_beacon_workspace_cross_ref " +
                "(beacon_id, workspace_id, ref_order) VALUES ('beacon', ?, ?)",
            arrayOf<Any>(ownerId, order),
        )
    }

    private inline fun <R> withMigratedDatabase(
        name: String,
        block: (SupportSQLiteOpenHelper) -> R,
    ): R {
        val database = migrate(name)
        return try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun migrate(name: String): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(195) {
                        override fun onConfigure(db: SupportSQLiteDatabase) {
                            db.setForeignKeyConstraintsEnabled(true)
                        }

                        override fun onCreate(db: SupportSQLiteDatabase) {
                            error("Historical schema 194 must already exist")
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {
                            check(oldVersion == 194 && newVersion == 195)
                            MIGRATION_194_195.migrate(db)
                        }
                    },
                )
                .build(),
        )

    private fun createFromExported194(name: String) {
        context.deleteDatabase(name)

        val schema =
            JsonParser.parseString(findSchema194().readText())
                .asJsonObject
                .getAsJsonObject("database")

        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(194) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            schema.getAsJsonArray("entities").forEach { item ->
                                val entity = item.asJsonObject
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
                )
                .build(),
        ).use { helper ->
            helper.writableDatabase
        }
    }

    private fun openExisting(
        name: String,
        version: Int,
    ): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )

    private fun findSchema194(): File {
        var current: File? =
            File(requireNotNull(System.getProperty("user.dir"))).absoluteFile

        val relative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/194.json"

        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }

        error("Unable to locate exported Room schema 194")
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        name: String,
    ): Boolean =
        db.query(
            "SELECT 1 FROM sqlite_master " +
                "WHERE type = 'table' AND name = ?",
            arrayOf(name),
        ).use { it.moveToFirst() }

    private fun formalContextFkCount(db: SupportSQLiteDatabase): Int =
        listOf(
            "day_tasks",
            "notes",
            "tactical_missions",
            "tactical_activity_slots",
            "main_beacon_workspace_cross_ref",
        ).sumOf { table ->
            db.query("PRAGMA foreign_key_list(`$table`)").use { cursor ->
                val referenceIndex = cursor.getColumnIndexOrThrow("table")
                var matches = 0
                while (cursor.moveToNext()) {
                    if (cursor.getString(referenceIndex) == "contexts") matches++
                }
                matches
            }
        }

    private fun scalar(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            requireNotNull(cursor.getString(0))
        }
}
