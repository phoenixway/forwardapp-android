package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.data.dao.AttachmentDao
import com.romankozak.forwardappmobile.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.dao.ReminderDao
import com.romankozak.forwardappmobile.data.repository.AttachmentRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ProjectLogRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.domain.reminders.AlarmScheduler
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
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ReminderRepository = ReminderRepository(reminderDao, alarmScheduler, ioDispatcher)

    @Provides
    @Singleton
    fun provideProjectLogRepository(
        projectManagementDao: ProjectManagementDao
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
    fun provideNoteDocumentRepository(
        noteDocumentDao: NoteDocumentDao,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository
    ): NoteDocumentRepository =
        NoteDocumentRepository(noteDocumentDao, attachmentRepository, recentItemsRepository)

    @Provides
    @Singleton
    fun provideChecklistRepository(
        checklistDao: ChecklistDao,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository,
    ): ChecklistRepository = ChecklistRepository(checklistDao, attachmentRepository, recentItemsRepository)
}
