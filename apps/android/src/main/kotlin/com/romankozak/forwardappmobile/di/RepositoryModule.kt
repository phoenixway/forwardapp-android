package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.features.attachments.data.repository.AttachmentsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.attachments.domain.repository.AttachmentsRepository
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.data.repository.LinkItemsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.attachments.linkitems.domain.repository.LinkItemsRepository
import com.romankozak.forwardappmobile.shared.features.activitytracker.data.repository.ActivityRecordsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.activitytracker.domain.repository.ActivityRecordsRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.data.repository.LegacyNotesRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.attachments.types.legacynotes.domain.repository.LegacyNotesRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.repository.DayPlanRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.data.repository.DayTaskRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository.DayPlanRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.dayplan.domain.repository.DayTaskRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.data.repository.DailyMetricsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.daymanagement.dailymetrics.domain.repository.DailyMetricsRepository
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.data.repository.RecurringTaskRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.daymanagement.recurringtasks.domain.repository.RecurringTaskRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.data.repository.NoteDocumentsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.attachments.types.notedocuments.domain.repository.NoteDocumentsRepository
import com.romankozak.forwardappmobile.shared.features.projects.logs.data.repository.ProjectExecutionLogsRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.logs.domain.repository.ProjectExecutionLogsRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.data.repository.ProjectArtifactRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.views.advancedview.projectartifacts.domain.repository.ProjectArtifactRepository
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.data.repository.InboxRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.views.inbox.domain.repository.InboxRepository
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.data.repository.ChecklistRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.attachments.types.checklists.domain.repository.ChecklistRepository
import com.romankozak.forwardappmobile.shared.features.reminders.data.repository.RemindersRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.reminders.domain.repository.RemindersRepository
import com.romankozak.forwardappmobile.shared.features.projects.core.data.repository.ProjectRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.repository.ProjectRepository
import com.romankozak.forwardappmobile.shared.features.recent.data.repository.RecentItemRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.recent.domain.repository.RecentItemRepository
import com.romankozak.forwardappmobile.shared.features.aichat.data.repository.ChatRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.aichat.data.repository.ConversationFolderRepositoryImpl
import com.romankozak.forwardappmobile.shared.features.aichat.domain.repository.ChatRepository
import com.romankozak.forwardappmobile.shared.features.aichat.domain.repository.ConversationFolderRepository
import kotlinx.coroutines.CoroutineDispatcher
import me.tatarka.inject.annotations.Provides

interface RepositoryModule {

    @Provides
    @AndroidSingleton
    fun provideProjectRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectRepository = ProjectRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideRecentItemRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RecentItemRepository = RecentItemRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideConversationFolderRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ConversationFolderRepository = ConversationFolderRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideChatRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ChatRepository = ChatRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideInboxRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): InboxRepository = InboxRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideLegacyNotesRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): LegacyNotesRepository = LegacyNotesRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideNoteDocumentsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): NoteDocumentsRepository = NoteDocumentsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideProjectArtifactRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectArtifactRepository = ProjectArtifactRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideProjectExecutionLogsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ProjectExecutionLogsRepository = ProjectExecutionLogsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideAttachmentsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): AttachmentsRepository = AttachmentsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideLinkItemsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): LinkItemsRepository = LinkItemsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideChecklistRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ChecklistRepository = ChecklistRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideActivityRecordsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): ActivityRecordsRepository = ActivityRecordsRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideRemindersRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RemindersRepository = RemindersRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideDayPlanRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DayPlanRepository = DayPlanRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideDayTaskRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DayTaskRepository = DayTaskRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideRecurringTaskRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): RecurringTaskRepository = RecurringTaskRepositoryImpl(database, ioDispatcher)

    @Provides
    @AndroidSingleton
    fun provideDailyMetricsRepository(
        database: ForwardAppDatabase,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ): DailyMetricsRepository = DailyMetricsRepositoryImpl(database, ioDispatcher)
}
