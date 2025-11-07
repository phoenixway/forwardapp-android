package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.ListItemQueries
import com.romankozak.forwardappmobile.data.notes.AndroidNoteBacklogLinkDataSource
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ProjectArtifactRepository
import com.romankozak.forwardappmobile.data.repository.ProjectLogRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.shared.database.AttachmentQueries
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.LegacyNoteQueries
import com.romankozak.forwardappmobile.shared.database.NoteDocumentQueries
import com.romankozak.forwardappmobile.shared.database.ProjectExecutionLogQueries
import com.romankozak.forwardappmobile.shared.database.RecentItemQueries
import com.romankozak.forwardappmobile.shared.database.ReminderQueries
import com.romankozak.forwardappmobile.shared.database.ChecklistQueries
import com.romankozak.forwardappmobile.shared.features.attachments.data.SqlDelightLinkItemDataSource
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemDataSource
import com.romankozak.forwardappmobile.shared.features.notes.data.datasource.NoteBacklogLinkDataSource
import com.romankozak.forwardappmobile.shared.features.reminders.domain.AlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideReminderRepository(
        reminderQueries: ReminderQueries,
        alarmScheduler: AlarmScheduler,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): com.romankozak.forwardappmobile.shared.features.reminders.data.repository.ReminderRepository =
        com.romankozak.forwardappmobile.shared.features.reminders.data.repository.ReminderRepository(
            reminderQueries,
            alarmScheduler,
            ioDispatcher,
        )

    @Provides
    @Singleton
    fun provideProjectLogRepository(
        projectExecutionLogQueries: ProjectExecutionLogQueries,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectLogRepository = ProjectLogRepository(projectExecutionLogQueries, ioDispatcher)

    @Provides
    @Singleton
    fun provideProjectArtifactRepository(
        forwardAppDatabase: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectArtifactRepository =
        ProjectArtifactRepository(forwardAppDatabase.projectArtifactQueries, ioDispatcher)

    @Provides
    @Singleton
    fun provideRecentItemsRepository(
        recentItemQueries: RecentItemQueries,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RecentItemsRepository = RecentItemsRepository(recentItemQueries, ioDispatcher)

    @Provides
    @Singleton
    fun provideNoteBacklogLinkDataSource(
        listItemQueries: ListItemQueries,
    ): NoteBacklogLinkDataSource = AndroidNoteBacklogLinkDataSource(listItemQueries)

    @Provides
    @Singleton
    fun provideLegacyNoteRepository(
        legacyNoteQueries: LegacyNoteQueries,
        backlogLinkDataSource: NoteBacklogLinkDataSource,
        recentItemsRepository: RecentItemsRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): LegacyNoteRepository =
        LegacyNoteRepository(legacyNoteQueries, backlogLinkDataSource, recentItemsRepository, ioDispatcher)

    @Provides
    @Singleton
    fun provideAttachmentRepository(
        attachmentQueries: AttachmentQueries,
        linkItemDataSource: LinkItemDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AttachmentRepository = AttachmentRepository(attachmentQueries, linkItemDataSource, ioDispatcher)

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideLinkItemDataSource(
        db: ForwardAppDatabase,
        json: Json,
    ): LinkItemDataSource = SqlDelightLinkItemDataSource(db, json)

    @Provides
    @Singleton
    fun provideNoteDocumentRepository(
        noteDocumentQueries: NoteDocumentQueries,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): NoteDocumentRepository =
        NoteDocumentRepository(noteDocumentQueries, attachmentRepository, recentItemsRepository, ioDispatcher)

    @Provides
    @Singleton
    fun provideChecklistRepository(
        checklistQueries: ChecklistQueries,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ChecklistRepository =
        ChecklistRepository(checklistQueries, attachmentRepository, recentItemsRepository, ioDispatcher)
}
