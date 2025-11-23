package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.data.repository.ActivityRecordRepository
import com.romankozak.forwardappmobile.data.repository.SystemAppRepository
import com.romankozak.forwardappmobile.domain.aichat.OllamaService
import com.romankozak.forwardappmobile.data.repository.SettingsRepository
import com.romankozak.forwardappmobile.domain.lifestate.AiAnalyzerService
import com.romankozak.forwardappmobile.domain.lifestate.LlmApi
import com.romankozak.forwardappmobile.domain.lifestate.OllamaLlmApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideLlmApi(
        ollamaService: OllamaService,
        settingsRepository: SettingsRepository,
    ): LlmApi = OllamaLlmApi(ollamaService, settingsRepository)

    @Provides
    @Singleton
    fun provideAiAnalyzerService(
        activityRecordRepository: ActivityRecordRepository,
        systemAppRepository: SystemAppRepository,
        llmApi: LlmApi,
    ): AiAnalyzerService = AiAnalyzerService(activityRecordRepository, systemAppRepository, llmApi)
}
