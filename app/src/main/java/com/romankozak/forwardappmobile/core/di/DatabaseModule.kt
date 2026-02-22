package com.romankozak.forwardappmobile.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.ScriptDao
import com.romankozak.forwardappmobile.data.database.MIGRATION_10_11
import com.romankozak.forwardappmobile.data.database.MIGRATION_11_12
import com.romankozak.forwardappmobile.data.database.MIGRATION_12_13
import com.romankozak.forwardappmobile.data.database.MIGRATION_13_14
import com.romankozak.forwardappmobile.data.database.MIGRATION_14_15
import com.romankozak.forwardappmobile.data.database.MIGRATION_15_16
import com.romankozak.forwardappmobile.data.database.MIGRATION_16_17
import com.romankozak.forwardappmobile.data.database.MIGRATION_17_18
import com.romankozak.forwardappmobile.data.database.MIGRATION_18_19
import com.romankozak.forwardappmobile.data.database.MIGRATION_19_20
import com.romankozak.forwardappmobile.data.database.MIGRATION_20_21
import com.romankozak.forwardappmobile.data.database.MIGRATION_21_22
import com.romankozak.forwardappmobile.data.database.MIGRATION_22_23
import com.romankozak.forwardappmobile.data.database.MIGRATION_23_24
import com.romankozak.forwardappmobile.data.database.MIGRATION_24_25
import com.romankozak.forwardappmobile.data.database.MIGRATION_25_26
import com.romankozak.forwardappmobile.data.database.MIGRATION_26_27
import com.romankozak.forwardappmobile.data.database.MIGRATION_27_28
import com.romankozak.forwardappmobile.data.database.MIGRATION_28_29
import com.romankozak.forwardappmobile.data.database.MIGRATION_29_30
import com.romankozak.forwardappmobile.data.database.MIGRATION_30_31
import com.romankozak.forwardappmobile.data.database.MIGRATION_31_32
import com.romankozak.forwardappmobile.data.database.MIGRATION_32_33
import com.romankozak.forwardappmobile.data.database.MIGRATION_33_34
import com.romankozak.forwardappmobile.data.database.MIGRATION_34_35
import com.romankozak.forwardappmobile.data.database.MIGRATION_35_36
import com.romankozak.forwardappmobile.data.database.MIGRATION_36_37
import com.romankozak.forwardappmobile.data.database.MIGRATION_37_38
import com.romankozak.forwardappmobile.data.database.MIGRATION_38_39
import com.romankozak.forwardappmobile.data.database.MIGRATION_39_40
import com.romankozak.forwardappmobile.data.database.MIGRATION_40_41
import com.romankozak.forwardappmobile.data.database.MIGRATION_41_42
import com.romankozak.forwardappmobile.data.database.MIGRATION_42_43
import com.romankozak.forwardappmobile.data.database.MIGRATION_44_45
import com.romankozak.forwardappmobile.data.database.MIGRATION_45_46
import com.romankozak.forwardappmobile.data.database.MIGRATION_46_47
import com.romankozak.forwardappmobile.data.database.MIGRATION_47_48
import com.romankozak.forwardappmobile.data.database.MIGRATION_48_49
import com.romankozak.forwardappmobile.data.database.MIGRATION_49_50
import com.romankozak.forwardappmobile.data.database.MIGRATION_50_51
import com.romankozak.forwardappmobile.data.database.MIGRATION_51_52
import com.romankozak.forwardappmobile.data.database.MIGRATION_52_53
import com.romankozak.forwardappmobile.data.database.MIGRATION_53_54
import com.romankozak.forwardappmobile.data.database.MIGRATION_54_55
import com.romankozak.forwardappmobile.data.database.MIGRATION_55_56
import com.romankozak.forwardappmobile.data.database.MIGRATION_57_58
import com.romankozak.forwardappmobile.data.database.MIGRATION_58_59
import com.romankozak.forwardappmobile.data.database.MIGRATION_59_60
import com.romankozak.forwardappmobile.data.database.MIGRATION_60_61
import com.romankozak.forwardappmobile.data.database.MIGRATION_61_62
import com.romankozak.forwardappmobile.data.database.MIGRATION_62_63
import com.romankozak.forwardappmobile.data.database.MIGRATION_63_64
import com.romankozak.forwardappmobile.data.database.MIGRATION_64_65
import com.romankozak.forwardappmobile.data.database.MIGRATION_65_66
import com.romankozak.forwardappmobile.data.database.MIGRATION_66_67
import com.romankozak.forwardappmobile.data.database.MIGRATION_67_68
import com.romankozak.forwardappmobile.data.database.MIGRATION_68_69
import com.romankozak.forwardappmobile.data.database.MIGRATION_69_70
import com.romankozak.forwardappmobile.data.database.MIGRATION_70_71
import com.romankozak.forwardappmobile.data.database.MIGRATION_71_72
import com.romankozak.forwardappmobile.data.database.MIGRATION_72_73
import com.romankozak.forwardappmobile.data.database.MIGRATION_73_74
import com.romankozak.forwardappmobile.data.database.MIGRATION_74_75
import com.romankozak.forwardappmobile.data.database.MIGRATION_75_76
import com.romankozak.forwardappmobile.data.database.MIGRATION_76_77
import com.romankozak.forwardappmobile.data.database.MIGRATION_79_80
import com.romankozak.forwardappmobile.data.database.MIGRATION_80_81
import com.romankozak.forwardappmobile.data.database.MIGRATION_81_82
import com.romankozak.forwardappmobile.data.database.MIGRATION_82_83
import com.romankozak.forwardappmobile.data.database.MIGRATION_83_84
import com.romankozak.forwardappmobile.data.database.MIGRATION_84_85
import com.romankozak.forwardappmobile.data.database.MIGRATION_85_86
import com.romankozak.forwardappmobile.data.database.MIGRATION_86_87
import com.romankozak.forwardappmobile.data.database.MIGRATION_87_88
import com.romankozak.forwardappmobile.data.database.MIGRATION_88_89
import com.romankozak.forwardappmobile.data.database.MIGRATION_89_90
import com.romankozak.forwardappmobile.data.database.MIGRATION_8_9
import com.romankozak.forwardappmobile.data.database.MIGRATION_90_91
import com.romankozak.forwardappmobile.data.database.MIGRATION_91_92
import com.romankozak.forwardappmobile.data.database.MIGRATION_92_93
import com.romankozak.forwardappmobile.data.database.MIGRATION_94_95
import com.romankozak.forwardappmobile.data.database.MIGRATION_100_101
import com.romankozak.forwardappmobile.data.database.MIGRATION_101_102
import com.romankozak.forwardappmobile.data.database.MIGRATION_102_103
import com.romankozak.forwardappmobile.data.database.MIGRATION_103_104
import com.romankozak.forwardappmobile.data.database.MIGRATION_104_105
import com.romankozak.forwardappmobile.data.database.MIGRATION_105_106
import com.romankozak.forwardappmobile.data.database.MIGRATION_106_107
import com.romankozak.forwardappmobile.data.database.MIGRATION_107_108
import com.romankozak.forwardappmobile.data.database.MIGRATION_108_109
import com.romankozak.forwardappmobile.data.database.MIGRATION_109_110
import com.romankozak.forwardappmobile.data.database.MIGRATION_110_111
import com.romankozak.forwardappmobile.data.database.MIGRATION_111_112
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.ai.data.dao.AiInsightDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.BacklogOrderDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextArtifactDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextKeyProblemsDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextInboxSortingDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextStructureDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.DirectionDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.GoalDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ListItemDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.MusicNoteDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.StructurePresetItemDao
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

val MIGRATION_93_94 =
    object : Migration(93, 94) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Rename tables
            db.execSQL("ALTER TABLE projects RENAME TO contexts")
            db.execSQL("ALTER TABLE project_execution_logs RENAME TO context_execution_logs")
            db.execSQL("ALTER TABLE project_structures RENAME TO context_structures")
            db.execSQL("ALTER TABLE project_structure_items RENAME TO context_structure_items")
            db.execSQL("ALTER TABLE project_artifacts RENAME TO context_artifacts")
            // No direct rename for project_attachment_cross_ref as it might not exist or be handled differently.
            // It was a generated file, so it might not be in the Room schema directly.

            // 2. Rename columns in 'contexts' table
            db.execSQL("ALTER TABLE contexts RENAME COLUMN is_project_management_enabled TO is_context_management_enabled")
            db.execSQL("ALTER TABLE contexts RENAME COLUMN project_status TO context_status")
            db.execSQL("ALTER TABLE contexts RENAME COLUMN project_status_text TO context_status_text")
            db.execSQL("ALTER TABLE contexts RENAME COLUMN project_log_level TO context_log_level")
            db.execSQL("ALTER TABLE contexts RENAME COLUMN project_type TO context_type")

            // 3. Rename columns in other tables (foreign keys)
            db.execSQL("ALTER TABLE system_apps RENAME COLUMN project_id TO context_id")
            // No direct rename for inbox_records's projectId because Room handles it via annotations.
            db.execSQL("ALTER TABLE list_items RENAME COLUMN project_id TO context_id")
            // No direct rename for backlog_orders, as it refers to "list_id" not "project_id"
        }
    }

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
        ).fallbackToDestructiveMigration().addMigrations(
            MIGRATION_8_9,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14,
            MIGRATION_14_15,
            MIGRATION_15_16,
            MIGRATION_16_17,
            MIGRATION_17_18,
            MIGRATION_18_19,
            MIGRATION_19_20,
            MIGRATION_20_21,
            MIGRATION_21_22,
            MIGRATION_22_23,
            MIGRATION_23_24,
            MIGRATION_24_25,
            MIGRATION_25_26,
            MIGRATION_26_27,
            MIGRATION_27_28,
            MIGRATION_28_29,
            MIGRATION_29_30,
            MIGRATION_30_31,
            MIGRATION_31_32,
            MIGRATION_32_33,
            MIGRATION_33_34,
            MIGRATION_34_35,
            MIGRATION_35_36,
            MIGRATION_36_37,
            MIGRATION_37_38,
            MIGRATION_38_39,
            MIGRATION_39_40,
            MIGRATION_40_41,
            MIGRATION_41_42,
            MIGRATION_42_43,
            MIGRATION_44_45,
            MIGRATION_45_46,
            MIGRATION_46_47,
            MIGRATION_47_48,
            MIGRATION_48_49,
            MIGRATION_49_50,
            MIGRATION_50_51,
            MIGRATION_51_52,
            MIGRATION_52_53,
            MIGRATION_53_54,
            MIGRATION_54_55,
            MIGRATION_55_56,
            MIGRATION_57_58,
            MIGRATION_58_59,
            MIGRATION_59_60,
            MIGRATION_60_61,
            MIGRATION_61_62,
            MIGRATION_62_63,
            MIGRATION_63_64,
            MIGRATION_64_65,
            MIGRATION_65_66,
            MIGRATION_66_67,
            MIGRATION_67_68,
            MIGRATION_68_69,
            MIGRATION_69_70,
            MIGRATION_70_71,
            MIGRATION_71_72,
            MIGRATION_72_73,
            MIGRATION_73_74,
            MIGRATION_74_75,
            MIGRATION_75_76,
            MIGRATION_76_77,
            MIGRATION_79_80,
            MIGRATION_80_81,
            MIGRATION_81_82,
            MIGRATION_82_83,
            MIGRATION_83_84,
            MIGRATION_84_85,
            MIGRATION_85_86,
            MIGRATION_86_87,
            MIGRATION_87_88,
            MIGRATION_88_89,
            MIGRATION_89_90,
            MIGRATION_90_91,
            MIGRATION_91_92,
            MIGRATION_92_93,
            MIGRATION_93_94,
            MIGRATION_94_95,
            MIGRATION_100_101,
            MIGRATION_101_102,
            MIGRATION_102_103,
            MIGRATION_103_104,
            MIGRATION_104_105,
            MIGRATION_105_106,
            MIGRATION_106_107,
            MIGRATION_107_108,
            MIGRATION_108_109,
            MIGRATION_109_110,
            MIGRATION_110_111,
            MIGRATION_111_112,
        ).build()
    }

    @Provides
    @Singleton
    fun provideContextDao(appDatabase: AppDatabase): ContextDao = appDatabase.contextDao()

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
    fun provideContextKeyProblemsDao(appDatabase: AppDatabase): ContextKeyProblemsDao = appDatabase.contextKeyProblemsDao()

    @Provides
    @Singleton
    fun provideContextInboxSortingDao(appDatabase: AppDatabase): ContextInboxSortingDao = appDatabase.contextInboxSortingDao()

    @Provides
    @Singleton
    fun provideAiEventDao(appDatabase: AppDatabase) = appDatabase.aiEventDao()

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
    fun provideDirectionDao(appDatabase: AppDatabase): DirectionDao = appDatabase.directionDao()

    @Provides
    @Singleton
    fun provideInboxRecordDao(appDatabase: AppDatabase): InboxRecordDao = appDatabase.inboxRecordDao()

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
    fun provideStructurePresetDao(appDatabase: AppDatabase): StructurePresetDao = appDatabase.structurePresetDao()

    @Provides
    @Singleton
    fun provideStructurePresetItemDao(appDatabase: AppDatabase): StructurePresetItemDao = appDatabase.structurePresetItemDao()

    @Provides
    @Singleton
    fun provideContextStructureDao(appDatabase: AppDatabase): ContextStructureDao = appDatabase.contextStructureDao()
}
