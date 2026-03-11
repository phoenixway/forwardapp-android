package com.romankozak.forwardappmobile.features.contexts.di.capabilities

import com.romankozak.forwardappmobile.core.capability.Capability
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
object InboxSortingCapabilityModule {
    @Provides
    @IntoSet
    fun provideInboxSortingCapability(): Capability = InboxSortingCapability

    @Provides
    @IntoSet
    fun provideInboxSortingCapabilityDescriptor(): CapabilityDescriptor = InboxSortingCapability.descriptor
}

object InboxSortingCapability : Capability {
    override val descriptor =
        object : CapabilityDescriptor {
            override val id = CapabilityId("inbox_sorting")
            override val label = "Inbox sorting"
            override val iconRes: Int? = null
            override val navRoute = "inbox_sorting_settings"
            override val supportedViews: Set<ViewId> = emptySet()
        }

    override fun register(runtime: com.romankozak.forwardappmobile.core.capability.CapabilityRuntime) {
        // Capability only contributes settings UI and actions.
    }
}
