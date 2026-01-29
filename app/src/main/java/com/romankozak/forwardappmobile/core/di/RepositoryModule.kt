package com.romankozak.forwardappmobile.core.di

import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.repository.*
import com.romankozak.forwardappmobile.data.sync.FullBackupLocalDataSourceImpl
import com.romankozak.forwardappmobile.data.sync.SyncLocalDataSourceImpl
import com.romankozak.forwardappmobile.data.sync.SyncSettingsSourceImpl
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.ai.data.repository.AiInsightRepository
import com.romankozak.forwardappmobile.features.contexts.data.dao.*
import com.romankozak.forwardappmobile.sync.* import com.romankozak.forwardappmobile.sync.datasource.AttachmentsLocalDataSource
import com.romankozak.forwardappmobile.sync.datasource.FullBackupLocalDataSource
import com.romankozak.forwardappmobile.sync.datasource.SyncLocalDataSource
import com.romankozak.forwardappmobile.sync.datasource.SyncSettingsSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFullBackupLocalDataSource(
        impl: FullBackupLocalDataSourceImpl
    ): FullBackupLocalDataSource = impl

    @Provides
    @Singleton
    fun provideAttachmentsRepository(
        localDataSource: AttachmentsLocalDataSource,
        syncFileService: SyncFileService,
        logicHelper: SyncLogicHelper
    ): AttachmentsRepository = AttachmentsRepositoryImpl(
        localDataSource,
        syncFileService,
        logicHelper
    )

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

    @Provides
    @Singleton
    fun provideMergeLocalDataSource(
        // Hilt автоматично створить MergeLocalDataSourceImpl,
        // оскільки в його конструкторі є анотація @Inject
        impl: com.romankozak.forwardappmobile.data.sync.MergeLocalDataSourceImpl
    ): com.romankozak.forwardappmobile.sync.datasource.MergeLocalDataSource = impl

    @Provides
    @Singleton
    fun provideSyncSettingsSource(
        impl: FullBackupLocalDataSourceImpl // Використовуємо реалізацію, яку Hilt вже вміє створювати через @Inject
    ): SyncSettingsSource = SyncSettingsSourceImpl(impl.settingsRepository)

    @Provides
    @Singleton
    fun provideSyncApi(repository: SyncRepository): SyncApi = repository

    @Provides
    @Singleton
    fun provideSyncLocalDataSource(
        impl: SyncLocalDataSourceImpl
    ): SyncLocalDataSource = impl
}