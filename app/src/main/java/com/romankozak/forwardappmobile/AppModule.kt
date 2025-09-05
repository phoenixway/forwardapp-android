// --- File: app/src/main/java/com/romankozak/forwardappmobile/AppModule.kt ---
package com.romankozak.forwardappmobile

import android.content.Context
import androidx.room.Room
import com.romankozak.forwardappmobile.data.dao.*
import com.romankozak.forwardappmobile.data.database.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        // INFO: Видалено старі міграції, яких немає у наданих файлах, для уникнення помилок
        // Якщо вони вам потрібні, переконайтесь, що файли з ними є.
        // Я додав усі міграції до 19->20, які ми обговорювали.
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "forward_app_database"
        )
            .addMigrations(
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
                MIGRATION_19_20
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideGoalDao(database: AppDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    @Singleton
    fun provideGoalListDao(database: AppDatabase): GoalListDao {
        return database.goalListDao()
    }

    @Provides
    @Singleton
    fun provideListItemDao(database: AppDatabase): ListItemDao {
        return database.listItemDao()
    }

    @Provides
    @Singleton
    fun provideActivityRecordDao(database: AppDatabase): ActivityRecordDao {
        return database.activityRecordDao()
    }

    @Provides
    @Singleton
    fun provideRecentListDao(database: AppDatabase): RecentListDao {
        return database.recentListDao()
    }

    @Provides
    @Singleton
    fun provideLinkItemDao(database: AppDatabase): LinkItemDao {
        return database.linkItemDao()
    }

    // --- ПОЧАТОК ЗМІНИ ---

    @Provides
    @Singleton // AlarmScheduler має бути синглтоном
    fun provideAlarmScheduler(@ApplicationContext context: Context): com.romankozak.forwardappmobile.reminders.AlarmScheduler {
        return com.romankozak.forwardappmobile.reminders.AlarmScheduler(context)
    }

    @Provides
    @Singleton // Також додамо @Singleton для консистентності
    fun provideInboxRecordDao(appDatabase: AppDatabase): InboxRecordDao {
        return appDatabase.inboxRecordDao()
    }

    // --- КІНЕЦЬ ЗМІНИ ---
}