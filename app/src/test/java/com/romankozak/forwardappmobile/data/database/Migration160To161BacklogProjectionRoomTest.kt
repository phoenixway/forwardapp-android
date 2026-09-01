package com.romankozak.forwardappmobile.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration160To161BacklogProjectionRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `159 through 161 separates hashtag projection and preserves external identity`() {
        val dbName = "migration_159_161_backlog_projection"
        createFixture(dbName)

        val helper = openHistorical161(dbName)

        try {
            val db = helper.writableDatabase
            val projectionId = "goal_association:goal-1:target-context"

            assertEquals(161L, scalarLong(db, "PRAGMA user_version"))
            assertTrue(tableExists(db, "workspace_backlog_entries"))
            assertTrue(tableExists(db, "backlog_goal_association_links"))

            // Stage 1 foundation remains schema-only for Context-backed data.
            assertEquals(
                0L,
                scalarLong(db, "SELECT COUNT(*) FROM workspace_backlog_entries"),
            )

            db.query(
                """
                SELECT
                    projection_id,
                    goal_id,
                    context_id,
                    owner_context_id,
                    association_tag,
                    item_order,
                    linked_at
                FROM backlog_goal_association_links
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(projectionId, cursor.getString(0))
                assertEquals("goal-1", cursor.getString(1))
                assertEquals("target-context", cursor.getString(2))
                assertEquals("owner-context", cursor.getString(3))
                assertEquals("#target", cursor.getString(4))
                assertEquals(20L, cursor.getLong(5))
                assertEquals(250L, cursor.getLong(6))
                assertFalse(cursor.moveToNext())
            }

            // A persisted reference to the old derived list_items row follows
            // the appearance into deterministic projection identity.
            assertEquals(
                projectionId,
                scalarString(
                    db,
                    "SELECT source_backlog_item_id FROM tactical_missions WHERE id = 1",
                ),
            )

            // A real explicit placement reference must remain untouched.
            assertEquals(
                "explicit-placement",
                scalarString(
                    db,
                    "SELECT source_backlog_item_id FROM tactical_missions WHERE id = 2",
                ),
            )

            // Stage 3 deliberately keeps legacy rows for frozen migration
            // accounting. They are no longer runtime projection authority.
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM list_items WHERE id = 'legacy-derived-placement'",
                ),
            )
            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM list_items WHERE id = 'explicit-placement'",
                ),
            )

            // Explicit owner placement is not copied into the projection cache.
            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM backlog_goal_association_links
                    WHERE goal_id = 'goal-1'
                      AND context_id = 'owner-context'
                    """.trimIndent(),
                ),
            )

            db.query("PRAGMA foreign_key_check").use {
                assertEquals(0, it.count)
            }
            assertEquals("ok", scalarString(db, "PRAGMA integrity_check"))
        } finally {
            helper.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun openHistorical161(dbName: String): SupportSQLiteOpenHelper {
        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(161) {
                        override fun onCreate(db: SupportSQLiteDatabase) =
                            error("Historical fixture must already exist at schema 159")

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {
                            check(oldVersion == 159 && newVersion == 161) {
                                "Unexpected historical Backlog migration $oldVersion->$newVersion"
                            }
                            MIGRATION_159_160.migrate(db)
                            MIGRATION_160_161.migrate(db)
                        }
                    },
                ).build()

        return FrameworkSQLiteOpenHelperFactory().create(configuration)
    }

    private fun createFixture(dbName: String) {
        context.deleteDatabase(dbName)

        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(159) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchema(db, 159)
                            populateFixture(db)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()

        FrameworkSQLiteOpenHelperFactory()
            .create(configuration)
            .use { it.writableDatabase }
    }

    private fun populateFixture(db: SupportSQLiteDatabase) {
        insertSchemaAwareRow(
            db,
            "contexts",
            mapOf(
                "id" to "owner-context",
                "name" to "Owner",
                "is_deleted" to 0L,
            ),
        )
        insertSchemaAwareRow(
            db,
            "contexts",
            mapOf(
                "id" to "target-context",
                "name" to "Target",
                "is_deleted" to 0L,
            ),
        )
        insertSchemaAwareRow(
            db,
            "goals",
            mapOf(
                "id" to "goal-1",
                "text" to "Goal #target",
                "createdAt" to 100L,
                "updatedAt" to 200L,
                "is_deleted" to 0L,
            ),
        )

        insertSchemaAwareRow(
            db,
            "list_items",
            mapOf(
                "id" to "explicit-placement",
                "context_id" to "owner-context",
                "itemType" to "GOAL",
                "entityId" to "goal-1",
                "association_owner_context_id" to null,
                "association_tag" to null,
                "item_order" to 10L,
                "updatedAt" to 200L,
                "is_deleted" to 0L,
                "version" to 1L,
            ),
        )
        insertSchemaAwareRow(
            db,
            "list_items",
            mapOf(
                "id" to "legacy-derived-placement",
                "context_id" to "target-context",
                "itemType" to "GOAL",
                "entityId" to "goal-1",
                "association_owner_context_id" to "owner-context",
                "association_tag" to "#target",
                "item_order" to 20L,
                "updatedAt" to 250L,
                "is_deleted" to 0L,
                "version" to 1L,
            ),
        )

        insertSchemaAwareRow(
            db,
            "tactical_missions",
            mapOf(
                "id" to 1L,
                "title" to "Derived source",
                "source_backlog_item_id" to "legacy-derived-placement",
                "is_deleted" to 0L,
            ),
        )
        insertSchemaAwareRow(
            db,
            "tactical_missions",
            mapOf(
                "id" to 2L,
                "title" to "Explicit source",
                "source_backlog_item_id" to "explicit-placement",
                "is_deleted" to 0L,
            ),
        )
    }

    private fun insertSchemaAwareRow(
        db: SupportSQLiteDatabase,
        table: String,
        overrides: Map<String, Any?>,
    ) {
        val values = ContentValues()
        overrides.forEach { (name, value) -> putValue(values, name, value) }

        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            val notNullIndex = cursor.getColumnIndexOrThrow("notnull")
            val defaultIndex = cursor.getColumnIndexOrThrow("dflt_value")

            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIndex)
                val type = cursor.getString(typeIndex).uppercase()
                val notNull = cursor.getInt(notNullIndex) != 0
                val hasDefault = !cursor.isNull(defaultIndex)

                if (!notNull || hasDefault || values.containsKey(name)) continue

                when {
                    "INT" in type -> values.put(name, 0L)
                    "REAL" in type || "FLOA" in type || "DOUB" in type ->
                        values.put(name, 0.0)
                    "BLOB" in type -> values.put(name, byteArrayOf())
                    else -> values.put(name, "")
                }
            }
        }

        val result =
            db.insert(
                table,
                SQLiteDatabase.CONFLICT_ABORT,
                values,
            )

        assertTrue("Failed fixture insert into $table", result != -1L)
    }

    private fun putValue(
        values: ContentValues,
        name: String,
        value: Any?,
    ) {
        when (value) {
            null -> values.putNull(name)
            is String -> values.put(name, value)
            is Long -> values.put(name, value)
            is Int -> values.put(name, value)
            is Boolean -> values.put(name, value)
            is Double -> values.put(name, value)
            is Float -> values.put(name, value)
            is ByteArray -> values.put(name, value)
            else -> error("Unsupported fixture value for $name")
        }
    }

    private fun createSchema(
        db: SupportSQLiteDatabase,
        version: Int,
    ) {
        val database =
            schemaFile(version).reader().use {
                JsonParser.parseReader(it).asJsonObject.getAsJsonObject("database")
            }

        database.getAsJsonArray("entities").forEach { element ->
            val entity = element.asJsonObject
            val table = entity.get("tableName").asString

            db.execSQL(
                entity.get("createSql")
                    .asString
                    .replace("\${TABLE_NAME}", table),
            )

            entity.getAsJsonArray("indices")?.forEach { index ->
                db.execSQL(
                    index.asJsonObject.get("createSql")
                        .asString
                        .replace("\${TABLE_NAME}", table),
                )
            }
        }

        database.getAsJsonArray("views")?.forEach { view ->
            db.execSQL(view.asJsonObject.get("createSql").asString)
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
            ?: error("Room schema $version not found from ${userDir.absolutePath}")
    }

    private fun tableExists(
        db: SupportSQLiteDatabase,
        table: String,
    ): Boolean =
        scalarLong(
            db,
            "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'",
        ) == 1L

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
