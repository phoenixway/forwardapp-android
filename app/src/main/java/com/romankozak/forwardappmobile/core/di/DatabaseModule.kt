@file:Suppress("MaxLineLength", "UnusedParameter", "TooManyFunctions", "SpreadOperator")

package com.romankozak.forwardappmobile.core.di

import android.content.Context
import androidx.room.Room
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.ScriptDao
import com.romankozak.forwardappmobile.data.database.ALL_MIGRATIONS
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.BacklogOrderDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextArtifactDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextInboxSortingDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextKeyProblemsDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextParentLinkDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextTagRefDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.DirectionDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordLinkDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.MusicNoteDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.features.mainscreen.core.MainBeaconDao
import com.romankozak.forwardappmobile.features.mainscreen.arc.ArcQuestDao
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.features.missions.data.TacticalActivitySlotDao
import com.romankozak.forwardappmobile.features.missions.data.TacticalIterationDao
import com.romankozak.forwardappmobile.features.missions.data.MissionStreamDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "forward_app_database",
        ).addMigrations(*ALL_MIGRATIONS).build()
    }

    @Provides
    @Singleton
    fun provideContextDao(appDatabase: AppDatabase): ContextDao = appDatabase.contextDao()

    @Provides
    @Singleton
    fun provideContextTagRefDao(appDatabase: AppDatabase): ContextTagRefDao = appDatabase.contextTagRefDao()

    @Provides
    @Singleton
    fun provideGoalDao(appDatabase: AppDatabase): GoalDao = appDatabase.goalDao()

    @Provides
    @Singleton
    fun provideListItemDao(appDatabase: AppDatabase): ListItemDao = appDatabase.listItemDao()

    @Provides
    @Singleton
    fun provideLegacyNoteDao(appDatabase: AppDatabase): LegacyNoteDao = appDatabase.legacyNoteDao()

    @Provides
    @Singleton
    fun provideAttachmentDao(appDatabase: AppDatabase): AttachmentDao = appDatabase.attachmentDao()

    @Provides
    @Singleton
    fun provideRecentItemDao(appDatabase: AppDatabase) = appDatabase.recentItemDao()

    @Provides
    @Singleton
    fun provideReminderDao(appDatabase: AppDatabase) = appDatabase.reminderDao()

    @Provides
    @Singleton
    fun provideActivityRecordDao(appDatabase: AppDatabase) = appDatabase.activityRecordDao()

    @Provides
    @Singleton
    fun provideContextManagementDao(appDatabase: AppDatabase): ContextManagementDao = appDatabase.contextManagementDao()

    @Provides
    @Singleton
    fun provideContextParentLinkDao(appDatabase: AppDatabase): ContextParentLinkDao = appDatabase.contextParentLinkDao()

    @Provides
    @Singleton
    fun provideContextKeyProblemsDao(appDatabase: AppDatabase): ContextKeyProblemsDao = appDatabase.contextKeyProblemsDao()

    @Provides
    @Singleton
    fun provideContextInboxSortingDao(appDatabase: AppDatabase): ContextInboxSortingDao = appDatabase.contextInboxSortingDao()

    @Provides
    @Singleton
    fun provideAiEventDao(appDatabase: AppDatabase) = appDatabase.aiEventDao()

    @Provides
    @Singleton
    fun provideLifeManagementLevelStatusDao(appDatabase: AppDatabase) = appDatabase.lifeManagementLevelStatusDao()

    @Provides
    @Singleton
    fun provideLifeSystemStateDao(appDatabase: AppDatabase) = appDatabase.lifeSystemStateDao()

    @Provides
    @Singleton
    fun provideAiInsightDao(appDatabase: AppDatabase): AiInsightDao = appDatabase.aiInsightDao()

    @Provides
    @Singleton
    fun provideUserStateIntervalDao(appDatabase: AppDatabase) = appDatabase.userStateIntervalDao()

    @Provides
    @Singleton
    fun provideFocusContextIntervalDao(appDatabase: AppDatabase) = appDatabase.focusContextIntervalDao()

    @Provides
    @Singleton
    fun provideLinkItemDao(appDatabase: AppDatabase): LinkItemDao = appDatabase.linkItemDao()

    @Provides
    @Singleton
    fun provideMainBeaconDao(appDatabase: AppDatabase): MainBeaconDao = appDatabase.mainBeaconDao()

    @Provides
    @Singleton
    fun provideDirectionDao(appDatabase: AppDatabase): DirectionDao = appDatabase.directionDao()

    @Provides
    @Singleton
    fun provideArcQuestDao(appDatabase: AppDatabase): ArcQuestDao = appDatabase.arcQuestDao()

    @Provides
    @Singleton
    fun provideInboxRecordDao(appDatabase: AppDatabase): InboxRecordDao = appDatabase.inboxRecordDao()

    @Provides
    @Singleton
    fun provideInboxRecordLinkDao(appDatabase: AppDatabase): InboxRecordLinkDao = appDatabase.inboxRecordLinkDao()

    @Provides
    @Singleton
    fun provideNoteDocumentDao(appDatabase: AppDatabase): NoteDocumentDao = appDatabase.noteDocumentDao()

    @Provides
    @Singleton
    fun provideMusicNoteDao(appDatabase: AppDatabase): MusicNoteDao = appDatabase.musicNoteDao()

    @Provides
    @Singleton
    fun provideChecklistDao(appDatabase: AppDatabase): ChecklistDao = appDatabase.checklistDao()

    @Provides
    @Singleton
    fun provideContextArtifactDao(appDatabase: AppDatabase): ContextArtifactDao = appDatabase.contextArtifactDao()

    @Provides
    @Singleton
    fun provideDayPlanDao(appDatabase: AppDatabase) = appDatabase.dayPlanDao()

    @Provides
    @Singleton
    fun provideDayFocusItemDao(appDatabase: AppDatabase) = appDatabase.dayFocusItemDao()

    @Provides
    @Singleton
    fun provideDayTaskDao(appDatabase: AppDatabase) = appDatabase.dayTaskDao()

    @Provides
    @Singleton
    fun provideDailyMetricDao(appDatabase: AppDatabase) = appDatabase.dailyMetricDao()

    @Provides
    @Singleton
    fun provideRecurringTaskDao(appDatabase: AppDatabase) = appDatabase.recurringTaskDao()

    @Provides
    @Singleton
    fun provideChatDao(appDatabase: AppDatabase) = appDatabase.chatDao()

    @Provides
    @Singleton
    fun provideConversationFolderDao(appDatabase: AppDatabase) = appDatabase.conversationFolderDao()

    @Provides
    @Singleton
    fun provideSystemAppDao(appDatabase: AppDatabase) = appDatabase.systemAppDao()

    @Provides
    @Singleton
    fun provideScriptDao(appDatabase: AppDatabase): ScriptDao = appDatabase.scriptDao()

    @Provides
    @Singleton
    fun provideBacklogOrderDao(appDatabase: AppDatabase): BacklogOrderDao = appDatabase.backlogOrderDao()

    @Provides
    @Singleton
    fun provideTacticalMissionDao(appDatabase: AppDatabase): TacticalMissionDao = appDatabase.tacticalMissionDao()

    @Provides
    @Singleton
    fun provideTacticalActivitySlotDao(appDatabase: AppDatabase): TacticalActivitySlotDao = appDatabase.tacticalActivitySlotDao()

    @Provides
    @Singleton
    fun provideTacticalIterationDao(appDatabase: AppDatabase): TacticalIterationDao = appDatabase.tacticalIterationDao()

    @Provides
    @Singleton
    fun provideMissionStreamDao(appDatabase: AppDatabase): MissionStreamDao = appDatabase.missionStreamDao()

    @Provides
    @Singleton
    fun provideStructurePresetDao(appDatabase: AppDatabase): StructurePresetDao = appDatabase.structurePresetDao()

    @Provides
    @Singleton
    fun provideStructurePresetItemDao(appDatabase: AppDatabase): StructurePresetItemDao = appDatabase.structurePresetItemDao()

    @Provides
    @Singleton
    fun provideContextStructureDao(appDatabase: AppDatabase): ContextStructureDao = appDatabase.contextStructureDao()
}
