package com.romankozak.forwardappmobile.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.romankozak.forwardappmobile.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_55_56 = object : Migration(55, 56) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE projects ADD COLUMN relatedLinks TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "forward_app_database"
        ).addMigrations(MIGRATION_55_56).build()
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
    fun provideNoteDao(appDatabase: AppDatabase) = appDatabase.noteDao()

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
    fun provideCustomListDao(appDatabase: AppDatabase) = appDatabase.customListDao()

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
}