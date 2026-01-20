package com.romankozak.forwardappmobile.core.di.capability

import com.romankozak.forwardappmobile.core.capability.Capability
import com.romankozak.forwardappmobile.core.capability.CapabilityRegistry
import com.romankozak.forwardappmobile.core.capability.InMemoryCapabilityRegistry
import com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.notes.NotesCapability
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// core/di/CapabilityModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class CapabilityModule {

    @Binds
    @Singleton
    abstract fun bindRegistry(
        impl: InMemoryCapabilityRegistry
    ): CapabilityRegistry
}