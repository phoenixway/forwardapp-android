package com.romankozak.forwardappmobile.di

import com.romankozak.forwardappmobile.data.dao.AiEventDao
import com.romankozak.forwardappmobile.data.dao.LifeSystemStateDao
import com.romankozak.forwardappmobile.data.repository.AiEventRepository
import com.romankozak.forwardappmobile.data.repository.AiEventRepositoryImpl
import com.romankozak.forwardappmobile.data.repository.LifeSystemStateRepository
import com.romankozak.forwardappmobile.data.repository.LifeSystemStateRepositoryImpl
import com.romankozak.forwardappmobile.domain.ai.actuators.AiActuator
import com.romankozak.forwardappmobile.domain.ai.actuators.RecommendationActuator
import com.romankozak.forwardappmobile.domain.ai.actuators.UiAdaptationActuator
import com.romankozak.forwardappmobile.domain.ai.actuators.WorkerSchedulerActuator
import com.romankozak.forwardappmobile.domain.ai.advisor.AiAdvisor
import com.romankozak.forwardappmobile.domain.ai.advisor.NoOpAiAdvisor
import com.romankozak.forwardappmobile.domain.ai.inference.DeterministicLifeStateInferencer
import com.romankozak.forwardappmobile.domain.ai.inference.LifeStateInferencer
import com.romankozak.forwardappmobile.domain.ai.policy.AiPolicy
import com.romankozak.forwardappmobile.domain.ai.policy.EntropyPolicy
import com.romankozak.forwardappmobile.domain.ai.policy.OverloadPolicy
import com.romankozak.forwardappmobile.domain.ai.policy.StuckPolicy
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule
import com.romankozak.forwardappmobile.domain.ai.serialization.InstantAsLongSerializer
import java.time.Instant
import com.romankozak.forwardappmobile.domain.lifestate.LlmApi
import com.romankozak.forwardappmobile.domain.lifestate.OllamaLlmApi

@Module
@InstallIn(SingletonComponent::class)
abstract class AiBindingModule {
    @Binds
    @Singleton
    abstract fun bindAiEventRepository(impl: AiEventRepositoryImpl): AiEventRepository

    @Binds
    @Singleton
    abstract fun bindLifeStateRepository(impl: LifeSystemStateRepositoryImpl): LifeSystemStateRepository

    @Binds
    abstract fun bindInferencer(impl: DeterministicLifeStateInferencer): LifeStateInferencer

    @Binds
    abstract fun bindAdvisor(impl: NoOpAiAdvisor): AiAdvisor

    @Binds
    @IntoSet
    abstract fun bindUiActuator(actuator: UiAdaptationActuator): AiActuator

    @Binds
    @IntoSet
    abstract fun bindRecommendationActuator(actuator: RecommendationActuator): AiActuator

    @Binds
    @IntoSet
    abstract fun bindWorkerActuator(actuator: WorkerSchedulerActuator): AiActuator

    @Binds
    @IntoSet
    abstract fun bindOverloadPolicy(policy: OverloadPolicy): AiPolicy

    @Binds
    @IntoSet
    abstract fun bindStuckPolicy(policy: StuckPolicy): AiPolicy

    @Binds
    @IntoSet
    abstract fun bindEntropyPolicy(policy: EntropyPolicy): AiPolicy

    @Binds
    @Singleton
    abstract fun bindLlmApi(impl: OllamaLlmApi): LlmApi
}

@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        serializersModule = SerializersModule {
            contextual(Instant::class, InstantAsLongSerializer)
        }
    }
}
