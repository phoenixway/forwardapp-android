package com.romankozak.forwardappmobile.core.sync

import android.util.Log
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.interfaces.SystemContextEnsurer
import com.romankozak.forwardappmobile.core.data.models.sync.DatabaseContent
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.mappers.*
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot
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
class FullBackupLocalDataSourceImpl @Inject constructor(
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
    private val backlogItemDao: ListItemDao,
    private val contextArtifactDao: ContextArtifactDao,
    private val contextLogDao: ContextManagementDao,
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

    // --- Snapshot V2 Support ---

    override suspend fun loadFullSnapshotBundle(): SnapshotBundle {
        Log.d("SyncV2", "Starting export to SnapshotBundle V2")
        return SnapshotBundle(
            version = 2,
            exportedAt = System.currentTimeMillis(),

            // Core & Structure
            contexts = contextDao.getAllRaw().map { it.toSnapshot() },
            goals = goalDao.getAllRaw().map { it.toSnapshot() },
            backlogItems = backlogItemDao.getAllRaw().map { it.toSnapshot() },
            backlogOrders = backlogOrderDao.getAllRaw().map { it.toSnapshot() },
            inbox = inboxRecordDao.getAllRaw().map { it.toSnapshot() },
            logs = contextLogDao.getAllLogs().map { it.toSnapshot() },
            artifacts = contextArtifactDao.getAllRaw().map { it.toSnapshot() },

            // Knowledge Base
            documents = noteDocumentDao.getAllDocumentsRaw().map { it.toSnapshot() },
            checklists = checklistDao.getAllChecklistsRaw().map { it.toSnapshot() },
            checklistItems = checklistDao.getAllChecklistItemsRaw().map { it.toSnapshot() },
            scripts = scriptDao.getAllRaw().map { it.toSnapshot() },
            attachments = attachmentDao.getAllRaw().map { it.toSnapshot() },
            crossRefs = attachmentDao.getAllContextAttachmentCrossRefsRaw().map { it.toSnapshot() },

            // Activity & RPG
            activityRecords = activityRecordDao.getAllRaw().map { it.toSnapshot() },
            dayPlans = dayPlanDao.getAllPlansSync().map { it.toSnapshot() },
            dayTasks = dayTaskDao.getAllTasksSync().map { it.toSnapshot() },
            // У loadFullSnapshotBundle()
            dailyMetrics = dailyMetricDao.getAll().map { it.toSnapshot() }, // Змінено getAllRaw -> getAll
            recurringTasks = recurringTaskDao.getAll().map { it.toSnapshot() }, // Ймовірно, там теж getAll()

            // AI Domain
            conversations = chatDao.getAllConversationsSync().map { it.toSnapshot() },
            chatMessages = chatDao.getAllMessagesSync().map { it.toSnapshot() },
            conversationFolders = conversationFolderDao.getAllSync().map { it.toSnapshot() },
            aiInsights = aiInsightDao.getAllSync().map { it.toSnapshot() },
            aiEvents = aiEventDao.getAllSync().map { it.toSnapshot() },

            // System & Tactical
            tacticalMissions = tacticalMissionDao.getAllMissionsSync().map { it.toSnapshot() },
            tacticalMissionAttachments = tacticalMissionDao.getAllMissionAttachmentCrossRefsSync().map { it.toSnapshot() },
            reminders = reminderDao.getAllRaw().map { it.toSnapshot() },
            systemApps = systemAppDao.getAllRaw().map { it.toSnapshot() },
            lifeSystemStates = lifeSystemStateDao.getAllRaw().map { it.toSnapshot() },
            recentProjectEntries = recentItemDao.getAllRaw().map { it.toSnapshot() },
            linkItemEntities = linkItemDao.getAllRaw().map { it.toSnapshot() },

            // Configuration
            contextRoleProfiles = structurePresetDao.getAllRaw().map { it.toSnapshot() },
            contextRoleProfileItems = structurePresetItemDao.getAllRaw().map { it.toSnapshot() },
            contextConfigurations = contextManagementDao.getAllConfigsRaw().map { it.toSnapshot() },
            projectStructureItems = contextStructureDao.getAllRaw().map { it.toSnapshot() }
        )
    }

    override suspend fun applySnapshotBundle(bundle: SnapshotBundle) {
        db.withTransaction {
            Log.d("SyncV2", "Applying bundle V${bundle.version} in Merge Mode")
            insertBundleData(bundle)
        }
    }

    private suspend fun insertBundleData(bundle: SnapshotBundle) {
        // ПОРЯДОК ВАЖЛИВИЙ: від батьків до дітей

        // 1. Глобальні налаштування та Ролі
        structurePresetDao.insertAll(bundle.contextRoleProfiles.map { it.toEntity() })
        systemAppDao.insertAll(bundle.systemApps.map { it.toEntity() })
        conversationFolderDao.insertAll(bundle.conversationFolders.map { it.toEntity() })

        // 2. Контексти та Цілі
        contextDao.insertAll(bundle.contexts.map { it.toEntity() })
        goalDao.insertAll(bundle.goals.map { it.toEntity() })
        structurePresetItemDao.insertAll(bundle.contextRoleProfileItems.map { it.toEntity() })

        // 3. Конфігурації та Плани
        contextManagementDao.insertAllConfigs(bundle.contextConfigurations.map { it.toEntity() })
        contextStructureDao.insertAll(bundle.projectStructureItems.map { it.toEntity() })
        dayPlanDao.insertPlans(bundle.dayPlans.map { it.toEntity() })
        checklistDao.insertChecklists(bundle.checklists.map { it.toEntity() })

        // 4. Завдання, Повідомлення та Нотатки
        dayTaskDao.insertTasks(bundle.dayTasks.map { it.toEntity() })
        checklistDao.insertItems(bundle.checklistItems.map { it.toEntity() })
        chatDao.insertConversations(bundle.conversations.map { it.toEntity() })
        chatDao.insertMessages(bundle.chatMessages.map { it.toEntity() })

        noteDocumentDao.insertAllDocuments(bundle.documents.map { it.toEntity() })
        legacyNoteDao.insertAll(bundle.notes.map { it.toEntity() })
        scriptDao.insertAll(bundle.scripts.map { it.toEntity() })

        // 5. Логи та Атомарні дані
        activityRecordDao.insertAll(bundle.activityRecords.map { it.toEntity() })
        inboxRecordDao.insertAll(bundle.inbox.map { it.toEntity() })
        contextLogDao.insertAll(bundle.logs.map { it.toEntity() })
        contextArtifactDao.insertAll(bundle.artifacts.map { it.toEntity() })

        // 6. Метрики та RPG
        dailyMetricDao.insertAll(bundle.dailyMetrics.map { it.toEntity() })
        recurringTaskDao.insertAll(bundle.recurringTasks.map { it.toEntity() })
        lifeSystemStateDao.insertAll(bundle.lifeSystemStates.map { it.toEntity() })

        // 7. Вкладення та Cross-references
        attachmentDao.insertAttachments(bundle.attachments.map { it.toEntity() })
        attachmentDao.insertContextAttachmentCrossRefs(bundle.crossRefs.map { it.toEntity() })

        // 8. Tactical Domain
        tacticalMissionDao.insertMissions(bundle.tacticalMissions.map { it.toEntity() })
        tacticalMissionDao.insertMissionAttachmentCrossRefs(bundle.tacticalMissionAttachments.map { it.toEntity() })

        // 9. Misc
        reminderDao.insertAll(bundle.reminders.map { it.toEntity() })
        backlogItemDao.insertAll(bundle.backlogItems.map { it.toEntity() })
        backlogOrderDao.insertAll(bundle.backlogOrders.map { it.toEntity() })
        recentItemDao.insertAll(bundle.recentProjectEntries.map { it.toEntity() })
        linkItemDao.insertAll(bundle.linkItemEntities.map { it.toEntity() })
        aiInsightDao.insertAll(bundle.aiInsights.map { it.toEntity() })
        aiEventDao.insertAll(bundle.aiEvents.map { it.toEntity() })

        systemContextEnsurer.ensureAllSystemContextsExist()
        Log.d("SyncV2", "Successfully restored ${bundle.contexts.size} contexts and all related data.")
    }

    // --- Legacy V1 Support ---

    override suspend fun loadFullDatabaseContent(): DatabaseContent {
        return DatabaseContent(
            projects = contextDao.getAll(),
            goals = goalDao.getAll(),
            documents = noteDocumentDao.getAllDocuments(),
            checklists = checklistDao.getAllChecklistsRaw(),
            checklistItems = checklistDao.getAllChecklistItemsRaw(),
            activityRecords = activityRecordDao.getAllRaw(),
            inboxRecords = inboxRecordDao.getAllRaw(),
            tacticalMissions = tacticalMissionDao.getAllMissionsSync(),
        )
    }

    override suspend fun restoreDatabaseFromBackup(content: DatabaseContent) {
        val snapshotBundle = SyncMapper.migrateV1ToV2(content)
        db.withTransaction {
            Log.d("SyncV1", "Migrating Legacy V1 to Snapshot V2")
            clearAllTables()
            insertBundleData(snapshotBundle)
        }
    }

    // --- System Methods ---

    override suspend fun clearAllTables() {
        Log.w("Sync", "Clearing all database tables!")
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