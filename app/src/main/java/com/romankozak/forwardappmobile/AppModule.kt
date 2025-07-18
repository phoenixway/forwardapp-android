package com.romankozak.forwardappmobile

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Рецепт №1: Як створити базу даних
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "forward_app_database"
        ).fallbackToDestructiveMigration().build()
    }

    // Рецепт №2: Як створити GoalDao
    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao {
        return database.goalDao()
    }

    // --- ДОДАНО: Рецепт №3: Як створити GoalListDao ---
    @Provides
    fun provideGoalListDao(database: AppDatabase): GoalListDao {
        return database.goalListDao()
    }
}