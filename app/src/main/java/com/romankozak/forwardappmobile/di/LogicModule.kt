package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.domain.lifecontext.DefaultLifeContextProcessor
import com.romankozak.forwardappmobile.domain.lifecontext.LifeContextProcessor
import com.romankozak.forwardappmobile.domain.lifecontext.LifeContextRule
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

    @Provides
    @Singleton
    fun provideLifeContextRules(): List<LifeContextRule> = emptyList()

    @Provides
    @Singleton
    fun provideLifeContextProcessor(
        rules: @JvmSuppressWildcards List<LifeContextRule>
    ): LifeContextProcessor = DefaultLifeContextProcessor(rules)
}
