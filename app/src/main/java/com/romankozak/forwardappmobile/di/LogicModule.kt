package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LogicModule {

    @Provides
    @Singleton
    fun provideGoalScoringManager(): GoalScoringManager = GoalScoringManager
}
