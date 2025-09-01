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
                MIGRATION_15_16 // ✨ ВИПРАВЛЕНО: Додано відсутню міграцію
            )
            .build()
    }

    @Provides
    @Singleton // ✨ ДОДАНО: Найкраща практика - робити DAO синглтонами
    fun provideGoalDao(database: AppDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    @Singleton // ✨ ДОДАНО
    fun provideGoalListDao(database: AppDatabase): GoalListDao {
        return database.goalListDao()
    }

    @Provides
    @Singleton // ✨ ДОДАНО
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    @Singleton // ✨ ДОДАНО
    fun provideListItemDao(database: AppDatabase): ListItemDao {
        return database.listItemDao()
    }

    @Provides
    @Singleton // ✨ ДОДАНО
    fun provideActivityRecordDao(database: AppDatabase): ActivityRecordDao {
        return database.activityRecordDao()
    }

    @Provides
    @Singleton // ✨ ДОДАНО
    fun provideRecentListDao(database: AppDatabase): RecentListDao {
        return database.recentListDao()
    }

    // ✨ ДОДАНО: Провайдер для нового LinkItemDao, без якого була помилка Hilt
    @Provides
    @Singleton
    fun provideLinkItemDao(database: AppDatabase): LinkItemDao {
        return database.linkItemDao()
    }

    @Provides
    fun provideInboxRecordDao(appDatabase: AppDatabase): InboxRecordDao {
        return appDatabase.inboxRecordDao()
    }

}