package com.romankozak.forwardappmobile.di



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

object DatabaseModule {

    @Provides

    @Singleton

    fun provideAppDatabase(

        @ApplicationContext context: Context,

        ): AppDatabase =

        Room

            .databaseBuilder(

                context,

                AppDatabase::class.java,

                "forward_app_database",

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

                ).build()



    @Provides

    fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()



    @Provides

    fun provideGoalListDao(db: AppDatabase): GoalListDao = db.goalListDao()



    @Provides

    fun provideListItemDao(db: AppDatabase): ListItemDao = db.listItemDao()



    @Provides

    fun provideActivityRecordDao(db: AppDatabase): ActivityRecordDao = db.activityRecordDao()



    @Provides

    fun provideRecentListDao(db: AppDatabase): RecentListDao = db.recentListDao()



    @Provides

    fun provideLinkItemDao(db: AppDatabase): LinkItemDao = db.linkItemDao()



    @Provides

    fun provideInboxRecordDao(db: AppDatabase): InboxRecordDao = db.inboxRecordDao()



    @Provides

    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()



    @Provides

    fun provideProjectManagementDao(db: AppDatabase): ProjectManagementDao = db.projectManagementDao()

    @Provides

    fun provideDayPlanDao(db: AppDatabase): DayPlanDao = db.dayPlanDao()



    @Provides

    fun provideDayTaskDao(db: AppDatabase): DayTaskDao = db.dayTaskDao()



    @Provides

    fun provideDailyMetricDao(db: AppDatabase): DailyMetricDao = db.dailyMetricDao()



}