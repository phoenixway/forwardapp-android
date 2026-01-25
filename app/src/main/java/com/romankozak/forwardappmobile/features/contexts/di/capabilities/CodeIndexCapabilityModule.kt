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
object CodeIndexCapabilityModule {
    @Provides
    @IntoSet
    fun provideCodeIndexCapability(): CapabilityDescriptor {
        return object : CapabilityDescriptor {
            override val id = CapabilityId("code_index")
            override val label = "Індекс коду"
            override val iconRes: Int? = null
            override val navRoute = "code_index_root" // Placeholder
            override val supportedViews: Set<ViewId>
                get() = setOf(ViewId("kanban"))
        }
    }
}
