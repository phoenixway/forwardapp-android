package com.romankozak.forwardappmobile.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.database.AppDatabase
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration161To162BacklogCutoverRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `161 to 162 atomically materializes canonical Backlog and preserves legacy evidence`() {
        val dbName = "migration_161_162_backlog_cutover_success"
        createFixture(
            dbName = dbName,
            itemType = "CHECKLIST",
            entityId = "checklist-1",
            withChecklistTarget = true,
        )

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_161_162)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase

            assertEquals(162L, scalarLong(db, "PRAGMA user_version"))

            assertEquals(
                1L,
                scalarLong(
                    db,
                    "SELECT COUNT(*) FROM workspace_backlog_entries WHERE id='placement-1'",
                ),
            )

            db.query(
                """
                SELECT
                    workspaceId,
                    capabilityInstanceId,
                    targetKind,
                    targetId,
                    entryOrder,
                    createdAt,
                    updatedAt,
                    isDeleted,
                    version
                FROM workspace_backlog_entries
                WHERE id='placement-1'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("owner", cursor.getString(0))
                val capabilityId = cursor.getString(1)
                assertTrue(capabilityId.isNotBlank())
                assertEquals("CHECKLIST", cursor.getString(2))
                assertEquals("checklist-1", cursor.getString(3))
                assertEquals(0L, cursor.getLong(4))
                assertEquals(0L, cursor.getLong(5))
                assertEquals(30L, cursor.getLong(6))
                assertEquals(0, cursor.getInt(7))
                assertEquals(7L, cursor.getLong(8))

                db.query(
                    """
                    SELECT
                        workspaceId,
                        capabilityType,
                        instanceKey,
                        state,
                        configurationVersion,
                        configuration,
                        isDeleted,
                        version
                    FROM workspace_capability_instances
                    WHERE id=?
                    """.trimIndent(),
                    arrayOf(capabilityId),
                ).use { capability ->
                    assertTrue(capability.moveToFirst())
                    assertEquals("owner", capability.getString(0))
                    assertEquals("BACKLOG", capability.getString(1))
                    assertEquals("default", capability.getString(2))
                    assertEquals("DISABLED", capability.getString(3))
                    assertEquals(1, capability.getInt(4))
                    assertEquals("{}", capability.getString(5))
                    assertEquals(0, capability.getInt(6))
                    assertEquals(1L, capability.getLong(7))
                }
            }

            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM list_items"))
            assertEquals(
                "placement-1",
                scalarString(db, "SELECT id FROM list_items LIMIT 1"),
            )
            assertEquals(
                50L,
                scalarLong(db, "SELECT item_order FROM list_items WHERE id='placement-1'"),
            )
            assertEquals(
                7L,
                scalarLong(db, "SELECT version FROM list_items WHERE id='placement-1'"),
            )

            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM backlog_orders"))
            assertEquals(
                "order-1",
                scalarString(db, "SELECT id FROM backlog_orders LIMIT 1"),
            )
            assertEquals(
                50L,
                scalarLong(db, "SELECT item_order FROM backlog_orders WHERE id='order-1'"),
            )

            assertTrue(tableExists(db, "list_items"))
            assertTrue(tableExists(db, "backlog_orders"))
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `161 to 162 preserves legacy Note as a distinct canonical target`() {
        val dbName = "migration_161_162_legacy_note"
        createFixture(
            dbName = dbName,
            itemType = "NOTE",
            entityId = "note-1",
            withChecklistTarget = false,
            withLegacyNoteTarget = true,
        )

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_161_162)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase
            assertEquals(162L, scalarLong(db, "PRAGMA user_version"))
            assertEquals(
                "LEGACY_NOTE",
                scalarString(
                    db,
                    "SELECT targetKind FROM workspace_backlog_entries WHERE id='placement-1'",
                ),
            )
            assertEquals(
                "note-1",
                scalarString(
                    db,
                    "SELECT targetId FROM workspace_backlog_entries WHERE id='placement-1'",
                ),
            )
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `161 to 162 unsupported legacy placement fails before canonical mutation and rolls back`() {
        val dbName = "migration_161_162_backlog_cutover_fail_closed"
        createFixture(
            dbName = dbName,
            itemType = "SCRIPT",
            entityId = "unsupported-script",
            withChecklistTarget = false,
        )

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_161_162)
                .allowMainThreadQueries()
                .build()

        val failure =
            try {
                runCatching { room.openHelper.writableDatabase }.exceptionOrNull()
            } finally {
                room.close()
            }

        assertNotNull(failure)
        val messages = generateSequence(failure) { it.cause }
            .mapNotNull { it.message }
            .joinToString("\n")
        assertTrue(messages.contains("BACKLOG cutover blocked"))
        assertTrue(messages.contains("UNSUPPORTED_ITEM_TYPE"))

        openExisting161(dbName).use { helper ->
            val db = helper.writableDatabase
            assertEquals(161L, scalarLong(db, "PRAGMA user_version"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM list_items"))
            assertEquals(
                0L,
                scalarLong(db, "SELECT COUNT(*) FROM workspace_backlog_entries"),
            )
            assertEquals(
                0L,
                scalarLong(
                    db,
                    """
                    SELECT COUNT(*)
                    FROM workspace_capability_instances
                    WHERE workspaceId='owner'
                      AND capabilityType='BACKLOG'
                      AND instanceKey='default'
                    """.trimIndent(),
                ),
            )
        }

        context.deleteDatabase(dbName)
    }

    private fun createFixture(
        dbName: String,
        itemType: String,
        entityId: String,
        withChecklistTarget: Boolean,
        withLegacyNoteTarget: Boolean = false,
    ) {
        context.deleteDatabase(dbName)

        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(161) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchema(db, 161)
                            populateFixture(
                                db = db,
                                itemType = itemType,
                                entityId = entityId,
                                withChecklistTarget = withChecklistTarget,
                                withLegacyNoteTarget = withLegacyNoteTarget,
                            )
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

    private fun populateFixture(
        db: SupportSQLiteDatabase,
        itemType: String,
        entityId: String,
        withChecklistTarget: Boolean,
        withLegacyNoteTarget: Boolean,
    ) {
        insertSchemaAwareRow(
            db,
            "contexts",
            mapOf(
                "id" to "owner",
                "name" to "Owner",
                "createdAt" to 1L,
                "updatedAt" to 1L,
                "is_deleted" to 0L,
                "version" to 1L,
            ),
        )

        insertSchemaAwareRow(
            db,
            "workspaces",
            mapOf(
                "id" to "owner",
                "nameOverride" to "Owner",
                "workspaceOrder" to 0L,
                "createdAt" to 1L,
                "updatedAt" to 1L,
                "isDeleted" to 0L,
                "version" to 1L,
                "provenance" to "CONTEXT_BACKED",
                "sourceContextId" to "owner",
            ),
        )

        if (withChecklistTarget) {
            insertSchemaAwareRow(
                db,
                "checklists",
                mapOf(
                    "id" to "checklist-1",
                    "contextId" to "owner",
                    "name" to "Checklist",
                    "createdAt" to 2L,
                    "updatedAt" to 2L,
                    "isDeleted" to 0L,
                    "version" to 1L,
                ),
            )
        }

        if (withLegacyNoteTarget) {
            insertSchemaAwareRow(
                db,
                "notes",
                mapOf(
                    "id" to "note-1",
                    "contextId" to "owner",
                    "title" to "Historical note",
                    "content" to "Preserved without conversion",
                    "createdAt" to 2L,
                    "updatedAt" to 2L,
                    "isDeleted" to 0L,
                    "version" to 1L,
                ),
            )
        }

        insertSchemaAwareRow(
            db,
            "list_items",
            mapOf(
                "id" to "placement-1",
                "context_id" to "owner",
                "itemType" to itemType,
                "entityId" to entityId,
                "association_owner_context_id" to null,
                "association_tag" to null,
                "item_order" to 50L,
                "updatedAt" to 30L,
                "synced_at" to 25L,
                "is_deleted" to 0L,
                "version" to 7L,
            ),
        )

        insertSchemaAwareRow(
            db,
            "backlog_orders",
            mapOf(
                "id" to "order-1",
                "list_id" to "owner",
                "item_id" to "placement-1",
                "item_order" to 50L,
                "order_version" to 3L,
                "updatedAt" to 31L,
                "synced_at" to 26L,
                "is_deleted" to 0L,
            ),
        )
    }

    private fun openExisting161(dbName: String): SupportSQLiteOpenHelper {
        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(161) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                ).build()

        return FrameworkSQLiteOpenHelperFactory().create(configuration)
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
