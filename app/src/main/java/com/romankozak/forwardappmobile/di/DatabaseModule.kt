package com.romankozak.forwardappmobile.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.core.database.AppDatabase
import com.romankozak.forwardappmobile.data.dao.GoalDao

import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.ProjectManagementDao
import com.romankozak.forwardappmobile.data.dao.InboxRecordDao

import com.romankozak.forwardappmobile.data.dao.DayTaskDao
import com.romankozak.forwardappmobile.data.dao.DailyMetricDao
import com.romankozak.forwardappmobile.data.dao.RecurringTaskDao
import com.romankozak.forwardappmobile.data.dao.ChatDao
import com.romankozak.forwardappmobile.data.dao.ConversationFolderDao
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.romankozak.forwardappmobile.shared.database.AttachmentQueries
import com.romankozak.forwardappmobile.shared.database.LinkItemQueries
import com.romankozak.forwardappmobile.shared.database.ListItemQueries
import com.romankozak.forwardappmobile.shared.database.ProjectExecutionLogQueries
import com.romankozak.forwardappmobile.shared.database.ProjectQueries
import com.romankozak.forwardappmobile.shared.database.ReminderQueries
import com.romankozak.forwardappmobile.shared.database.RecentItemQueries
import com.romankozak.forwardappmobile.shared.database.ForwardAppDatabase
import com.romankozak.forwardappmobile.shared.database.LegacyNoteQueries
import com.romankozak.forwardappmobile.shared.database.NoteDocumentQueries
import com.romankozak.forwardappmobile.shared.database.ChecklistQueries
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.room.RoomDatabase
import com.romankozak.forwardappmobile.core.database.DatabaseInitializer
import com.romankozak.forwardappmobile.core.database.MIGRATION_8_9
import com.romankozak.forwardappmobile.core.database.MIGRATION_10_11
import com.romankozak.forwardappmobile.core.database.MIGRATION_11_12
import com.romankozak.forwardappmobile.core.database.MIGRATION_12_13
import com.romankozak.forwardappmobile.core.database.MIGRATION_13_14
import com.romankozak.forwardappmobile.core.database.MIGRATION_14_15
import com.romankozak.forwardappmobile.core.database.MIGRATION_15_16
import com.romankozak.forwardappmobile.core.database.MIGRATION_16_17
import com.romankozak.forwardappmobile.core.database.MIGRATION_17_18
import com.romankozak.forwardappmobile.core.database.MIGRATION_18_19
import com.romankozak.forwardappmobile.core.database.MIGRATION_19_20
import com.romankozak.forwardappmobile.core.database.MIGRATION_20_21
import com.romankozak.forwardappmobile.core.database.MIGRATION_21_22
import com.romankozak.forwardappmobile.core.database.MIGRATION_22_23
import com.romankozak.forwardappmobile.core.database.MIGRATION_23_24
import com.romankozak.forwardappmobile.core.database.MIGRATION_24_25
import com.romankozak.forwardappmobile.core.database.MIGRATION_25_26
import com.romankozak.forwardappmobile.core.database.MIGRATION_26_27
import com.romankozak.forwardappmobile.core.database.MIGRATION_27_28
import com.romankozak.forwardappmobile.core.database.MIGRATION_28_29
import com.romankozak.forwardappmobile.core.database.MIGRATION_29_30
import com.romankozak.forwardappmobile.core.database.MIGRATION_30_31
import com.romankozak.forwardappmobile.core.database.MIGRATION_31_32
import com.romankozak.forwardappmobile.core.database.MIGRATION_32_33
import com.romankozak.forwardappmobile.core.database.MIGRATION_33_34
import com.romankozak.forwardappmobile.core.database.MIGRATION_34_35
import com.romankozak.forwardappmobile.core.database.MIGRATION_35_36
import com.romankozak.forwardappmobile.core.database.MIGRATION_36_37
import com.romankozak.forwardappmobile.core.database.MIGRATION_37_38
import com.romankozak.forwardappmobile.core.database.MIGRATION_38_39
import com.romankozak.forwardappmobile.core.database.MIGRATION_39_40
import com.romankozak.forwardappmobile.core.database.MIGRATION_40_41
import com.romankozak.forwardappmobile.core.database.MIGRATION_41_42
import com.romankozak.forwardappmobile.core.database.MIGRATION_42_43
import com.romankozak.forwardappmobile.core.database.MIGRATION_44_45
import com.romankozak.forwardappmobile.core.database.MIGRATION_45_46
import com.romankozak.forwardappmobile.core.database.MIGRATION_46_47
import com.romankozak.forwardappmobile.core.database.MIGRATION_47_48
import com.romankozak.forwardappmobile.core.database.MIGRATION_48_49
import com.romankozak.forwardappmobile.core.database.MIGRATION_49_50
import com.romankozak.forwardappmobile.core.database.MIGRATION_50_51
import com.romankozak.forwardappmobile.core.database.MIGRATION_51_52
import com.romankozak.forwardappmobile.core.database.MIGRATION_52_53
import com.romankozak.forwardappmobile.core.database.MIGRATION_53_54
import com.romankozak.forwardappmobile.core.database.MIGRATION_54_55
import com.romankozak.forwardappmobile.core.database.MIGRATION_55_56
import com.romankozak.forwardappmobile.core.database.MIGRATION_57_58
import com.romankozak.forwardappmobile.core.database.MIGRATION_58_59
import com.romankozak.forwardappmobile.core.database.MIGRATION_59_60
import com.romankozak.forwardappmobile.core.database.MIGRATION_60_61
import com.romankozak.forwardappmobile.core.database.MIGRATION_61_62
import com.romankozak.forwardappmobile.core.database.MIGRATION_62_63
import com.romankozak.forwardappmobile.core.database.MIGRATION_63_64
import com.romankozak.forwardappmobile.core.database.MIGRATION_64_65
import com.romankozak.forwardappmobile.core.database.MIGRATION_65_66
import com.romankozak.forwardappmobile.core.database.MIGRATION_66_67



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
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope.launch(Dispatchers.IO) {
                    val entryPoint =
                        EntryPointAccessors.fromApplication(
                            context,
                            DatabaseInitializerEntryPoint::class.java,
                        )
                    entryPoint.databaseInitializer().prePopulate()
                }
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "forward_app_database"
        ).addMigrations(
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
        ).addCallback(callback).build()
    }

    @Provides
    @Singleton
    fun provideForwardAppDatabase(
        appDatabase: AppDatabase
    ): ForwardAppDatabase {
        val driver = AndroidSqliteDriver(schema = ForwardAppDatabase.Schema, openHelper = appDatabase.openHelper)
        return ForwardAppDatabase(driver)
    }

    @Provides
    fun provideListItemQueries(db: ForwardAppDatabase): ListItemQueries = db.listItemQueries

    @Provides
    fun provideLinkItemQueries(db: ForwardAppDatabase): LinkItemQueries = db.linkItemQueries

    @Provides
    @Singleton
    fun provideAttachmentQueries(
        forwardAppDatabase: ForwardAppDatabase,
    ): AttachmentQueries = forwardAppDatabase.attachmentQueries

    @Provides
    @Singleton
    fun provideProjectQueries(
        forwardAppDatabase: ForwardAppDatabase,
    ): ProjectQueries = forwardAppDatabase.projectQueries

    @Provides
    fun provideReminderQueries(db: ForwardAppDatabase): ReminderQueries = db.reminderQueries

    @Provides
    fun provideProjectExecutionLogQueries(db: ForwardAppDatabase): ProjectExecutionLogQueries = db.projectExecutionLogQueries

    @Provides
    fun provideRecentItemQueries(
        forwardAppDatabase: ForwardAppDatabase,
    ): RecentItemQueries = forwardAppDatabase.recentItemQueries

    @Provides
    fun provideLegacyNoteQueries(
        forwardAppDatabase: ForwardAppDatabase,
    ): LegacyNoteQueries = forwardAppDatabase.legacyNoteQueries

    @Provides
    fun provideNoteDocumentQueries(
        forwardAppDatabase: ForwardAppDatabase,
    ): NoteDocumentQueries = forwardAppDatabase.noteDocumentQueries

    @Provides
    fun provideDayPlanQueries(db: ForwardAppDatabase): com.romankozak.forwardappmobile.shared.database.DayPlanQueries = db.dayPlanQueries

    @Provides
    fun provideChecklistQueries(
        forwardAppDatabase: ForwardAppDatabase,
    ): ChecklistQueries = forwardAppDatabase.checklistQueries




}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DatabaseInitializerEntryPoint {
    fun databaseInitializer(): DatabaseInitializer
}