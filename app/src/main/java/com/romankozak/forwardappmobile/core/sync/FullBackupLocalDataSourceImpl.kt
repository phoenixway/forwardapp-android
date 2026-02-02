package com.romankozak.forwardappmobile.core.sync

import android.util.Log
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.toSnapshot
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiEventDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.sync.SyncMapper
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FullBackupLocalDataSourceImpl
    @Inject
    constructor(
        private val db: AppDatabase,
        val settingsRepository: SettingsRepository,
        private val contextDao: ContextDao,
        private val goalDao: GoalDao,
        private val listItemDao: ListItemDao,
        private val noteDocumentDao: NoteDocumentDao,
        private val checklistDao: ChecklistDao,
        private val attachmentDao: AttachmentDao,
        private val recentItemDao: RecentItemDao,
        private val dayPlanDao: DayPlanDao,
        private val dayTaskDao: DayTaskDao,
        private val dailyMetricDao: DailyMetricDao,
        private val chatDao: ChatDao,
        private val reminderDao: ReminderDao,
        private val tacticalMissionDao: TacticalMissionDao,
        private val aiInsightDao: AiInsightDao,
        private val systemContextEnsurer: SystemContextEnsurer,
        private val legacyNoteDao: LegacyNoteDao,
        private val backlogOrderDao: BacklogOrderDao,
        private val contextArtifactDao: ContextArtifactDao,
        private val scriptDao: ScriptDao,
        private val inboxRecordDao: InboxRecordDao,
        private val contextManagementDao: ContextManagementDao,
        private val systemAppDao: SystemAppDao,
        private val activityRecordDao: ActivityRecordDao,
        private val linkItemDao: LinkItemDao,
        private val conversationFolderDao: ConversationFolderDao,
        private val recurringTaskDao: RecurringTaskDao,
        private val aiEventDao: AiEventDao,
        private val lifeSystemStateDao: LifeSystemStateDao,
        private val structurePresetDao: StructurePresetDao,
        private val structurePresetItemDao: StructurePresetItemDao,
        private val contextStructureDao: ContextStructureDao,
    ) : FullBackupLocalDataSource {
        // --- Legacy V1 Support ---

// --- Legacy V1 Support ---

        override suspend fun loadFullDatabaseContent(): DatabaseContent {
            return DatabaseContent(
                projects = contextDao.getAll(),
                goals = goalDao.getAll(),
                documents = noteDocumentDao.getAllDocuments(),
                // Виправлено згідно з вашим ChecklistDao
                checklists = checklistDao.getAllChecklistsRaw(),
                checklistItems = checklistDao.getAllChecklistItemsRaw(),
                // Виправлено назви методів для інших DAO
                activityRecords = activityRecordDao.getAllRaw(),
                inboxRecords = inboxRecordDao.getAllRaw(),
                tacticalMissions = tacticalMissionDao.getAllMissionsSync(),
            )
        }

        // ... решта методів (restoreDatabaseFromBackup, applySnapshotBundle) залишаються без змін ...

        // --- Snapshot V2 Support ---

        override suspend fun loadFullSnapshotBundle(): SnapshotBundle {
            return SnapshotBundle(
                version = 2,
                exportedAt = System.currentTimeMillis(),
                contexts = contextDao.getAllRaw().map { it.toSnapshot() },
                goals = goalDao.getAllRaw().map { it.toSnapshot() },
                documents = noteDocumentDao.getAllDocumentsRaw().map { it.toSnapshot() },
                // Використовуємо методи з вашого ChecklistDao
                checklists = checklistDao.getAllChecklistsRaw().map { it.toSnapshot() },
                checklistItems = checklistDao.getAllChecklistItemsRaw().map { it.toSnapshot() },
                activityRecords = activityRecordDao.getAllRaw().map { it.toSnapshot() },
                tacticalMissions = tacticalMissionDao.getAllMissionsSync().map { it.toSnapshot() },
                attachments = attachmentDao.getAllRaw().map { it.toSnapshot() },
                crossRefs = attachmentDao.getAllContextAttachmentCrossRefsRaw().map { it.toSnapshot() },
                inbox = inboxRecordDao.getAllRaw().map { it.toSnapshot() },
                // Для сутностей AI та Day Management викликайте відповідні методи Sync
                dayPlans = dayPlanDao.getAllPlansSync().map { it.toSnapshot() },
                dayTasks = dayTaskDao.getAllTasksSync().map { it.toSnapshot() },
            )
        }

        override suspend fun restoreDatabaseFromBackup(content: DatabaseContent) {
            // ЦЕ СЕРЦЕ РЕФАКТОРИНГУ:
            // Ми перетворюємо старий формат у новий Snapshot за допомогою нашого "двигуна"
            val snapshotBundle = SyncMapper.migrateV1ToV2(content)

            db.withTransaction {
                Log.d("FullBackupRestore", "--- CLEARING DATABASE AND RESTORING FROM V1 ---")
                clearAllTables()

                // Тепер використовуємо уніфікований метод вставки
                insertBundleData(snapshotBundle)
            }
        }

        override suspend fun applySnapshotBundle(bundle: SnapshotBundle) {
            db.withTransaction {
                Log.d("FullBackupRestore", "--- APPLYING SNAPSHOT V${bundle.version} (MERGE MODE) ---")
                insertBundleData(bundle)
            }
        }

        /**
         * Уніфікований метод для вставки даних у базу.
         * Не містить логіки зшивання — він просто довіряє бандлу.
         */
        private suspend fun insertBundleData(bundle: SnapshotBundle) {
            // Порядок вставки важливий через Foreign Keys (Parents -> Children)
            contextDao.insertAll(bundle.contexts.map { it.toEntity() })
            goalDao.insertAll(bundle.goals.map { it.toEntity() })

            noteDocumentDao.insertAllDocuments(bundle.documents.map { it.toEntity() })
            checklistDao.insertChecklists(bundle.checklists.map { it.toEntity() })
            checklistDao.insertItems(bundle.checklistItems.map { it.toEntity() })

            // Вкладення тепер просто вставляються, бо SyncMapper вже їх згенерував
            attachmentDao.insertAttachments(bundle.attachments.map { it.toEntity() })
            attachmentDao.insertContextAttachmentCrossRefs(bundle.crossRefs.map { it.toEntity() })

            activityRecordDao.insertAll(bundle.activityRecords.map { it.toEntity() })
            inboxRecordDao.insertAll(bundle.inbox.map { it.toEntity() })
            tacticalMissionDao.insertMissions(bundle.tacticalMissions.map { it.toEntity() })

            // Плани на день
            dayPlanDao.insertPlans(bundle.dayPlans.map { it.toEntity() })
            dayTaskDao.insertTasks(bundle.dayTasks.map { it.toEntity() })

            systemContextEnsurer.ensureAllSystemContextsExist()
            Log.d("FullBackupRestore", "Successfully inserted bundle data.")
        }

        // --- System Methods ---

        override suspend fun clearAllTables() {
            db.clearAllTables()
        }

        override suspend fun getSettingsSnapshot(): Map<String, String> {
            return settingsRepository.getPreferencesSnapshot().asMap()
                .mapKeys { it.key.name }
                .mapValues { it.value.toString() }
        }

        override suspend fun restoreSettings(settings: Map<String, String>) {
            settingsRepository.restoreFromMap(settings)
        }
    }
