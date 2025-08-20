// File: AppModule.kt

package com.romankozak.forwardappmobile

import android.content.Context
import androidx.room.Room
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao
import com.romankozak.forwardappmobile.data.dao.RecentListDao
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.database.MIGRATION_10_11
import com.romankozak.forwardappmobile.data.database.MIGRATION_11_12
import com.romankozak.forwardappmobile.data.database.MIGRATION_12_13
import com.romankozak.forwardappmobile.data.database.MIGRATION_13_14
import com.romankozak.forwardappmobile.data.database.MIGRATION_8_9
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
            // Оновлено: Підключаємо всі необхідні міграції
            .addMigrations(
                MIGRATION_8_9,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14
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

    @Provides
    fun provideActivityRecordDao(database: AppDatabase): ActivityRecordDao {
        return database.activityRecordDao()
    }

    // ✨ ДОДАНО: Провайдер для нового RecentListDao, що виправляє помилку збірки
    @Provides
    fun provideRecentListDao(database: AppDatabase): RecentListDao {
        return database.recentListDao()
    }
}