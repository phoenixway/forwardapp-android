package com.romankozak.forwardappmobile.di

import ProjectRepositoryImpl
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
//import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.logic.ContextHandler
import com.romankozak.forwardappmobile.data.notes.AndroidNoteBacklogLinkDataSource
import com.romankozak.forwardappmobile.data.repository.ActivityRepository
import com.romankozak.forwardappmobile.data.repository.GoalRepository
import com.romankozak.forwardappmobile.data.repository.InboxRepository
import com.romankozak.forwardappmobile.data.repository.ListItemRepository
import com.romankozak.forwardappmobile.data.repository.ProjectTimeTrackingRepository
import com.romankozak.forwardappmobile.data.repository.SearchRepository
import com.romankozak.forwardappmobile.features.attachments.data.AndroidLinkItemDataSource
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository
import com.romankozak.forwardappmobile.features.checklists.data.ChecklistRepository
import com.romankozak.forwardappmobile.features.notes.data.LegacyNoteRepository
import com.romankozak.forwardappmobile.features.notes.data.NoteDocumentRepository
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectArtifactRepository
import com.romankozak.forwardappmobile.features.projects.data.artifacts.ProjectArtifactRepositoryImpl
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.data.model.LinkItemDataSource
import com.romankozak.forwardappmobile.shared.features.notes.data.datasource.NoteBacklogLinkDataSource
import com.romankozak.forwardappmobile.shared.features.projects.domain.ProjectRepositoryCore
import com.romankozak.forwardappmobile.shared.features.recentitems.data.RecentItemsRepository
import com.romankozak.forwardappmobile.shared.features.reminders.data.repository.ReminderRepository
import com.romankozak.forwardappmobile.shared.features.reminders.domain.AlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Provider
import javax.inject.Singleton
import com.romankozak.forwardappmobile.shared.features.projects.data.ProjectLocalDataSource
import com.romankozak.forwardappmobile.shared.features.projects.data.ProjectLocalDataSourceImpl
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.shared.features.inbox.InboxRecordRepository
import com.romankozak.forwardappmobile.shared.features.inbox.InboxRecordRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.data.logs.ProjectLogRepository
import com.romankozak.forwardappmobile.shared.features.recurring_tasks.RecurringTaskRepository
import com.romankozak.forwardappmobile.shared.features.recurring_tasks.RecurringTaskRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.daily_metrics.DailyMetricRepository
import com.romankozak.forwardappmobile.shared.features.daily_metrics.DailyMetricRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.conversations.ConversationFolderRepository
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.ProjectExecutionLogRepository
import com.romankozak.forwardappmobile.shared.features.projects.data.logs.ProjectExecutionLogRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.conversations.ConversationFolderRepositoryImpl


@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideProjectExecutionLogRepository(
        db: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ProjectExecutionLogRepository = ProjectExecutionLogRepositoryImpl(db, ioDispatcher)

    @Provides
    @Singleton
    fun provideConversationFolderRepository(
        db: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ConversationFolderRepository = ConversationFolderRepositoryImpl(db, ioDispatcher)

    @Provides
    @Singleton
    fun provideDailyMetricRepository(
        db: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): DailyMetricRepository = DailyMetricRepositoryImpl(db, ioDispatcher)

    @Provides
    @Singleton
    fun provideRecurringTaskRepository(
        db: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): RecurringTaskRepository = RecurringTaskRepositoryImpl(db, ioDispatcher)

    @Provides
    @Singleton
    fun provideInboxRecordRepository(
        db: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): InboxRecordRepository = InboxRecordRepositoryImpl(db, ioDispatcher)

    @Provides
    @Singleton
    fun provideSqlDriver(@ApplicationContext ctx: Context): SqlDriver =
        AndroidSqliteDriver(
            schema = ForwardAppDatabase.Schema,
            context = ctx,
            name = "forwardapp.db"
        )

    @Provides
    @Singleton
    fun provideDatabase(driver: SqlDriver): ForwardAppDatabase =
        ForwardAppDatabase(driver)

    @Provides
    @Singleton
    fun provideProjectLocalDataSource(
        db: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ProjectLocalDataSource = ProjectLocalDataSourceImpl(db, ioDispatcher)


    @Provides
    @Singleton
    fun provideReminderRepository(
        db: ForwardAppDatabase,
        alarmScheduler: AlarmScheduler,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ReminderRepository =
        ReminderRepository(
            db,
            alarmScheduler,
            ioDispatcher,
        )




    @Provides
    @Singleton
    fun provideProjectArtifactRepository(
        db: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectArtifactRepository =
        ProjectArtifactRepositoryImpl(db, ioDispatcher)


    @Provides
    @Singleton
    fun provideRecentItemsRepository(
        db: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RecentItemsRepository = RecentItemsRepository(db, ioDispatcher)

    @Provides
    @Singleton
    fun provideNoteBacklogLinkDataSource(
        listItemDao: ListItemDao,
    ): NoteBacklogLinkDataSource = AndroidNoteBacklogLinkDataSource(listItemDao)

    @Provides
    @Singleton
    fun provideLegacyNoteRepository(
        db: ForwardAppDatabase,
        backlogLinkDataSource: NoteBacklogLinkDataSource,
        recentItemsRepository: RecentItemsRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): LegacyNoteRepository =
        LegacyNoteRepository(db, backlogLinkDataSource, recentItemsRepository, ioDispatcher)

    @Provides
    @Singleton
    fun provideAttachmentRepository(
        db: ForwardAppDatabase,
        linkItemDataSource: LinkItemDataSource,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AttachmentRepository = AttachmentRepository(db, linkItemDataSource, ioDispatcher)

    @Provides
    @Singleton
    fun provideLinkItemDataSource(
        linkItemDao: LinkItemDao,
    ): LinkItemDataSource = AndroidLinkItemDataSource(linkItemDao)

    @Provides
    @Singleton
    fun provideNoteDocumentRepository(
        db: ForwardAppDatabase,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): NoteDocumentRepository =
        NoteDocumentRepository(db, attachmentRepository, recentItemsRepository, ioDispatcher)

    @Provides
    @Singleton
    fun provideChecklistRepository(
        db: ForwardAppDatabase,
        attachmentRepository: AttachmentRepository,
        recentItemsRepository: RecentItemsRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ChecklistRepository =
        ChecklistRepository(db, attachmentRepository, recentItemsRepository, ioDispatcher)

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
        reminderRepository: ReminderRepository,
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
        legacyNoteRepository: LegacyNoteRepository,
        activityRepository: ActivityRepository,
        recentItemsRepository: RecentItemsRepository,
        reminderRepository: ReminderRepository,
        projectLogRepository: ProjectExecutionLogRepository,
        searchRepository: SearchRepository,
        noteDocumentRepository: NoteDocumentRepository,
        checklistRepository: ChecklistRepository,
        attachmentRepository: AttachmentRepository,
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
