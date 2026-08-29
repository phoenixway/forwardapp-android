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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class Migration151To154ExecutionLogOwnershipBridgeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun migrate151To155_preservesExecutionLogOwnershipAndAddsDirectionEntryFoundation() {
        val dbName = "migration_151_154_execution_log_bridge"
        createFixtureDatabase(dbName)

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(
                    MIGRATION_151_152,
                    MIGRATION_152_153,
                    MIGRATION_153_154,
                    MIGRATION_154_155,
                )
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(155L, scalarLong(db, "PRAGMA user_version"))

            db.query(
                """
                SELECT contextId, workspaceId
                FROM context_execution_logs
                WHERE id = 'legacy-log'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("context-1", cursor.getString(0))
                assertTrue(cursor.isNull(1))
            }

            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM pragma_index_list('context_execution_logs')
                    WHERE name = 'index_context_execution_logs_workspaceId'
                    """.trimIndent(),
                ),
            )

            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT `notnull`
                    FROM pragma_table_info('context_execution_logs')
                    WHERE name = 'contextId'
                    """.trimIndent(),
                ),
            )

            db.execSQL(
                """
                INSERT INTO context_execution_logs (
                    id, contextId, timestamp, type, description,
                    details, updatedAt, synced_at, is_deleted,
                    version, workspaceId
                )
                VALUES (?, NULL, ?, ?, ?, NULL, ?, NULL, 0, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "canonical-only-log",
                    200L,
                    "COMMENT",
                    "canonical-only",
                    200L,
                    1L,
                    "workspace-canonical",
                ),
            )

            db.query(
                """
                SELECT contextId, workspaceId
                FROM context_execution_logs
                WHERE id = 'canonical-only-log'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.isNull(0))
                assertEquals("workspace-canonical", cursor.getString(1))
            }


            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM sqlite_master
                    WHERE type = 'table'
                      AND name = 'workspace_direction_entries'
                    """.trimIndent(),
                ),
            )

            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM pragma_index_list('workspace_direction_entries')
                    WHERE name = 'index_workspace_direction_entries_provenance'
                    """.trimIndent(),
                ),
            )

            assertEquals(
                0L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM workspace_direction_entries",
                ),
            )

            assertEquals(
                1L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM sqlite_master
                    WHERE type = 'table'
                      AND name = 'workspace_direction_entry_issues'
                    """.trimIndent(),
                ),
            )

            assertEquals(
                0L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM workspace_direction_entry_issues",
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

    private fun createFixtureDatabase(dbName: String) {
        context.deleteDatabase(dbName)

        val configuration =
            SupportSQLiteOpenHelper.Configuration
                .builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(151) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchemaFromJson(db, 151)

                            insertMinimalRow(
                                db,
                                "contexts",
                                mapOf(
                                    "id" to "context-1",
                                    "name" to "Legacy context",
                                    "createdAt" to 10L,
                                ),
                            )

                            insertMinimalRow(
                                db,
                                "context_execution_logs",
                                mapOf(
                                    "id" to "legacy-log",
                                    "contextId" to "context-1",
                                    "timestamp" to 100L,
                                    "type" to "COMMENT",
                                    "description" to "legacy",
                                    "updatedAt" to 100L,
                                    "synced_at" to null,
                                    "is_deleted" to false,
                                    "version" to 3L,
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

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)

        try {
            helper.writableDatabase
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
}
