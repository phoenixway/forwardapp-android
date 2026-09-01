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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration162To163InboxSortingCutoverRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `162 to 163 materializes typed policy and clears legacy authority`() {
        val dbName = "migration_162_163_inbox_sorting_success"
        createFixture(dbName, "inbox:alpha\nattachments:type")

        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_162_163)
                .allowMainThreadQueries()
                .build()

        try {
            val db = room.openHelper.writableDatabase
            assertEquals(163L, scalarLong(db, "PRAGMA user_version"))
            db.query(
                """
                SELECT configurationVersion, configuration, updatedAt, syncedAt, state, isDeleted, version
                FROM workspace_capability_instances WHERE id = 'sorting-owner'
                """.trimIndent(),
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
                assertEquals(
                    "{\"rules\":[{\"target\":\"INBOX\",\"mode\":\"ALPHA\"}," +
                        "{\"target\":\"CONNECTIONS\",\"mode\":\"TYPE\"}]}",
                    cursor.getString(1),
                )
                assertEquals(20L, cursor.getLong(2))
                assertTrue(cursor.isNull(3))
                assertEquals("ACTIVE", cursor.getString(4))
                assertEquals(0, cursor.getInt(5))
                assertEquals(4L, cursor.getLong(6))
            }
            assertEquals(0L, scalarLong(db, "SELECT COUNT(*) FROM context_inbox_sorting"))
            assertTrue(tableExists(db, "context_inbox_sorting"))
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `162 to 163 malformed policy rolls back before mutation`() {
        val dbName = "migration_162_163_inbox_sorting_fail_closed"
        createFixture(dbName, "inbox:unsupported")
        val room =
            Room.databaseBuilder(context, AppDatabase::class.java, dbName)
                .addMigrations(MIGRATION_162_163)
                .allowMainThreadQueries()
                .build()

        val failure =
            try {
                runCatching { room.openHelper.writableDatabase }.exceptionOrNull()
            } finally {
                room.close()
            }

        assertNotNull(failure)
        val messages =
            generateSequence(failure) { it.cause }
                .mapNotNull { it.message }
                .joinToString("\n")
        assertTrue(messages.contains("INBOX_SORTING cutover blocked"))
        assertTrue(messages.contains("UNKNOWN_MODE"))

        openExisting162(dbName).use { helper ->
            val db = helper.writableDatabase
            assertEquals(162L, scalarLong(db, "PRAGMA user_version"))
            assertEquals("{}", scalarString(db, "SELECT configuration FROM workspace_capability_instances WHERE id='sorting-owner'"))
            assertEquals(3L, scalarLong(db, "SELECT version FROM workspace_capability_instances WHERE id='sorting-owner'"))
            assertEquals(1L, scalarLong(db, "SELECT COUNT(*) FROM context_inbox_sorting"))
        }
        context.deleteDatabase(dbName)
    }

    private fun createFixture(dbName: String, rulesText: String) {
        context.deleteDatabase(dbName)
        val configuration =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(162) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createSchema(db, 162)
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
                            insertSchemaAwareRow(
                                db,
                                "workspace_capability_instances",
                                mapOf(
                                    "id" to "sorting-owner",
                                    "workspaceId" to "owner",
                                    "capabilityType" to "INBOX_SORTING",
                                    "instanceKey" to "default",
                                    "capabilityOrder" to 1L,
                                    "state" to "ACTIVE",
                                    "configurationVersion" to 1L,
                                    "configuration" to "{}",
                                    "createdAt" to 1L,
                                    "updatedAt" to 5L,
                                    "syncedAt" to 4L,
                                    "isDeleted" to 0L,
                                    "version" to 3L,
                                ),
                            )
                            insertSchemaAwareRow(
                                db,
                                "context_inbox_sorting",
                                mapOf(
                                    "context_id" to "owner",
                                    "rules_text" to rulesText,
                                    "updated_at" to 20L,
                                ),
                            )
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                ).build()
        FrameworkSQLiteOpenHelperFactory().create(configuration).use { it.writableDatabase }
    }

    private fun openExisting162(dbName: String): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(162) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                ).build(),
        )

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
                if (cursor.getInt(notNullIndex) == 0 || !cursor.isNull(defaultIndex) || values.containsKey(name)) continue
                when {
                    "INT" in type -> values.put(name, 0L)
                    "REAL" in type || "FLOA" in type || "DOUB" in type -> values.put(name, 0.0)
                    "BLOB" in type -> values.put(name, byteArrayOf())
                    else -> values.put(name, "")
                }
            }
        }
        assertTrue(db.insert(table, SQLiteDatabase.CONFLICT_ABORT, values) != -1L)
    }

    private fun putValue(values: ContentValues, name: String, value: Any?) {
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

    private fun createSchema(db: SupportSQLiteDatabase, version: Int) {
        val database = schemaFile(version).reader().use { JsonParser.parseReader(it).asJsonObject.getAsJsonObject("database") }
        database.getAsJsonArray("entities").forEach { element ->
            val entity = element.asJsonObject
            val table = entity.get("tableName").asString
            db.execSQL(entity.get("createSql").asString.replace("\${TABLE_NAME}", table))
            entity.getAsJsonArray("indices")?.forEach { index ->
                db.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table))
            }
        }
        database.getAsJsonArray("views")?.forEach { view -> db.execSQL(view.asJsonObject.get("createSql").asString) }
    }

    private fun schemaFile(version: Int): File {
        val relative = "schemas/com.romankozak.forwardappmobile.database.AppDatabase/$version.json"
        val userDir = File(System.getProperty("user.dir"))
        return listOf(File(relative), File("app/$relative"), File(userDir, relative), File(userDir, "app/$relative"))
            .firstOrNull { it.isFile }
            ?: error("Room schema $version not found from ${userDir.absolutePath}")
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        scalarLong(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$table'") == 1L

    private fun scalarLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun scalarString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getString(0)
        }
}
