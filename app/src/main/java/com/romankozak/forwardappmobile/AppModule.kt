package com.romankozak.forwardappmobile

import android.content.Context
import androidx.room.Room
import com.romankozak.forwardappmobile.data.dao.ActivityRecordDao
import com.romankozak.forwardappmobile.data.database.AppDatabase
import com.romankozak.forwardappmobile.data.database.MIGRATION_8_9
import com.romankozak.forwardappmobile.data.dao.GoalDao
import com.romankozak.forwardappmobile.data.dao.GoalListDao
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
            // ДОДАНО: Підключаємо міграцію
            .addMigrations(MIGRATION_8_9)
            // Важливо: видаліть fallbackToDestructiveMigration, щоб міграція спрацювала
            // .fallbackToDestructiveMigration()
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
    fun provideActivityRecordDao(database: AppDatabase): ActivityRecordDao { // ✨ ДОДАНО
        return database.activityRecordDao()
    }
}
