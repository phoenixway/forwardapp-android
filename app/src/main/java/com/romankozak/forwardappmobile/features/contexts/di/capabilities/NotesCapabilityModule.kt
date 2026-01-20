package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import com.romankozak.forwardappmobile.core.capability.CapabilityDescriptor
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ViewId
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
    fun provideNotesCapability(): CapabilityDescriptor {
        return object : CapabilityDescriptor {
            override val id = CapabilityId("notes")
            override val label = "Нотатки"

            // Чітко вказуємо, що іконки немає
            override val iconRes: Int? = null

            override val navRoute = "notes_root"
            override val supportedViews: Set<ViewId>
                get() = TODO("Not yet implemented")
        }
    }
}