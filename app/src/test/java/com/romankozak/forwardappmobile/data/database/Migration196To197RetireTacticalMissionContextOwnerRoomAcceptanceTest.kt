package com.romankozak.forwardappmobile.data.database

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.logicalProjectId
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

/**
 * Context-FK.6: production Room 196 -> 197 physical retirement acceptance.
 *
 * Uses exported schema 196 rather than an approximated historical DDL.
 * Verifies real Room opening, exact data preservation, attachment FK safety,
 * AUTOINCREMENT continuity and atomic fail-closed migration rejection.
 */
@RunWith(RobolectricTestRunner::class)
class Migration196To197RetireTacticalMissionContextOwnerRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `valid mission ownership converges and preserves specialized data`() =
        runBlocking {
            val name = "fk6_196_197_valid"

            createFromExported196(name)

            openExisting(name, 196).use { helper ->
                val db = helper.writableDatabase

                seedContext(db, "ordinary")
                seedWorkspace(
                    db, "ordinary", "CONTEXT_BACKED", "ordinary",
                )

                seedContext(db, "retired", deleted = true)
                seedWorkspace(db, "retired", "CANONICAL_ONLY")

                seedWorkspace(db, "sys_today", "CANONICAL_ONLY")
                seedWorkspace(db, "standalone", "STANDALONE")
                seedWorkspace(db, "independent", "CANONICAL_ONLY")

                seedMission(db, 11, legacy = "ordinary")
                seedMission(
                    db, 12,
                    canonical = "ordinary",
                    source = "PREVIOUS_WEEK",
                )
                seedMission(
                    db, 13,
                    legacy = "ordinary",
                    canonical = "ordinary",
                    source = "ARC_QUEST",
                )
                seedMission(db, 14)
                seedMission(
                    db, 15,
                    canonical = "sys_today",
                    source = "SLOT_BACKLOG_ITEM",
                )
                seedMission(
                    db, 16,
                    legacy = "retired",
                    source = "CONTEXT_BACKLOG_ITEM",
                )
                seedMission(db, 17, canonical = "standalone")
                seedMission(
                    db, 18,
                    canonical = "independent",
                    source = "PREVIOUS_WEEK",
                )
                seedMission(
                    db, 19,
                    canonical = "ordinary",
                    source = "CONTEXT_BACKLOG_ITEM",
                )
                seedMission(
                    db, 20,
                    canonical = "ordinary",
                    source = "SLOT_BACKLOG_ITEM",
                )
                seedMission(
                    db, 21,
                    canonical = "ordinary",
                    source = "ARC_QUEST",
                )
                seedMission(
                    db, 22,
                    canonical = "ordinary",
                    source = "PREVIOUS_WEEK",
                )

                seedAttachment(db, "attachment-one")
                seedAttachment(db, "attachment-two")
                seedAttachmentRef(db, 11, "attachment-one")
                seedAttachmentRef(db, 16, "attachment-two")

                // Advance SQLite high-water mark above surviving ids.
                seedMission(db, 800)
                db.execSQL(
                    "DELETE FROM tactical_missions WHERE id = 800",
                )
            }

            val originalPayload =
                openExisting(name, 196).use { helper ->
                    captureMissionPayload(
                        helper.writableDatabase,
                        setOf("projectId", "project_workspace_id"),
                    )
                }

            val oldSequence =
                openExisting(name, 196).use { helper ->
                    scalar(
                        helper.writableDatabase,
                        "SELECT seq FROM sqlite_sequence " +
                            "WHERE name = 'tactical_missions'",
                    )
                }

            assertTrue(oldSequence >= 800L)

            withMigrated(name) { room ->
                val db = room.openHelper.writableDatabase

                assertEquals(198L, scalar(db, "PRAGMA user_version"))
                assertFalse(columnExists(db, "tactical_missions", "projectId"))
                assertTrue(
                    columnExists(
                        db, "tactical_missions", "project_workspace_id",
                    ),
                )

                assertEquals(12L, scalar(db, "SELECT COUNT(*) FROM tactical_missions"))
                assertEquals(
                    originalPayload,
                    captureMissionPayload(
                        db,
                        setOf("projectId", "project_workspace_id"),
                    ),
                )

                val expectedOwners =
                    mapOf(
                        11L to "ordinary",
                        12L to "ordinary",
                        13L to "ordinary",
                        14L to null,
                        15L to "sys_today",
                        16L to "retired",
                        17L to "standalone",
                        18L to "independent",
                        19L to "ordinary",
                        20L to "ordinary",
                        21L to "ordinary",
                        22L to "ordinary",
                    )

                expectedOwners.forEach { (id, owner) ->
                    db.query(
                        "SELECT project_workspace_id FROM tactical_missions " +
                            "WHERE id = ?",
                        arrayOf(id),
                    ).use { cursor ->
                        assertTrue(cursor.moveToFirst())
                        val actual =
                            if (cursor.isNull(0)) null else cursor.getString(0)
                        assertEquals("Mission $id", owner, actual)
                    }

                    val mission =
                        requireNotNull(room.tacticalMissionDao().getMissionById(id))

                    assertNull("Mission $id legacy owner", mission.projectId)
                    assertEquals(owner, mission.projectWorkspaceId)
                    assertEquals(owner, mission.logicalProjectId)
                }

                assertEquals(
                    listOf(
                        "11|attachment-one",
                        "16|attachment-two",
                    ),
                    attachmentPairs(db),
                )

                assertEquals(2L, scalar(
                    db,
                    "SELECT COUNT(*) FROM tactical_mission_attachment_cross_ref",
                ))

                assertEquals(1, formalContextFkCount(db))
                assertEquals(
                    0L,
                    scalar(
                        db,
                        "SELECT COUNT(*) FROM pragma_foreign_key_check",
                    ),
                )
                assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))

                assertEquals(
                    listOf(11L, 12L, 13L, 19L, 20L, 21L, 22L),
                    room.tacticalMissionDao()
                        .getMissionsForProject("ordinary")
                        .first()
                        .map { it.id }
                        .sorted(),
                )

                // Version-guarded ACK changes metadata only.
                val before = requireNotNull(
                    room.tacticalMissionDao().getMissionById(11L),
                )
                assertEquals(
                    1,
                    room.tacticalMissionDao().markMissionSyncedIfVersionMatches(
                        missionId = before.id,
                        expectedVersion = before.version,
                        syncedAt = 98765L,
                    ),
                )

                val after = requireNotNull(
                    room.tacticalMissionDao().getMissionById(11L),
                )

                assertEquals(before.copy(syncedAt = 98765L), after)
                assertEquals("ordinary", after.projectWorkspaceId)
                assertNull(after.projectId)

                assertEquals(
                    0,
                    room.tacticalMissionDao().markMissionSyncedIfVersionMatches(
                        missionId = before.id,
                        expectedVersion = before.version + 1L,
                        syncedAt = 99999L,
                    ),
                )

                assertTrue(
                    scalar(
                        db,
                        "SELECT seq FROM sqlite_sequence " +
                            "WHERE name = 'tactical_missions'",
                    ) >= oldSequence,
                )

                db.execSQL(
                    """
                    INSERT INTO tactical_missions
                        (title, deadline, status, priority)
                    VALUES ('Post migration mission', 999, 'ACTIVE', 'MEDIUM')
                    """.trimIndent(),
                )

                assertTrue(
                    scalar(
                        db,
                        "SELECT MAX(id) FROM tactical_missions",
                    ) > oldSequence,
                )
            }
        }

    @Test
    fun `invalid ownership and unsupported sources roll back atomically`() {
        val invalidCases:
            List<Pair<String, (SupportSQLiteDatabase) -> Unit>> =
            listOf(
                "divergent" to { db ->
                    seedContext(db, "other")
                    seedWorkspace(db, "other", "CONTEXT_BACKED", "other")
                    seedMission(
                        db, 99,
                        legacy = "ordinary",
                        canonical = "other",
                    )
                },
                "missing-workspace" to { db ->
                    seedContext(db, "missing")
                    seedMission(db, 99, legacy = "missing")
                },
                "deleted-workspace" to { db ->
                    seedContext(db, "deleted")
                    seedWorkspace(
                        db, "deleted", "CONTEXT_BACKED",
                        "deleted", deleted = true,
                    )
                    seedMission(db, 99, legacy = "deleted")
                },
                "unproven-live" to { db ->
                    seedContext(db, "unproven")
                    seedWorkspace(db, "unproven", "CANONICAL_ONLY")
                    seedMission(db, 99, legacy = "unproven")
                },
                "unproven-retired" to { db ->
                    seedContext(db, "retired", deleted = true)
                    seedWorkspace(
                        db, "retired", "CONTEXT_BACKED", "retired",
                    )
                    seedMission(db, 99, legacy = "retired")
                },
                "invalid-system" to { db ->
                    seedWorkspace(
                        db, "sys_today", "CONTEXT_BACKED", "sys_today",
                    )
                    seedMission(db, 99, canonical = "sys_today")
                },
                "unproven-contextless" to { db ->
                    seedWorkspace(
                        db, "orphan", "CONTEXT_BACKED", "wrong-source",
                    )
                    seedMission(db, 99, canonical = "orphan")
                },
                "unsupported-source" to { db ->
                    seedMission(db, 99, source = "UNKNOWN_SOURCE")
                },
            )

        invalidCases.forEach { (case, makeInvalid) ->
            val name = "fk6_196_197_reject_$case"
            createFromExported196(name)

            openExisting(name, 196).use { helper ->
                val db = helper.writableDatabase
                seedContext(db, "ordinary")
                seedWorkspace(
                    db, "ordinary", "CONTEXT_BACKED", "ordinary",
                )
                seedMission(db, 5, legacy = "ordinary")
                seedAttachment(db, "retained-attachment")
                seedAttachmentRef(db, 5, "retained-attachment")
                makeInvalid(db)
            }

            val before =
                openExisting(name, 196).use { helper ->
                    captureMissionPayload(helper.writableDatabase, emptySet())
                }

            val room = migrate(name)
            val failure =
                try {
                    runCatching { room.openHelper.writableDatabase }
                        .exceptionOrNull()
                } finally {
                    room.close()
                }

            assertNotNull("Expected migration failure: $case", failure)

            openExisting(name, 196).use { helper ->
                val db = helper.writableDatabase

                assertEquals(196L, scalar(db, "PRAGMA user_version"))
                assertTrue(columnExists(db, "tactical_missions", "projectId"))
                assertFalse(
                    columnExists(db, "tactical_missions", "project_workspace_id")
                        .not(),
                )
                assertFalse(columnExists(db, "tactical_missions_new", "id"))

                assertEquals(
                    before,
                    captureMissionPayload(db, emptySet()),
                )

                assertEquals(
                    listOf("5|retained-attachment"),
                    attachmentPairs(db),
                )

                assertEquals(
                    0L,
                    scalar(
                        db,
                        "SELECT COUNT(*) FROM pragma_foreign_key_check",
                    ),
                )
                assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
            }
        }
    }

    private fun seedContext(
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
            arrayOf<Any?>(id, id, if (deleted) 1 else 0),
        )
    }

    private fun seedWorkspace(
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
            arrayOf<Any?>(
                id,
                if (deleted) 1 else 0,
                provenance,
                source,
            ),
        )
    }

    private fun seedMission(
        db: SupportSQLiteDatabase,
        id: Long,
        legacy: String? = null,
        canonical: String? = null,
        source: String = "MANUAL",
    ) {
        val values =
            ContentValues().apply {
                put("id", id)
                put("title", "Mission $id")
                put("description", "Preserved payload $id")
                put("startTime", 1000L + id)
                put("deadline", 2000L + id)
                put("status", "ACTIVE")
                put("priority", "MEDIUM")
                put("projectId", legacy)
                put("project_workspace_id", canonical)
                put("linkedProjectIds", "[]")
                put("linkedAttachmentIds", "[]")
                put("mission_order", id)
                put("week_key", "2026-W41")
                put("iteration_id", "iteration-$id")
                put("carried_from_mission_id", if (id > 11) 11L else null)
                put("order_in_week", id + 2)
                put("order_in_slot", 3L)
                put("mission_stream_id", "stream-1")
                put("activity_slot_context_id", "slot-compat-$id")
                put("source_type", source)
                put("source_context_id", "lineage-$id")
                put("source_backlog_item_id", "backlog-$id")
                put("source_arc_quest_id", "arc-$id")
                put("created_at", 3000L + id)
                put("updated_at", 4000L + id)
                put("synced_at", 5000L + id)
                put("is_deleted", 0)
                put("version", 7L)
            }

        check(db.insert("tactical_missions", 0, values) != -1L) {
            "Failed to seed TacticalMission $id"
        }
    }

    private fun seedAttachment(
        db: SupportSQLiteDatabase,
        id: String,
    ) {
        db.execSQL(
            """
            INSERT INTO attachments
                (id, attachment_type, entity_id,
                 createdAt, updatedAt, isDeleted, version)
            VALUES (?, 'NOTE', ?, 1, 1, 0, 1)
            """.trimIndent(),
            arrayOf<Any?>(id, "entity-$id"),
        )
    }

    private fun seedAttachmentRef(
        db: SupportSQLiteDatabase,
        missionId: Long,
        attachmentId: String,
    ) {
        db.execSQL(
            """
            INSERT INTO tactical_mission_attachment_cross_ref
                (missionId, attachmentId)
            VALUES (?, ?)
            """.trimIndent(),
            arrayOf<Any?>(missionId, attachmentId),
        )
    }

    private fun captureMissionPayload(
        db: SupportSQLiteDatabase,
        excluded: Set<String>,
    ): Map<Long, Map<String, String?>> =
        db.query("SELECT * FROM tactical_missions ORDER BY id").use { cursor ->
            val columns = cursor.columnNames.filterNot { it in excluded }

            buildMap {
                while (cursor.moveToNext()) {
                    val row =
                        columns.associateWith { column ->
                            val index = cursor.getColumnIndexOrThrow(column)
                            if (cursor.isNull(index)) null else cursor.getString(index)
                        }

                    put(
                        requireNotNull(row["id"]).toLong(),
                        row,
                    )
                }
            }
        }

    private fun attachmentPairs(db: SupportSQLiteDatabase): List<String> =
        db.query(
            """
            SELECT missionId, attachmentId
            FROM tactical_mission_attachment_cross_ref
            ORDER BY missionId, attachmentId
            """.trimIndent(),
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add("${cursor.getLong(0)}|${cursor.getString(1)}")
                }
            }
        }

    private fun formalContextFkCount(db: SupportSQLiteDatabase): Int =
        listOf(
            "notes",
            "tactical_missions",
            "tactical_activity_slots",
        ).sumOf { table ->
            db.query("PRAGMA foreign_key_list(`$table`)").use { cursor ->
                val index = cursor.getColumnIndexOrThrow("table")
                var count = 0

                while (cursor.moveToNext()) {
                    if (cursor.getString(index) == "contexts") count++
                }

                count
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
            .addMigrations(MIGRATION_196_197, MIGRATION_197_198)
            .allowMainThreadQueries()
            .build()

    private fun createFromExported196(name: String) {
        context.deleteDatabase(name)

        val schema =
            JsonParser.parseString(findSchema196().readText())
                .asJsonObject
                .getAsJsonObject("database")

        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(196) {
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

    private fun findSchema196(): File {
        var current: File? =
            File(requireNotNull(System.getProperty("user.dir"))).absoluteFile

        val relative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/196.json"

        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }

        error("Unable to locate exported Room schema 196")
    }
}
