package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.dao.ReminderDao
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.data.dao.SystemAppDao
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.AiEventRepository
import com.romankozak.forwardappmobile.data.repository.ProjectLogRepository
import com.romankozak.forwardappmobile.data.repository.ProjectStructureRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.data.repository.ActivityRecordRepository
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
        dayManagementRepository: com.romankozak.forwardappmobile.data.repository.DayManagementRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ReminderRepository = ReminderRepository(reminderDao, alarmScheduler, dayManagementRepository, ioDispatcher)

    @Provides
    @Singleton
    fun provideProjectLogRepository(
        projectManagementDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectManagementDao
    ): ProjectLogRepository {
        return ProjectLogRepository(projectManagementDao)
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
        listItemDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao,
        recentItemsRepository: RecentItemsRepository
    ): LegacyNoteRepository = LegacyNoteRepository(noteDao, listItemDao, recentItemsRepository)

    @Provides
    @Singleton
    fun provideAttachmentRepository(
        attachmentDao: AttachmentDao,
        linkItemDao: com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao,
    ): AttachmentRepository = AttachmentRepository(attachmentDao, linkItemDao)

    @Provides
    @Singleton
    fun provideAiInsightRepository(
        aiInsightDao: com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao,
    ): AiInsightRepository = AiInsightRepository(aiInsightDao)

    @Provides
    @Singleton
    fun provideNoteDocumentRepository(
        noteDocumentDao: com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository,
        aiEventRepository: AiEventRepository,
    ): NoteDocumentRepository =
        NoteDocumentRepository(noteDocumentDao, attachmentRepository, recentItemsRepository, aiEventRepository)

    @Provides
    @Singleton
    fun provideChecklistRepository(
        checklistDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao,
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
        projectDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectDao,
        noteDocumentDao: com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao,
        attachmentRepository: AttachmentRepository,
    ): SystemAppRepository = SystemAppRepository(systemAppDao, projectDao, noteDocumentDao, attachmentRepository)

    @Provides
    @Singleton
    fun provideProjectStructureRepository(
        projectStructureDao: com.romankozak.forwardappmobile.features.contexts.data.dao.ProjectStructureDao,
        structurePresetDao: com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao,
        structurePresetItemDao: com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao,
    ): ProjectStructureRepository = ProjectStructureRepository(projectStructureDao, structurePresetDao, structurePresetItemDao)
}
