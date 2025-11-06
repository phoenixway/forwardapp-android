package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ProjectArtifactRepository
import com.romankozak.forwardappmobile.data.repository.ProjectLogRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.attachments.data.AndroidLinkItemDataSource
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.shared.database.ProjectExecutionLogQueriesQueries
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.AttachmentQueriesQueries
import com.romankozak.forwardappmobile.shared.database.ReminderQueriesQueries
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemDataSource
import com.romankozak.forwardappmobile.shared.database.RecentItemQueriesQueries
import com.romankozak.forwardappmobile.shared.features.reminders.domain.AlarmScheduler
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
        reminderQueries: ReminderQueriesQueries,
        alarmScheduler: AlarmScheduler,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): com.romankozak.forwardappmobile.shared.features.reminders.data.repository.ReminderRepository {
        return com.romankozak.forwardappmobile.shared.features.reminders.data.repository.ReminderRepository(reminderQueries, alarmScheduler, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideProjectLogRepository(
        projectExecutionLogQueries: ProjectExecutionLogQueriesQueries,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ProjectLogRepository {
        return ProjectLogRepository(projectExecutionLogQueries, ioDispatcher)
    }

    @Provides
    @Singleton
    fun provideProjectArtifactRepository(
        forwardAppDatabase: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectArtifactRepository = ProjectArtifactRepository(
        forwardAppDatabase.projectArtifactQueriesQueries,
        ioDispatcher,
    )

    @Provides
    @Singleton
    fun provideRecentItemsRepository(
        recentItemQueries: RecentItemQueriesQueries,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RecentItemsRepository = RecentItemsRepository(recentItemQueries, ioDispatcher)

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
        attachmentQueries: AttachmentQueriesQueries,
        linkItemDataSource: LinkItemDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AttachmentRepository = AttachmentRepository(attachmentQueries, linkItemDataSource, ioDispatcher)

    @Provides
    @Singleton
    fun provideLinkItemDataSource(
        linkItemDao: LinkItemDao,
    ): LinkItemDataSource = AndroidLinkItemDataSource(linkItemDao)

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
