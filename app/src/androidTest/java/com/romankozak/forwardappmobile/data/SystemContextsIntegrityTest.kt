package com.romankozak.forwardappmobile.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.ContextId
import com.romankozak.forwardappmobile.core.sync.FullBackupLocalDataSourceImpl
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.DatabaseInitializer
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SystemContextsIntegrityTest {
    private lateinit var db: AppDatabase
    private lateinit var contextDao: ContextDao
    private lateinit var databaseInitializer: DatabaseInitializer
    private lateinit var fullBackupLocalDataSource: FullBackupLocalDataSourceImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        contextDao = db.contextDao()

        // DatabaseInitializer тепер приймає тільки ContextDao згідно з вашою помилкою
        databaseInitializer = DatabaseInitializer(contextDao)

        // Наповнюємо DataSource всіма необхідними DAO
        fullBackupLocalDataSource =
            FullBackupLocalDataSourceImpl(
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
                tacticalIterationDao = db.tacticalIterationDao(),
                missionStreamDao = db.missionStreamDao(),
                tacticalActivitySlotDao = db.tacticalActivitySlotDao(),
                arcQuestDao = db.arcQuestDao(),
                aiInsightDao = db.aiInsightDao(),
                systemContextEnsurer = databaseInitializer,
                // Додаємо нові DAO, яких не вистачало
                legacyNoteDao = db.legacyNoteDao(),
                backlogOrderDao = db.backlogOrderDao(),
                contextArtifactDao = db.contextArtifactDao(),
                scriptDao = db.scriptDao(),
                inboxRecordDao = db.inboxRecordDao(),
                contextManagementDao = db.contextManagementDao(),
                systemAppDao = db.systemAppDao(),
                activityRecordDao = db.activityRecordDao(),
                linkItemDao = db.linkItemDao(),
                conversationFolderDao = db.conversationFolderDao(),
                aiEventDao = db.aiEventDao(),
                lifeSystemStateDao = db.lifeSystemStateDao(),
                structurePresetDao = db.structurePresetDao(),
                structurePresetItemDao = db.structurePresetItemDao(),
                contextStructureDao = db.contextStructureDao(),
            )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun systemContextsAreEnsuredAfterFullRestore() =
        runBlocking {
            // 1. Початковий стан
            databaseInitializer.ensureAllSystemContextsExist()
            val initialContexts = contextDao.getAllContextsFlow().first()

            assertTrue(
                "Size should be at least system contexts size",
                initialContexts.size >= SystemContexts.ALL.size,
            )

            SystemContexts.ALL.forEach { systemContext ->
                assertTrue(
                    "System context ${systemContext.id} missing",
                    initialContexts.any { it.id == systemContext.id.value },
                )
            }

            // 2. Симуляція бекапу без системних контекстів
            val contentBeforeRestore = fullBackupLocalDataSource.loadFullDatabaseContent()
            val contentWithoutSystemContexts =
                contentBeforeRestore.copy(
                    projects =
                        contentBeforeRestore.projects.filterNot {
                            SystemContexts.isSystem(ContextId(it.id)) // Обгортаємо String в ContextId
                        },
                )

            assertTrue(
                "Backup should not contain system contexts",
                contentWithoutSystemContexts.projects.none { SystemContexts.isSystem(ContextId(it.id)) },
            )

            // 3. Відновлення (тут спрацює ваша логіка ensureAllSystemContextsExist)
            fullBackupLocalDataSource.restoreDatabaseFromBackup(contentWithoutSystemContexts)

            // 4. Перевірка результату
            val contextsAfterRestore = contextDao.getAllContextsFlow().first()
            assertTrue(
                "System contexts should be restored after process",
                contextsAfterRestore.size >= SystemContexts.ALL.size,
            )

            SystemContexts.ALL.forEach { systemContext ->
                assertTrue(
                    "System context ${systemContext.id} must be present after restore",
                    contextsAfterRestore.any { it.id == systemContext.id.value },
                )
            }
        }
}
