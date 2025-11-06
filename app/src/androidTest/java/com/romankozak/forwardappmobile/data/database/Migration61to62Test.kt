package com.romankozak.forwardappmobile.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.romankozak.forwardappmobile.data.database.MIGRATION_61_62
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration61to62Test {

    private val testDbName = "migration_61_62_test"

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    @Throws(IOException::class)
    fun migrate61To62_createsChecklistTables() {
        helper.createDatabase(testDbName, 61).close()

        helper.runMigrationsAndValidate(testDbName, 62, true, MIGRATION_61_62).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val migratedDb =
            Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
                .addMigrations(MIGRATION_61_62)
                .allowMainThreadQueries()
                .build()

        val sqliteDb = migratedDb.openHelper.writableDatabase
        sqliteDb.query("SELECT COUNT(*) FROM checklists").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getLong(0) == 0L)
        }
        sqliteDb.query("SELECT COUNT(*) FROM checklist_items").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.getLong(0) == 0L)
        }

        migratedDb.close()
    }
}
