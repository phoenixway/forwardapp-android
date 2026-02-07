package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import com.romankozak.forwardappmobile.core.capability.Capability
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.direction.DirectionCapability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object DirectionCapabilityModule {
    @Provides
    @IntoSet
    fun provideDirectionCapability(): Capability {
        return DirectionCapability
    }

    @Provides
    @IntoSet
    fun provideDirectionCapabilityDescriptor(): CapabilityDescriptor {
        return DirectionCapability.descriptor
    }
}
