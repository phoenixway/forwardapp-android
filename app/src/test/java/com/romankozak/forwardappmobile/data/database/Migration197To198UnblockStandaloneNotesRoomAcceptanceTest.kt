package com.romankozak.forwardappmobile.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * FK.10A physical-storage acceptance using the real exported Room 197
 * schema and the production 197 -> 198 migration.
 *
 * Does not claim FK.10B canonical repository/runtime convergence.
 */
@RunWith(RobolectricTestRunner::class)
class Migration197To198UnblockStandaloneNotesRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `migration preserves notes tombstones and zero one many placements`() {
        val name = "fk10a_197_198_preservation"
        createFromExported197(name)

        lateinit var originalNotes: List<List<String?>>
        lateinit var originalPlacements: List<List<String?>>

        openExisting(name, 197).use { helper ->
            val db = helper.writableDatabase

            seedContext(db, "context-live")
            seedContext(db, "context-deleted", deleted = true)

            seedNote(db, "note-zero", "context-live", "pomegranate zero")
            seedNote(db, "note-one", "context-deleted", "historical one")
            seedNote(db, "note-many", "context-live", "multiple places")
            seedNote(
                db, "note-tombstone", "context-deleted",
                "deleted content", deleted = true,
            )

            seedWorkspace(db, "workspace-a")
            seedWorkspace(db, "workspace-b")
            seedCapability(db, "capability-a", "workspace-a")
            seedCapability(db, "capability-b", "workspace-b")

            seedPlacement(
                db, "placement-one", "workspace-a",
                "capability-a", "note-one", 0,
            )
            seedPlacement(
                db, "placement-many-a", "workspace-a",
                "capability-a", "note-many", 1,
            )
            seedPlacement(
                db, "placement-many-b", "workspace-b",
                "capability-b", "note-many", 0,
            )

            originalNotes = rows(db, NOTE_PAYLOAD_QUERY)
            originalPlacements = rows(db, PLACEMENT_PAYLOAD_QUERY)

            assertEquals(4, originalNotes.size)
            assertEquals(3, originalPlacements.size)
            assertEquals(2L, contextFkCount(db))
            assertEquals(0L, scalar(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
        }

        withMigrated(name) { room ->
            val db = room.openHelper.writableDatabase

            assertEquals(198L, scalar(db, "PRAGMA user_version"))
            assertEquals(originalNotes, rows(db, NOTE_PAYLOAD_QUERY))
            assertEquals(originalPlacements, rows(db, PLACEMENT_PAYLOAD_QUERY))

            assertEquals(1L, contextFkCount(db))
            assertEquals(0L, scalar(db, "SELECT COUNT(*) FROM pragma_foreign_key_list('notes')"))
            assertEquals(1L, scalar(db, "SELECT COUNT(*) FROM pragma_foreign_key_list('tactical_activity_slots') WHERE \"table\" = 'contexts'"))
            assertEquals(0L, noteContextNotNull(db))
            assertEquals(0L, scalar(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", stringScalar(db, "PRAGMA integrity_check"))

            assertEquals(
                1L,
                scalar(db, "SELECT COUNT(*) FROM notes_fts WHERE notes_fts MATCH 'pomegranate'"),
            )
            assertEquals(
                1L,
                scalar(db, "SELECT COUNT(*) FROM notes WHERE id = 'note-tombstone' AND isDeleted = 1 AND version = 7"),
            )

            // Physical Context deletion is no longer Note deletion.
            db.execSQL("DELETE FROM contexts WHERE id = 'context-live'")
            db.execSQL("DELETE FROM contexts WHERE id = 'context-deleted'")

            assertEquals(originalNotes, rows(db, NOTE_PAYLOAD_QUERY))
            assertEquals(originalPlacements, rows(db, PLACEMENT_PAYLOAD_QUERY))
            assertEquals(0L, scalar(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
            assertEquals("ok", stringScalar(db, "PRAGMA integrity_check"))
        }
    }

    @Test
    fun `standalone and missing historical context persist with version guarded ACK`() =
        runBlocking {
            val name = "fk10a_197_198_standalone"
            createFromExported197(name)

            openExisting(name, 197).use { helper ->
                seedContext(helper.writableDatabase, "legacy-existing")
                seedNote(
                    helper.writableDatabase,
                    "legacy-note",
                    "legacy-existing",
                    "preserved old content",
                )
            }

            withMigrated(name) { room ->
                val dao = room.legacyNoteDao()
                val db = room.openHelper.writableDatabase

                val standalone =
                    LegacyNoteEntity(
                        id = "standalone-note",
                        contextId = null,
                        title = "standalone gentian",
                        content = "independent text",
                        createdAt = 100L,
                        updatedAt = 200L,
                        syncedAt = null,
                        isDeleted = false,
                        version = 7L,
                    )

                dao.insert(standalone)

                val loaded = requireNotNull(dao.getNoteById(standalone.id))
                assertEquals(standalone, loaded)
                assertNull(loaded.contextId)
                assertEquals(
                    0L,
                    scalar(db, "SELECT COUNT(*) FROM workspace_backlog_entries WHERE targetKind = 'LEGACY_NOTE' AND targetId = 'standalone-note'"),
                )

                val historical =
                    standalone.copy(
                        id = "missing-historical-context",
                        contextId = "does-not-exist",
                        title = "historical locator",
                        content = "no Context resurrection",
                    )
                dao.insert(historical)
                assertEquals(historical, dao.getNoteById(historical.id))
                assertEquals(
                    0L,
                    scalar(db, "SELECT COUNT(*) FROM contexts WHERE id = 'does-not-exist'"),
                )

                assertEquals(
                    1,
                    dao.markSyncedIfVersionMatches(
                        noteId = standalone.id,
                        expectedVersion = 7L,
                        syncedAt = 777L,
                    ),
                )
                assertEquals(
                    0,
                    dao.markSyncedIfVersionMatches(
                        noteId = standalone.id,
                        expectedVersion = 6L,
                        syncedAt = 888L,
                    ),
                )

                val acknowledged = requireNotNull(dao.getNoteById(standalone.id))
                assertEquals(standalone.title, acknowledged.title)
                assertEquals(standalone.content, acknowledged.content)
                assertEquals(standalone.version, acknowledged.version)
                assertEquals(777L, acknowledged.syncedAt)
                assertNull(acknowledged.contextId)

                // External-content FTS must also track post-migration writes.
                assertEquals(
                    1L,
                    scalar(db, "SELECT COUNT(*) FROM notes_fts WHERE notes_fts MATCH 'gentian'"),
                )
                dao.update(
                    acknowledged.copy(
                        title = "standalone iris",
                        content = "changed searchable text",
                    ),
                )
                assertEquals(
                    0L,
                    scalar(db, "SELECT COUNT(*) FROM notes_fts WHERE notes_fts MATCH 'gentian'"),
                )
                assertEquals(
                    1L,
                    scalar(db, "SELECT COUNT(*) FROM notes_fts WHERE notes_fts MATCH 'iris'"),
                )

                assertEquals(0L, scalar(db, "SELECT COUNT(*) FROM pragma_foreign_key_check"))
                assertEquals("ok", stringScalar(db, "PRAGMA integrity_check"))
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

    private fun seedNote(
        db: SupportSQLiteDatabase,
        id: String,
        contextId: String,
        title: String,
        deleted: Boolean = false,
    ) {
        db.execSQL(
            """
            INSERT INTO notes
                (id, contextId, title, content, createdAt,
                 updatedAt, syncedAt, isDeleted, version)
            VALUES (?, ?, ?, ?, 101, 202, 303, ?, 7)
            """.trimIndent(),
            arrayOf<Any?>(
                id, contextId, title, "payload-$id",
                if (deleted) 1 else 0,
            ),
        )
    }

    private fun seedWorkspace(db: SupportSQLiteDatabase, id: String) {
        db.execSQL(
            """
            INSERT INTO workspaces
                (id, createdAt, updatedAt, isDeleted, version,
                 provenance, sourceContextId)
            VALUES (?, 1, 1, 0, 1, 'STANDALONE', NULL)
            """.trimIndent(),
            arrayOf<Any?>(id),
        )
    }

    private fun seedCapability(
        db: SupportSQLiteDatabase,
        id: String,
        workspaceId: String,
    ) {
        db.execSQL(
            """
            INSERT INTO workspace_capability_instances
                (id, workspaceId, capabilityType, instanceKey,
                 capabilityOrder, state, configurationVersion,
                 configuration, createdAt, updatedAt,
                 syncedAt, isDeleted, version)
            VALUES (?, ?, 'BACKLOG', 'default', 0, 'ACTIVE', 1,
                    '{}', 1, 1, NULL, 0, 1)
            """.trimIndent(),
            arrayOf<Any?>(id, workspaceId),
        )
    }

    private fun seedPlacement(
        db: SupportSQLiteDatabase,
        id: String,
        workspaceId: String,
        capabilityId: String,
        noteId: String,
        order: Int,
    ) {
        db.execSQL(
            """
            INSERT INTO workspace_backlog_entries
                (id, workspaceId, capabilityInstanceId, targetKind,
                 targetId, entryOrder, createdAt, updatedAt,
                 syncedAt, isDeleted, version)
            VALUES (?, ?, ?, 'LEGACY_NOTE', ?, ?, 1, 1,
                    NULL, 0, 1)
            """.trimIndent(),
            arrayOf<Any?>(id, workspaceId, capabilityId, noteId, order),
        )
    }

    private fun contextFkCount(db: SupportSQLiteDatabase): Long =
        listOf("notes", "tactical_activity_slots").sumOf { table ->
            scalar(
                db,
                "SELECT COUNT(*) FROM pragma_foreign_key_list('$table') WHERE \"table\" = 'contexts'",
            )
        }

    private fun noteContextNotNull(db: SupportSQLiteDatabase): Long =
        scalar(
            db,
            "SELECT \"notnull\" FROM pragma_table_info('notes') WHERE name = 'contextId'",
        )

    private fun rows(
        db: SupportSQLiteDatabase,
        query: String,
    ): List<List<String?>> =
        db.query(query).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        (0 until cursor.columnCount).map { column ->
                            if (cursor.isNull(column)) null else cursor.getString(column)
                        },
                    )
                }
            }
        }

    private fun scalar(
        db: SupportSQLiteDatabase,
        query: String,
    ): Long =
        db.query(query).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun stringScalar(
        db: SupportSQLiteDatabase,
        query: String,
    ): String =
        db.query(query).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getString(0)
        }

    private inline fun <R> withMigrated(
        name: String,
        block: (AppDatabase) -> R,
    ): R {
        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_197_198)
                .allowMainThreadQueries()
                .build()

        return try {
            block(room)
        } finally {
            room.close()
        }
    }

    private fun createFromExported197(name: String) {
        context.deleteDatabase(name)

        val schema =
            JsonParser.parseString(findSchema197().readText())
                .asJsonObject
                .getAsJsonObject("database")

        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(197) {
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

    private fun findSchema197(): File {
        var current: File? =
            File(requireNotNull(System.getProperty("user.dir"))).absoluteFile

        val relative =
            "app/schemas/com.romankozak.forwardappmobile.database.AppDatabase/197.json"

        while (current != null) {
            File(current, relative).takeIf(File::isFile)?.let { return it }
            current = current.parentFile
        }

        error("Unable to locate exported Room schema 197")
    }

    companion object {
        private const val NOTE_PAYLOAD_QUERY =
            "SELECT id, contextId, title, content, createdAt, updatedAt, " +
                "syncedAt, isDeleted, version FROM notes ORDER BY id"

        private const val PLACEMENT_PAYLOAD_QUERY =
            "SELECT id, workspaceId, capabilityInstanceId, targetKind, " +
                "targetId, entryOrder, createdAt, updatedAt, syncedAt, " +
                "isDeleted, version FROM workspace_backlog_entries ORDER BY id"
    }
}
