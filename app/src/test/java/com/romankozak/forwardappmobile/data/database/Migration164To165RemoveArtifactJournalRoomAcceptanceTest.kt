package com.romankozak.forwardappmobile.data.database

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration164To165RemoveArtifactJournalRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `164 to 165 hard deletes retired artifact and journal while preserving ordinary document graph`() {
        runAcceptance(
            dbName = "migration_164_165_remove_artifact_journal",
            startVersion = 164,
            migrations = arrayOf(MIGRATION_164_165),
        )
    }

    @Test
    fun `163 to 165 chain uses no-op bridge then performs the same hard retirement`() {
        runAcceptance(
            dbName = "migration_163_165_remove_artifact_journal_chain",
            startVersion = 163,
            migrations = arrayOf(MIGRATION_163_164, MIGRATION_164_165),
        )
    }

    private fun runAcceptance(
        dbName: String,
        startVersion: Int,
        migrations: Array<Migration>,
    ) {
        createFixture(dbName, startVersion)

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(*migrations)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(165L, scalarLong(db, "PRAGMA user_version"))

            // Room opened successfully, so schema 165 validation passed.
            assertFalse(tableExists(db, "context_artifacts"))
            assertFalse(columnExists(db, "structure_presets", "enable_artifact"))
            assertFalse(columnExists(db, "context_structures", "enable_artifact"))

            // Rebuilt configuration tables preserve every non-retired value.
            db.query(
                """
                SELECT code, label, description,
                       enable_inbox, enable_log, enable_advanced,
                       enable_dashboard, enable_backlog, enable_attachments,
                       enable_auto_link_subprojects,
                       createdAt, updatedAt, version, isDeleted
                FROM structure_presets
                WHERE id = 'preset-1'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("custom", cursor.getString(0))
                assertEquals("Custom preset", cursor.getString(1))
                assertEquals("kept description", cursor.getString(2))
                assertEquals(1, cursor.getInt(3))
                assertEquals(0, cursor.getInt(4))
                assertEquals(1, cursor.getInt(5))
                assertEquals(1, cursor.getInt(6))
                assertEquals(1, cursor.getInt(7))
                assertEquals(1, cursor.getInt(8))
                assertEquals(0, cursor.getInt(9))
                assertEquals(10L, cursor.getLong(10))
                assertEquals(20L, cursor.getLong(11))
                assertEquals(7L, cursor.getLong(12))
                assertEquals(0, cursor.getInt(13))
            }

            db.query(
                """
                SELECT contextId, base_preset_code, experimental_capability_ids,
                       apply_mode, enable_inbox, enable_log, enable_advanced,
                       enable_dashboard, enable_backlog, enable_attachments,
                       enable_auto_link_subprojects,
                       remove_inbox_entry_after_tag_autocopy,
                       remove_backlog_entry_after_tag_autocopy,
                       updatedAt, version, isDeleted
                FROM context_structures
                WHERE id = 'context-structure-1'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("context-1", cursor.getString(0))
                assertEquals("custom", cursor.getString(1))
                assertEquals("""["future-capability"]""", cursor.getString(2))
                assertEquals("OVERRIDE", cursor.getString(3))
                assertEquals(1, cursor.getInt(4))
                assertEquals(0, cursor.getInt(5))
                assertEquals(1, cursor.getInt(6))
                assertEquals(1, cursor.getInt(7))
                assertEquals(1, cursor.getInt(8))
                assertEquals(1, cursor.getInt(9))
                assertEquals(0, cursor.getInt(10))
                assertEquals(1, cursor.getInt(11))
                assertEquals(0, cursor.getInt(12))
                assertEquals(30L, cursor.getLong(13))
                assertEquals(8L, cursor.getLong(14))
                assertEquals(0, cursor.getInt(15))
            }

            // Retired Context Artifact storage is physically gone.
            assertFalse(tableExists(db, "context_artifacts"))

            // Retired document payloads.
            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM note_documents
                    WHERE id IN (
                        'journal-doc',
                        'system_journal_log_context-1',
                        'RETIRED_ARTIFACT_DOCUMENT:artifact-legacy'
                    )
                    """.trimIndent(),
                ),
            )

            // Retired attachment representations.
            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM attachments
                    WHERE id IN (
                        'journal-attachment',
                        'system-journal-attachment',
                        'RETIRED_ARTIFACT_ATTACHMENT:artifact-legacy'
                    )
                    """.trimIndent(),
                ),
            )
            assertEquals(
                0L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM attachments WHERE attachment_type = 'JOURNAL_DOCUMENT'",
                ),
            )

            // Their canonical placements are gone.
            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM workspace_connections
                    WHERE id IN (
                        'journal-connection',
                        'artifact-connection'
                    )
                    """.trimIndent(),
                ),
            )
            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM workspace_backlog_entries
                    WHERE targetKind = 'JOURNAL_DOCUMENT'
                    """.trimIndent(),
                ),
            )

            // Persisted retired capability instances are gone.
            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM workspace_capability_instances
                    WHERE lower(capabilityType) IN ('artifact', 'journal', 'journal_log')
                    """.trimIndent(),
                ),
            )

            // Ordinary NOTE_DOCUMENT content authority survives.
            db.query(
                """
                SELECT contextId, name, content, updatedAt, isDeleted, version
                FROM note_documents
                WHERE id = 'ordinary-doc'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("context-1", cursor.getString(0))
                assertEquals("Ordinary document", cursor.getString(1))
                assertEquals("ordinary payload survives", cursor.getString(2))
                assertEquals(200L, cursor.getLong(3))
                assertEquals(0, cursor.getInt(4))
                assertEquals(9L, cursor.getLong(5))
            }

            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM attachments
                    WHERE id = 'ordinary-attachment'
                      AND attachment_type = 'NOTE_DOCUMENT'
                      AND entity_id = 'ordinary-doc'
                    """.trimIndent(),
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM workspace_connections
                    WHERE id = 'ordinary-connection'
                      AND attachmentId = 'ordinary-attachment'
                    """.trimIndent(),
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM workspace_backlog_entries
                    WHERE id = 'ordinary-backlog-entry'
                      AND targetKind = 'NOTE_DOCUMENT'
                      AND targetId = 'ordinary-doc'
                    """.trimIndent(),
                ),
            )

            // Non-retired canonical capability ownership survives.
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM workspace_capability_instances
                    WHERE id = 'connections-cap'
                      AND capabilityType = 'CONNECTIONS'
                    """.trimIndent(),
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*) FROM workspace_capability_instances
                    WHERE id = 'backlog-cap'
                      AND capabilityType = 'BACKLOG'
                    """.trimIndent(),
                ),
            )

            db.query("PRAGMA foreign_key_check").use { cursor ->
                assertEquals(0, cursor.count)
            }
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun createFixture(
        dbName: String,
        version: Int,
    ) {
        context.deleteDatabase(dbName)

        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(version) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchema(db, version)

                            insertMinimalRow(
                                db,
                                "contexts",
                                mapOf(
                                    "id" to "context-1",
                                    "name" to "Context",
                                    "createdAt" to 1L,
                                    "updatedAt" to 2L,
                                    "scoring_status" to "CURRENT",
                                ),
                            )

                            insertMinimalRow(
                                db,
                                "workspaces",
                                mapOf(
                                    "id" to "workspace-1",
                                    "nameOverride" to "Workspace",
                                    "workspaceOrder" to 0L,
                                    "createdAt" to 1L,
                                    "updatedAt" to 2L,
                                    "isDeleted" to false,
                                    "version" to 1L,
                                    "provenance" to "CONTEXT_BACKED",
                                    "sourceContextId" to "context-1",
                                ),
                            )

                            insertCapability(
                                db = db,
                                id = "connections-cap",
                                type = "CONNECTIONS",
                                order = 0L,
                            )
                            insertCapability(
                                db = db,
                                id = "backlog-cap",
                                type = "BACKLOG",
                                order = 1L,
                            )
                            insertCapability(
                                db = db,
                                id = "artifact-cap",
                                type = "ARTIFACT",
                                order = 2L,
                            )
                            insertCapability(
                                db = db,
                                id = "journal-cap",
                                type = "JOURNAL",
                                order = 3L,
                            )
                            insertCapability(
                                db = db,
                                id = "journal-log-cap",
                                type = "JOURNAL_LOG",
                                order = 4L,
                            )

                            insertMinimalRow(
                                db,
                                "structure_presets",
                                mapOf(
                                    "id" to "preset-1",
                                    "code" to "custom",
                                    "label" to "Custom preset",
                                    "description" to "kept description",
                                    "enable_inbox" to true,
                                    "enable_log" to false,
                                    "enable_artifact" to true,
                                    "enable_advanced" to true,
                                    "enable_dashboard" to true,
                                    "enable_backlog" to true,
                                    "enable_attachments" to true,
                                    "enable_auto_link_subprojects" to false,
                                    "createdAt" to 10L,
                                    "updatedAt" to 20L,
                                    "version" to 7L,
                                    "isDeleted" to false,
                                ),
                            )

                            insertMinimalRow(
                                db,
                                "context_structures",
                                mapOf(
                                    "id" to "context-structure-1",
                                    "contextId" to "context-1",
                                    "base_preset_code" to "custom",
                                    "experimental_capability_ids" to """["future-capability"]""",
                                    "apply_mode" to "OVERRIDE",
                                    "enable_inbox" to true,
                                    "enable_log" to false,
                                    "enable_artifact" to true,
                                    "enable_advanced" to true,
                                    "enable_dashboard" to true,
                                    "enable_backlog" to true,
                                    "enable_attachments" to true,
                                    "enable_auto_link_subprojects" to false,
                                    "remove_inbox_entry_after_tag_autocopy" to true,
                                    "remove_backlog_entry_after_tag_autocopy" to false,
                                    "updatedAt" to 30L,
                                    "version" to 8L,
                                    "isDeleted" to false,
                                ),
                            )

                            insertMinimalRow(
                                db,
                                "context_artifacts",
                                mapOf(
                                    "id" to "legacy-context-artifact",
                                    "contextId" to "context-1",
                                    "content" to "delete me",
                                    "createdAt" to 1L,
                                    "updatedAt" to 2L,
                                ),
                            )

                            insertDocument(
                                db = db,
                                id = "ordinary-doc",
                                name = "Ordinary document",
                                content = "ordinary payload survives",
                                updatedAt = 200L,
                                version = 9L,
                            )
                            insertDocument(
                                db = db,
                                id = "journal-doc",
                                name = "Retired journal",
                                content = "delete journal",
                            )
                            insertDocument(
                                db = db,
                                id = "system_journal_log_context-1",
                                name = "Retired system journal",
                                content = "delete system journal",
                            )
                            insertDocument(
                                db = db,
                                id = "RETIRED_ARTIFACT_DOCUMENT:artifact-legacy",
                                name = "Retired artifact materialization",
                                content = "delete artifact materialization",
                            )

                            insertAttachment(
                                db = db,
                                id = "ordinary-attachment",
                                type = "NOTE_DOCUMENT",
                                entityId = "ordinary-doc",
                            )
                            insertAttachment(
                                db = db,
                                id = "journal-attachment",
                                type = "JOURNAL_DOCUMENT",
                                entityId = "journal-doc",
                            )
                            insertAttachment(
                                db = db,
                                id = "system-journal-attachment",
                                type = "NOTE_DOCUMENT",
                                entityId = "system_journal_log_context-1",
                            )
                            insertAttachment(
                                db = db,
                                id = "RETIRED_ARTIFACT_ATTACHMENT:artifact-legacy",
                                type = "NOTE_DOCUMENT",
                                entityId = "RETIRED_ARTIFACT_DOCUMENT:artifact-legacy",
                            )

                            insertConnection(
                                db = db,
                                id = "ordinary-connection",
                                attachmentId = "ordinary-attachment",
                                order = 0L,
                            )
                            insertConnection(
                                db = db,
                                id = "journal-connection",
                                attachmentId = "journal-attachment",
                                order = 1L,
                            )
                            insertConnection(
                                db = db,
                                id = "artifact-connection",
                                attachmentId = "RETIRED_ARTIFACT_ATTACHMENT:artifact-legacy",
                                order = 2L,
                            )

                            insertMinimalRow(
                                db,
                                "workspace_backlog_entries",
                                mapOf(
                                    "id" to "ordinary-backlog-entry",
                                    "workspaceId" to "workspace-1",
                                    "capabilityInstanceId" to "backlog-cap",
                                    "targetKind" to "NOTE_DOCUMENT",
                                    "targetId" to "ordinary-doc",
                                    "entryOrder" to 0L,
                                    "createdAt" to 1L,
                                    "updatedAt" to 2L,
                                    "isDeleted" to false,
                                    "version" to 1L,
                                ),
                            )
                            insertMinimalRow(
                                db,
                                "workspace_backlog_entries",
                                mapOf(
                                    "id" to "journal-backlog-entry",
                                    "workspaceId" to "workspace-1",
                                    "capabilityInstanceId" to "backlog-cap",
                                    "targetKind" to "JOURNAL_DOCUMENT",
                                    "targetId" to "journal-doc",
                                    "entryOrder" to 1L,
                                    "createdAt" to 1L,
                                    "updatedAt" to 2L,
                                    "isDeleted" to false,
                                    "version" to 1L,
                                ),
                            )
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = error("Unexpected fixture upgrade $oldVersion->$newVersion")
                    },
                ).build()

        FrameworkSQLiteOpenHelperFactory().create(configuration).use { helper ->
            helper.writableDatabase
        }
    }

    private fun insertCapability(
        db: SupportSQLiteDatabase,
        id: String,
        type: String,
        order: Long,
    ) {
        insertMinimalRow(
            db,
            "workspace_capability_instances",
            mapOf(
                "id" to id,
                "workspaceId" to "workspace-1",
                "capabilityType" to type,
                "instanceKey" to "default",
                "capabilityOrder" to order,
                "state" to "ACTIVE",
                "configurationVersion" to 1L,
                "configuration" to "{}",
                "createdAt" to 1L,
                "updatedAt" to 2L,
                "isDeleted" to false,
                "version" to 1L,
            ),
        )
    }

    private fun insertDocument(
        db: SupportSQLiteDatabase,
        id: String,
        name: String,
        content: String,
        updatedAt: Long = 2L,
        version: Long = 1L,
    ) {
        insertMinimalRow(
            db,
            "note_documents",
            mapOf(
                "id" to id,
                "contextId" to "context-1",
                "name" to name,
                "createdAt" to 1L,
                "updatedAt" to updatedAt,
                "content" to content,
                "isDeleted" to false,
                "version" to version,
            ),
        )
    }

    private fun insertAttachment(
        db: SupportSQLiteDatabase,
        id: String,
        type: String,
        entityId: String,
    ) {
        insertMinimalRow(
            db,
            "attachments",
            mapOf(
                "id" to id,
                "attachment_type" to type,
                "entity_id" to entityId,
                "owner_context_id" to "context-1",
                "role_code" to null,
                "is_system" to false,
                "createdAt" to 1L,
                "updatedAt" to 2L,
                "isDeleted" to false,
                "version" to 1L,
            ),
        )
    }

    private fun insertConnection(
        db: SupportSQLiteDatabase,
        id: String,
        attachmentId: String,
        order: Long,
    ) {
        insertMinimalRow(
            db,
            "workspace_connections",
            mapOf(
                "id" to id,
                "workspaceId" to "workspace-1",
                "capabilityInstanceId" to "connections-cap",
                "attachmentId" to attachmentId,
                "connectionOrder" to order,
                "createdAt" to 1L,
                "updatedAt" to 2L,
                "isDeleted" to false,
                "version" to 1L,
            ),
        )
    }

    private fun createSchema(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val databaseJson =
            schemaFile(version)
                .reader()
                .use {
                    JsonParser
                        .parseReader(it)
                        .asJsonObject
                        .getAsJsonObject("database")
                }

        val entities =
            databaseJson
                .getAsJsonArray("entities")
                .map { it.asJsonObject }

        entities
            .filterNot { it.has("ftsVersion") }
            .forEach { entity ->
                createEntity(
                    db,
                    entity.get("tableName").asString,
                    entity.get("createSql").asString,
                )
            }

        entities
            .filter { it.has("ftsVersion") }
            .forEach { entity ->
                createEntity(
                    db,
                    entity.get("tableName").asString,
                    entity.get("createSql").asString,
                )
            }

        entities.forEach { entity ->
            val tableName = entity.get("tableName").asString

            entity.get("indices")?.asJsonArray?.forEach { index ->
                createEntity(
                    db,
                    tableName,
                    index.asJsonObject.get("createSql").asString,
                )
            }

            entity.get("contentSyncTriggers")?.asJsonArray?.forEach { trigger ->
                db.execSQL(trigger.asString)
            }
        }

        databaseJson.get("views")?.asJsonArray?.forEach { view ->
            db.execSQL(view.asJsonObject.get("createSql").asString)
        }

        databaseJson.get("setupQueries")?.asJsonArray?.forEach { query ->
            db.execSQL(query.asString)
        }
    }

    private fun createEntity(
        db: SupportSQLiteDatabase,
        tableName: String,
        sql: String,
    ) {
        db.execSQL(sql.replace("\${TABLE_NAME}", tableName))
    }

    private fun insertMinimalRow(
        db: SupportSQLiteDatabase,
        table: String,
        overrides: Map<String, Any?>,
    ) {
        val values = ContentValues()
        val seen = mutableSetOf<String>()

        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIx = cursor.getColumnIndexOrThrow("name")
            val typeIx = cursor.getColumnIndexOrThrow("type")
            val notNullIx = cursor.getColumnIndexOrThrow("notnull")
            val defaultIx = cursor.getColumnIndexOrThrow("dflt_value")
            val pkIx = cursor.getColumnIndexOrThrow("pk")

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIx)
                seen += name

                if (overrides.containsKey(name)) {
                    putValue(values, name, overrides[name])
                    continue
                }

                val required =
                    cursor.getInt(notNullIx) != 0 &&
                        cursor.isNull(defaultIx) &&
                        cursor.getInt(pkIx) == 0

                if (!required) continue

                val type = cursor.getString(typeIx).uppercase()
                when {
                    "INT" in type -> values.put(name, 0L)
                    "REAL" in type || "FLOA" in type || "DOUB" in type ->
                        values.put(name, 0.0)
                    "BLOB" in type -> values.put(name, ByteArray(0))
                    else -> values.put(name, "")
                }
            }
        }

        assertTrue(
            "Unknown fixture columns in $table: ${overrides.keys - seen}",
            (overrides.keys - seen).isEmpty(),
        )

        val columns = values.keySet().toList()
        val quotedColumns =
            columns.joinToString(",") { column ->
                "`${column.replace("`", "``")}`"
            }
        val placeholders = List(columns.size) { "?" }.joinToString(",")
        val bindArgs =
            columns
                .map { column ->
                    when (val value = values.get(column)) {
                        is Boolean -> if (value) 1L else 0L
                        else -> value
                    }
                }.toTypedArray()

        db.execSQL(
            "INSERT OR ABORT INTO `${table.replace("`", "``")}` ($quotedColumns) VALUES ($placeholders)",
            bindArgs,
        )
    }

    private fun putValue(
        values: ContentValues,
        key: String,
        value: Any?,
    ) {
        when (value) {
            null -> values.putNull(key)
            is String -> values.put(key, value)
            is Int -> values.put(key, value)
            is Long -> values.put(key, value)
            is Boolean -> values.put(key, value)
            is Float -> values.put(key, value)
            is Double -> values.put(key, value)
            is ByteArray -> values.put(key, value)
            else -> error("Unsupported fixture value for $key: ${value::class.java.name}")
        }
    }

    private fun schemaFile(version: Int): File {
        val relative =
            "schemas/com.romankozak.forwardappmobile.database.AppDatabase/$version.json"
        val userDir = File(System.getProperty("user.dir"))

        return listOf(
            File(relative),
            File("app/$relative"),
            File(userDir, relative),
            File(userDir, "app/$relative"),
        ).firstOrNull { it.isFile }
            ?: error("Room schema $version not found. user.dir=${userDir.absolutePath}")
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'",
        ) != 0L

    private fun columnExists(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
    ): Boolean =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIx = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIx) == column) {
                    return@use true
                }
            }
            false
        }

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use { cursor ->
            assertTrue("Query returned no rows: $sql", cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use { cursor ->
            assertTrue("Query returned no rows: $sql", cursor.moveToFirst())
            cursor.getString(0)
        }
}
