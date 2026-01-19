package com.romankozak.forwardappmobile.core.di.contexts

import com.romankozak.forwardappmobile.core.capability.Capability
import com.romankozak.forwardappmobile.core.capability.CapabilityCatalog
import com.romankozak.forwardappmobile.core.capability.InMemoryCapabilityCatalog
import com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.notes.NotesCapability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// core/di/CapabilityModule.kt
@Module
@InstallIn(SingletonComponent::class)
object CapabilityModule {

    @Provides
    @Singleton
    fun provideCapabilities(): Set<Capability> =
        setOf(
            NotesCapability,
        )

    @Provides
    @Singleton
    fun provideCapabilityCatalog(
        capabilities: Set<@JvmSuppressWildcards Capability>
    ): CapabilityCatalog =
        InMemoryCapabilityCatalog(capabilities)
}
