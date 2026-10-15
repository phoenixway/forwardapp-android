package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.logicalProjectId
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration195To196RetireDayTaskContextOwnerRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `valid historical owners converge without losing DayTask data`() =
        runBlocking {
            val name = "day_task_195_196_valid"
            seed195(name) { db ->
                historicalContext(db, "ordinary")
                workspace(db, "ordinary", "CONTEXT_BACKED", "ordinary")

                historicalContext(db, "retired", deleted = true)
                workspace(db, "retired", "CANONICAL_ONLY")

                workspace(db, "sys_today", "CANONICAL_ONLY")
                workspace(db, "standalone", "STANDALONE")
                workspace(db, "independent", "CANONICAL_ONLY")

                dayTask(db, "legacy", legacy = "ordinary")
                dayTask(db, "workspace", canonical = "ordinary")
                dayTask(
                    db,
                    "equivalent",
                    legacy = "ordinary",
                    canonical = "ordinary",
                )
                dayTask(db, "unowned")
                dayTask(db, "system", canonical = "sys_today")
                dayTask(db, "retired-task", legacy = "retired")
                dayTask(db, "standalone-task", canonical = "standalone")
                dayTask(db, "independent-task", canonical = "independent")
            }

            val originalPayload =
                openExisting(name, 195).use {
                    captureNonOwnerPayload(it.writableDatabase)
                }

            withMigrated(name) { room ->
                val db = room.openHelper.writableDatabase

                assertEquals(198L, scalar(db, "PRAGMA user_version"))
                assertFalse(columnExists(db, "day_tasks", "projectId"))
                assertTrue(columnExists(db, "day_tasks", "project_workspace_id"))

                assertEquals(
                    originalPayload,
                    captureNonOwnerPayload(db),
                )
                assertEquals(8L, scalar(db, "SELECT COUNT(*) FROM day_tasks"))

                val expected =
                    mapOf(
                        "legacy" to "ordinary",
                        "workspace" to "ordinary",
                        "equivalent" to "ordinary",
                        "unowned" to null,
                        "system" to "sys_today",
                        "retired-task" to "retired",
                        "standalone-task" to "standalone",
                        "independent-task" to "independent",
                    )

                expected.forEach { (id, ownerId) ->
                    db.query(
                        "SELECT project_workspace_id FROM day_tasks WHERE id = ?",
                        arrayOf(id),
                    ).use { cursor ->
                        assertTrue(cursor.moveToFirst())
                        val actual =
                            if (cursor.isNull(0)) null else cursor.getString(0)
                        assertEquals("DayTask $id", ownerId, actual)
                    }
                }

                assertEquals(1, formalContextFkCount(db))
                assertEquals(
                    0L,
                    scalar(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"),
                )
                assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))

                val tasks = room.dayTaskDao().getAllTasksSync().associateBy { it.id }
                assertEquals(expected.keys, tasks.keys)

                expected.forEach { (id, ownerId) ->
                    val task = requireNotNull(tasks[id])
                    assertNull(task.projectId)
                    assertEquals(ownerId, task.projectWorkspaceId)
                    assertEquals(ownerId, task.logicalProjectId)
                }

                assertEquals(
                    listOf("equivalent", "legacy", "workspace"),
                    room.dayTaskDao()
                        .getTasksForProject("ordinary")
                        .first()
                        .map { it.id }
                        .sorted(),
                )

                val before = requireNotNull(tasks["legacy"])
                assertEquals(
                    1,
                    room.dayTaskDao().markSyncedIfVersionMatches(
                        taskId = before.id,
                        expectedVersion = before.version,
                        syncedAt = 8765L,
                    ),
                )

                val acknowledged =
                    requireNotNull(room.dayTaskDao().getTaskById(before.id))
                assertEquals(before.copy(syncedAt = 8765L), acknowledged)
                assertEquals("ordinary", acknowledged.projectWorkspaceId)
                assertEquals(before.version, acknowledged.version)

                assertEquals(
                    0,
                    room.dayTaskDao().markSyncedIfVersionMatches(
                        taskId = before.id,
                        expectedVersion = before.version + 1L,
                        syncedAt = 9999L,
                    ),
                )
            }
        }

    @Test
    fun `invalid owners reject upgrade and preserve exact schema 195 data`() {
        val cases: List<Pair<String, (SupportSQLiteDatabase) -> Unit>> =
            listOf(
                "divergent" to { db ->
                    historicalContext(db, "z-legacy")
                    workspace(db, "z-canonical", "STANDALONE")
                    dayTask(
                        db,
                        "z-conflict",
                        legacy = "z-legacy",
                        canonical = "z-canonical",
                    )
                },
                "missing" to { db ->
                    historicalContext(db, "z-missing")
                    dayTask(db, "z-conflict", legacy = "z-missing")
                },
                "deleted" to { db ->
                    historicalContext(db, "z-deleted")
                    workspace(db, "z-deleted", "CONTEXT_BACKED", "z-deleted", true)
                    dayTask(db, "z-conflict", legacy = "z-deleted")
                },
                "unproven-live" to { db ->
                    historicalContext(db, "z-live")
                    workspace(db, "z-live", "CANONICAL_ONLY")
                    dayTask(db, "z-conflict", legacy = "z-live")
                },
                "unproven-retired" to { db ->
                    historicalContext(db, "z-retired", deleted = true)
                    workspace(db, "z-retired", "CONTEXT_BACKED", "z-retired")
                    dayTask(db, "z-conflict", legacy = "z-retired")
                },
                "unproven-contextless" to { db ->
                    workspace(db, "z-orphan", "CONTEXT_BACKED", "wrong-source")
                    dayTask(db, "z-conflict", canonical = "z-orphan")
                },
                "invalid-system" to { db ->
                    workspace(db, "sys_today", "CONTEXT_BACKED", "sys_today")
                    dayTask(db, "z-conflict", canonical = "sys_today")
                },
            )

        cases.forEach { (case, invalid) ->
            val name = "day_task_195_196_reject_$case"
            seed195(name) { db ->
                historicalContext(db, "a-valid")
                workspace(db, "a-valid", "CONTEXT_BACKED", "a-valid")
                dayTask(db, "a-valid-task", legacy = "a-valid")
                invalid(db)
            }

            val before =
                openExisting(name, 195).use {
                    captureAllDayTaskPayload(it.writableDatabase)
                }

            val room = migrate(name)
            val failure =
                try {
                    runCatching { room.openHelper.writableDatabase }
                        .exceptionOrNull()
                } finally {
                    room.close()
                }

            assertNotNull("Expected migration rejection: $case", failure)

            openExisting(name, 195).use { helper ->
                val db = helper.writableDatabase
                assertEquals(195L, scalar(db, "PRAGMA user_version"))
                assertTrue(columnExists(db, "day_tasks", "projectId"))
                assertFalse(columnExists(db, "day_tasks_new", "id"))
                assertEquals(before, captureAllDayTaskPayload(db))
                assertEquals(
                    0L,
                    scalar(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"),
                )
                assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
            }
        }
    }

    private fun seed195(
        name: String,
        seed: (SupportSQLiteDatabase) -> Unit,
    ) {
        createFromExported195(name)
        openExisting(name, 195).use { helper ->
            val db = helper.writableDatabase

            db.execSQL(
                """
                INSERT INTO day_plans
                (id, date, status, totalPlannedMinutes,
                 totalCompletedMinutes, completionPercentage,
                 createdAt, isDeleted, version)
                VALUES ('plan', 1, 'NOT_STARTED', 0, 0, 0.0, 1, 0, 1)
                """.trimIndent(),
            )

            seed(db)
        }
    }

    private fun historicalContext(
        db: SupportSQLiteDatabase,
        id: String,
        deleted: Boolean = false,
    ) {
        db.execSQL(
            """
            INSERT INTO contexts
            (id, name, createdAt, scoring_status, is_deleted)
            VALUES (?, ?, 1, 'UNASSESSED', ?)
            """.trimIndent(),
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
            """
            INSERT INTO workspaces
            (id, createdAt, updatedAt, isDeleted, version,
             provenance, sourceContextId)
            VALUES (?, 1, 1, ?, 1, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(id, if (deleted) 1 else 0, provenance, source),
        )
    }

    private fun dayTask(
        db: SupportSQLiteDatabase,
        id: String,
        legacy: String? = null,
        canonical: String? = null,
    ) {
        db.execSQL(
            """
            INSERT INTO day_tasks (
                id, dayPlanId, title, projectId, project_workspace_id,
                `order`, priority, status, completed, createdAt,
                updatedAt, isDeleted, version, description, syncedAt,
                recurrenceSeriesId, recurrenceOccurrenceDayKey,
                recurrenceSourceSeriesVersion, scheduledTime,
                estimatedDurationMinutes, points, executionStrictness,
                valueImportance, valueImpact, effort, cost, risk,
                tags, linkedProjectIds
            )
            VALUES (
                ?, 'plan', ?, ?, ?,
                7, 'MEDIUM', 'NOT_STARTED', 0, 1234,
                2345, 0, 11, 'Preserve this payload', NULL,
                'series-1', '2026-10-10',
                4, 3000,
                15, 8, 'NORMAL',
                1.5, 2.5, 3.5, 4.5, 5.5,
                '[]', '[]'
            )
            """.trimIndent(),
            arrayOf<Any?>(id, id, legacy, canonical),
        )
    }

    private fun captureNonOwnerPayload(
        db: SupportSQLiteDatabase,
    ): Map<String, Map<String, String?>> =
        capturePayload(db, setOf("projectId", "project_workspace_id"))

    private fun captureAllDayTaskPayload(
        db: SupportSQLiteDatabase,
    ): Map<String, Map<String, String?>> =
        capturePayload(db, emptySet())

    private fun capturePayload(
        db: SupportSQLiteDatabase,
        excluded: Set<String>,
    ): Map<String, Map<String, String?>> =
        db.query("SELECT * FROM day_tasks ORDER BY id").use { cursor ->
            val columns = cursor.columnNames.filterNot { it in excluded }
            buildMap {
                while (cursor.moveToNext()) {
                    val row =
                        columns.associateWith { column ->
                            val index = cursor.getColumnIndexOrThrow(column)
                            if (cursor.isNull(index)) null else cursor.getString(index)
                        }
                    put(requireNotNull(row["id"]), row)
                }
            }
        }

    private fun formalContextFkCount(db: SupportSQLiteDatabase): Int =
        listOf(
            "day_tasks",
            "notes",
            "tactical_missions",
            "tactical_activity_slots",
        ).sumOf { table ->
            db.query("PRAGMA foreign_key_list(`$table`)").use { cursor ->
                val index = cursor.getColumnIndexOrThrow("table")
                var matches = 0
                while (cursor.moveToNext()) {
                    if (cursor.getString(index) == "contexts") matches++
                }
                matches
            }
        }

    private fun columnExists(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
    ): Boolean =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val index = cursor.getColumnIndexOrThrow("name")
            var found = false
            while (cursor.moveToNext()) {
                if (cursor.getString(index) == column) found = true
            }
            found
        }

    private fun scalar(db: SupportSQLiteDatabase, sql: String): Long =
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
            cursor.getString(0)
        }

    private inline fun <R> withMigrated(
        name: String,
        block: (AppDatabase) -> R,
    ): R {
        val room = migrate(name)
        return try {
            block(room)
        } finally {
            room.close()
        }
    }

    private fun migrate(name: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, name)
            // Preserve the 195->196 DayTask gate, then reach current Room 198.
            .addMigrations(MIGRATION_195_196, MIGRATION_196_197, MIGRATION_197_198)
            .allowMainThreadQueries()
            .build()

    private fun createFromExported195(name: String) {
        context.deleteDatabase(name)

        val schema =
            JsonParser.parseString(findSchema195().readText())
                .asJsonObject
                .getAsJsonObject("database")

        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(195) {
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

    private fun findSchema195(): File {
        var current: File? =
            File(requireNotNull(System.getProperty("user.dir"))).absoluteFile

        val relative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/195.json"

        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }

        error("Unable to locate exported Room schema 195")
    }
}
