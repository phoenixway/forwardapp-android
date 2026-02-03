package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import com.romankozak.forwardappmobile.core.capability.Capability
import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.features.contexts.data.models.capabilities.notes.NotesCapability
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object NotesCapabilityModule {

    @Provides
    @IntoSet
    fun provideNotesCapability(): Capability {
        return NotesCapability 
    }

    @Provides
    @IntoSet
    fun provideNotesCapabilityDescriptor(): CapabilityDescriptor {
        return NotesCapability.descriptor 
    }
}
