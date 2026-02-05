package com.romankozak.forwardappmobile.core.di

import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.capability.CapabilitySet
import com.romankozak.forwardappmobile.core.capability.InMemoryCapabilityRegistry
import com.romankozak.forwardappmobile.core.context.ContextController
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.ContextRole
import com.romankozak.forwardappmobile.core.context.ContextState
import com.romankozak.forwardappmobile.core.context.DefaultContextController
import com.romankozak.forwardappmobile.core.context.ViewId
import com.romankozak.forwardappmobile.core.context.ViewSet
import com.romankozak.forwardappmobile.core.data.models.entities.ContextConfiguration
import com.romankozak.forwardappmobile.core.gate.CapabilityGate
import com.romankozak.forwardappmobile.core.navigation.capability.ContextAwareViewResolver
import com.romankozak.forwardappmobile.core.navigation.capability.ViewRegistry
import com.romankozak.forwardappmobile.core.navigation.capability.ViewResolver
import com.romankozak.forwardappmobile.data.logic.GoalScoringManager
import com.romankozak.forwardappmobile.domain.lifecontext.DefaultLifeContextProcessor
import com.romankozak.forwardappmobile.domain.lifecontext.LifeContextProcessor
import com.romankozak.forwardappmobile.domain.lifecontext.LifeContextRule
import com.romankozak.forwardappmobile.features.context_lab.ContextLabController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LogicModule {
    @Provides @Singleton
    fun provideGoalScoringManager(): GoalScoringManager = GoalScoringManager

    @Provides @Singleton
    fun provideLifeContextRules(): List<LifeContextRule> = emptyList()

    @Provides
    @Singleton
    fun provideLifeContextProcessor(rules: @JvmSuppressWildcards List<LifeContextRule>): LifeContextProcessor =
        DefaultLifeContextProcessor(rules)

    @Provides
    @Singleton
    fun provideCapabilityRegistry(availableCapabilities: @JvmSuppressWildcards Set<CapabilityDescriptor>): CapabilityRegistry =
        InMemoryCapabilityRegistry(availableCapabilities)

    @Provides
    @Singleton
    fun provideContextController(): ContextController {
        val initial =
            object : ContextState {
                override val id = ContextId("default")
                override val features = CapabilitySet(emptySet())
                override val views = ViewSet(emptySet(), ViewId("main"))

                // Додаємо дефолтну конфігурацію для задоволення інтерфейсу ConfigurableState
                override val config =
                    ContextConfiguration(
                        id = "initial_default",
                        contextId = "default",
                    )
            }
        return DefaultContextController(initial)
    }

    @Provides
    @Singleton
    fun provideContextLabController(
        roles: Map<String, ContextRole>,
        viewRegistry: ViewRegistry,
    ): ContextLabController = ContextLabController(roles, viewRegistry)

    @Provides
    @Singleton
    fun provideViewResolver(
        viewRegistry: ViewRegistry,
        capabilityGate: CapabilityGate,
    ): ViewResolver = ContextAwareViewResolver(viewRegistry, capabilityGate)

    @Provides
    @Singleton
    fun provideExperimentalRoles(): Map<String, ContextRole> =
        mapOf(
            "vet_case" to
                ContextRole(
                    code = "vet_case",
                    label = "Ветеринарний кейс",
                    defaultCapabilities =
                        setOf(CapabilityId("notes"), CapabilityId("treatment_plan")),
                    availableViews = setOf(ViewId("summary"), ViewId("history")),
                    startView = ViewId("summary"),
                ),
            "dev_task" to
                ContextRole(
                    code = "dev_task",
                    label = "Розробка",
                    defaultCapabilities = setOf(CapabilityId("notes"), CapabilityId("code_index")),
                    availableViews = setOf(ViewId("kanban")),
                    startView = ViewId("kanban"),
                ),
        )

    @Provides
    @Singleton
    fun provideCapabilityGate(
        registry: CapabilityRegistry,
        contextController: ContextController,
    ): CapabilityGate = CapabilityGate(registry, contextController)
}
