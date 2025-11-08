package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.notes.AndroidNoteBacklogLinkDataSource
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.ChecklistRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.LegacyNoteRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.data.repository.NoteDocumentRepository
import com.romankozak.forwardappmobile.data.repository.ProjectArtifactRepository
import com.romankozak.forwardappmobile.data.repository.ProjectLogRepository
import com.romankozak.forwardappmobile.data.repository.ProjectTimeTrackingRepository
import com.romankozak.forwardappmobile.data.repository.RecentItemsRepository
import com.romankozak.forwardappmobile.data.repository.SearchRepository
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.features.attachments.data.AndroidLinkItemDataSource
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.projects.data.ProjectRepositoryImpl
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemDataSource
import com.romankozak.forwardappmobile.shared.features.notes.data.datasource.NoteBacklogLinkDataSource
import com.romankozak.forwardappmobile.shared.features.projects.data.ProjectLocalDataSource
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore
import com.romankozak.forwardappmobile.shared.features.reminders.domain.AlarmScheduler
import com.romankozak.forwardappmobile.ui.common.IconProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProjectLocalDataSource(
        forwardAppDatabase: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectLocalDataSource = ProjectLocalDataSource(forwardAppDatabase.projectsQueries, ioDispatcher)

    @Provides
    @Singleton
    fun provideReminderRepository(
        forwardAppDatabase: ForwardAppDatabase,
        alarmScheduler: AlarmScheduler,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): com.romankozak.forwardappmobile.shared.features.reminders.data.repository.ReminderRepository =
        com.romankozak.forwardappmobile.shared.features.reminders.data.repository.ReminderRepository(
            forwardAppDatabase.remindersQueries,
            alarmScheduler,
            ioDispatcher,
        )

    @Provides
    @Singleton
    fun provideProjectLogRepository(
        forwardAppDatabase: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectLogRepository = ProjectLogRepository(forwardAppDatabase.projectExecutionLogQueries, ioDispatcher)

    @Provides
    @Singleton
    fun provideProjectArtifactRepository(
        forwardAppDatabase: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectArtifactRepository =
        ProjectArtifactRepository(forwardAppDatabase.projectArtifactsQueries, ioDispatcher)

    @Provides
    @Singleton
    fun provideRecentItemsRepository(
        forwardAppDatabase: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RecentItemsRepository = RecentItemsRepository(forwardAppDatabase.recentItemQueries, ioDispatcher)

    @Provides
    @Singleton
    fun provideNoteBacklogLinkDataSource(
        listItemDao: ListItemDao,
    ): NoteBacklogLinkDataSource = AndroidNoteBacklogLinkDataSource(listItemDao)

    @Provides
    @Singleton
    fun provideLegacyNoteRepository(
        forwardAppDatabase: ForwardAppDatabase,
        backlogLinkDataSource: NoteBacklogLinkDataSource,
        recentItemsRepository: RecentItemsRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): LegacyNoteRepository =
        LegacyNoteRepository(forwardAppDatabase.legacyNoteQueries, backlogLinkDataSource, recentItemsRepository, ioDispatcher)

    @Provides
    @Singleton
    fun provideAttachmentRepository(
        forwardAppDatabase: ForwardAppDatabase,
        linkItemDataSource: LinkItemDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AttachmentRepository = AttachmentRepository(forwardAppDatabase.attachmentQueries, linkItemDataSource, ioDispatcher)

    @Provides
    @Singleton
    fun provideLinkItemDataSource(
        linkItemDao: LinkItemDao,
    ): LinkItemDataSource = AndroidLinkItemDataSource(linkItemDao)

    @Provides
    @Singleton
    fun provideNoteDocumentRepository(
        forwardAppDatabase: ForwardAppDatabase,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): NoteDocumentRepository =
        NoteDocumentRepository(forwardAppDatabase.noteDocumentQueries, attachmentRepository, recentItemsRepository, ioDispatcher)

    @Provides
    @Singleton
    fun provideChecklistRepository(
        forwardAppDatabase: ForwardAppDatabase,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ChecklistRepository =
        ChecklistRepository(forwardAppDatabase, attachmentRepository, recentItemsRepository, ioDispatcher)

    @Provides
    @Singleton
    fun provideActivityRepository(
        activityRecordDao: ActivityRecordDao,
        goalDao: GoalDao,
        projectLocalDataSource: ProjectLocalDataSource,
    ): ActivityRepository = ActivityRepository(activityRecordDao, goalDao, projectLocalDataSource)

    @Provides
    @Singleton
    fun provideGoalRepository(
        goalDao: GoalDao,
        listItemDao: ListItemDao,
        reminderRepository: com.romankozak.forwardappmobile.shared.features.reminders.data.repository.ReminderRepository,
        contextHandlerProvider: Provider<ContextHandler>,
        projectLocalDataSource: ProjectLocalDataSource,
    ): GoalRepository = GoalRepository(goalDao, listItemDao, reminderRepository, contextHandlerProvider, projectLocalDataSource)

    @Provides
    @Singleton
    fun provideInboxRepository(
        inboxRecordDao: InboxRecordDao,
        goalRepository: GoalRepository
    ): InboxRepository = InboxRepository(inboxRecordDao, goalRepository)

    @Provides
    @Singleton
    fun provideListItemRepository(
        listItemDao: ListItemDao,
        linkItemDao: LinkItemDao
    ): ListItemRepository = ListItemRepository(listItemDao, linkItemDao)

    @Provides
    @Singleton
    fun provideProjectTimeTrackingRepository(
        activityRepository: ActivityRepository,
        listItemDao: ListItemDao,
        projectLogRepository: ProjectLogRepository
    ): ProjectTimeTrackingRepository = ProjectTimeTrackingRepository(activityRepository, listItemDao, projectLogRepository)

    @Provides
    @Singleton
    fun provideSearchRepository(
        projectLocalDataSource: ProjectLocalDataSource,
        listItemDao: ListItemDao,
        activityRepository: ActivityRepository,
        inboxRecordDao: InboxRecordDao,
    ): SearchRepository = SearchRepository(projectLocalDataSource, listItemDao, activityRepository, inboxRecordDao)

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectLocalDataSource: ProjectLocalDataSource,
        legacyNoteRepository: com.romankozak.forwardappmobile.features.notes.data.LegacyNoteRepository,
        activityRepository: ActivityRepository,
        recentItemsRepository: com.romankozak.forwardappmobile.shared.features.recentitems.data.RecentItemsRepository,
        reminderRepository: com.romankozak.forwardappmobile.shared.features.reminders.data.repository.ReminderRepository,
        projectLogRepository: ProjectLogRepository,
        searchRepository: SearchRepository,
        noteDocumentRepository: com.romankozak.forwardappmobile.features.notes.data.NoteDocumentRepository,
        checklistRepository: com.romankozak.forwardappmobile.features.checklists.data.ChecklistRepository,
        attachmentRepository: com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository,
        goalRepository: GoalRepository,
        inboxRepository: InboxRepository,
        projectTimeTrackingRepository: ProjectTimeTrackingRepository,
        projectArtifactRepository: ProjectArtifactRepository,
        listItemRepository: ListItemRepository,
    ): ProjectRepositoryCore = ProjectRepositoryImpl(
        projectLocalDataSource,
        legacyNoteRepository,
        activityRepository,
        recentItemsRepository,
        reminderRepository,
        projectLogRepository,
        searchRepository,
        noteDocumentRepository,
        checklistRepository,
        attachmentRepository,
        goalRepository,
        inboxRepository,
        projectTimeTrackingRepository,
        projectArtifactRepository,
        listItemRepository,
    )

}