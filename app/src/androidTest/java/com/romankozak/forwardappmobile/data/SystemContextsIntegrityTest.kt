package com.romankozak.forwardappmobile.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.sync.FullBackupLocalDataSourceImpl
import com.romankozak.forwardappmobile.features.contexts.data.DatabaseInitializer
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import io.mockk.mockk

@RunWith(AndroidJUnit4::class)
class SystemContextsIntegrityTest {

    private lateinit var db: AppDatabase
    private lateinit var contextDao: ContextDao
    private lateinit var databaseInitializer: DatabaseInitializer
    private lateinit var fullBackupLocalDataSource: FullBackupLocalDataSourceImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contextDao = db.contextDao()
        // DatabaseInitializer needs a ContextDao
        databaseInitializer = DatabaseInitializer(contextDao, mockk(relaxed = true)) // Mock SystemAppRepository
        // FullBackupLocalDataSourceImpl needs many DAOs and SystemContextEnsurer
        fullBackupLocalDataSource = FullBackupLocalDataSourceImpl(
            db = db,
            settingsRepository = mockk(relaxed = true),
            contextDao = contextDao,
            goalDao = db.goalDao(),
            listItemDao = db.listItemDao(),
            noteDocumentDao = db.noteDocumentDao(),
            checklistDao = db.checklistDao(),
            attachmentDao = db.attachmentDao(),
            recentItemDao = db.recentItemDao(),
            dayPlanDao = db.dayPlanDao(),
            dayTaskDao = db.dayTaskDao(),
            dailyMetricDao = db.dailyMetricDao(),
            chatDao = db.chatDao(),
            reminderDao = db.reminderDao(),
            tacticalMissionDao = db.tacticalMissionDao(),
            aiInsightDao = db.aiInsightDao(),
            systemContextEnsurer = databaseInitializer // Provide the initializer as the ensurer
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun systemContextsAreEnsuredAfterFullRestore() = runBlocking {
        // 1. Initial state: System contexts exist
        databaseInitializer.ensureAllSystemContextsExist()
        var initialContexts = contextDao.getAllContextsFlow().first()
        assertThat(initialContexts.size).isAtLeast(SystemContexts.ALL.size)
        SystemContexts.ALL.forEach { systemContext ->
            assertThat(initialContexts.any { it.id == systemContext.id.value }).isTrue()
        }

        // 2. Simulate a backup that *lacks* system contexts
        val contentBeforeRestore = fullBackupLocalDataSource.loadFullDatabaseContent()
        val contentWithoutSystemContexts = contentBeforeRestore.copy(
            projects = contentBeforeRestore.projects.filterNot { SystemContexts.isSystem(it.id) }
        )
        // Verify that the simulated backup indeed lacks system contexts
        assertThat(contentWithoutSystemContexts.projects.any { SystemContexts.isSystem(it.id) }).isFalse()

        // 3. Perform the restore
        fullBackupLocalDataSource.restoreDatabaseFromBackup(contentWithoutSystemContexts)

        // 4. Verify system contexts are present after restore
        val contextsAfterRestore = contextDao.getAllContextsFlow().first()
        assertThat(contextsAfterRestore.size).isAtLeast(SystemContexts.ALL.size)
        SystemContexts.ALL.forEach { systemContext ->
            assertThat(contextsAfterRestore.any { it.id == systemContext.id.value }).isTrue()
        }
    }
}