package com.romankozak.forwardappmobile.core.di

import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.repository.*
import com.romankozak.forwardappmobile.data.sync.*
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.ai.data.repository.AiInsightRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.sync.*
import com.romankozak.forwardappmobile.sync.datasource.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // ========================================================================
    // BINDS (Інтерфейси -> Реалізації)
    // Використовуємо @Binds, бо це ефективніше за @Provides
    // ========================================================================

    @Binds
    @Singleton
    abstract fun bindFullBackupLocalDataSource(impl: FullBackupLocalDataSourceImpl): FullBackupLocalDataSource

    @Binds
    @Singleton
    abstract fun bindAttachmentsLocalDataSource(impl: AttachmentsLocalDataSourceImpl): AttachmentsLocalDataSource

    @Binds
    @Singleton
    abstract fun bindMergeLocalDataSource(impl: MergeLocalDataSourceImpl): MergeLocalDataSource

    @Binds
    @Singleton
    abstract fun bindSyncLocalDataSource(impl: SyncLocalDataSourceImpl): SyncLocalDataSource

    @Binds
    @Singleton
    abstract fun bindSyncApi(impl: SyncRepository): SyncApi

    // ========================================================================
    // PROVIDES (Логіка створення та складні репозиторії)
    // ========================================================================

    companion object {

        @Provides
        @Singleton
        fun provideSyncSettingsSource(
            settingsRepository: SettingsRepository
        ): SyncSettingsSource = SyncSettingsSourceImpl(settingsRepository)

        /*@Provides
        @Singleton
        fun provideAttachmentsRepository(
            localDataSource: AttachmentsLocalDataSource,
            syncFileService: SyncFileService,
            logicHelper: SyncLogicHelper
        ): AttachmentsRepository = AttachmentsRepositoryImpl(
            localDataSource,
            syncFileService,
            logicHelper
        )*/

        @Provides
        @Singleton
        fun provideNoteDocumentRepository(
            noteDocumentDao: NoteDocumentDao,
            attachmentsRepository: AttachmentsRepository,
            recentItemsRepository: RecentItemsRepository,
            aiEventRepository: AiEventRepository,
        ): NoteDocumentRepository = NoteDocumentRepository(
            noteDocumentDao,
            attachmentsRepository,
            recentItemsRepository,
            aiEventRepository
        )

        @Provides
        @Singleton
        fun provideChecklistRepository(
            checklistDao: ChecklistDao,
            attachmentsRepository: AttachmentsRepository,
            recentItemsRepository: RecentItemsRepository,
        ): ChecklistRepository = ChecklistRepository(
            checklistDao,
            attachmentsRepository,
            recentItemsRepository
        )

        @Provides
        @Singleton
        fun provideSystemAppRepository(
            systemAppDao: SystemAppDao,
            contextDao: ContextDao,
            noteDocumentDao: NoteDocumentDao,
            attachmentsRepository: AttachmentsRepository,
        ): SystemAppRepository = SystemAppRepository(
            systemAppDao,
            contextDao,
            noteDocumentDao,
            attachmentsRepository
        )

        @Provides
        @Singleton
        fun provideReminderRepository(
            reminderDao: ReminderDao,
            alarmScheduler: AlarmScheduler,
            dayManagementRepository: DayManagementRepository,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): ReminderRepository = ReminderRepository(
            reminderDao,
            alarmScheduler,
            dayManagementRepository,
            ioDispatcher
        )

        @Provides
        @Singleton
        fun provideAiInsightRepository(aiInsightDao: AiInsightDao): AiInsightRepository =
            AiInsightRepository(aiInsightDao)

        @Provides
        @Singleton
        fun provideRecentItemsRepository(recentItemDao: RecentItemDao): RecentItemsRepository =
            RecentItemsRepository(recentItemDao)

        @Provides
        @Singleton
        fun provideActivityRecordRepository(activityRecordDao: ActivityRecordDao): ActivityRecordRepository =
            ActivityRecordRepository(activityRecordDao)
    }
}