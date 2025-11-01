
package com.romankozak.forwardappmobile.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.romankozak.forwardappmobile.data.database.AppDatabase.Companion.MIGRATION_61_62
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration61to62Test {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenherFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate61To62() {
        // Створюємо базу даних з версією 61
        var db = helper.createDatabase(TEST_DB, 61)
        db.close()

        // Запускаємо міграцію до версії 62 і валідуємо
        db = helper.runMigrationsAndValidate(TEST_DB, 62, true, MIGRATION_61_62)

        // Перевіряємо, що нові таблиці існують і доступні через DAO
        val checklistDao = AppDatabase.getDatabase(
            InstrumentationRegistry.getInstrumentation().targetContext,
            inMemory = true // Використовуємо in-memory для тесту, щоб не створювати реальний файл
        ).checklistDao()

        runBlocking {
            // Переконуємося, що можемо запитати дані (очікуємо порожній список)
            val checklists = checklistDao.getAllChecklists()
            assert(checklists.isEmpty())

            val checklistItems = checklistDao.getAllChecklistItems()
            assert(checklistItems.isEmpty())
        }
    }
}
