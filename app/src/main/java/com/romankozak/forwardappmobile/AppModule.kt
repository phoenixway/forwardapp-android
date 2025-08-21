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
                MIGRATION_14_15 // ✨ ДОДАНО: Міграція на версію 15
            )
            .build()
    }

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao {
        return database.goalDao()
    }

    @Provides
    fun provideGoalListDao(database: AppDatabase): GoalListDao {
        return database.goalListDao()
    }

    // ✨ ДОДАНО: Провайдер для NoteDao
    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    // ✨ ДОДАНО: Провайдер для ListItemDao
    @Provides
    fun provideListItemDao(database: AppDatabase): ListItemDao {
        return database.listItemDao()
    }

    @Provides
    fun provideActivityRecordDao(database: AppDatabase): ActivityRecordDao {
        return database.activityRecordDao()
    }

    @Provides
    fun provideRecentListDao(database: AppDatabase): RecentListDao {
        return database.recentListDao()
    }
}