package com.romankozak.forwardappmobile.core.di

import com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.dao.ReminderDao
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.data.dao.SystemAppDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.AiEventRepository
import com.romankozak.forwardappmobile.data.repository.ContextLogRepository
import com.romankozak.forwardappmobile.data.repository.ContextStructureRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.repository.ActivityRecordRepository
import com.romankozak.forwardappmobile.data.repository.DayManagementRepository
import com.romankozak.forwardappmobile.data.repository.SystemAppRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
import com.romankozak.forwardappmobile.features.ai.data.repository.AiInsightRepository
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
    fun provideReminderRepository(
        reminderDao: ReminderDao,
        alarmScheduler: AlarmScheduler,
        dayManagementRepository: DayManagementRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ReminderRepository = ReminderRepository(reminderDao, alarmScheduler, dayManagementRepository, ioDispatcher)

    @Provides
    @Singleton
    fun provideProjectLogRepository(
        contextManagementDao: ContextManagementDao
    ): ContextLogRepository {
        return ContextLogRepository(contextManagementDao)
    }

    @Provides
    @Singleton
    fun provideRecentItemsRepository(
        recentItemDao: RecentItemDao
    ): RecentItemsRepository {
        return RecentItemsRepository(recentItemDao)
    }

    @Provides
    @Singleton
    fun provideLegacyNoteRepository(
        noteDao: LegacyNoteDao,
        listItemDao: ListItemDao,
        recentItemsRepository: RecentItemsRepository
    ): LegacyNoteRepository = LegacyNoteRepository(noteDao, listItemDao, recentItemsRepository)

    @Provides
    @Singleton
    fun provideAttachmentRepository(
        attachmentDao: AttachmentDao,
        linkItemDao: LinkItemDao,
    ): AttachmentRepository = AttachmentRepository(attachmentDao, linkItemDao)

    @Provides
    @Singleton
    fun provideAiInsightRepository(
        aiInsightDao: AiInsightDao,
    ): AiInsightRepository = AiInsightRepository(aiInsightDao)

    @Provides
    @Singleton
    fun provideNoteDocumentRepository(
        noteDocumentDao: NoteDocumentDao,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository,
        aiEventRepository: AiEventRepository,
    ): NoteDocumentRepository =
        NoteDocumentRepository(noteDocumentDao, attachmentRepository, recentItemsRepository, aiEventRepository)

    @Provides
    @Singleton
    fun provideChecklistRepository(
        checklistDao: ChecklistDao,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository,
    ): ChecklistRepository = ChecklistRepository(checklistDao, attachmentRepository, recentItemsRepository)

    @Provides
    @Singleton
    fun provideActivityRecordRepository(
        activityRecordDao: ActivityRecordDao,
    ): ActivityRecordRepository = ActivityRecordRepository(activityRecordDao)

    @Provides
    @Singleton
    fun provideSystemAppRepository(
        systemAppDao: SystemAppDao,
        contextDao: ContextDao,
        noteDocumentDao: NoteDocumentDao,
        attachmentRepository: AttachmentRepository,
    ): SystemAppRepository = SystemAppRepository(systemAppDao, contextDao, noteDocumentDao, attachmentRepository)

    @Provides
    @Singleton
    fun provideProjectStructureRepository(
        contextStructureDao: ContextStructureDao,
        structurePresetDao: StructurePresetDao,
        structurePresetItemDao: StructurePresetItemDao,
    ): ContextStructureRepository = ContextStructureRepository(contextStructureDao, structurePresetDao, structurePresetItemDao)
}
