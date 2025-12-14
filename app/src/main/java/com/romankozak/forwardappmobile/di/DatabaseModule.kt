package com.romankozak.forwardappmobile.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.dao.LegacyNoteDao
import com.romankozak.forwardappmobile.data.dao.NoteDocumentDao
import com.romankozak.forwardappmobile.data.dao.ProjectDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.ListItemDao
import com.romankozak.forwardappmobile.data.dao.RecentItemDao
import com.romankozak.forwardappmobile.data.dao.ReminderDao
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.LinkItemDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao
import com.romankozak.forwardappmobile.data.dao.ProjectArtifactDao
import com.romankozak.forwardappmobile.data.dao.DayPlanDao
import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.dao.DailyMetricDao
import com.romankozak.forwardappmobile.data.dao.RecurringTaskDao
import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.dao.ConversationFolderDao
import com.romankozak.forwardappmobile.data.dao.ChecklistDao
import com.romankozak.forwardappmobile.data.dao.ScriptDao
import com.romankozak.forwardappmobile.data.dao.BacklogOrderDao
import com.romankozak.forwardappmobile.features.missions.data.TacticalMissionDao
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.room.RoomDatabase
import com.romankozak.forwardappmobile.data.database.DatabaseInitializer
import com.romankozak.forwardappmobile.data.database.migrateSpecialProjects
import com.romankozak.forwardappmobile.data.database.MIGRATION_8_9
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
import com.romankozak.forwardappmobile.data.repository.SystemAppRepository
import com.romankozak.forwardappmobile.features.attachments.data.AttachmentRepository

private lateinit var db: AppDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): AppDatabase {
        val callback = object : RoomDatabase.Callback() {
            override fun onOpen(dbSupport: SupportSQLiteDatabase) {
                super.onOpen(dbSupport)
                scope.launch(Dispatchers.IO) {
                    val attachmentRepository = AttachmentRepository(db.attachmentDao(), db.linkItemDao())
                    val systemAppRepository = SystemAppRepository(db.systemAppDao(), db.projectDao(), db.noteDocumentDao(), attachmentRepository)
                    val databaseInitializer = com.romankozak.forwardappmobile.data.database.DatabaseInitializer(db.projectDao(), systemAppRepository)
                    databaseInitializer.prePopulate()
                    migrateSpecialProjects(dbSupport)
                    runCatching {
                        val projects = db.projectDao().getAll()
                        val missingSystemKey = projects.count { it.systemKey == null }
                        val missingReserved = projects.count { it.reservedGroup == null }
                        Log.d(
                            "DB_INIT",
                            "After prePopulate/migrate: total=${projects.size}, missingSystemKey=$missingSystemKey, missingReserved=$missingReserved, dbVersion=${db.openHelper.readableDatabase.version}"
                        )
                    }.onFailure {
                        Log.w("DB_INIT", "Failed to log systemKey state", it)
                    }
                }
            }
        }

        db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "forward_app_database"
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
        ).addCallback(callback).build()
        return db
    }

    @Provides
    @Singleton
    fun provideProjectDao(appDatabase: AppDatabase) = appDatabase.projectDao()

    @Provides
    @Singleton
    fun provideGoalDao(appDatabase: AppDatabase) = appDatabase.goalDao()

    @Provides
    @Singleton
    fun provideListItemDao(appDatabase: AppDatabase) = appDatabase.listItemDao()

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
    fun provideProjectManagementDao(appDatabase: AppDatabase) = appDatabase.projectManagementDao()

    @Provides
    @Singleton
    fun provideLinkItemDao(appDatabase: AppDatabase) = appDatabase.linkItemDao()

    @Provides
    @Singleton
    fun provideInboxRecordDao(appDatabase: AppDatabase) = appDatabase.inboxRecordDao()

    @Provides
    @Singleton
    fun provideNoteDocumentDao(appDatabase: AppDatabase): NoteDocumentDao = appDatabase.noteDocumentDao()

    @Provides
    @Singleton
    fun provideChecklistDao(appDatabase: AppDatabase): ChecklistDao = appDatabase.checklistDao()

    @Provides
    @Singleton
    fun provideProjectArtifactDao(appDatabase: AppDatabase) = appDatabase.projectArtifactDao()

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
    fun provideBacklogOrderDao(appDatabase: AppDatabase) = appDatabase.backlogOrderDao()

    @Provides
    @Singleton
    fun provideTacticalMissionDao(appDatabase: AppDatabase): TacticalMissionDao = appDatabase.tacticalMissionDao()

    @Provides
    @Singleton
    fun provideStructurePresetDao(appDatabase: AppDatabase) = appDatabase.structurePresetDao()

    @Provides
    @Singleton
    fun provideStructurePresetItemDao(appDatabase: AppDatabase) = appDatabase.structurePresetItemDao()

    @Provides
    @Singleton
    fun provideProjectStructureDao(appDatabase: AppDatabase) = appDatabase.projectStructureDao()
    }
