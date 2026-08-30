package com.romankozak.forwardappmobile.data.database

import android.content.ContentValues
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class Migration155To156DirectionCutoverRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `155 to 156 cuts over every Direction row and removes legacy authority`() {
        val dbName = "migration_155_156_direction_cutover"
        createFixtureDatabase(
            dbName = dbName,
            brokenLinkedTarget = false,
        )

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_155_156)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(156L, scalarLong(db, "PRAGMA user_version"))
            assertFalse(tableExists(db, "direction_items"))

            // One canonical placement exists for every legacy Direction row,
            // including the tombstone.
            assertEquals(
                3L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM workspace_direction_entries
                    WHERE provenance = 'LEGACY_DIRECTION_ITEM'
                    """.trimIndent(),
                ),
            )

            assertSemanticEntry(
                db = db,
                id = "semantic-live",
                expectedOrder = 2L,
                expectedVersion = 7L,
                expectedDeleted = false,
            )
            assertLinkedEntry(db)
            assertSemanticEntry(
                db = db,
                id = "semantic-deleted",
                expectedOrder = 7L,
                expectedVersion = 9L,
                expectedDeleted = true,
            )

            // Semantic legacy rows own deterministic canonical Orientation
            // mappings. Linked navigation rows do not infer semantic intent.
            assertMapping(
                db = db,
                sourceId = "semantic-live",
                expectedDeleted = false,
            )
            assertMapping(
                db = db,
                sourceId = "semantic-deleted",
                expectedDeleted = true,
            )
            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM legacy_subject_mappings
                    WHERE sourceType = 'DIRECTION'
                      AND sourceId = 'workspace-link'
                    """.trimIndent(),
                ),
            )

            // All live Contexts become usable Workspace owners after the hard
            // cutover, even when they had no legacy Direction rows.
            assertEquals(
                3L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM workspaces
                    WHERE id IN ('owner', 'target', 'empty-owner')
                      AND isDeleted = 0
                    """.trimIndent(),
                ),
            )
            assertEquals(
                3L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM workspace_capability_instances
                    WHERE workspaceId IN ('owner', 'target', 'empty-owner')
                      AND capabilityType = 'DIRECTION'
                      AND instanceKey = 'default'
                      AND state = 'ACTIVE'
                      AND isDeleted = 0
                    """.trimIndent(),
                ),
            )

            db.query(
                """
                SELECT configurationVersion, configuration
                FROM workspace_capability_instances
                WHERE workspaceId = 'empty-owner'
                  AND capabilityType = 'DIRECTION'
                  AND instanceKey = 'default'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
                assertEquals(
                    """{"autoLinkChildWorkspaces":true}""",
                    cursor.getString(1),
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

    @Test
    fun `155 to 156 rolls back completely when linked target Context is missing`() {
        val dbName = "migration_155_156_direction_fail_closed"
        createFixtureDatabase(
            dbName = dbName,
            brokenLinkedTarget = true,
        )

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_155_156)
                .allowMainThreadQueries()
                .build()

        val failure =
            try {
                runCatching {
                    room.openHelper.writableDatabase
                }.exceptionOrNull()
            } finally {
                room.close()
            }

        assertNotNull(failure)

        // Room wraps Migration.migrate() in a transaction. Verify that the
        // failed cutover did not advance schema version or remove legacy
        // authority.
        inspectSchema155Database(dbName) { db ->
            assertEquals(155L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "direction_items"))
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM direction_items
                    WHERE id = 'broken-link'
                      AND linked_context_id = 'missing-target'
                    """.trimIndent(),
                ),
            )

            // Any canonical provisioning attempted before the failure must
            // have rolled back with the migration transaction.
            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM workspace_direction_entries
                    WHERE id = 'broken-link'
                    """.trimIndent(),
                ),
            )
        }

        context.deleteDatabase(dbName)
    }

    private fun assertSemanticEntry(
        db: SupportSQLiteDatabase,
        id: String,
        expectedOrder: Long,
        expectedVersion: Long,
        expectedDeleted: Boolean,
    ) {
        db.query(
            """
            SELECT
                workspaceId,
                capabilityInstanceId,
                orientationId,
                targetWorkspaceId,
                labelOverride,
                entryOrder,
                provenance,
                syncedAt,
                isDeleted,
                version
            FROM workspace_direction_entries
            WHERE id = ?
            """.trimIndent(),
            arrayOf(id),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("owner", cursor.getString(0))
            assertTrue(cursor.getString(1).isNotBlank())
            assertFalse(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.isNull(4))
            assertEquals(expectedOrder, cursor.getLong(5))
            assertEquals("LEGACY_DIRECTION_ITEM", cursor.getString(6))
            assertTrue(cursor.isNull(7))
            assertEquals(expectedDeleted, cursor.getInt(8) != 0)
            assertEquals(expectedVersion, cursor.getLong(9))

            val orientationId = cursor.getString(2)
            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM managed_subjects AS subject
                    JOIN orientations AS orientation
                      ON orientation.subjectId = subject.id
                    WHERE subject.id = '${sqlLiteral(orientationId)}'
                      AND subject.subjectType = 'ORIENTATION'
                      AND orientation.kind = 'DIRECTION'
                      AND subject.isDeleted = ${if (expectedDeleted) 1 else 0}
                    """.trimIndent(),
                ),
            )
        }
    }

    private fun assertLinkedEntry(db: SupportSQLiteDatabase) {
        db.query(
            """
            SELECT
                workspaceId,
                capabilityInstanceId,
                orientationId,
                targetWorkspaceId,
                labelOverride,
                entryOrder,
                provenance,
                syncedAt,
                isDeleted,
                version
            FROM workspace_direction_entries
            WHERE id = 'workspace-link'
            """.trimIndent(),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("owner", cursor.getString(0))
            assertTrue(cursor.getString(1).isNotBlank())
            assertTrue(cursor.isNull(2))
            assertEquals("target", cursor.getString(3))
            assertEquals("Target label", cursor.getString(4))
            assertEquals(5L, cursor.getLong(5))
            assertEquals("LEGACY_DIRECTION_ITEM", cursor.getString(6))
            assertTrue(cursor.isNull(7))
            assertFalse(cursor.getInt(8) != 0)
            assertEquals(8L, cursor.getLong(9))
        }
    }

    private fun assertMapping(
        db: SupportSQLiteDatabase,
        sourceId: String,
        expectedDeleted: Boolean,
    ) {
        db.query(
            """
            SELECT
                mapping.subjectId,
                mapping.migrationVersion,
                mapping.state,
                mapping.isDeleted,
                subject.isDeleted,
                orientation.kind
            FROM legacy_subject_mappings AS mapping
            JOIN managed_subjects AS subject
              ON subject.id = mapping.subjectId
            JOIN orientations AS orientation
              ON orientation.subjectId = mapping.subjectId
            WHERE mapping.sourceType = 'DIRECTION'
              AND mapping.sourceId = ?
            """.trimIndent(),
            arrayOf(sourceId),
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getString(0).isNotBlank())
            assertEquals(4, cursor.getInt(1))
            assertEquals("CUT_OVER", cursor.getString(2))
            assertEquals(expectedDeleted, cursor.getInt(3) != 0)
            assertEquals(expectedDeleted, cursor.getInt(4) != 0)
            assertEquals("DIRECTION", cursor.getString(5))
            assertFalse(cursor.moveToNext())
        }
    }

    private fun createFixtureDatabase(
        dbName: String,
        brokenLinkedTarget: Boolean,
    ) {
        context.deleteDatabase(dbName)

        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(155) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchemaFromJson(db, 155)

                            insertContext(db, "owner", "Owner")
                            if (!brokenLinkedTarget) {
                                insertContext(db, "target", "Target")
                                insertContext(db, "empty-owner", "Empty owner")
                            }

                            if (brokenLinkedTarget) {
                                insertDirection(
                                    db = db,
                                    id = "broken-link",
                                    contextId = "owner",
                                    text = "Broken target",
                                    linkedContextId = "missing-target",
                                    order = 1,
                                    updatedAt = 100L,
                                    isDeleted = false,
                                    version = 1L,
                                )
                            } else {
                                insertDirection(
                                    db = db,
                                    id = "semantic-live",
                                    contextId = "owner",
                                    text = "Semantic live",
                                    linkedContextId = null,
                                    order = 2,
                                    updatedAt = 100L,
                                    isDeleted = false,
                                    version = 7L,
                                )
                                insertDirection(
                                    db = db,
                                    id = "workspace-link",
                                    contextId = "owner",
                                    text = "Target label",
                                    linkedContextId = "target",
                                    order = 5,
                                    updatedAt = 110L,
                                    isDeleted = false,
                                    version = 8L,
                                )
                                insertDirection(
                                    db = db,
                                    id = "semantic-deleted",
                                    contextId = "owner",
                                    text = "Deleted semantic",
                                    linkedContextId = null,
                                    order = 7,
                                    updatedAt = 120L,
                                    isDeleted = true,
                                    version = 9L,
                                )
                            }
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = error("Unexpected fixture upgrade $oldVersion->$newVersion")
                    },
                ).build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        try {
            helper.writableDatabase
        } finally {
            helper.close()
        }
    }

    private fun insertContext(
        db: SupportSQLiteDatabase,
        id: String,
        name: String,
    ) {
        insertMinimalRow(
            db,
            "contexts",
            mapOf(
                "id" to id,
                "name" to name,
                "createdAt" to 10L,
                "updatedAt" to 20L,
                "is_deleted" to false,
                "version" to 3L,
                "scoring_status" to "UNSET",
            ),
        )
    }

    private fun insertDirection(
        db: SupportSQLiteDatabase,
        id: String,
        contextId: String,
        text: String,
        linkedContextId: String?,
        order: Int,
        updatedAt: Long,
        isDeleted: Boolean,
        version: Long,
    ) {
        insertMinimalRow(
            db,
            "direction_items",
            mapOf(
                "id" to id,
                "contextId" to contextId,
                "text" to text,
                "linked_context_id" to linkedContextId,
                "itemOrder" to order,
                "updatedAt" to updatedAt,
                "synced_at" to updatedAt - 1L,
                "is_deleted" to isDeleted,
                "version" to version,
            ),
        )
    }

    private fun inspectSchema155Database(
        dbName: String,
        block: (SupportSQLiteDatabase) -> Unit,
    ) {
        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(155) {
                        override fun onCreate(db: SupportSQLiteDatabase) =
                            error("Expected existing schema-155 database")

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = error("Unexpected inspection upgrade $oldVersion->$newVersion")
                    },
                ).build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        try {
            block(helper.writableDatabase)
        } finally {
            helper.close()
        }
    }

    private fun createSchemaFromJson(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val databaseJson =
            schemaFile(version)
                .reader()
                .use {
                    JsonParser.parseReader(it)
                        .asJsonObject
                        .getAsJsonObject("database")
                }

        val entities =
            databaseJson.getAsJsonArray("entities")
                .map { it.asJsonObject }

        entities
            .filterNot { it.has("ftsVersion") }
            .forEach {
                createEntity(
                    db,
                    it.get("tableName").asString,
                    it.get("createSql").asString,
                )
            }

        entities
            .filter { it.has("ftsVersion") }
            .forEach {
                createEntity(
                    db,
                    it.get("tableName").asString,
                    it.get("createSql").asString,
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

        databaseJson.get("views")?.asJsonArray?.forEach {
            db.execSQL(it.asJsonObject.get("createSql").asString)
        }

        databaseJson.get("setupQueries")?.asJsonArray?.forEach {
            db.execSQL(it.asString)
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
        val quoted =
            columns.joinToString(",") { column ->
                "`${column.replace("`", "``")}`"
            }
        val placeholders = List(columns.size) { "?" }.joinToString(",")
        val bindArgs =
            columns.map { column ->
                when (val value = values.get(column)) {
                    is Boolean -> if (value) 1L else 0L
                    else -> value
                }
            }.toTypedArray()

        db.execSQL(
            "INSERT OR ABORT INTO `${table.replace("`", "``")}` ($quoted) VALUES ($placeholders)",
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
            ?: error("Room schema $version not found")
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        scalarLong(
            db,
            """
            SELECT COUNT(*)
            FROM sqlite_master
            WHERE type = 'table'
              AND name = '${sqlLiteral(table)}'
            """.trimIndent(),
        ) == 1L

    private fun scalarLong(
        db: SupportSQLiteDatabase,
        sql: String,
    ): Long =
        db.query(sql).use {
            assertTrue(it.moveToFirst())
            it.getLong(0)
        }

    private fun scalarString(
        db: SupportSQLiteDatabase,
        sql: String,
    ): String =
        db.query(sql).use {
            assertTrue(it.moveToFirst())
            it.getString(0)
        }

    private fun sqlLiteral(value: String): String =
        value.replace("'", "''")
}
